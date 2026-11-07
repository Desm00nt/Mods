#!/usr/bin/env python3
"""Parse mappings.dev (zlataovce/mappings) HTML pages for one MC version into
a JSON model of the vanilla API (official/Mojang names) with generics."""
import html, json, os, posixpath, re, sys

ROOT = sys.argv[1] if len(sys.argv) > 1 else '/tmp/maps/zmap/1.20.1'
OUT = sys.argv[2] if len(sys.argv) > 2 else '/tmp/work/stubs/mc_model.json'

A_RE = re.compile(r'<a href="([^"]*)">(.*?)</a>', re.S)
TAG_RE = re.compile(r'<[^>]+>')
MODS = {'public', 'protected', 'private', 'static', 'final', 'abstract', 'default',
        'native', 'synchronized', 'transient', 'volatile', 'strictfp', 'sealed', 'non-sealed'}


def conv(frag, page_dir):
    """HTML fragment -> Java type text with fully qualified names."""
    def rep(m):
        href, text = m.group(1), m.group(2)
        href = href.split('#')[0]
        if href.startswith('http'):
            i = href.find('/api/')
            if i < 0:
                return TAG_RE.sub('', text)
            path = href[i + 5:]
            parts = path.split('/')
            if parts and '.' in parts[0] and not parts[0].endswith('.html'):
                parts = parts[1:]  # module name
            return '/'.join(parts)[:-5].replace('/', '.')
        p = posixpath.normpath(posixpath.join(page_dir, href))
        if p.endswith('.html'):
            p = p[:-5]
        return p.replace('/', '.').replace('$', '.')
    s = A_RE.sub(rep, frag)
    s = TAG_RE.sub('', s)
    s = html.unescape(s)
    return ' '.join(s.split())


def split_top(s, sep=','):
    out, depth, cur = [], 0, ''
    for ch in s:
        if ch == '<':
            depth += 1
        elif ch == '>':
            depth -= 1
        if ch == sep and depth == 0:
            out.append(cur.strip())
            cur = ''
        else:
            cur += ch
    if cur.strip():
        out.append(cur.strip())
    return out


def take_typeparams(s):
    """If s starts with '<', return (typeparams, rest)."""
    s = s.strip()
    if not s.startswith('<'):
        return '', s
    depth = 0
    for i, ch in enumerate(s):
        if ch == '<':
            depth += 1
        elif ch == '>':
            depth -= 1
            if depth == 0:
                return s[:i + 1], s[i + 1:].strip()
    raise ValueError(s)


def parse_params(s):
    s = s.strip()
    if not s:
        return []
    res = []
    for p in split_top(s):
        p = p.strip()
        idx = p.rfind(' ')
        typ, name = p[:idx].strip(), p[idx + 1:].strip()
        res.append((typ, name))
    return res


def split_sig(text):
    """'name(params)' -> name, params"""
    i = text.find('(')
    if i < 0:
        return text.strip(), None
    return text[:i].strip(), parse_params(text[i + 1:text.rfind(')')])


def parse_mods(s):
    toks = s.split(' ')
    mods = []
    while toks and toks[0] in MODS:
        mods.append(toks.pop(0))
    return mods, ' '.join(toks)


def parse_page(path, rel):
    s = open(path, encoding='utf-8').read()
    page_dir = posixpath.dirname(rel)
    binary = rel[:-5].replace('/', '.')
    m = re.search(r'<div class="A"><p>(.*?)</p>', s, re.S)
    if not m:
        return None
    header = conv(m.group(1), page_dir)
    mods, rest = parse_mods(header)
    kind = None
    for k in ('@interface', 'interface', 'enum', 'record', 'class'):
        if rest.startswith(k + ' '):
            kind = k
            rest = rest[len(k) + 1:]
            break
    if kind is None:
        raise ValueError('kind? ' + header)
    # name + typeparams
    mm = re.match(r'([\w$.]+)\s*(.*)$', rest)
    name, tparams = mm.group(1), mm.group(2).strip()
    ext, impl = [], []
    r = re.search(r'<p class="R">(.*?)</p>', s, re.S)
    if r:
        rt = conv(r.group(1), page_dir)
        # "extends A implements B, C" or "implements B" or "extends A, B" (interfaces)
        em = re.match(r'(?:extends (.*?))?\s*(?:implements (.*))?$', rt)
        if em.group(1):
            ext = split_top(em.group(1))
        if em.group(2):
            impl = split_top(em.group(2))
    cls = {'binary': binary, 'kind': kind, 'mods': mods, 'tparams': tparams,
           'extends': ext, 'implements': impl, 'enum_constants': [],
           'fields': [], 'ctors': [], 'methods': []}
    # sections
    parts = re.split(r'<h4>(.*?)</h4>', s)
    for i in range(1, len(parts), 2):
        title, body = parts[i], parts[i + 1]
        if title.startswith('Enum constant'):
            for row in re.finditer(r'<table><tbody>(.*?)</tbody></table>', body, re.S):
                g = re.search(r'<td class="D G"></td><td class="F">(.*?)</td>', row.group(1), re.S)
                if g:
                    cls['enum_constants'].append(conv(g.group(1), page_dir))
        elif title.startswith('Constructor'):
            for row in re.finditer(r'<tr><td class="O">(.*?)</td><td class="Q">(.*?)</td></tr>', body, re.S):
                cmods, _ = parse_mods(conv(row.group(1), page_dir))
                ptxt = conv(row.group(2), page_dir)
                cls['ctors'].append({'mods': cmods, 'params': parse_params(ptxt[ptxt.find('(') + 1:ptxt.rfind(')')])})
        elif title.startswith('Field') or title.startswith('Method'):
            for row in re.finditer(r'<tr><td class="O">(.*?)</td><td><table><tbody>(.*?)</tbody></table></td></tr>', body, re.S):
                o = conv(row.group(1), page_dir)
                g = re.search(r'<td class="D G"></td><td class="F">(.*?)</td>', row.group(2), re.S)
                if not g:
                    continue
                gtxt = conv(g.group(1), page_dir)
                fmods, rest2 = parse_mods(o)
                if title.startswith('Field'):
                    cls['fields'].append({'mods': fmods, 'type': rest2, 'name': gtxt})
                else:
                    tp, ret = take_typeparams(rest2)
                    name2, params = split_sig(gtxt)
                    cls['methods'].append({'mods': fmods, 'tparams': tp, 'ret': ret,
                                           'name': name2, 'params': params or []})
    return cls


def main():
    model = {}
    errors = 0
    for dp, _, fs in os.walk(ROOT):
        for f in fs:
            if not f.endswith('.html') or f in ('index.html', 'licenses.html'):
                continue
            full = os.path.join(dp, f)
            rel = os.path.relpath(full, ROOT).replace(os.sep, '/')
            try:
                c = parse_page(full, rel)
            except Exception as e:  # noqa
                errors += 1
                print('ERR', rel, e)
                continue
            if c:
                model[c['binary']] = c
    json.dump(model, open(OUT, 'w'), indent=0)
    print('classes', len(model), 'errors', errors)


if __name__ == '__main__':
    main()
