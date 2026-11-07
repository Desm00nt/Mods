"""Turn Vineflower's "semaphore variable" try/finally output back into a plain
try { } catch { } finally { } block.

Vineflower emits (when it cannot verify finally blocks):

    boolean varN = false /* VF: Semaphore variable */;
    labelM: {
        try { varN = true; BODY; varN = false; break labelM; }
        catch (...) { ...; varN = false; }
        finally { if (varN) { FIN } }
        FIN; return;
    }
    FIN

javac had copied FIN into every exit path; this script checks that the copies
after the try are identical to FIN and folds them back into `finally`.

usage: python3 unsemaphore.py FILE...
"""
import re, sys


def ind(s):
    return len(s) - len(s.lstrip(' '))


def dedent(lines, n):
    return [l[n:] if l.startswith(' ' * n) else l.lstrip(' ') if not l.strip() else l for l in lines]


def match_brace(L, i):
    depth = 0
    for j in range(i, len(L)):
        depth += L[j].count('{') - L[j].count('}')
        if depth == 0:
            return j
    raise ValueError('unbalanced')


def fix_one(L):
    s = next((i for i, l in enumerate(L) if '/* VF: Semaphore variable */' in l), None)
    if s is None:
        return False
    v = re.search(r'boolean (var\d+) = false', L[s]).group(1)
    lab = s + 1
    while not L[lab].strip():
        lab += 1
    m = re.match(r'^(\s*)(label\d+): \{$', L[lab])
    if not m:
        return fix_unlabeled(L, s, v, lab)
    label_ind, label = len(m.group(1)), m.group(2)
    label_end = match_brace(L, lab)
    t = lab + 1
    assert L[t].strip() == 'try {' and L[t + 1].strip() == f'{v} = true;', (L[t], L[t + 1])
    ti = ind(L[t])
    c = next(i for i in range(t + 2, label_end) if ind(L[i]) == ti and L[i].lstrip().startswith('} catch ('))
    f = next(i for i in range(c, label_end) if ind(L[i]) == ti and L[i].strip() == '} finally {')
    assert L[f + 1].strip() == f'if ({v}) {{'
    fin_end = match_brace(L, f + 1)
    fin = dedent(L[f + 2:fin_end], ti + 8)
    fin = [x for x in fin]
    try_close = fin_end + 1
    assert L[try_close].strip() == '}' and ind(L[try_close]) == ti
    # duplicates after the try (inside label block) and after the label block
    dup1 = [x for x in dedent(L[try_close + 1:label_end], label_ind + 4) if x.strip()]
    meth_end = next(i for i in range(label_end + 1, len(L)) if ind(L[i]) < label_ind and L[i].strip() == '}')
    dup2 = [x for x in dedent(L[label_end + 1:meth_end], label_ind) if x.strip()]
    finc = [x for x in fin if x.strip()]
    assert dup1 and dup1[-1].strip() == 'return;' and dup1[:-1] == finc, ('dup1 mismatch', dup1, finc)
    assert dup2 == finc, ('dup2 mismatch', dup2, finc)

    def clean(block):
        out = [x for x in block if x.strip() not in (f'{v} = false;', f'break {label};')]
        txt = '\n'.join(out)
        txt = re.sub(r'\} else \{\n\s*\}', '}', txt)          # else { varN = false; } leftovers
        return txt.split('\n')

    body = clean(dedent(L[t + 2:c], 4))
    catch = clean(dedent(L[c:f], 4))
    pad = ' ' * label_ind
    new = [pad + 'try {'] + body + catch + [pad + '} finally {'] + [pad + '    ' + x if x.strip() else x for x in fin] + [pad + '}']
    # drop the semaphore declaration (+ following blank line)
    start = s
    L[start:meth_end] = new
    # remove Vineflower's warning comment above the method
    for i in range(start - 1, max(0, start - 400), -1):
        if '// $VF: Could not verify finally blocks' in L[i]:
            del L[i:i + 2]
            break
    return True


def fix_unlabeled(L, s, v, t):
    """Variant without a label: try { v = true; BODY } [catch] finally { if (v) { FIN } } FIN"""
    assert L[t].strip() == 'try {' and L[t + 1].strip() == f'{v} = true;', (L[t], L[t + 1])
    ti = ind(L[t])
    f = next(i for i in range(t + 2, len(L)) if ind(L[i]) == ti and L[i].strip() == '} finally {')
    assert L[f + 1].strip() == f'if ({v}) {{'
    fin_end = match_brace(L, f + 1)
    fin = dedent(L[f + 2:fin_end], ti + 8)
    try_close = fin_end + 1
    assert L[try_close].strip() == '}' and ind(L[try_close]) == ti
    blk_end = next(i for i in range(try_close + 1, len(L)) if L[i].strip() and ind(L[i]) < ti)
    dup = [x for x in dedent(L[try_close + 1:blk_end], ti) if x.strip()]
    finc = [x for x in fin if x.strip()]
    assert dup == finc, ('dup mismatch', dup, finc)
    out = [x for x in L[t + 2:f] if x.strip() != f'{v} = false;']
    txt = re.sub(r'\} else \{\n\s*\}', '}', '\n'.join(out))
    pad = ' ' * ti
    new = [pad + 'try {'] + txt.split('\n') + [pad + '} finally {'] + [pad + '    ' + x if x.strip() else x for x in fin] + [pad + '}']
    L[s:blk_end] = new
    for i in range(s - 1, max(0, s - 400), -1):
        if '// $VF: Could not verify finally blocks' in L[i]:
            del L[i:i + 2]
            break
    return True


for path in sys.argv[1:]:
    L = open(path, encoding='utf-8').read().split('\n')
    n = 0
    while fix_one(L):
        n += 1
    open(path, 'w', encoding='utf-8').write('\n'.join(L))
    print(path, 'fixed', n)
