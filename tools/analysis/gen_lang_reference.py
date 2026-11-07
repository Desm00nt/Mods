"""Generate docs/LANGUAGE_REFERENCE.md (Russian) from the registries of the recompiled mod.

Inputs are produced by running the *recompiled* classes (DumpSpecs.java / DumpEvents.java), i.e. the
reference is taken from the mod's own built-in help (lang/Specs, lang/EventDefs), not written by hand.
usage: gen_lang_reference.py specs.tsv events.tsv demo.sst OUT.md
"""
import sys
from collections import OrderedDict

specs_tsv, events_tsv, demo_path, out_path = sys.argv[1:5]

commands = OrderedDict()  # category -> list
functions, keywords, events, lists = [], [], [], {}
for line in open(specs_tsv, encoding='utf-8'):
    p = line.rstrip('\n').split('\t')
    if p[0] == 'C':
        _, cat, key, full, syntax, doc = p
        commands.setdefault(cat, []).append((key, full == 'FULL', syntax, doc))
    elif p[0] == 'F':
        functions.append((p[1], p[2], p[3]))
    elif p[0] == 'K':
        keywords.append((p[1], p[2]))
    elif p[0] == 'L':
        lists[p[1]] = p[2]
for line in open(events_tsv, encoding='utf-8'):
    p = line.rstrip('\n').split('\t')
    kv = dict(x.split('=', 1) for x in p[2:] if '=' in x)
    events.append((p[1], kv.get('title', ''), kv.get('doc', ''), kv.get('filterLabel', '')))

FULL_EVENTS = {'skilllearn', 'skilllevel', 'skilluse', 'mobspawn'}  # Specs.fullOnly(): "wait <event>" variants


def esc(s):
    return s.replace('|', '\\|').replace('<', '&lt;').replace('>', '&gt;')


total_cmd = sum(len(v) for v in commands.values())
total_full = sum(1 for v in commands.values() for c in v if c[1])
out = []
w = out.append
w('# Язык сюжетов Spraute Engine — справочник\n')
w('> Сгенерировано автоматически из встроенной справки мода (`lang/Specs`, `lang/EventDefs`) — классы были ')
w('> перекомпилированы из декомпилированных исходников и **запущены**, так что список точно совпадает с тем, ')
w('> что знает версия **Lite 1.9.5**. Генератор: `tools/analysis/gen_lang_reference.py`.\n')
w(f'Итого: **{len(keywords)}** ключевых слов, **{total_cmd}** команд (из них **{total_full}** — 🔒 только в полной версии), ')
w(f'**{len(functions)}** функций, **{len(events)}** событий.\n')
w('🔒 — команда распознаётся, но в Lite-версии анализатор выдаёт ошибку «Есть только в полной версии Spraute Engine», ')
w('а сборщик сюжета (`Emitter`) отказывается её компилировать; в блочном редакторе такие блоки скрыты.\n')

w('## Как устроен файл сюжета\n')
w('* Текстовые сюжеты — файлы `*.sst` в папке проекта `scripts/`; блочные (визуальные) — `*.svs`, они переводятся в тот же язык (`editor/visual/BlockCodegen`).')
w('* Первая строка — заголовок `script <id> "Название" [autostart [once]] [solo] [general]`. `id` уникален для мира.')
w('* Далее объявления (`character`, `location`, `var`), затем тело: команды выполняются сверху вниз, '
  'команды ожидания (`wait …`, `say`, `choice`) приостанавливают сюжет, пока условие не выполнится.')
w('* Ход выполнения: `label`/`goto`, `if`/`else`, `while`, `repeat`, `for`, `switch`, `choice`, `parallel`, '
  '`func`/`call`/`return`, `async`/`await`/`cancel`, обработчики событий `on <событие> { … }`.')
w('* Выражения: числа, строки (с подстановкой `{выражение}`), `true/false`, списки, `and/or/not`, '
  '`+ - * / %`, сравнения. `+` склеивает строки (если хотя бы одна сторона — строка) и списки; деление на ноль — ошибка «Деление на ноль».\n')
w('### Пример (шаблон «Пример», который создаёт сам редактор)\n')
w('```')
w(open(demo_path, encoding='utf-8').read().rstrip())
w('```\n')

w('## Ключевые слова\n')
for k, d in keywords:
    w(f'* **`{k}`** — {d}')
w('')

w('## Допустимые значения\n')
LIST_TITLES = {'EMOTIONS': 'Эмоции (`choice … emotion <эмоция>`; регистр не важен)', 'LAYERS': 'Слои анимации (`layer`)',
               'MOVE_TYPES': 'Типы движения (`moveanim`)', 'KEYS': 'Клавиши (`wait key`, `on keypress`)',
               'ENGINE_SYSTEMS': 'Системы движка (`enable`/`disable`)'}
for k, title in LIST_TITLES.items():
    if k in lists:
        w(f'* **{title}:** {lists[k]}')
w('')
w('> В версии 1.9.5 эмоций `HAPPY_NEW`, `SAD1`, `ANGRY` **нет**, хотя старые подсказки мода их предлагали ')
w('> (исправлено в исходниках, см. `docs/ANALYSIS.md`).\n')

w('## Команды по категориям\n')
for cat, cmds in commands.items():
    nfull = sum(1 for c in cmds if c[1])
    w(f'### {cat} ({len(cmds)}{f", 🔒 {nfull}" if nfull else ""})\n')
    for key, full, syntax, doc in cmds:
        lock = '🔒 ' if full else ''
        syn = f' `{syntax}`' if syntax.strip() else ''
        w(f'* {lock}**`{key}`**{syn} — {doc}')
    w('')

w('## Функции (в выражениях)\n')
w('| Сигнатура | Возвращает | Описание |')
w('|---|---|---|')
for sig, ret, doc in functions:
    w(f'| `{esc(sig)}` | {esc(ret)} | {esc(doc)} |')
w('')

w('## События (`on <событие>` и `wait <событие>`)\n')
w('| Событие | Когда | Фильтр | Описание |')
w('|---|---|---|---|')
for name, title, doc, flt in events:
    lock = '🔒 ' if name in FULL_EVENTS else ''
    w(f'| {lock}`{name}` | {esc(title)} | {esc(flt)} | {esc(doc)} |')
w('')
open(out_path, 'w', encoding='utf-8').write('\n'.join(out))
print('written', out_path, len(out), 'lines')
