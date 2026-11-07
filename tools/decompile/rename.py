"""rename.py FILE START END OLD NEW : rename identifier OLD->NEW (word boundary, not after '.') in lines START..END."""
import sys, re
path, a, b, old, new = sys.argv[1], int(sys.argv[2]), int(sys.argv[3]), sys.argv[4], sys.argv[5]
lines = open(path, encoding='utf-8').read().split('\n')
pat = re.compile(r'(?<![\w.$])' + re.escape(old) + r'(?![\w$])')
n = 0
for i in range(a - 1, b):
    # do not touch string literals
    parts = re.split(r'("(?:[^"\\]|\\.)*")', lines[i])
    for k in range(0, len(parts), 2):
        parts[k], c = pat.subn(new, parts[k])
        n += c
    lines[i] = ''.join(parts)
open(path, 'w', encoding='utf-8').write('\n'.join(lines))
print(f'renamed {n} x {old}->{new} in {path}:{a}-{b}')
