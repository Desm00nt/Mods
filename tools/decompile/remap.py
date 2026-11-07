#!/usr/bin/env python3
"""Remap Forge SRG member names (m_NNN_/f_NNN_) to Mojang official names
directly in .class files.

Only member *references* (CONSTANT_NameAndType) and member *declarations*
(field_info / method_info) are touched.  Utf8 entries are never edited in
place: a new Utf8 is appended instead, so string literals (reflection,
annotation values) that happen to share the same Utf8 stay untouched.
"""
import json, os, re, struct, sys, zipfile, collections

SRG_RE = re.compile(r'^(m|f)_\d+_$')


class CP:
    def __init__(self, data):
        self.data = data
        count = struct.unpack('>H', data[8:10])[0]
        self.entries = [None] * count
        pos = 10
        i = 1
        while i < count:
            tag = data[pos]
            if tag == 1:
                ln = struct.unpack('>H', data[pos + 1:pos + 3])[0]
                raw = data[pos + 3:pos + 3 + ln]
                self.entries[i] = [1, raw]
                pos += 3 + ln
            elif tag in (3, 4):
                self.entries[i] = [tag, data[pos + 1:pos + 5]]
                pos += 5
            elif tag in (5, 6):
                self.entries[i] = [tag, data[pos + 1:pos + 9]]
                pos += 9
                i += 1
            elif tag in (7, 8, 16, 19, 20):
                self.entries[i] = [tag, struct.unpack('>H', data[pos + 1:pos + 3])[0]]
                pos += 3
            elif tag in (9, 10, 11, 12, 17, 18):
                a, b = struct.unpack('>HH', data[pos + 1:pos + 5])
                self.entries[i] = [tag, a, b]
                pos += 5
            elif tag == 15:
                kind = data[pos + 1]
                ref = struct.unpack('>H', data[pos + 2:pos + 4])[0]
                self.entries[i] = [15, kind, ref]
                pos += 4
            else:
                raise ValueError('bad cp tag %d at %d' % (tag, pos))
            i += 1
        self.end = pos
        self.new_utf8 = {}

    def utf8(self, idx):
        e = self.entries[idx]
        assert e[0] == 1
        # Java "modified UTF-8": NUL is encoded as C0 80
        return e[1].replace(b'\xc0\x80', b'\x00').decode('utf-8', 'surrogatepass')

    def add_utf8(self, s):
        if s in self.new_utf8:
            return self.new_utf8[s]
        raw = s.encode('utf-8')
        self.entries.append([1, raw])
        idx = len(self.entries) - 1
        self.new_utf8[s] = idx
        return idx

    def serialize(self):
        out = bytearray(struct.pack('>H', len(self.entries)))
        i = 1
        while i < len(self.entries):
            e = self.entries[i]
            tag = e[0]
            out.append(tag)
            if tag == 1:
                out += struct.pack('>H', len(e[1])) + e[1]
            elif tag in (3, 4, 5, 6):
                out += e[1]
                if tag in (5, 6):
                    i += 1
            elif tag in (7, 8, 16, 19, 20):
                out += struct.pack('>H', e[1])
            elif tag in (9, 10, 11, 12, 17, 18):
                out += struct.pack('>HH', e[1], e[2])
            elif tag == 15:
                out += bytes([e[1]]) + struct.pack('>H', e[2])
            i += 1
        return bytes(out)


def remap_class(data, fmap, mmap, stats, cls_name):
    cp = CP(data)

    def mapped(name):
        if not SRG_RE.match(name):
            return None
        return (fmap if name[0] == 'f' else mmap).get(name)

    # String literals containing SRG names (reflection) -> report only
    for e in cp.entries:
        if e and e[0] == 8:
            s = cp.utf8(e[1])
            if SRG_RE.match(s):
                stats['string_literals'].append((cls_name, s, mapped(s)))

    # NameAndType references
    for e in cp.entries:
        if e and e[0] == 12:
            name = cp.utf8(e[1])
            new = mapped(name)
            if new:
                e[1] = cp.add_utf8(new)
                stats['refs'] += 1
            elif SRG_RE.match(name):
                stats['unmapped'][name] += 1

    # Body: fields & methods declarations
    body = bytearray(data[cp.end:])
    pos = 6  # access, this, super
    n_if = struct.unpack('>H', body[pos:pos + 2])[0]
    pos += 2 + 2 * n_if
    for kind in ('field', 'method'):
        n = struct.unpack('>H', body[pos:pos + 2])[0]
        pos += 2
        for _ in range(n):
            name_idx = struct.unpack('>H', body[pos + 2:pos + 4])[0]
            name = cp.utf8(name_idx)
            new = mapped(name)
            if new:
                body[pos + 2:pos + 4] = struct.pack('>H', cp.add_utf8(new))
                stats['decls'] += 1
                stats['decl_list'].append((cls_name, kind, name, new))
            elif SRG_RE.match(name):
                stats['unmapped_decl'].append((cls_name, kind, name))
            n_attr = struct.unpack('>H', body[pos + 6:pos + 8])[0]
            pos += 8
            for _ in range(n_attr):
                ln = struct.unpack('>I', body[pos + 2:pos + 6])[0]
                pos += 6 + ln
    return data[:8] + cp.serialize() + bytes(body)


def main():
    src_jar, out_jar, mapfile, report = sys.argv[1:5]
    mp = json.load(open(mapfile))
    fmap, mmap = mp['fields'], mp['methods']
    stats = {'refs': 0, 'decls': 0, 'unmapped': collections.Counter(),
             'unmapped_decl': [], 'string_literals': [], 'decl_list': []}
    zin = zipfile.ZipFile(src_jar)
    zout = zipfile.ZipFile(out_jar, 'w', zipfile.ZIP_DEFLATED)
    for info in zin.infolist():
        data = zin.read(info.filename)
        if info.filename.endswith('.class'):
            data = remap_class(data, fmap, mmap, stats, info.filename[:-6])
        zout.writestr(info, data)
    zout.close()
    rep = {
        'refs_remapped': stats['refs'],
        'decls_remapped': stats['decls'],
        'unmapped_refs': dict(stats['unmapped']),
        'unmapped_decls': stats['unmapped_decl'],
        'srg_string_literals': stats['string_literals'],
        'renamed_declarations': stats['decl_list'],
    }
    json.dump(rep, open(report, 'w'), indent=1)
    print('refs', stats['refs'], 'decls', stats['decls'],
          'unmapped refs', dict(stats['unmapped']),
          'unmapped decls', stats['unmapped_decl'],
          'string literals', len(stats['string_literals']))


if __name__ == '__main__':
    main()
