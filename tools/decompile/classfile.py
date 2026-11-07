"""Minimal JVM class-file reader (constant pool, members, Code scanning)."""
import os, struct, sys
sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
from remap import CP  # noqa

ACC_PUBLIC, ACC_PRIVATE, ACC_PROTECTED, ACC_STATIC = 0x1, 0x2, 0x4, 0x8
ACC_FINAL, ACC_INTERFACE, ACC_ABSTRACT, ACC_VARARGS = 0x10, 0x200, 0x400, 0x80
ACC_SYNTHETIC, ACC_ANNOTATION, ACC_ENUM, ACC_BRIDGE = 0x1000, 0x2000, 0x4000, 0x40


class Member:
    __slots__ = ('access', 'name', 'desc', 'attrs')

    def __init__(self, access, name, desc, attrs):
        self.access, self.name, self.desc, self.attrs = access, name, desc, attrs


class ClassFile:
    def __init__(self, data):
        self.cp = CP(data)
        cp = self.cp
        b = data[cp.end:]
        self.access, this_i, super_i = struct.unpack('>HHH', b[0:6])
        self.name = self.cls(this_i)
        self.super = self.cls(super_i) if super_i else None
        n = struct.unpack('>H', b[6:8])[0]
        pos = 8
        self.interfaces = []
        for _ in range(n):
            self.interfaces.append(self.cls(struct.unpack('>H', b[pos:pos + 2])[0]))
            pos += 2
        self.fields, pos = self._members(b, pos)
        self.methods, pos = self._members(b, pos)
        self.attrs, pos = self._attrs(b, pos)

    def utf(self, i):
        return self.cp.utf8(i)

    def cls(self, i):
        return self.utf(self.cp.entries[i][1])

    def _attrs(self, b, pos):
        n = struct.unpack('>H', b[pos:pos + 2])[0]
        pos += 2
        attrs = {}
        for _ in range(n):
            ni, ln = struct.unpack('>HI', b[pos:pos + 6])
            attrs[self.utf(ni)] = b[pos + 6:pos + 6 + ln]
            pos += 6 + ln
        return attrs, pos

    def _members(self, b, pos):
        n = struct.unpack('>H', b[pos:pos + 2])[0]
        pos += 2
        out = []
        for _ in range(n):
            acc, ni, di = struct.unpack('>HHH', b[pos:pos + 6])
            attrs, pos = self._attrs(b, pos + 6)
            out.append(Member(acc, self.utf(ni), self.utf(di), attrs))
        return out, pos

    # ---- constant pool helpers
    def ref(self, i):
        """Field/Method/InterfaceMethod ref -> (tag, owner, name, desc)"""
        e = self.cp.entries[i]
        owner = self.cls(e[1])
        nat = self.cp.entries[e[2]]
        return e[0], owner, self.utf(nat[1]), self.utf(nat[2])

    def bootstrap_methods(self):
        a = self.attrs.get('BootstrapMethods')
        if not a:
            return []
        n = struct.unpack('>H', a[0:2])[0]
        pos = 2
        res = []
        for _ in range(n):
            mh, na = struct.unpack('>HH', a[pos:pos + 4])
            args = list(struct.unpack('>%dH' % na, a[pos + 4:pos + 4 + 2 * na]))
            res.append((mh, args))
            pos += 4 + 2 * na
        return res

    def signature(self, attrs=None):
        a = (attrs if attrs is not None else self.attrs).get('Signature')
        return self.utf(struct.unpack('>H', a)[0]) if a else None


# opcode operand lengths (excluding opcode), -1 = special
OPLEN = [0] * 256
for op in (0x10, 0x12, 0x15, 0x16, 0x17, 0x18, 0x19, 0x36, 0x37, 0x38, 0x39, 0x3a, 0xa9, 0xbc):
    OPLEN[op] = 1
for op in list(range(0x99, 0xa9)) + [0x11, 0x13, 0x14, 0x84, 0xc6, 0xc7, 0xb2, 0xb3, 0xb4, 0xb5,
                                      0xb6, 0xb7, 0xb8, 0xbb, 0xbd, 0xc0, 0xc1]:
    OPLEN[op] = 2
for op in (0xb9, 0xba, 0xc8, 0xc9):
    OPLEN[op] = 4
OPLEN[0xc5] = 3
OPLEN[0xaa] = OPLEN[0xab] = OPLEN[0xc4] = -1


def iter_code(code):
    """Yield (pc, opcode, operand_bytes) for a Code attribute body."""
    max_stack, max_locals, code_len = struct.unpack('>HHI', code[0:8])
    c = code[8:8 + code_len]
    pc = 0
    while pc < len(c):
        op = c[pc]
        ln = OPLEN[op]
        if ln >= 0:
            yield pc, op, c[pc + 1:pc + 1 + ln]
            pc += 1 + ln
        elif op == 0xc4:  # wide
            op2 = c[pc + 1]
            ln = 5 if op2 == 0x84 else 3
            yield pc, op, c[pc + 1:pc + 1 + ln]
            pc += 1 + ln
        else:
            p = pc + 1
            p += (4 - p % 4) % 4
            if op == 0xaa:
                lo, hi = struct.unpack('>ii', c[p + 4:p + 12])
                end = p + 12 + 4 * (hi - lo + 1)
            else:
                npairs = struct.unpack('>i', c[p + 4:p + 8])[0]
                end = p + 8 + 8 * npairs
            yield pc, op, c[pc + 1:end]
            pc = end
