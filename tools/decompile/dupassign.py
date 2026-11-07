"""Collapse Vineflower artefact `T x; T varN = x = expr;` into `T x = expr;` (only when varN is unused). usage: dupassign.py SRC_ROOT"""
import re, sys, os
root = sys.argv[1]
pat = re.compile(r'^(\s*)([\w.<>?, \[\]]+) (\w+);\n\1\2 (var\d+) = \3 = (.*)$', re.M)
total = 0
for dp, dn, fn in os.walk(root):
    for f in fn:
        if not f.endswith('.java'):
            continue
        p = os.path.join(dp, f)
        s = open(p, encoding='utf-8').read()
        def sub(m):
            global total
            ind, typ, name, var, rest = m.groups()
            if len(re.findall(r'\b%s\b' % var, s)) != 1:
                return m.group(0)
            total += 1
            return f'{ind}{typ} {name} = {rest}'
        s2 = pat.sub(sub, s)
        if s2 != s:
            open(p, 'w', encoding='utf-8').write(s2)
            print('fixed', p)
print('total', total)
