package org.sprautestudio.spraute_engine.general.client;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.TitleScreen;
import org.sprautestudio.spraute_engine.SprauteEngine;
import org.sprautestudio.spraute_engine.compat.Mc;
import org.sprautestudio.spraute_engine.editor.ClientState;
import org.sprautestudio.spraute_engine.editor.project.Project;
import org.sprautestudio.spraute_engine.editor.project.Workspace;
import org.sprautestudio.spraute_engine.editor.visual.BlockCodegen;
import org.sprautestudio.spraute_engine.editor.visual.VisualScript;
import org.sprautestudio.spraute_engine.engine.client.ProjectSounds;
import org.sprautestudio.spraute_engine.general.GeneralLang;
import org.sprautestudio.spraute_engine.general.GeneralRunner;
import org.sprautestudio.spraute_engine.gui.GuiDoc;
import org.sprautestudio.spraute_engine.gui.client.MenuRuntime;
import org.sprautestudio.spraute_engine.gui.client.SprauteGuiScreen;
import org.sprautestudio.spraute_engine.lang.Analyzer;
import org.sprautestudio.spraute_engine.platform.ClientHooks;
import org.sprautestudio.spraute_engine.runtime.Eval;
import org.sprautestudio.spraute_engine.runtime.SprauteRuntimeException;
import org.sprautestudio.spraute_engine.runtime.Values;

public final class GeneralRuntime {
    private static final String LOG_SOURCE = "Общий скрипт";
    private static final Map<String, List<GeneralRunner>> RUNNING = new LinkedHashMap<>();
    private static boolean started;
    private static String currentMenu;

    private GeneralRuntime() {
    }

    public static void registerHooks() {
        ClientHooks.CLIENT_TICK.add(e -> tick());
    }

    private static void tick() {
        if (!started) {
            if (Minecraft.getInstance().getOverlay() != null) {
                return;
            }

            started = true;

            for (Workspace.ProjectInfo p : Workspace.projects()) {
                startProject(p.name(), p.dir());
            }

            if (currentMenu != null) {
                menu(currentMenu);
            }
        }

        for (List<GeneralRunner> list : new ArrayList<>(RUNNING.values())) {
            for (GeneralRunner r : new ArrayList<>(list)) {
                r.tick();
            }
        }
    }

    public static void reload(String project) {
        List<GeneralRunner> old = RUNNING.remove(project);
        if (old != null) {
            old.forEach(GeneralRunner::stop);
        }

        started = true;

        for (Workspace.ProjectInfo p : Workspace.projects()) {
            if (p.name().equals(project)) {
                startProject(p.name(), p.dir());
            }
        }

        if (currentMenu != null) {
            for (GeneralRunner r : RUNNING.getOrDefault(project, List.of())) {
                r.menu(currentMenu);
            }
        }
    }

    private static void startProject(String name, Path dir) {
        try {
            Project project = Project.openReadOnly(dir);

            for (Project.FileRef f : project.files()) {
                if (f.kind() == Project.Kind.SCRIPT || f.kind() == Project.Kind.BLOCKS) {
                    String text = source(project, f);
                    if (text != null && GeneralLang.isGeneral(text)) {
                        startScript(name, f.fileName(), text);
                    }
                }
            }
        } catch (RuntimeException | IOException var6) {
            SprauteEngine.LOGGER.warn("Spraute: cannot start the general scripts of {}", name, var6);
        }
    }

    private static String source(Project project, Project.FileRef f) throws IOException {
        String text = project.read(f);
        if (f.kind() == Project.Kind.SCRIPT) {
            return text;
        } else if (!text.contains("общий: с запуска игры")) {
            return null;
        } else {
            BlockCodegen.Result r = BlockCodegen.generate(VisualScript.fromJson(text));
            return r.hasErrors() ? null : r.text;
        }
    }

    private static void startScript(String project, String file, String source) {
        Analyzer.Result analysis = Analyzer.analyze(source);
        if (analysis.hasErrors()) {
            report(project, file + ": не запущен, в нём ошибки");
        } else {
            GeneralRunner runner = new GeneralRunner(analysis.program(), source, new GeneralRuntime.Host(project, file));
            RUNNING.computeIfAbsent(project, k -> new ArrayList<>()).add(runner);
            runner.start();
        }
    }

    private static void report(String project, String message) {
        SprauteEngine.LOGGER.warn("Spraute general script of {}: {}", project, message);
        ClientState.get().addLocal(3, "Общий скрипт", message);
    }

    public static void menu(String name) {
        currentMenu = name;
        if (started) {
            for (List<GeneralRunner> list : RUNNING.values()) {
                for (GeneralRunner r : list) {
                    r.menu(name);
                }
            }
        }
    }

    public static void button(String project, String id) {
        for (GeneralRunner r : RUNNING.getOrDefault(project, List.of())) {
            r.button(id);
        }
    }

    public static boolean listens(String project, String id) {
        for (GeneralRunner r : RUNNING.getOrDefault(project, List.of())) {
            if (r.listens(id)) {
                return true;
            }
        }

        return false;
    }

