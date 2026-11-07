#!/usr/bin/env python3
"""Generate compile-only Java stubs:
 * vanilla Minecraft API from mappings.dev model (official names + generics)
 * members the mod uses but vanilla lacks (Forge patches) from bytecode usage
 * external libraries not available as jars from bytecode usage
Hand-written stubs in hand/ take precedence (by top-level class)."""
import collections, json, os, re, shutil, sys, zipfile

BASE = '/tmp/work/stubs'
MODEL = json.load(open(f'{BASE}/mc_model.json'))
USAGE = json.load(open(f'{BASE}/usage.json'))
OUT = f'{BASE}/src'
HAND = f'{BASE}/hand'
LIBS = '/tmp/work/libs'

PRIM = {'int': 'I', 'long': 'J', 'short': 'S', 'byte': 'B', 'char': 'C', 'boolean': 'Z',
        'float': 'F', 'double': 'D', 'void': 'V'}
DESC_PRIM = {v: k for k, v in PRIM.items()}
JDK_PREFIX = ('java/', 'javax/', 'jdk/', 'sun/', 'com/sun/', 'org/w3c/', 'org/xml/')
JAVA_KW = {'abstract', 'assert', 'boolean', 'break', 'byte', 'case', 'catch', 'char', 'class', 'const',
           'continue', 'default', 'do', 'double', 'else', 'enum', 'extends', 'final', 'finally', 'float',
           'for', 'goto', 'if', 'implements', 'import', 'instanceof', 'int', 'interface', 'long', 'native',
           'new', 'package', 'private', 'protected', 'public', 'return', 'short', 'static', 'strictfp',
           'super', 'switch', 'synchronized', 'this', 'throw', 'throws', 'transient', 'try', 'void',
           'volatile', 'while', 'record', 'var', 'yield', 'sealed', 'permits'}

def split_top(s, sep=','):
    out, depth, cur = [], 0, ''
    for ch in s:
        if ch == '<':
            depth += 1
        elif ch == '>':
            depth -= 1
        if ch == sep and depth == 0:
            out.append(cur.strip())
            cur = ''
        else:
            cur += ch
    if cur.strip():
        out.append(cur.strip())
    return out


# ---------------------------------------------------------------- provided jars
PROVIDED = set()
for j in os.listdir(LIBS):
    if j.endswith('.jar'):
        for n in zipfile.ZipFile(os.path.join(LIBS, j)).namelist():
            if n.endswith('.class'):
                PROVIDED.add(n[:-6])

# hand-written top-level classes
HAND_TOP = set()
for dp, _, fs in os.walk(HAND):
    for f in fs:
        if f.endswith('.java'):
            HAND_TOP.add(os.path.relpath(os.path.join(dp, f), HAND)[:-5].replace(os.sep, '/'))

MC = {k.replace('.', '/'): v for k, v in MODEL.items()}  # internal name -> class model
JDKM = json.load(open(f'{BASE}/jdk_members.json'))
MC_CANON = {k.replace('$', '/').replace('/', '.'): k for k in MC}  # canonical -> internal


def is_jdk(internal):
    return internal.startswith(JDK_PREFIX)


def canon_to_internal(base):
    if base in MC_CANON:
        return MC_CANON[base]
    parts = base.split('.')
    for i, p in enumerate(parts):
        if p[:1].isupper():
            return '/'.join(parts[:i]) + ('/' if i else '') + '$'.join(parts[i:])
    return base.replace('.', '/')


def internal_to_canon(internal):
    return internal.replace('/', '.').replace('$', '.')


OBF_RE = re.compile(r'(?<![\w.])[a-z]{1,3}(?:\.[a-z]{1,2}){1,3}(?![\w.])')
OBF_TAIL = re.compile(r'(?:>|[A-Z]\w*)((?:\.[a-z]{1,3})+)(?=[\s,>)\[<]|$)')


def _kill_obf_tails(t):
    while True:
        m = OBF_TAIL.search(t)
        if not m:
            return t
        end = m.end(1)
        # walk left to the start of this type expression
        i = m.start(1) - 1
        depth = 0
        while i >= 0:
            ch = t[i]
            if ch == '>':
                depth += 1
            elif ch == '<':
                if depth == 0:
                    break
                depth -= 1
            elif depth == 0 and not (ch.isalnum() or ch in '_.'):
                break
            i -= 1
        if end < len(t) and t[end] == '<':
            depth = 0
            for j in range(end, len(t)):
                if t[j] == '<':
                    depth += 1
                elif t[j] == '>':
                    depth -= 1
                    if depth == 0:
                        end = j + 1
                        break
        t = t[:i + 1] + 'java.lang.Object' + t[end:]


def fix_type(t):
    t = t.replace('$', '.')
    t = OBF_RE.sub('java.lang.Object', t)
    t = _kill_obf_tails(t)
    return t


