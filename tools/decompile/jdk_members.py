#!/usr/bin/env python3
"""Build /tmp/work/stubs/jdk_members.json (JDK supertypes/members needed by gen_stubs.py).

usage: jdk_members.py JAVA        (DumpJdk.class must be compiled into /tmp/work/stubs)

1. collect every JDK class name mentioned by mc_model.json (vanilla API) and usage.json (what the mod touches);
2. run DumpJdk (reflection on the running JDK) on that list;
3. convert its text output to {internal: {kind, methods, fields, ctors}}.
"""
import json
import re
import subprocess
import sys

BASE = '/tmp/work/stubs'
JDK_RE = re.compile(r'\b((?:java|javax|jdk|sun|com\.sun|org\.w3c|org\.xml)\.(?:[a-z0-9_]+\.)*[A-Z][A-Za-z0-9_$]*(?:\.[A-Z][A-Za-z0-9_$]*)*)')


def main(java):
    names = set()
    model_text = open(f'{BASE}/mc_model.json', encoding='utf-8').read()
    names.update(JDK_RE.findall(model_text))
    usage = json.load(open(f'{BASE}/usage.json'))
    for k in usage:
        if k.startswith(('java/', 'javax/', 'jdk/', 'sun/', 'com/sun/', 'org/w3c/', 'org/xml/')):
            names.add(k.replace('/', '.'))
    # nested classes are written A.B in signatures; Class.forName needs A$B
    fixed = set()
    for n in names:
        parts = n.split('.')
        for i, p in enumerate(parts):
            if p[:1].isupper():
                fixed.add('.'.join(parts[:i + 1]) + ''.join('$' + x for x in parts[i + 1:]))
                break
    out = subprocess.run([java, '-cp', BASE, 'DumpJdk'], input='\n'.join(sorted(fixed)), capture_output=True,
                         text=True, check=True).stdout
    res, cur, missing = {}, None, []
    for line in out.splitlines():
        if line.startswith('!'):
            missing.append(line[1:])
            cur = None
        elif line.startswith('#'):
            name, kind = line[1:].rsplit(' ', 1)
            cur = res.setdefault(name.replace('.', '/'), {'kind': kind, 'methods': [], 'fields': [], 'ctors': []})
        elif cur is None:
            continue
        elif line.startswith('= '):
            cur['fields'].append(line[2:])
        elif line.startswith('C '):
            cur['ctors'].append(line[2:])
        elif line[:2] in ('F ', '  '):
            cur['methods'].append(line[2:])
    json.dump(res, open(f'{BASE}/jdk_members.json', 'w'), indent=0, sort_keys=True)
    print('jdk classes', len(res), 'not found', len(missing), missing[:10])


if __name__ == '__main__':
    main(sys.argv[1] if len(sys.argv) > 1 else '/tmp/tools/jdk4py/jdk4py/java-runtime/bin/java')
