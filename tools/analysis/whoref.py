import sys, zipfile, re
sys.path.insert(0, '/home/user/Mods/tools/decompile')
from classfile import ClassFile
z = zipfile.ZipFile('/tmp/work/spraute-mojmap.jar')
targets = [l.split()[1].replace('.', '/') for l in open('/tmp/srvsim/result.txt') if l.startswith('FAIL')]
P = 'org/sprautestudio/spraute_engine/'
def clientish(c):
    parts = c.split('/')
    return 'client' in parts or parts[3] in ('editor', 'ui', 'mixin')
refs = {}
for n in z.namelist():
    if not n.endswith('.class') or not n.startswith(P): continue
    cf = ClassFile(z.read(n)); me = n[:-6]
    for e in cf.cp.entries:
        if e and e[0] == 7:
            t = cf.utf(e[1])
            if t in targets and t != me and not clientish(me) and t.split('$')[0] != me.split('$')[0] and me not in targets:
                refs.setdefault(t, set()).add(me)
for t in targets:
    print(t.replace(P, ''), '<-', ', '.join(sorted(x.replace(P, '') for x in refs.get(t, []))) or '(only client code)')