def fix_tparams(tp):
    """Normalise '<T extends A extends B, U>' / '<T extends A, B>' into proper Java."""
    tp = fix_type(tp).strip()
    if not tp:
        return ''
    inner = tp[1:-1]
    out = []
    for part in split_top(inner):
        part = part.strip()
        m = re.match(r'^([A-Za-z_]\w*)(\s+extends\s+(.+))?$', part)
        if m and '.' not in m.group(1) and out is not None and (m.group(2) or True) and not ('.' in part and not m.group(2)):
            name, bounds = m.group(1), m.group(3)
            if bounds:
                bl = split_top(bounds.replace(' extends ', ' & '), '&')
                out.append(name + ' extends ' + ' & '.join(b.strip() for b in bl))
            else:
                out.append(name)
        else:
            # additional bound of the previous type parameter
            if out:
                if ' extends ' in out[-1]:
                    out[-1] += ' & ' + part
                else:
                    out[-1] += ' extends ' + part
            else:
                out.append(part)
    return '<' + ', '.join(out) + '>'


# ---------------------------------------------------------------- type helpers
def split_top_unused(s, sep=','):
    out, depth, cur = [], 0, ''
    for ch in s:
        if ch == '<':
            depth += 1
        elif ch == '>':
            depth -= 1
        if ch == sep and depth == 0:
            out.append(cur.strip())
            cur = ''
        else:
            cur += ch
    if cur.strip():
        out.append(cur.strip())
    return out


def parse_type(t):
    t = t.strip()
    dims = 0
    if t.endswith('...'):
        t = t[:-3].strip()
        dims += 1
    while t.endswith('[]'):
        t = t[:-2].strip()
        dims += 1
    i = t.find('<')
    if i >= 0:
        return t[:i].strip(), t[i:], dims
    return t, '', dims


def parse_tparams(tp):
    res = {}
    tp = tp.strip()
    if not tp:
        return res
    for part in split_top(tp[1:-1]):
        m = re.match(r'(\w+)(?:\s+extends\s+(.*))?$', part.strip())
        if m:
            b = m.group(2)
            if b:
                b = split_top(b, '&')[0]
            res[m.group(1)] = b
    return res


def erase(t, tvars, depth=0):
    base, args, dims = parse_type(t)
    if base in PRIM:
        d = PRIM[base]
    elif base in tvars and depth < 10:
        b = tvars[base]
        d = erase(b, tvars, depth + 1) if b else 'Ljava/lang/Object;'
    elif base.startswith('?') or (re.match(r'^[A-Z][A-Z0-9]?$', base) and base not in MC_CANON):
        d = 'Ljava/lang/Object;'
    else:
        d = 'L' + canon_to_internal(base) + ';'
    return '[' * dims + d


def desc_split(desc):
    """'(IL..;)V' -> ([params], ret)"""
    params, i = [], 1
    while desc[i] != ')':
        j = i
        while desc[j] == '[':
            j += 1
        if desc[j] == 'L':
            j = desc.index(';', j)
        params.append(desc[i:j + 1])
        i = j + 1
    return params, desc[i + 1:]


def desc_to_java(d):
    dims = 0
    while d.startswith('['):
        dims += 1
        d = d[1:]
    if d in DESC_PRIM:
        t = DESC_PRIM[d]
    else:
        t = internal_to_canon(d[1:-1])
    return t + '[]' * dims


def default_value(t):
    t = t.strip()
    if t.endswith(']') or t.endswith('...'):
        return 'null'
    return {'int': '0', 'long': '0L', 'short': '(short) 0', 'byte': '(byte) 0', 'char': "'\\0'",
            'boolean': 'false', 'float': '0f', 'double': '0d'}.get(t, 'null')


# ---------------------------------------------------------------- arities of external generics
ARITY = collections.Counter()


def note_arity(text):
    # find Name<...> occurrences
    for m in re.finditer(r'([A-Za-z_][\w.]*)<', text):
        name = m.group(1)
        # extract args
        i = m.end() - 1
        depth = 0
        for j in range(i, len(text)):
            if text[j] == '<':
                depth += 1
            elif text[j] == '>':
                depth -= 1
                if depth == 0:
                    n = len(split_top(text[i + 1:j]))
                    ARITY[canon_to_internal(name)] = max(ARITY[canon_to_internal(name)], n)
                    break


for c in MODEL.values():
    for t in [c['tparams']] + c['extends'] + c['implements']:
        note_arity(fix_type(t))
    for f in c['fields']:
        note_arity(fix_type(f['type']))
    for mt in c['methods']:
        note_arity(fix_type(mt['ret']))
        note_arity(fix_type(mt['tparams']))
        for p in mt['params']:
            note_arity(fix_type(p[0]))
    for ct in c['ctors']:
        for p in ct['params']:
            note_arity(fix_type(p[0]))

