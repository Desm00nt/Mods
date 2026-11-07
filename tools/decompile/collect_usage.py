#!/usr/bin/env python3
"""Collect every external (non-mod, non-JDK) class/member referenced by the mod."""
import json, struct, sys, zipfile, collections
sys.path.insert(0, '/tmp/work/stubs')
from classfile import ClassFile, iter_code, ACC_INTERFACE, ACC_STATIC  # noqa

JAR = sys.argv[1] if len(sys.argv) > 1 else '/tmp/work/spraute-mojmap.jar'
OUT = sys.argv[2] if len(sys.argv) > 2 else '/tmp/work/stubs/usage.json'
JDK_PREFIX = ('java/', 'javax/', 'jdk/', 'sun/', 'com/sun/', 'org/w3c/', 'org/xml/')


def strip_arr(t):
    t = t.lstrip('[')
    if t.startswith('L') and t.endswith(';'):
        t = t[1:-1]
    return t


def desc_types(desc):
    out, i = [], 0
    while i < len(desc):
        ch = desc[i]
        if ch == 'L':
            j = desc.index(';', i)
            out.append(desc[i + 1:j])
            i = j + 1
        else:
            i += 1
    return out


def main():
    z = zipfile.ZipFile(JAR)
    mod = {}
    for n in z.namelist():
        if n.endswith('.class'):
            cf = ClassFile(z.read(n))
            mod[cf.name] = cf
    usage = collections.defaultdict(lambda: {'kind': set(), 'fields': {}, 'methods': {}, 'sam': None,
                                             'subclassed_by': [], 'implemented_by': [], 'refs': 0})

    def ext(name):
        return name and not name.startswith(JDK_PREFIX) and name not in mod and not name.startswith('[')

    def declares(cname, key, is_field):
        cf = mod.get(cname)
        if not cf:
            return False
        for m in (cf.fields if is_field else cf.methods):
            if (m.name + ':' + m.desc if is_field else m.name + m.desc) == key:
                return True
        return False

    def resolve(owner, key, is_field):
        """If owner is a mod class not declaring the member, walk up to the external ancestor."""
        seen = 0
        while owner in mod and seen < 50:
            seen += 1
            if declares(owner, key, is_field):
                return None
            cf = mod[owner]
            if not is_field:
                for i in cf.interfaces:
                    if ext(i):
                        pass
            owner = cf.super
        return owner

    def touch(name):
        name = strip_arr(name)
        if ext(name) and '/' in name or (ext(name) and name[0].isupper() and len(name) > 1):
            usage[name]['refs'] += 1

    for cf in mod.values():
        # hierarchy
        if ext(cf.super):
            usage[cf.super]['kind'].add('class')
            usage[cf.super]['subclassed_by'].append(cf.name)
        for i in cf.interfaces:
            if ext(i):
                usage[i]['kind'].add('interface')
                usage[i]['implemented_by'].append(cf.name)
        # any class constant / descriptor references
        for e in cf.cp.entries:
            if e and e[0] == 7:
                touch(cf.utf(e[1]))
            if e and e[0] == 12:
                for t in desc_types(cf.utf(e[2])):
                    touch(t)
        for m in cf.fields + cf.methods:
            for t in desc_types(m.desc):
                touch(t)
        bsms = cf.bootstrap_methods()
        for m in cf.methods:
            code = m.attrs.get('Code')
            if not code:
                continue
            for pc, op, arg in iter_code(code):
                if op in (0xb2, 0xb3, 0xb4, 0xb5):
                    tag, owner, name, desc = cf.ref(struct.unpack('>H', arg[:2])[0])
                    if owner in mod:
                        owner = resolve(owner, name + ':' + desc, True)
                    if ext(owner):
                        st = op in (0xb2, 0xb3)
                        usage[owner]['fields'][name + ':' + desc] = {'static': st}
                elif op in (0xb6, 0xb7, 0xb8, 0xb9):
                    tag, owner, name, desc = cf.ref(struct.unpack('>H', arg[:2])[0])
                    if owner.startswith('['):
                        continue
                    if owner in mod and name != '<init>':
                        owner = resolve(owner, name + desc, False)
                    if ext(owner):
                        u = usage[owner]
                        if tag == 11 or op == 0xb9:
                            u['kind'].add('interface')
                        elif op != 0xb7 or name == '<init>':
                            u['kind'].add('class_ref')
                        key = name + desc
                        rec = u['methods'].setdefault(key, {'static': False, 'special': False})
                        if op == 0xb8:
                            rec['static'] = True
                        if op == 0xb7:
                            rec['special'] = True
                elif op == 0xbb:
                    owner = cf.cls(struct.unpack('>H', arg[:2])[0])
                    if ext(owner):
                        usage[owner]['kind'].add('class_new')
                elif op == 0xba:
                    idx = struct.unpack('>H', arg[:2])[0]
                    e = cf.cp.entries[idx]
                    bsm_i, nat = e[1], cf.cp.entries[e[2]]
                    name, desc = cf.utf(nat[1]), cf.utf(nat[2])
                    mh, args = bsms[bsm_i]
                    mhe = cf.cp.entries[mh]
                    _, bowner, bname, _ = cf.ref(mhe[2])
                    if bowner == 'java/lang/invoke/LambdaMetafactory' and args:
                        fi = desc[desc.rindex(')') + 1:]
                        fi = fi[1:-1]
                        sam = cf.utf(cf.cp.entries[args[0]][1])
                        if ext(fi):
                            usage[fi]['kind'].add('interface')
                            usage[fi]['sam'] = name + sam
                        # implementation method handle
                        impl = cf.cp.entries[args[1]]
                        kind, ref = impl[1], impl[2]
                        tag, owner, iname, idesc = cf.ref(ref)
                        if ext(owner):
                            u = usage[owner]
                            if tag == 11 or kind == 9:
                                u['kind'].add('interface')
                            rec = u['methods'].setdefault(iname + idesc, {'static': False, 'special': False})
                            if kind == 6:
                                rec['static'] = True
                            if kind == 8:
                                u['kind'].add('class_new')
    res = {}
    for k, v in usage.items():
        v['kind'] = sorted(v['kind'])
        res[k] = v
    json.dump(res, open(OUT, 'w'), indent=1, sort_keys=True)
    pk = collections.Counter('/'.join(k.split('/')[:2]) for k in res)
    print('external classes', len(res))
    for k, c in pk.most_common(40):
        print('  ', c, k)


if __name__ == '__main__':
    main()
