"""brace.py FILE LINE [LINE...] : wrap the body of the switch-case label at LINE in { }."""
import sys, re
path = sys.argv[1]
targets = sorted(int(x) for x in sys.argv[2:])
lines = open(path, encoding='utf-8').read().split('\n')
offset = 0
for t in targets:
    i = t - 1 + offset
    lab = lines[i]
    m = re.match(r'^(\s*)(case .*|default):\s*$', lab)
    assert m, f'not a bare case label: {lab!r}'
    ind = m.group(1)
    lines[i] = lab.rstrip() + ' {'
    j = i + 1
    while j < len(lines):
        l = lines[j]
        if re.match('^' + ind + r'(case |default\b)', l) or re.match('^' + ind[:-4] + r'}\s*$', l):
            break
        j += 1
    # drop trailing blank lines inside the block
    k = j
    while lines[k - 1].strip() == '':
        k -= 1
    lines.insert(k, ind + '}')
    offset += 1
open(path, 'w', encoding='utf-8').write('\n'.join(lines))
print('braced', len(targets), 'labels in', path)