# arities from the mod's own generic signatures
SIG_ARITY = json.load(open(f'{BASE}/sig_arity.json')) if os.path.exists(f'{BASE}/sig_arity.json') else {}
for k, v in SIG_ARITY.items():
    ARITY[k] = max(ARITY[k], v)

# ---------------------------------------------------------------- external type registry
EXT = {}  # internal -> dict(kind, fields{}, methods{}, sam)


def ext_entry(internal):
    e = EXT.get(internal)
    if e is None:
        e = EXT[internal] = {'kind': set(), 'fields': {}, 'methods': {}, 'sam': None}
    return e


def scan_external(text, hint=None):
    for tok in re.findall(r'[A-Za-z_][\w]*(?:\.[A-Za-z_][\w]*)+', text):
        internal = canon_to_internal(tok)
        if internal in MC or is_jdk(internal) or internal in PROVIDED:
            continue
        e = ext_entry(internal)
        if hint:
            e['kind'].add(hint)


for c in MODEL.values():
    is_iface = c['kind'] in ('interface', '@interface')
    for t in c['extends']:
        scan_external(fix_type(t), 'interface' if is_iface else 'class')
    for t in c['implements']:
        scan_external(fix_type(t), 'interface')
    for t in [c['tparams']]:
        scan_external(fix_type(t))
    for f in c['fields']:
        scan_external(fix_type(f['type']))
    for mt in c['methods']:
        scan_external(fix_type(mt['ret']))
        scan_external(fix_type(mt['tparams']))
        for p in mt['params']:
            scan_external(fix_type(p[0]))
    for ct in c['ctors']:
        for p in ct['params']:
            scan_external(fix_type(p[0]))

for internal, u in USAGE.items():
    if internal in MC or internal in PROVIDED or is_jdk(internal):
        continue
    e = ext_entry(internal)
    for k in u['kind']:
        e['kind'].add('interface' if k == 'interface' else 'class')
    e['fields'].update(u['fields'])
    for k, v in u['methods'].items():
        e['methods'][k] = v
    if u['sam']:
        e['sam'] = u['sam']

# referenced types in usage descriptors must exist too
for internal, u in list(USAGE.items()):
    for key in list(u['fields']) + list(u['methods']):
        desc = key.split(':', 1)[1] if ':' in key else key[key.index('('):]
        for t in re.findall(r'L([^;]+);', desc):
            if t in MC or t in PROVIDED or is_jdk(t):
                continue
            ext_entry(t)


# ---------------------------------------------------------------- MC member index (erased)
def class_tvars(c):
    return parse_tparams(fix_tparams(c['tparams']))


MC_MEMBERS = {}
for internal, c in MC.items():
    tv = class_tvars(c)
    meths, flds = set(), set()
    for mt in c['methods']:
        tv2 = dict(tv)
        tv2.update(parse_tparams(fix_tparams(mt['tparams'])))
        d = '(' + ''.join(erase(fix_type(p[0]), tv2) for p in mt['params']) + ')' + erase(fix_type(mt['ret']), tv2)
        meths.add(mt['name'] + d)
    for ct in c['ctors']:
        d = '(' + ''.join(erase(fix_type(p[0]), tv) for p in ct['params']) + ')V'
        meths.add('<init>' + d)
    for f in c['fields']:
        flds.add(f['name'])
    for ec in c['enum_constants']:
        flds.add(ec)
    MC_MEMBERS[internal] = (meths, flds)


def supertypes(internal):
    c = MC.get(internal)
    if not c:
        return []
    res = []
    for t in c['extends'] + c['implements']:
        base, _, _ = parse_type(fix_type(t))
        res.append(canon_to_internal(base))
    return res


ENUM_IMPLICIT = ('values', 'valueOf', 'ordinal', 'name', 'compareTo', 'getDeclaringClass', 'describeConstable')


def mc_has(internal, key, is_field, seen=None):
    seen = seen or set()
    if internal in seen:
        return False
    seen.add(internal)
    if internal not in MC:
        j = JDKM.get(internal)
        if j is not None:
            if is_field:
                return any(f.split(':')[0] == key.split(':')[0] for f in j['fields'])
            return key in j['methods'] or any(k.split('(')[0] == key.split('(')[0] and k.split(')')[0] == key.split(')')[0] for k in j['methods'])
        return None  # unknown (external ancestor)
    if MC[internal]['kind'] == 'enum' and not is_field and key.split('(')[0] in ENUM_IMPLICIT:
        return True
    meths, flds = MC_MEMBERS[internal]
    if is_field:
        if key.split(':')[0] in flds:
            return True
    else:
        if key in meths:
            return True
        if key.startswith('<init>'):
            return False
    unknown = False
    for s in supertypes(internal):
        r = mc_has(s, key, is_field, seen)
        if r:
            return True
        if r is None:
            unknown = True
    if not is_field and not key.startswith('<init>'):
        if key.split('(')[0] in ('equals', 'hashCode', 'toString', 'getClass', 'clone', 'finalize'):
            return True
    return None if unknown else False


