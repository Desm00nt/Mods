"""Tiny line-anchored editor: fx.py FILE  then stdin JSON list of [line, old, new] (line=1-based; 0 = whole file, single occurrence)."""
import sys, json
path = sys.argv[1]
edits = json.load(sys.stdin)
lines = open(path, encoding='utf-8').read().split('\n')
for ln, old, new in edits:
    if ln == 0:
        s = '\n'.join(lines)
        c = s.count(old)
        assert c == 1, f'{path}: expected 1 occurrence, got {c}: {old[:80]!r}'
        lines = s.replace(old, new).split('\n')
    else:
        assert old in lines[ln - 1], f'{path}:{ln}: {old!r} not in {lines[ln-1]!r}'
        lines[ln - 1] = lines[ln - 1].replace(old, new, 1)
open(path, 'w', encoding='utf-8').write('\n'.join(lines))
print('ok', path, len(edits))
