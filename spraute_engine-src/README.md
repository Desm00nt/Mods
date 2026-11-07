# Spraute Engine Lite 1.9.5 — исходники (декомпиляция)

Проект Forge MDK с декомпилированными исходниками `spraute_engine-lite-1.9.5-forge-1.20.1.jar`.
Имена Minecraft — официальные (Mojang), как в MDK; ресурсы (`src/main/resources`) взяты из jar без изменений,
кроме `spraute_engine.refmap.json`: его генерирует сборка.

Как устроен мод и какие ошибки в нём найдены — [`../docs/ANALYSIS.md`](../docs/ANALYSIS.md);
справочник языка сюжетов — [`../docs/LANGUAGE_REFERENCE.md`](../docs/LANGUAGE_REFERENCE.md).

## Сборка

Нужен **JDK 17** (Gradle 8.8 скачает себя сам через wrapper).

```bash
./gradlew build          # Windows: gradlew.bat build
```

Результат: `build/libs/spraute_engine-lite-1.9.5-forge-1.20.1.jar` — то же имя, что у оригинала
(задача `reobfJar` переводит имена в SRG, как требует Forge в продакшене).

Запуск из исходников: `./gradlew runClient` или `./gradlew runServer` (папка `run/`).
Для IntelliJ IDEA: открыть папку как Gradle-проект, затем `./gradlew genIntellijRuns`.

## Важно знать

* **Сборка Gradle здесь не проверялась**: из среды, где делалась декомпиляция, недоступен maven.minecraftforge.net.
  Исходники проверены компилятором ECJ (Java 17) против заглушек Minecraft/Forge — 0 ошибок — и сравнением с
  оригинальным jar (структура классов, статическое состояние, поведение языка; подробности — в `docs/ANALYSIS.md`).
  Если первая сборка споткнётся, вероятнее всего это будет сигнатура какого-то метода Forge/Minecraft, которую
  заглушки не отразили, — правится точечно.
* **Версия Forge** `47.3.0` в `gradle.properties` выбрана как совместимая с требованием мода `[47,)`;
  с какой именно версией собирали оригинал, в jar не записано. Подойдёт любая 47.x.
* Nashorn (`org.openjdk.nashorn`, для команд JavaScript) отдельно подключать не нужно — его приносит Forge.
* Миксины: конфиг `spraute_engine.mixins.json`, refmap генерирует плагин `org.spongepowered.mixin`.

## Состав

```
src/main/java/org/sprautestudio/spraute_engine/
  SprauteEngine, Modules, ClientModules, Edition — запуск и регистрация подсистем, флаг Lite
  platform/ (+ forge/)   — абстракция над загрузчиком; весь код Forge (и @Mod ForgeMod) — только в platform/forge
  lang/                  — язык сюжетов: Lexer, Parser, Ast, Analyzer, Specs (команды), Eval, Emitter
  runtime/, core/script/ — выполнение сюжетов, команда /script, JS-движок
  editor/                — внутриигровая IDE (крупнейшая часть, ~42 тыс. строк)
  ui/                    — собственный UI-тулкит редактора
  net/                   — сетевой канал и 68 пакетов
  mixin/                 — 35 миксинов
  core/, custom/, gui/, camera/, cutscene/, quest/, shop/, zone/, path/, nav/, fx/, playeranim/ … — подсистемы мира
  fight/, skills/, stealth/, post/, mcp/ — функции полной версии (в Lite закрыты или заглушки)
  export/                — экспорт проекта в виде отдельного мода (ModExport) и запуск проектов, вложенных в моды (BundledProjects)
```

История git разделена: точная декомпиляция (`389b1e1` и раньше) и исправления ошибок мода (следующий коммит) —
каждое исправление помечено в коде комментарием `Fix`.