# visibility of used members: record used names to force-public
USED_NAMES = collections.defaultdict(set)
EXTRA = collections.defaultdict(lambda: {'fields': {}, 'methods': {}})
for internal, u in USAGE.items():
    if internal not in MC:
        continue
    for key, info in u['fields'].items():
        USED_NAMES[internal].add(key.split(':')[0])
        r = mc_has(internal, key, True)
        if r is False:
            EXTRA[internal]['fields'][key] = info
    for key, info in u['methods'].items():
        USED_NAMES[internal].add(key.split('(')[0])
        r = mc_has(internal, key, False)
        if r is False:
            EXTRA[internal]['methods'][key] = info

# ---------------------------------------------------------------- emit
BODY = '{ throw new RuntimeException(); }'
THROWS = {  # (internal class, method name) -> throws clause
    ('net/minecraft/nbt/TagParser', 'parseTag'): 'com.mojang.brigadier.exceptions.CommandSyntaxException',
    ('net/minecraft/nbt/TagParser', 'readStruct'): 'com.mojang.brigadier.exceptions.CommandSyntaxException',
    ('net/minecraft/nbt/TagParser', 'readValue'): 'com.mojang.brigadier.exceptions.CommandSyntaxException',
    ('net/minecraft/client/sounds/AudioStream', 'read'): 'java.io.IOException',
    ('net/minecraft/client/sounds/AudioStream', 'close'): 'java.io.IOException',
    ('net/minecraft/server/packs/resources/Resource', 'open'): 'java.io.IOException',
    ('net/minecraft/server/packs/resources/Resource', 'openAsReader'): 'java.io.IOException',
    ('net/minecraft/server/packs/resources/Resource', 'metadata'): 'java.io.IOException',
    ('net/minecraft/server/packs/resources/ResourceProvider', 'open'): 'java.io.IOException',
    ('net/minecraft/server/packs/resources/ResourceProvider', 'openAsReader'): 'java.io.IOException',
    ('net/minecraft/server/packs/resources/ResourceProvider', 'getResourceOrThrow'): 'java.io.FileNotFoundException',
    ('com/mojang/blaze3d/platform/NativeImage', 'read'): 'java.io.IOException',
    ('com/mojang/blaze3d/platform/NativeImage', 'writeToFile'): 'java.io.IOException',
    ('com/mojang/blaze3d/platform/NativeImage', 'asByteArray'): 'java.io.IOException',
    ('net/minecraft/commands/arguments/blocks/BlockStateParser', 'parseForBlock'): 'com.mojang.brigadier.exceptions.CommandSyntaxException',
    ('net/minecraft/commands/arguments/blocks/BlockStateParser', 'parseForTesting'): 'com.mojang.brigadier.exceptions.CommandSyntaxException',
    ('net/minecraft/commands/arguments/item/ItemParser', 'parseForItem'): 'com.mojang.brigadier.exceptions.CommandSyntaxException',
    ('net/minecraft/server/packs/PackResources', 'getMetadataSection'): 'java.io.IOException',
    ('net/minecraft/server/packs/AbstractPackResources', 'getMetadataSection'): 'java.io.IOException',
    ('net/minecraft/client/renderer/texture/AbstractTexture', 'load'): 'java.io.IOException',
    ('net/minecraft/client/renderer/texture/DynamicTexture', 'load'): 'java.io.IOException',
    ('net/minecraft/client/renderer/texture/SimpleTexture', 'load'): 'java.io.IOException',
    ('net/minecraft/client/renderer/ShaderInstance', '<init>'): 'java.io.IOException',
    ('net/minecraft/client/renderer/PostChain', '<init>'): 'java.io.IOException',
    ('net/minecraft/client/renderer/EffectInstance', '<init>'): 'java.io.IOException',
}
_NBTIO_CHECKED = ('read', 'readCompressed', 'write', 'writeCompressed', 'safeWrite', 'parseCompressed', 'writeUnnamedTag', 'readUnnamedTag')


def throws_for(internal, name):
    t = THROWS.get((internal, name))
    if t is None and internal == 'net/minecraft/nbt/NbtIo' and name in _NBTIO_CHECKED:
        t = 'java.io.IOException'
    if t is None and internal.startswith('net/minecraft/commands/arguments/') and name.startswith('get') and internal.endswith('Argument'):
        t = 'com.mojang.brigadier.exceptions.CommandSyntaxException'
    if t is None and internal.startswith('net/minecraft/commands/arguments/') and name == 'parse':
        t = 'com.mojang.brigadier.exceptions.CommandSyntaxException'
    if t is None and internal == 'net/minecraft/commands/CommandSourceStack' and name in ('getPlayerOrException', 'getEntityOrException'):
        t = 'com.mojang.brigadier.exceptions.CommandSyntaxException'
    return (' throws ' + t) if t else ''