    private record Host(String project, String file) implements GeneralRunner.Host {
        private static final Random RANDOM = new Random();

        private static String text(Map<String, Object> a, String key) {
            Object v = a.get(key);
            return v == null ? "" : Eval.str(v);
        }

        @Override
        public void command(String key, Map<String, Object> a) {
            Minecraft mc = Minecraft.getInstance();
            switch (key) {
                case "log":
                    SprauteEngine.LOGGER.info("Spraute general script {}: {}", this.file, text(a, "text"));
                    ClientState.get().addLocal(0, "Общий скрипт", this.file + ": " + text(a, "text"));
                    break;
                case "sound":
                    ProjectSounds.ui(this.project, text(a, "sound"));
                    break;
                case "menu open":
                    MenuRuntime.open(text(a, "name"));
                    break;
                case "game quit":
                    mc.stop();
                    break;
                case "world open":
                    String world = text(a, "name");
                    if (!mc.getLevelSource().levelExists(world)) {
                        throw new SprauteRuntimeException("Нет мира «" + world + "»");
                    }

                    if (mc.level != null) {
                        throw new SprauteRuntimeException("Сначала нужно выйти из этого мира");
                    }

                    mc.createWorldOpenFlows().loadLevel((Screen)(mc.screen != null ? mc.screen : new TitleScreen()), world);
                    break;
                case "server join":
                    if (mc.level != null) {
                        throw new SprauteRuntimeException("Сначала нужно выйти из этого мира");
                    }

                    Mc.connect((Screen)(mc.screen != null ? mc.screen : new TitleScreen()), text(a, "address"));
                    break;
                case "link open":
                    String url = text(a, "url");
                    if (!url.startsWith("https://") && !url.startsWith("http://")) {
                        throw new SprauteRuntimeException("Открыть можно только ссылку http или https");
                    }

                    Util.getPlatform().openUri(url);
                    break;
                case "gui open":
                    this.openGui(text(a, "name"));
                    break;
                case "gui close":
                    SprauteGuiScreen s = SprauteGuiScreen.current();
                    if (s != null) {
                        s.onClose();
                    }
                    break;
                case "gui text":
                    this.update(text(a, "el"), "text", text(a, "text"));
                    break;
                case "gui show":
                    this.update(text(a, "el"), "visible", "true");
                    break;
                case "gui hide":
                    this.update(text(a, "el"), "visible", "false");
                    break;
                case "gui progress":
                    this.update(text(a, "el"), "value", text(a, "value"));
                    break;
                case "gui image":
                    this.update(text(a, "el"), "image", text(a, "image"));
                    break;
                case "gui set":
                    this.update(text(a, "el"), text(a, "prop"), text(a, "value"));
                    break;
                default:
                    throw new SprauteRuntimeException("В общем скрипте нет команды " + key);
            }
        }

        private void update(String element, String prop, String value) {
            MenuRuntime.update(this.project, element, prop, value);
            SprauteGuiScreen.updateLocal(this.project, element, prop, value);
        }

        private void openGui(String name) {
            Project p = null;

            for (Workspace.ProjectInfo info : Workspace.projects()) {
                if (info.name().equals(this.project)) {
                    try {
                        p = Project.openReadOnly(info.dir());
                    } catch (IOException var7) {
                        throw new SprauteRuntimeException("Проект не читается: " + var7.getMessage());
                    }
                }
            }

            if (p == null) {
                throw new SprauteRuntimeException("Нет проекта " + this.project);
            } else {
                try {
                    for (Project.FileRef f : p.files()) {
                        if (f.kind() == Project.Kind.GUI && f.name().equals(name)) {
                            Minecraft mc = Minecraft.getInstance();
                            Screen back = mc.screen;
                            SprauteGuiScreen.openLocal(GuiDoc.fromJson(p.read(f)), p, (element, kind) -> {
                                if (kind.equals("click")) {
                                    GeneralRuntime.button(this.project, element);
                                }
                            }, () -> {
                                if (back != null) {
                                    mc.setScreen(back);
                                }
                            });
                            return;
                        }
                    }
                } catch (IOException var8) {
                    throw new SprauteRuntimeException("Интерфейс не читается: " + var8.getMessage());
                }

                throw new SprauteRuntimeException("Интерфейс «" + name + "» не найден в проекте");
            }
        }

