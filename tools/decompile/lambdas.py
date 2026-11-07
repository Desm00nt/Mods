"""List lambdas passed to Forge event-bus addListener() together with the
real (instantiated) lambda parameter types taken from the bytecode.

Vineflower/FernFlower drop explicit lambda parameter types, but Forge's
IEventBus.addListener(Consumer<T>) infers the event class T from the lambda
at runtime: an untyped `e -> ...` recompiles as Consumer<Event> and would
subscribe to *every* event.  Use this listing to restore the types.

usage: python3 lambdas.py <remapped.jar> [class-prefix]
"""
import struct, sys, zipfile, os
sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
from classfile import ClassFile, iter_code  # noqa


def code_attr(cf, m):
    return m.attrs.get('Code')


def main():
    jar = sys.argv[1]
    prefix = sys.argv[2] if len(sys.argv) > 2 else ''
    z = zipfile.ZipFile(jar)
    for name in sorted(z.namelist()):
        if not name.endswith('.class') or not name.startswith(prefix):
            continue
        cf = ClassFile(z.read(name))
        bsms = cf.bootstrap_methods()
        for m in cf.methods:
            code = code_attr(cf, m)
            if not code:
                continue
            pending = []  # indy lambdas seen since the last invoke
            for pc, op, arg in iter_code(code):
                if op == 0xba:  # invokedynamic
                    idx = struct.unpack('>H', arg[0:2])[0]
                    e = cf.cp.entries[idx]
                    bsm_i, nat_i = e[1], e[2]
                    nat = cf.cp.entries[nat_i]
                    iname, idesc = cf.utf(nat[1]), cf.utf(nat[2])
                    mh, args = bsms[bsm_i]
                    if len(args) == 3 and cf.cp.entries[args[1]][0] == 15:
                        h = cf.cp.entries[args[1]]
                        _, owner, impl, impl_desc = cf.ref(h[2])
                        inst = cf.utf(cf.cp.entries[args[2]][1])
                        pending.append((pc, iname, idesc, impl, inst))
                elif op in (0xb6, 0xb7, 0xb8, 0xb9):
                    idx = struct.unpack('>H', arg[0:2])[0]
                    _, owner, mname, mdesc = cf.ref(idx)
                    if mname == 'addListener' and pending:
                        pc0, iname, idesc, impl, inst = pending[-1]
                        print('%s.%s  pc=%d  %s.%s(...)  lambda=%s  type=%s' % (
                            cf.name.split('/')[-1], m.name, pc, owner.split('/')[-1], mname, impl, inst))
                    pending = []


if __name__ == '__main__':
    main()
