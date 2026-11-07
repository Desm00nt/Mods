"""Check every mixin target from the original refmap against Minecraft 1.20.1 (SRG -> Mojang via srg2moj.json,
member lists from the stub model mc_model.json).  usage: check_mixin_targets.py REFMAP SRG2MOJ MC_MODEL"""
import json, re, sys

refmap, s2m_path, model_path = sys.argv[1:4]
r = json.load(open(refmap))
s2m = json.load(open(s2m_path)); F, M = s2m['fields'], s2m['methods']
model = json.load(open(model_path))
mixin_target = {}  # accessor entries carry no owner: take it from the mixin's @Mixin target (filled manually below)
ACCESSOR_OWNERS = {
    'SpawnPlacementsAccess': 'net.minecraft.world.entity.SpawnPlacements',
    'WidgetSizeMixin': 'net.minecraft.client.gui.components.AbstractWidget',
    'FireBlockAccess': 'net.minecraft.world.level.block.FireBlock',
    'ContainerScreenAccess': 'net.minecraft.client.gui.screens.inventory.AbstractContainerScreen',
}

def lookup(cls, name):
    info = model.get(cls) or next((v for v in model.values() if v.get('binary') == cls), None)
    if info is None:
        return None, 'class unknown'
    if name.startswith('m_'):
        moj = M.get(name); return moj, any(m['name'] == moj for m in info['methods'])
    if name.startswith('f_'):
        moj = F.get(name); return moj, any(f['name'] == moj for f in info['fields'])
    if name == '<init>':
        return name, bool(info['ctors'])
    return name, any(m['name'] == name for m in info['methods']) or any(f['name'] == name for f in info['fields'])

total = bad = 0
for mixin, mp in r['mappings'].items():
    short = mixin.split('/')[-1]
    for key, tgt in mp.items():
        total += 1
        m = re.match(r'^L([\w/$]+);([\w$<>]+)', tgt)
        if m:
            owner, name = m.group(1).replace('/', '.'), m.group(2)
        else:
            owner, name = ACCESSOR_OWNERS.get(short), re.match(r'^([\w$<>]+)', tgt).group(1)
        moj, ok = lookup(owner, name) if owner else (None, False)
        if ok is not True:
            bad += 1
            print(f'NOT FOUND {short}.{key} -> {tgt} (mojang: {moj})')
print(f'checked {total} refmap targets, {bad} not found')
