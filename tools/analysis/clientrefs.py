import sys, zipfile, re
sys.path.insert(0, '/home/user/Mods/tools/decompile')
from classfile import ClassFile
z = zipfile.ZipFile('/tmp/work/spraute-mojmap.jar')
CLIENT = re.compile(r'^(net/minecraft/client/|com/mojang/blaze3d/|net/minecraftforge/client/|org/lwjgl/)')
res = {}
for n in z.namelist():
    if not n.endswith('.class') or not n.startswith('org/sprautestudio'): continue
    cf = ClassFile(z.read(n))
    refs = set()
    for i, e in enumerate(cf.cp.entries):
        if e and e[0] == 7:
            name = cf.utf(e[1]).lstrip('[').lstrip('L').rstrip(';')
            if CLIENT.match(name): refs.add(name)
        if e and e[0] == 1:
            s = cf.utf(i)
            for m in re.finditer(r'L(net/minecraft/client/[\w/$]+|com/mojang/blaze3d/[\w/$]+);', s):
                refs.add(m.group(1))
    if refs:
        res[n[:-6]] = refs
# classify
pk = lambda c: c.split('/')[3] if len(c.split('/'))>3 else ''
def is_client_pkg(c):
    parts = c.split('/')
    return 'client' in parts or parts[3] in ('editor','ui') or c.endswith('ClientModules') or 'Client' in parts[-1].split('$')[0]
common = {c: r for c, r in res.items() if not is_client_pkg(c)}
print('classes with client refs:', len(res), ' outside client pkgs:', len(common))
for c in sorted(common):
    print(c.replace('org/sprautestudio/spraute_engine/',''), '->', ', '.join(sorted(x.split('/')[-1] for x in common[c]))[:150])
