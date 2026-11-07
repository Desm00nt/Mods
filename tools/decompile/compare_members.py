"""Compare declared members (fields + methods, name+descriptor) of every class
between the original (remapped) jar and a directory of recompiled classes.
Synthetic/compiler-specific members are ignored (lambdas, switch tables, bridges...).

usage: python3 compare_members.py <original.jar> <classes-dir>
"""
import os, sys, zipfile, re
sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
from classfile import ClassFile, ACC_SYNTHETIC, ACC_BRIDGE  # noqa

SKIP = re.compile(r'^(lambda\$|access\$|\$SWITCH_TABLE\$|\$values$|\$deserializeLambda\$|\$assertionsDisabled$|\$SwitchMap\$)')


def members(cf):
    out = set()
    for kind, lst in (('F', cf.fields), ('M', cf.methods)):
        for m in lst:
            if m.access & ACC_SYNTHETIC or (kind == 'M' and m.access & ACC_BRIDGE):
                continue
            if SKIP.match(m.name):
                continue
            out.add((kind, m.name, m.desc, m.access & 0x0F))  # public/private/protected/static
    return out


ANON = re.compile(r'^(.*)\$(\d+)\.class$')


def signature_of(cf):
    return (cf.super, tuple(sorted(cf.interfaces)), frozenset(members(cf)))


def main():
    jar, d = sys.argv[1], sys.argv[2]
    z = zipfile.ZipFile(jar)
    diffs = 0
    checked = 0
    anon_orig, anon_new = {}, {}
    for name in sorted(z.namelist()):
        if not name.endswith('.class'):
            continue
        a = ClassFile(z.read(name))
        if any(n.endswith('$SwitchMap') or '$SwitchMap$' in n for n in [m.name for m in a.fields]):
            continue  # javac enum-switch helper, ECJ uses $SWITCH_TABLE$ methods instead
        m = ANON.match(name)
        if m:
            anon_orig.setdefault(m.group(1), []).append((name, signature_of(a)))
            continue
        p = os.path.join(d, name)
        if not os.path.exists(p):
            print(f'{name}: MISSING CLASS')
            diffs += 1
            continue
        b = ClassFile(open(p, 'rb').read())
        checked += 1
        ma, mb = members(a), members(b)
        if a.super != b.super or sorted(a.interfaces) != sorted(b.interfaces):
            print(f'{name}: hierarchy differs: {a.super} {a.interfaces} vs {b.super} {b.interfaces}')
            diffs += 1
        for x in sorted(ma - mb):
            print(f'{name}: missing   {x}')
            diffs += 1
        for x in sorted(mb - ma):
            print(f'{name}: extra     {x}')
            diffs += 1
    for root, _, files in os.walk(d):
        for f in files:
            full = os.path.join(root, f)
            rel = os.path.relpath(full, d).replace(os.sep, '/')
            m = ANON.match(rel)
            if m:
                anon_new.setdefault(m.group(1), []).append((rel, signature_of(ClassFile(open(full, 'rb').read()))))
    for outer, lst in sorted(anon_orig.items()):
        pool = [s for _, s in anon_new.get(outer, [])]
        for name, sig in lst:
            checked += 1
            if sig in pool:
                pool.remove(sig)
            else:
                print(f'{name}: anonymous class has no structural match in rebuilt output')
                diffs += 1
    print(f'checked {checked} classes, {diffs} differences')


if __name__ == '__main__':
    main()
