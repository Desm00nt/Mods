#!/usr/bin/env python3
"""Mechanical clean-up of Vineflower output so it compiles with javac 17.

Usage: postprocess.py <source-root>
Only safe, purely syntactic rewrites are done here; semantic repairs are
applied separately (see the git history of spraute_engine-src)."""
import os
import re
import sys

ROOT = sys.argv[1]

# for (Path p : stream.sorted()::iterator)  ->  for (Path p : (Iterable<Path>) stream.sorted()::iterator)
FOR_ITER = re.compile(r'for \((final )?([\w.$]+(?:<[^()]*?>)?(?:\[\])*) (\w+) : (?!\(Iterable)(.+?)::iterator\) \{')
# (Entity)this -> (Entity)(Object)this   (mixin classes are never the target type at compile time)
MIXIN_CAST = re.compile(r'\(([A-Z][\w.]*(?:<[^()]*?>)?)\)this\b')
MIXIN_INSTANCEOF = re.compile(r'(?<![\w.)])this instanceof ')

stats = {'for_iter': 0, 'mixin_cast': 0, 'mixin_instanceof': 0, 'unrepresentable': 0}
for dp, _, files in os.walk(ROOT):
    for f in files:
        if not f.endswith('.java'):
            continue
        p = os.path.join(dp, f)
        src = open(p, encoding='utf-8').read()
        orig = src

        def for_iter(m):
            stats['for_iter'] += 1
            return f'for ({m.group(1) or ""}{m.group(2)} {m.group(3)} : (Iterable<{m.group(2)}>) {m.group(4)}::iterator) {{'

        src = FOR_ITER.sub(for_iter, src)
        n = src.count('this.<<unrepresentable>>')
        if n:
            stats['unrepresentable'] += n
            src = src.replace('this.<<unrepresentable>>', 'this.')
        if '/mixin/' in p.replace(os.sep, '/'):
            src, n1 = MIXIN_CAST.subn(lambda m: m.group(0) if m.group(1) == 'Object' else f'({m.group(1)})(Object)this', src)
            src, n2 = MIXIN_INSTANCEOF.subn('(Object)this instanceof ', src)
            stats['mixin_cast'] += n1
            stats['mixin_instanceof'] += n2
        if src != orig:
            open(p, 'w', encoding='utf-8').write(src)
print(stats)