        @Override
        public Object function(String name, List<Object> a) {
            return switch (name) {
                case "random" -> {
                    int n = (int)Eval.num(a.get(0));
                    Double var31 = n <= 0 ? 0.0 : RANDOM.nextInt(n);
                    yield var31;
                }
                case "chance" -> {
                    Boolean var30 = RANDOM.nextDouble() * 100.0 < Eval.num(a.get(0));
                    yield var30;
                }
                case "randint" -> {
                    int lo = (int)Math.round(Eval.num(a.get(0)));
                    int hi = (int)Math.round(Eval.num(a.get(1)));
                    Double var29 = (double)(Math.min(lo, hi) + RANDOM.nextInt(Math.abs(hi - lo) + 1));
                    yield var29;
                }
                case "round" -> {
                    Double var28 = (double)Math.round(Eval.num(a.get(0)));
                    yield var28;
                }
                case "floor", "int" -> {
                    Double var27 = Math.floor(Eval.num(a.get(0)));
                    yield var27;
                }
                case "abs" -> {
                    Double var26 = Math.abs(Eval.num(a.get(0)));
                    yield var26;
                }
                case "min" -> {
                    Object var25 = a.size() == 1 ? Values.method(a.get(0), "min", List.of()) : Math.min(Eval.num(a.get(0)), Eval.num(a.get(1)));
                    yield var25;
                }
                case "max" -> {
                    Object var24 = a.size() == 1 ? Values.method(a.get(0), "max", List.of()) : Math.max(Eval.num(a.get(0)), Eval.num(a.get(1)));
                    yield var24;
                }
                case "num" -> {
                    Double var23;
                    try {
                        var23 = Double.parseDouble(Eval.str(a.get(0)).trim().replace(',', '.'));
                    } catch (NumberFormatException var8) {
                        var23 = 0.0;
                        yield var23;
                    }

                    yield var23;
                }
                case "str" -> {
                    String var21 = Eval.str(a.get(0));
                    yield var21;
                }
                case "len" -> {
                    Object var20 = Values.property(a.get(0), "len");
                    yield var20;
                }
                case "range" -> {
                    List var19 = a.size() == 1
                        ? Values.range(0.0, Eval.num(a.get(0)), 1.0)
                        : Values.range(Eval.num(a.get(0)), Eval.num(a.get(1)), a.size() > 2 ? Eval.num(a.get(2)) : 1.0);
                    yield var19;
                }
                case "list" -> a;
                case "has" -> {
                    Object var18 = Values.method(a.get(0), "has", List.of(a.get(1)));
                    yield var18;
                }
                case "indexof" -> {
                    Object var17 = Values.method(a.get(0), "indexOf", List.of(a.get(1)));
                    yield var17;
                }
                case "keys" -> {
                    Object var16 = Values.property(a.get(0), "keys");
                    yield var16;
                }
                case "values" -> {
                    Object var15 = Values.property(a.get(0), "values");
                    yield var15;
                }
                case "sum" -> {
                    Object var14 = Values.method(a.get(0), "sum", List.of());
                    yield var14;
                }
                case "join" -> {
                    String var13 = Values.join(Values.iterable(a.get(0)), a.size() > 1 ? Eval.str(a.get(1)) : ", ");
                    yield var13;
                }
                case "split" -> {
                    List var12 = Values.split(Eval.str(a.get(0)), a.size() > 1 ? Eval.str(a.get(1)) : " ");
                    yield var12;
                }
                case "sorted" -> {
                    List<Object> out = Values.iterable(a.get(0));
                    out.sort(Values::compare);
                    yield out;
                }
                case "pick" -> {
                    List<Object> l = Values.iterable(a.get(0));
                    Object var11 = l.isEmpty() ? "" : l.get(RANDOM.nextInt(l.size()));
                    yield var11;
                }
                case "contains" -> {
                    Boolean var10 = Eval.str(a.get(0)).toLowerCase().contains(Eval.str(a.get(1)).toLowerCase());
                    yield var10;
                }
                case "upper" -> {
                    String var9 = Eval.str(a.get(0)).toUpperCase();
                    yield var9;
                }
                case "lower" -> {
                    String var5 = Eval.str(a.get(0)).toLowerCase();
                    yield var5;
                }
                default -> throw new SprauteRuntimeException("В общем скрипте нет функции " + name);
            };
        }

        @Override
        public String input(String element) {
            SprauteGuiScreen s = SprauteGuiScreen.current();
            String own = s != null ? s.input(element) : null;
            return own != null ? own : MenuRuntime.input(this.project, element);
        }

        @Override
        public boolean guiOpen() {
            return SprauteGuiScreen.current() != null;
        }

        @Override
        public void error(String message) {
            GeneralRuntime.report(this.project, this.file + ": " + message);
        }

        @Override
        public boolean truthy(Object v) {
            return Eval.truthy(v);
        }

        @Override
        public double num(Object v) {
            return Eval.num(v);
        }

        @Override
        public String str(Object v) {
            return Eval.str(v);
        }

        @Override
        public boolean same(Object a, Object b) {
            return Eval.same(a, b);
        }

        @Override
        public int compare(Object a, Object b) {
            return Eval.order(a, b);
        }

        @Override
        public Object index(Object target, Object index) {
            return Values.index(target, index);
        }

        @Override
        public void setIndex(Object target, Object index, Object value) {
            Values.setIndex(target, index, value);
        }

        @Override
        public Object property(Object target, String name) {
            return Values.property(target, name);
        }

        @Override
        public Object method(Object target, String name, List<Object> args) {
            return Values.method(target, name, args);
        }

        @Override
        public List<Object> iterable(Object v) {
            return Values.iterable(v);
        }

        @Override
        public Map<String, Object> map() {
            return Values.map();
        }
    }
}