SUPER_CALLS = json.load(open(f'{BASE}/super_calls.json')) if os.path.exists(f'{BASE}/super_calls.json') else {}
JDK_SUPER_CALL = {'java.io.FilterInputStream': 'super(null);', 'java.io.FilterOutputStream': 'super(null);',
                  'java.io.PrintStream': 'super((java.io.OutputStream) null);', 'java.lang.ref.WeakReference': 'super(null);',
                  'java.lang.ref.SoftReference': 'super(null);', 'java.io.FilterReader': 'super(null);',
                  'java.io.FilterWriter': 'super(null);', 'java.lang.Thread': '', 'java.io.BufferedReader': 'super(null);'}
for _k, _c in MC.items():
    if _c['kind'] == 'class' and _c['extends']:
        _b = parse_type(fix_type(_c['extends'][0]))[0]
        _j = JDKM.get(canon_to_internal(_b))
        if _j is not None and _j['ctors'] and '<init>()V' not in _j['ctors']:
            _ps, _ = desc_split(sorted(_j['ctors'], key=len)[0][6:])
            SUPER_CALLS.setdefault(_k, 'super(' + ', '.join('(' + desc_to_java(x) + ') ' + default_value(desc_to_java(x)) for x in _ps) + ');')


def pname(n, i):
    if not re.match(r'^[A-Za-z_]\w*$', n) or n in JAVA_KW:
        return f'arg{i}'
    return n


def params_text(params):
    seen = set()
    out = []
    for i, (t, n) in enumerate(params):
        n = pname(n, i)
        if n in seen:
            n = f'arg{i}'
        seen.add(n)
        out.append(f'{fix_type(t)} {n}')
    return ', '.join(out)


def vis(mods, force_public=False):
    for v in ('public', 'protected', 'private'):
        if v in mods:
            if force_public and v == 'private':
                return 'public'
            return v
    return 'public' if force_public else ''


def references_outer_tvars(c, outer_tv):
    if not outer_tv:
        return False
    own = set(parse_tparams(fix_tparams(c['tparams'])))
    names = [n for n in outer_tv if n not in own]
    if not names:
        return False
    texts = list(c['extends']) + list(c['implements'])
    texts += [f['type'] for f in c['fields'] if 'static' not in f['mods']]
    for mt in c['methods']:
        if 'static' in mt['mods']:
            continue
        mtv = set(parse_tparams(fix_tparams(mt['tparams'])))
        for t in [mt['ret']] + [p[0] for p in mt['params']]:
            if any(re.search(r'(?<![\w.])' + n + r'(?![\w])', t) and n not in mtv for n in names):
                return True
    for ct in c['ctors']:
        texts += [p[0] for p in ct['params']]
    return any(re.search(r'(?<![\w.])' + n + r'(?![\w])', t) for t in texts for n in names)


OUTER_TV = {}


