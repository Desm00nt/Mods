import re, os
S = {'text': '"привет {1+2}"', 'number': '1.5', 'int': '2', 'target': 'alice', 'expr': 'v > 0 and true', 'duration': '2s',
     'item': '"minecraft:diamond"', 'char': 'alice', 'guiel': '"yes"', 'vfx': '"nuke"', 'skilltree': '"tree"', 'quest': '"q"',
     'ease': 'smooth', 'zone': '"z"', 'loc': 'camp', 'image': '"img"', 'gui': '"menu"', 'var': 'v', 'task': '"t1"',
     'anim': '"entity/humanoid!walk"', 'slotset': 'main', 'shop': '"shop"', 'effect': '"minecraft:blindness"', 'bool': 'true',
     'system': 'health', 'speaker': '1', 'slotorder': 'default', 'skillnode': '"node"', 'post': '"noir"', 'playercan': 'jump',
     'path': '"p"', 'panelturn': 'viewer', 'panellook': 'plate', 'layer': 'action', 'entity': '"spraute:npc"', 'buildstyle': 'default',
     'structure': '"house"', 'stealthshape': 'fan', 'stealth': '"guard"', 'sound': '"minecraft:entity.player.levelup"', 'label': 'begin',
     'func': 'f', 'branch': 'b', 'script': '5001', 'key': 'F8', 'emotion': 'HAPPY_NEW', 'movetype': 'walk', 'cutscene': '"cs"',
     'fight': '"fists"', 'skillmenu': '"menu"'}
HEAD = 'script 9001 "t"\ncharacter alice = "spraute:npc" name "Алиса"\nlocation camp = (0, 64, 0)\nvar v = 1\nlabel begin\nfunc f {\n  say 1 "x"\n}\n'
def fill(syntax, with_opt):
    s = syntax
    if not with_opt:
        while re.search(r'\[[^\[\]]*\]', s):
            s = re.sub(r'\[[^\[\]]*\]', '', s)
    s = s.replace('[', '').replace(']', '')
    s = re.sub(r'<(\w+):(\w+)(\.\.\.)?>', lambda m: S.get(m.group(2), '1'), s)
    s = s.replace('|', ' ')
    return re.sub(r'\s+', ' ', s).strip()
n = 0
for line in open('/tmp/dump/specs.tsv', encoding='utf-8'):
    p = line.rstrip('\n').split('\t')
    if p[0] != 'C': continue
    key, syntax = p[2], p[4]
    for opt in (True, False):
        args = fill(syntax, opt)
        body = (key + ' ' + args).strip() if not args.startswith(key) else args
        open(f'cases/cmd_{n:04d}.sst', 'w', encoding='utf-8').write(HEAD + body + '\nstop\n'); n += 1
flow = r'''script 9002 "flow" autostart
character bob = "minecraft:villager" name "Боб" speed 0.3
location a = (1, 2, 3) yaw 90 pitch 10
var n = 0
var list = [1, 2, "три"]
label top
if n < 3 and not false {
  set n += 1
} else {
  set n = n * 2 - 1
}
while n < 10 {
  set n += 2
}
repeat 3 {
  set n -= 1
}
for x in list {
  say 1 "{x}"
}
switch n {
  1 { goto top }
  else { stop }
}
choice bob "Да?" emotion HAPPY_NEW {
  "Да" { say 1 "ok" }
  "Нет" { wait 1s }
}
parallel {
  wait 2s
  say 1 "a"
}
async b {
  wait 5s
}
await b
cancel b
func g(p, q) {
  return p + q
}
call g(1, 2)
on break {
  say 1 "сломал"
}
stop
'''
open('cases/flow.sst', 'w', encoding='utf-8').write(flow)
bad = ['script x', 'script 5 "a"\nif {', 'script 6 "b"\ngoto nowhere\nset = 1', 'script 7 "c"\nsay 1 "незакрытая', '', 'script 8 "d"\nvar a = (1 +\n', 'script 9 "e" general\nspawn x at y']
for i, b in enumerate(bad):
    open(f'cases/bad_{i}.sst', 'w', encoding='utf-8').write(b)
import shutil; shutil.copy('/tmp/dump/demo.sst', 'cases/demo.sst')
exprs = ['1 + 2 * 3', '(1 + 2) * 3', '10 / 4', '10 % 3', '7 / 0', '5 % 0', '"a" + 1', '1 + "a"', '"x" + true', '[1, 2] + [3]', '[1] + 5',
         '2 - "1"', '3 == 3.0', '"a" == "a"', '"a" != "b"', '1 < 2', '"a" < "b"', '2 >= 2', 'true and false', 'true or false', 'not true',
         'false and (1 / 0)', 'true or (1 / 0)', '-5 + 2', '- (3)', '1.5 * 2', '"привет" + " " + "мир"', '[1, "a", true]', '0.1 + 0.2',
         '1 < "x"', 'random(1)', 'abs(-3)', 'min(1, 2)', 'max(1, 2)', 'round(2.5)', 'floor(1.7)', 'len("abc")', 'len([1,2])',
         'upper("ab")', 'lower("AB")', 'str(5)', 'num("5")', 'int(5.7)', 'contains("abc", "b")', 'join([1,2], "-")', 'split("a,b", ",")',
         'flag("x")', 'chance(100)', 'clamp(5, 0, 3)', 'pow(2, 10)', 'sqrt(16)', '"{1+1}"', '2 * "3"', '[] == []', 'null', 'unknownfn(1)',
         '"a" * 3', 'true + 1', '1 == "1"', '((((1))))']
open('exprs.txt', 'w', encoding='utf-8').write('\n'.join(exprs))
print(n, 'command cases +', 1 + len(bad) + 1, 'scripts,', len(exprs), 'expressions')
