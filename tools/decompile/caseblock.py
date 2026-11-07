"""caseblock.py FILE LINE [OLD NEW ...]
Find the braced `case ...: {` (or `default: {`) block enclosing LINE, print its range;
optionally rename identifiers OLD->NEW inside that block (whole-word, skipping strings)."""
import re, sys
path, line = sys.argv[1], int(sys.argv[2])
pairs = sys.argv[3:]
lines = open(path, encoding='utf-8').read().split('\n')
start = None
for i in range(line - 1, -1, -1):
    if re.match(r'^\s*(case .*|default):\s*\{\s*$', lines[i]):
        # make sure LINE is inside its matching brace
        depth = 0
        for j in range(i, len(lines)):
            depth += lines[j].count('{') - lines[j].count('}')
            if depth == 0:
                break
        if j >= line - 1:
            start, end = i, j
            break
assert start is not None, 'no enclosing braced case'
print(f'block {start+1}-{end+1}: {lines[start].strip()}')
tok = re.compile(r'"(?:\\.|[^"\\])*"|\'(?:\\.|[^\'\\])*\'|[A-Za-z_$][\w$]*')
for k in range(0, len(pairs), 2):
    old, new = pairs[k], pairs[k + 1]
    n = 0
    for i in range(start, end + 1):
        def sub(m):
            global n
            if m.group(0) == old:
                n += 1
                return new
            return m.group(0)
        lines[i] = tok.sub(sub, lines[i])
    print(f'  {old} -> {new}: {n}')
if pairs:
    open(path, 'w', encoding='utf-8').write('\n'.join(lines))