def emit_mc_class(internal, indent, nested_parent_kind=None):
    c = MC[internal]
    kind = c['kind']
    simple = internal.split('/')[-1].split('$')[-1]
    used = USED_NAMES.get(internal, set())
    in_iface = nested_parent_kind in ('interface', '@interface')
    v = 'public'
    mods = [v] if v else []
    outer = internal.rsplit('$', 1)[0] if '$' in internal else None
    outer_tv = OUTER_TV.get(outer, {}) if outer else {}
    inner_nonstatic = (nested_parent_kind in ('class', 'enum', 'record') and kind == 'class'
                       and references_outer_tvars(c, outer_tv))
    if nested_parent_kind and not inner_nonstatic:
        mods.append('static')
    my_tv = dict(outer_tv) if inner_nonstatic else {}
    my_tv.update(class_tvars(c))
    OUTER_TV[internal] = my_tv
    if 'abstract' in c['mods'] and kind == 'class':
        mods.append('abstract')
    jkind = {'record': 'class', '@interface': '@interface'}.get(kind, kind)
    if kind == 'record':
        mods.append('final')
    head = ' '.join(mods + [jkind, simple]) + fix_tparams(c['tparams'])
    if kind == 'class' and c['extends']:
        head += ' extends ' + fix_type(c['extends'][0])
    if kind == 'interface' and c['extends']:
        head += ' extends ' + ', '.join(fix_type(t) for t in c['extends'])
    if kind in ('class', 'enum', 'record') and c['implements']:
        head += ' implements ' + ', '.join(fix_type(t) for t in c['implements'])
    pad = '    ' * indent
    lines = [pad + head + ' {']
    p2 = pad + '    '
    is_iface = kind in ('interface', '@interface')
    if kind == 'enum':
        lines.append(p2 + (', '.join(c['enum_constants']) or '') + ';')
    consts = set(c['enum_constants'])
    for f in c['fields']:
        if f['name'] in consts or '$' in f['name']:
            continue
        fv = vis(f['mods'], f['name'] in used)
        if is_iface:
            fv = 'public'
        if fv in ('private', ''):
            continue
        fm = [fv] + (['static'] if 'static' in f['mods'] else [])
        t = fix_type(f['type'])
        if is_iface:
            lines.append(f"{p2}{' '.join(fm)} {t} {f['name']} = {default_value(t)};")
        else:
            lines.append(f"{p2}{' '.join(fm)} {t} {f['name']};")
    extra = EXTRA.get(internal, {'fields': {}, 'methods': {}})
    for key, info in extra['fields'].items():
        name, d = key.split(':', 1)
        t = desc_to_java(d)
        st = 'static ' if info['static'] else ''
        if is_iface:
            lines.append(f'{p2}public {t} {name} = {default_value(t)};')
        else:
            lines.append(f'{p2}public {st}{t} {name};')
    # constructors
    if kind in ('class', 'record'):
        have_noarg = False
        emitted = set()
        for ct in c['ctors']:
            cv = vis(ct['mods'])
            tv = class_tvars(c)
            d = '(' + ''.join(erase(fix_type(p[0]), tv) for p in ct['params']) + ')V'
            if cv in ('private', '') and ('<init>' + d) not in USAGE.get(internal, {}).get('methods', {}):
                continue
            if cv in ('private', ''):
                cv = 'public'
            if d in emitted:
                continue
            emitted.add(d)
            if not ct['params']:
                have_noarg = True
            sc = SUPER_CALLS.get(internal, '')
            ctv = []
            for ptxt in [p[0] for p in ct['params']]:
                for tvn in re.findall(r'(?<![\w.])([A-Z][A-Z0-9]?)(?![\w.])', fix_type(ptxt)):
                    if tvn not in OUTER_TV.get(internal, {}) and tvn not in ctv and tvn not in MC_CANON:
                        ctv.append(tvn)
            ctp = ('<' + ', '.join(ctv) + '> ') if ctv else ''
            lines.append(f"{p2}{cv} {ctp}{simple}({params_text(ct['params'])}){throws_for(internal, '<init>')} {{ {sc} }}")
        for key, info in extra['methods'].items():
            if key.startswith('<init>'):
                ps, _ = desc_split(key[6:])
                d = key[6:]
                if d in emitted:
                    continue
                emitted.add(d)
                if not ps:
                    have_noarg = True
                sc = SUPER_CALLS.get(internal, '')
                lines.append(f"{p2}public {simple}({', '.join(f'{desc_to_java(p)} a{i}' for i, p in enumerate(ps))}) {{ {sc} }}")
        if not have_noarg:
            sc = SUPER_CALLS.get(internal, '')
            lines.append(f'{p2}protected {simple}() {{ {sc} }}')
    # methods
    seen_sigs = set()
    tv = class_tvars(c)
    for mt in c['methods']:
        mv = vis(mt['mods'], mt['name'] in used)
        if mv == 'private' and mt['name'] not in used:
            continue
        if mv == '' and not is_iface and mt['name'] not in used:
            continue
        if mv in ('private', '') and mt['name'] in used:
            mv = 'public'
        tv2 = dict(tv)
        tv2.update(parse_tparams(fix_tparams(mt['tparams'])))
        d = '(' + ''.join(erase(fix_type(p[0]), tv2) for p in mt['params']) + ')'
        if mt['name'] + d in seen_sigs:
            continue
        seen_sigs.add(mt['name'] + d)
        tp = fix_tparams(mt['tparams'])
        tp = (tp + ' ') if tp else ''
        ret = fix_type(mt['ret'])
        ps = params_text(mt['params'])
        st = 'static ' in (' '.join(mt['mods']) + ' ')
        if kind == 'enum' and mt['name'] in ('values', 'valueOf', 'name', 'ordinal', 'compareTo', 'getDeclaringClass',
                                             'equals', 'hashCode', 'clone', 'finalize', 'describeConstable'):
            continue
        if not is_iface and mt['params'] and all(p[0] == 'java.lang.Object' for p in mt['params']) and any(
                o is not mt and o['name'] == mt['name'] and len(o['params']) == len(mt['params']) for o in c['methods']):
            continue
        if kind == '@interface':
            lines.append(f"{p2}{ret} {mt['name']}();")
        elif is_iface:
            if 'private' in mt['mods'] and mt['name'] not in used:
                continue
            if st:
                lines.append(f"{p2}public static {tp}{ret} {mt['name']}({ps}){throws_for(internal, mt['name'])} {BODY}")
            elif 'default' in mt['mods'] or 'private' in mt['mods']:
                lines.append(f"{p2}public default {tp}{ret} {mt['name']}({ps}) {BODY}")
            else:
                lines.append(f"{p2}public {tp}{ret} {mt['name']}({ps}){throws_for(internal, mt['name'])};")
        else:
            mods2 = [mv] + (['static'] if st else [])
            lines.append(f"{p2}{' '.join(mods2)} {tp}{ret} {mt['name']}({ps}){throws_for(internal, mt['name'])} {BODY}")
    for key, info in extra['methods'].items():
        if key.startswith('<init>'):
            continue
        name = key[:key.index('(')]
        ps, r = desc_split(key[key.index('('):])
        d = key[key.index('('):key.index(')') + 1]
        if name + d in seen_sigs:
            continue
        seen_sigs.add(name + d)
        pt = ', '.join(f'{desc_to_java(p)} a{i}' for i, p in enumerate(ps))
        rt = desc_to_java(r)
        if is_iface:
            if info['static']:
                lines.append(f'{p2}public static {rt} {name}({pt}) {BODY}')
            else:
                lines.append(f'{p2}public default {rt} {name}({pt}) {BODY}')
        else:
            st = 'static ' if info['static'] else ''
            lines.append(f'{p2}public {st}{rt} {name}({pt}) {BODY}')
    return lines, kind


