#!/usr/bin/env python3
"""Compare two StaticDiff dumps (original jar vs recompiled sources) and separate real differences from known noise.

usage: staticdiff_compare.py ORIG.txt RECOMP.txt

Known noise (verified by hand on 1.9.5):
  * ClassNotFoundException for X$1/X$2...: javac emits "switch map" synthetic classes, ECJ uses $SWITCH_TABLE$ fields;
  * System.nanoTime() timestamps (lastFrame, previewFrame) and random UUIDs (McpServer.SESSION);
  * same elements in another order: HashMap/HashSet keyed by stub objects with identity hash codes.
Everything else is printed as REAL and needs a look (this is how the 3 Vineflower static-init bugs were found).
"""
import re
import sys

NOISE_FIELDS = re.compile(r'^\s+(lastFrame|previewFrame|SESSION) = ')


def load(path):
    d, cur = {}, None
    for line in open(path, encoding='utf-8', errors='replace'):
        line = line.rstrip('\n')
        if line.startswith('== '):
            cur = line[3:]
            d[cur] = []
        elif cur is not None:
            d[cur].append(line)
    return d


def main(a, b):
    o, r = load(a), load(b)
    real, noise = [], {'switch-map class': 0, 'timestamp/uuid': 0, 'order only': 0}
    for cls in sorted(set(o) | set(r)):
        lo, lr = o.get(cls), r.get(cls)
        if lo == lr:
            continue
        if lo is None or lr is None:
            real.append((cls, ['missing in ' + ('original' if lo is None else 'recompiled')]))
            continue
        if re.search(r'\$\d+$', cls) and any('ClassNotFoundException' in x for x in lo + lr):
            noise['switch-map class'] += 1
            continue
        only_o = [x for x in lo if x not in lr]
        only_r = [x for x in lr if x not in lo]
        rest_o = [x for x in only_o if not NOISE_FIELDS.match(x)]
        rest_r = [x for x in only_r if not NOISE_FIELDS.match(x)]
        if not rest_o and not rest_r:
            noise['timestamp/uuid'] += 1
            continue
        if len(rest_o) == len(rest_r) and all(sorted(x) == sorted(y) for x, y in zip(rest_o, rest_r)):
            noise['order only'] += 1
            continue
        real.append((cls, [f'  orig  : {x[:300]}' for x in rest_o] + [f'  recomp: {x[:300]}' for x in rest_r]))
    print(f'classes: {len(o)} / {len(r)}; noise: {noise}; REAL differences: {len(real)}')
    for cls, lines in real:
        print('REAL', cls)
        for x in lines:
            print('   ', x)
    return 1 if real else 0


if __name__ == '__main__':
    if len(sys.argv) != 3:
        sys.exit(__doc__)
    sys.exit(main(sys.argv[1], sys.argv[2]))