def tparams_for(internal):
    n = ARITY.get(internal, 0)
    if not n:
        return ''
    return '<' + ', '.join(f'T{i}' for i in range(n)) + '>'


KNOWN_SUPER = {
    'net/minecraftforge/event/entity/EntityEvent': 'net.minecraftforge.eventbus.api.Event',
    'net/minecraftforge/event/entity/living/LivingEvent': 'net.minecraftforge.event.entity.EntityEvent',
    'net/minecraftforge/event/entity/player/PlayerEvent': 'net.minecraftforge.event.entity.living.LivingEvent',
    'net/minecraftforge/event/entity/item/ItemEvent': 'net.minecraftforge.event.entity.EntityEvent',
    'net/minecraftforge/event/entity/item/ItemTossEvent': 'net.minecraftforge.event.entity.item.ItemEvent',
    'net/minecraftforge/event/entity/EntityJoinLevelEvent': 'net.minecraftforge.event.entity.EntityEvent',
    'net/minecraftforge/event/entity/EntityLeaveLevelEvent': 'net.minecraftforge.event.entity.EntityEvent',
    'net/minecraftforge/event/entity/EntityMountEvent': 'net.minecraftforge.event.entity.EntityEvent',
    'net/minecraftforge/event/entity/living/BabyEntitySpawnEvent': 'net.minecraftforge.eventbus.api.Event',
    'net/minecraftforge/client/gui/overlay/ForgeGui': 'net.minecraft.client.gui.Gui',
    'net/minecraftforge/common/util/FakePlayer': 'net.minecraft.server.level.ServerPlayer',
}
KNOWN_IMPL = {
    'net/minecraftforge/client/settings/KeyConflictContext': 'net.minecraftforge.client.settings.IKeyConflictContext',
}


def is_enum_like(internal, e):
    if not e['fields'] or any(k.startswith('<init>') for k in e['methods']):
        return False
    if USAGE.get(internal, {}).get('subclassed_by'):
        return False
    return all(info.get('static') and key.split(':', 1)[1] == 'L' + internal + ';'
               for key, info in e['fields'].items())


def ext_super(internal, kind, enum_like):
    if internal in KNOWN_SUPER and kind == 'class' and not enum_like:
        return KNOWN_SUPER[internal]
    if kind != 'class' or enum_like or not internal.startswith('net/minecraftforge/'):
        return None
    simple_all = internal.split('/')[-1]
    if 'Event' not in simple_all:
        return None
    if '$' in internal:
        outer = internal.rsplit('$', 1)[0]
        if 'Event' in outer.split('/')[-1]:
            return internal_to_canon(outer)
    if internal.startswith('net/minecraftforge/event/entity/player/'):
        return 'net.minecraftforge.event.entity.player.PlayerEvent'
    if internal.startswith('net/minecraftforge/event/entity/living/'):
        return 'net.minecraftforge.event.entity.living.LivingEvent'
    return 'net.minecraftforge.eventbus.api.Event'


def emit_ext_class(internal, indent, nested_parent_kind=None):
    e = EXT[internal]
    simple = internal.split('/')[-1].split('$')[-1]
    kinds = e['kind']
    kind = 'interface' if ('interface' in kinds and 'class' not in kinds) else 'class'
    if e['sam']:
        kind = 'interface'
    pad = '    ' * indent
    p2 = pad + '    '
    mods = ['public'] + (['static'] if nested_parent_kind else [])
    enum_like = kind == 'class' and is_enum_like(internal, e)
    impl = (' implements ' + KNOWN_IMPL[internal]) if internal in KNOWN_IMPL else ''
    if enum_like:
        lines = [pad + ' '.join(mods + ['enum', simple]) + impl + ' {']
        lines.append(p2 + ', '.join(sorted(k.split(':')[0] for k in e['fields'])) + ';')
        for key, info in sorted(e['methods'].items()):
            name = key[:key.index('(')]
            if name in ('values', 'valueOf', 'ordinal', 'name', '<clinit>'):
                continue
            ps, r = desc_split(key[key.index('('):])
            pt = ', '.join(f'{desc_to_java(p)} a{i}' for i, p in enumerate(ps))
            lines.append(f"{p2}public {'static ' if info.get('static') else ''}{desc_to_java(r)} {name}({pt}) {BODY}")
        return lines, 'enum'
    sup = ext_super(internal, kind, enum_like)
    lines = [pad + ' '.join(mods + [kind, simple]) + tparams_for(internal) + (f' extends {sup}' if sup else '') + ' {']
    for key, info in sorted(e['fields'].items()):
        name, d = key.split(':', 1)
        t = desc_to_java(d)
        if kind == 'interface':
            lines.append(f'{p2}public {t} {name} = {default_value(t)};')
        else:
            lines.append(f"{p2}public {'static ' if info.get('static') else ''}{t} {name};")
    has_noarg = False
    for key, info in sorted(e['methods'].items()):
        name = key[:key.index('(')]
        ps, r = desc_split(key[key.index('('):])
        pt = ', '.join(f'{desc_to_java(p)} a{i}' for i, p in enumerate(ps))
        if name == '<clinit>':
            continue
        if name == '<init>':
            if kind == 'class':
                if not ps:
                    has_noarg = True
                lines.append(f'{p2}public {simple}({pt}) {{ }}')
            continue
        rt = desc_to_java(r)
        if kind == 'interface':
            if e['sam'] and key == e['sam']:
                lines.append(f'{p2}public {rt} {name}({pt});')
            elif info.get('static'):
                lines.append(f'{p2}public static {rt} {name}({pt}) {BODY}')
            else:
                lines.append(f'{p2}public default {rt} {name}({pt}) {BODY}')
        else:
            lines.append(f"{p2}public {'static ' if info.get('static') else ''}{rt} {name}({pt}) {BODY}")
    if e['sam'] and e['sam'] not in e['methods']:
        name = e['sam'][:e['sam'].index('(')]
        ps, r = desc_split(e['sam'][e['sam'].index('('):])
        pt = ', '.join(f'{desc_to_java(p)} a{i}' for i, p in enumerate(ps))
        lines.append(f'{p2}public {desc_to_java(r)} {name}({pt});')
    if kind == 'class' and not has_noarg:
        lines.append(f'{p2}public {simple}() {{ }}')
    return lines, kind


def main():
    if os.path.exists(OUT):
        shutil.rmtree(OUT)
    for k, v in KNOWN_SUPER.items():
        par = canon_to_internal(v)
        if par not in MC and par not in PROVIDED and par not in HAND_TOP:
            ext_entry(par)
    # all classes to emit: MC + EXT; group by top-level
    everything = set(MC) | set(EXT)
    # make sure outer classes of nested ones exist
    for n in list(everything):
        parts = n.split('$')
        for i in range(1, len(parts)):
            outer = '$'.join(parts[:i])
            if outer not in everything:
                everything.add(outer)
                if outer not in MC:
                    ext_entry(outer)
    children = collections.defaultdict(list)
    tops = []
    for n in sorted(everything):
        if any(seg[:1].isdigit() for seg in n.split('$')[1:]):
            continue
        if '$' in n:
            children[n.rsplit('$', 1)[0]].append(n)
        else:
            tops.append(n)

    def emit(n, indent, parent_kind):
        if n in MC:
            lines, kind = emit_mc_class(n, indent, parent_kind)
        else:
            lines, kind = emit_ext_class(n, indent, parent_kind)
        for ch in children.get(n, []):
            lines.extend(emit(ch, indent + 1, kind))
        lines.append('    ' * indent + '}')
        return lines

    count = 0
    for t in tops:
        if t in HAND_TOP or t in PROVIDED or is_jdk(t):
            continue
        if '/' not in t:
            continue
        pkg = t.rsplit('/', 1)[0].replace('/', '.')
        lines = [f'package {pkg};', ''] + emit(t, 0, None)
        path = os.path.join(OUT, t + '.java')
        os.makedirs(os.path.dirname(path), exist_ok=True)
        open(path, 'w').write('\n'.join(lines) + '\n')
        count += 1
    # copy hand-written stubs
    for dp, _, fs in os.walk(HAND):
        for f in fs:
            if f.endswith('.java'):
                src = os.path.join(dp, f)
                dst = os.path.join(OUT, os.path.relpath(src, HAND))
                os.makedirs(os.path.dirname(dst), exist_ok=True)
                shutil.copy(src, dst)
    print('generated top-level stubs:', count, 'hand:', len(HAND_TOP), 'external:', len(EXT),
          'mc extra members:', sum(len(v['fields']) + len(v['methods']) for v in EXTRA.values()))
    json.dump({k: {'fields': sorted(v['fields']), 'methods': sorted(v['methods'])} for k, v in EXTRA.items()},
              open(f'{BASE}/mc_extra.json', 'w'), indent=1)


if __name__ == '__main__':
    main()
