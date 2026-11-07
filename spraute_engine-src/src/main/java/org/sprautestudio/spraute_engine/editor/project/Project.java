package org.sprautestudio.spraute_engine.editor.project;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.Map.Entry;
import java.util.stream.Stream;
import net.minecraft.world.phys.Vec3;
import org.sprautestudio.spraute_engine.SprauteEngine;
import org.sprautestudio.spraute_engine.build.StructureDef;
import org.sprautestudio.spraute_engine.camera.CamProfile;
import org.sprautestudio.spraute_engine.core.parser.SpAnimationParser;
import org.sprautestudio.spraute_engine.cutscene.CutsceneDoc;
import org.sprautestudio.spraute_engine.editor.visual.VisualScript;
import org.sprautestudio.spraute_engine.fight.FightDef;
import org.sprautestudio.spraute_engine.fx.FxDef;
import org.sprautestudio.spraute_engine.fx.FxPresets;
import org.sprautestudio.spraute_engine.general.GeneralLang;
import org.sprautestudio.spraute_engine.gui.GuiDoc;
import org.sprautestudio.spraute_engine.lang.Analyzer;
import org.sprautestudio.spraute_engine.lang.Parser;
import org.sprautestudio.spraute_engine.path.PathDef;
import org.sprautestudio.spraute_engine.post.PostPresets;
import org.sprautestudio.spraute_engine.post.PostProfile;
import org.sprautestudio.spraute_engine.quest.QuestDef;
import org.sprautestudio.spraute_engine.quest.QuestGoals;
import org.sprautestudio.spraute_engine.runtime.models.EntityLibrary;
import org.sprautestudio.spraute_engine.shop.ShopDef;
import org.sprautestudio.spraute_engine.skills.SkillTree;
import org.sprautestudio.spraute_engine.stealth.StealthDef;
import org.sprautestudio.spraute_engine.zone.ZoneDef;

public final class Project {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    public final Path dir;
    public Project.Meta meta;
    public static final List<String> IMAGE_EXTENSIONS = List.of(".png", ".jpg", ".jpeg", ".bmp", ".gif");
    public static final List<String> SOUND_EXTENSIONS = List.of(".ogg", ".wav");
    public static final List<String> VIDEO_EXTENSIONS = List.of(".mp4", ".webm", ".mkv", ".mov", ".m4v", ".avi");

    private Project(Path dir, Project.Meta meta) {
        this.dir = dir;
        this.meta = meta;
    }

    public String name() {
        return this.meta.name;
    }

    public Path scriptsDir() {
        return this.dir.resolve("scripts");
    }

    public Path blocksDir() {
        return this.dir.resolve("blocks");
    }

    public Path guisDir() {
        return this.dir.resolve("guis");
    }

    public Map<String, String> guiSources() {
        Map<String, String> map = new LinkedHashMap<>();

        for (Project.FileRef f : this.files()) {
            if (f.kind() == Project.Kind.GUI) {
                try {
                    map.put(f.name(), this.read(f));
                } catch (IOException var5) {
                }
            }
        }

        return map;
    }

    public Path cutscenesDir() {
        return this.dir.resolve("cutscenes");
    }

    public Path fightsDir() {
        return this.dir.resolve("fights");
    }

    public Path pathsDir() {
        return this.dir.resolve("paths");
    }

    public List<String> pathNames() {
        List<String> list = new ArrayList<>();

        for (Project.FileRef f : this.files()) {
            if (f.kind() == Project.Kind.PATH) {
                list.add(f.name());
            }
        }

        return list;
    }

    public Project.FileRef newPath(String name, Vec3 at) throws IOException {
        Files.createDirectories(this.pathsDir());
        Project.FileRef f = new Project.FileRef(this.pathsDir().resolve(sanitize(name) + Project.Kind.PATH.ext), Project.Kind.PATH);
        if (Files.exists(f.path())) {
            throw new IOException("Файл уже существует");
        } else {
            this.write(f, PathDef.example(at).toJson());
            return f;
        }
    }

    public List<String> fightNames() {
        List<String> list = new ArrayList<>();

        for (Project.FileRef f : this.files()) {
            if (f.kind() == Project.Kind.FIGHT) {
                list.add(f.name());
            }
        }

        return list;
    }

    public Path skillsDir() {
        return this.dir.resolve("skills");
    }

    public List<String> skillNames() {
        List<String> list = new ArrayList<>();

        for (Project.FileRef f : this.files()) {
            if (f.kind() == Project.Kind.SKILLS) {
                list.add(f.name());
            }
        }

        return list;
    }

    public List<String> skillNodeIds() {
        List<String> list = new ArrayList<>();

        for (Project.FileRef f : this.files()) {
            if (f.kind() == Project.Kind.SKILLS) {
                try {
                    for (SkillTree.Node n : SkillTree.fromJson(this.read(f)).nodes) {
                        if (!list.contains(n.id)) {
                            list.add(n.id);
                        }
                    }
                } catch (IOException var6) {
                }
            }
        }

        return list;
    }

    public List<String> skillMenuIds() {
        List<String> list = new ArrayList<>();

        for (Project.FileRef f : this.files()) {
            if (f.kind() == Project.Kind.SKILLS) {
                try {
                    for (SkillTree.Menu m : SkillTree.fromJson(this.read(f)).menus) {
                        if (!list.contains(m.id)) {
                            list.add(m.id);
                        }
                    }
                } catch (IOException var6) {
                }
            }
        }

        return list;
    }

    public Project.FileRef newSkills(String name) throws IOException {
        Files.createDirectories(this.skillsDir());
        Project.FileRef f = new Project.FileRef(this.skillsDir().resolve(sanitize(name) + Project.Kind.SKILLS.ext), Project.Kind.SKILLS);
        if (Files.exists(f.path())) {
            throw new IOException("Файл уже существует");
        } else {
            SkillTree tree = SkillTree.preset("classic");
            tree.title = name;
            this.write(f, tree.toJson());
            return f;
        }
    }

    public Path stealthsDir() {
        return this.dir.resolve("stealth");
    }

    public List<String> stealthNames() {
        List<String> list = new ArrayList<>();

        for (Project.FileRef f : this.files()) {
            if (f.kind() == Project.Kind.STEALTH) {
                list.add(f.name());
            }
        }

        return list;
    }

    public Project.FileRef newStealth(String name) throws IOException {
        Files.createDirectories(this.stealthsDir());
        Project.FileRef f = new Project.FileRef(this.stealthsDir().resolve(sanitize(name) + Project.Kind.STEALTH.ext), Project.Kind.STEALTH);
        if (Files.exists(f.path())) {
            throw new IOException("Файл уже существует");
        } else {
            this.write(f, StealthDef.example(name).toJson());
            return f;
        }
    }

    public Project.FileRef newFight(String name) throws IOException {
        Files.createDirectories(this.fightsDir());
        Project.FileRef f = new Project.FileRef(this.fightsDir().resolve(sanitize(name) + Project.Kind.FIGHT.ext), Project.Kind.FIGHT);
        if (Files.exists(f.path())) {
            throw new IOException("Файл уже существует");
        } else {
            this.write(f, FightDef.example(name).toJson());
            return f;
        }
    }

    public Path kindDir(Project.Kind k) {
        return switch (k) {
            case SCRIPT -> this.scriptsDir();
            case BLOCKS -> this.blocksDir();
            case GUI -> this.guisDir();
            case CUTSCENE -> this.cutscenesDir();
            case EFFECT -> this.effectsDir();
            case SHOP -> this.shopsDir();
            case QUEST -> this.questsDir();
            case ZONE -> this.zonesDir();
            case FIGHT -> this.fightsDir();
            case STEALTH -> this.stealthsDir();
            case SKILLS -> this.skillsDir();
            case PATH -> this.pathsDir();
            case POST -> this.postsDir();
            case CAMERA -> this.camerasDir();
            case STRUCTURE -> this.structuresDir();
        };
    }

    public List<Path> subfolders(Project.Kind k) {
        List<Path> out = new ArrayList<>();
        Path root = this.kindDir(k);
        if (!Files.isDirectory(root)) {
            return out;
        } else {
            try (Stream<Path> s = Files.walk(root)) {
                s.filter(p -> !p.equals(root) && Files.isDirectory(p))
                    .sorted(Comparator.comparing(p -> root.relativize(p).toString().toLowerCase()))
                    .forEach(out::add);
            } catch (IOException var9) {
            }

            return out;
        }
    }

    public Project.FileRef moveTo(Project.FileRef f, Path dir) throws IOException {
        Files.createDirectories(dir);
        Path to = dir.resolve(f.path().getFileName());
        if (Files.exists(to)) {
            throw new IOException("Там уже есть файл «" + f.fileName() + "»");
        } else {
            Files.move(f.path(), to);
            return new Project.FileRef(to, f.kind());
        }
    }

    public Path zonesDir() {
        return this.dir.resolve("zones");
    }

    public List<String> zoneNames() {
        List<String> list = new ArrayList<>();

        for (Project.FileRef f : this.files()) {
            if (f.kind() == Project.Kind.ZONE) {
                list.add(f.name());
            }
        }

        return list;
    }

    public Map<String, String> zoneSources() {
        Map<String, String> m = new LinkedHashMap<>();

        for (Project.FileRef f : this.files()) {
            if (f.kind() == Project.Kind.ZONE) {
                try {
                    m.put(f.name(), this.read(f));
                } catch (IOException var5) {
                }
            }
        }

        return m;
    }

    public Project.FileRef newZone(String name, Vec3 at) throws IOException {
        Files.createDirectories(this.zonesDir());
        Project.FileRef f = new Project.FileRef(this.zonesDir().resolve(sanitize(name) + Project.Kind.ZONE.ext), Project.Kind.ZONE);
        if (Files.exists(f.path())) {
            throw new IOException("Файл уже существует");
        } else {
            this.write(f, ZoneDef.example(name, at).toJson());
            return f;
        }
    }

    public Path questsDir() {
        return this.dir.resolve("quests");
    }

    public List<String> questNames() {
        List<String> list = new ArrayList<>();

        for (Project.FileRef f : this.files()) {
            if (f.kind() == Project.Kind.QUEST) {
                list.add(f.name());
            }
        }

        return list;
    }

    public Map<String, String> questTasks(String name) {
        Map<String, String> out = new LinkedHashMap<>();

        for (Project.FileRef f : this.files()) {
            if (f.kind() == Project.Kind.QUEST && f.name().equals(name)) {
                try {
                    for (QuestDef.Task t : QuestDef.fromJson(this.read(f)).tasks) {
                        out.put(t.id, QuestGoals.text(t, QuestGoals::shortId));
                    }
                } catch (IOException var7) {
                }
            }
        }

        return out;
    }

    public List<String> characterNames() {
        Set<String> names = new TreeSet<>();

        for (Project.FileRef f : this.files()) {
            try {
                if (f.kind() == Project.Kind.SCRIPT) {
                    names.addAll(Analyzer.analyze(this.read(f)).characters.keySet());
                } else if (f.kind() == Project.Kind.BLOCKS) {
                    for (VisualScript.Stack s : VisualScript.fromJson(this.read(f)).stacks) {
                        collectCharacters(s.blocks, names);
                    }
                }
            } catch (RuntimeException | IOException var6) {
            }
        }

        return new ArrayList<>(names);
    }

    private static void collectCharacters(List<VisualScript.Block> blocks, Set<String> out) {
        for (VisualScript.Block b : blocks) {
            if (b.type.equals("character") && !b.field("name").isBlank()) {
                out.add(b.field("name").trim());
            }

            for (List<VisualScript.Block> slot : b.slots) {
                collectCharacters(slot, out);
            }
        }
    }

    public Project.FileRef newQuest(String name) throws IOException {
        Files.createDirectories(this.questsDir());
        Project.FileRef f = new Project.FileRef(this.questsDir().resolve(sanitize(name) + Project.Kind.QUEST.ext), Project.Kind.QUEST);
        if (Files.exists(f.path())) {
            throw new IOException("Файл уже существует");
        } else {
            this.write(f, QuestDef.example(name).toJson());
            return f;
        }
    }

    public Path shopsDir() {
        return this.dir.resolve("shops");
    }

    public List<String> shopNames() {
        List<String> list = new ArrayList<>();

        for (Project.FileRef f : this.files()) {
            if (f.kind() == Project.Kind.SHOP) {
                list.add(f.name());
            }
        }

        return list;
    }

    public Project.FileRef newShop(String name) throws IOException {
        Files.createDirectories(this.shopsDir());
        Project.FileRef f = new Project.FileRef(this.shopsDir().resolve(sanitize(name) + Project.Kind.SHOP.ext), Project.Kind.SHOP);
        if (Files.exists(f.path())) {
            throw new IOException("Файл уже существует");
        } else {
            this.write(f, ShopDef.example(name).toJson());
            return f;
        }
    }

    public Path structuresDir() {
        return this.dir.resolve("structures");
    }

    public List<String> structureNames() {
        List<String> list = new ArrayList<>();

        for (Project.FileRef f : this.files()) {
            if (f.kind() == Project.Kind.STRUCTURE) {
                list.add(f.name());
            }
        }

        return list;
    }

    public Project.FileRef newStructure(String name, StructureDef def) throws IOException {
        Files.createDirectories(this.structuresDir());
        String base = sanitize(name);
        String free = base;

        for (int i = 2; Files.exists(this.structuresDir().resolve(free + Project.Kind.STRUCTURE.ext)); i++) {
            free = base + " " + i;
        }

        Project.FileRef f = new Project.FileRef(this.structuresDir().resolve(free + Project.Kind.STRUCTURE.ext), Project.Kind.STRUCTURE);
        this.write(f, def.toJson());
        return f;
    }

    public Path camerasDir() {
        return this.dir.resolve("cameras");
    }

    public List<String> cameraNames() {
        List<String> list = new ArrayList<>();

        for (Project.FileRef f : this.files()) {
            if (f.kind() == Project.Kind.CAMERA) {
                list.add(f.name());
            }
        }

        return list;
    }

    public Project.FileRef newCamera(String name, String preset) throws IOException {
        Files.createDirectories(this.camerasDir());
        Project.FileRef f = new Project.FileRef(this.camerasDir().resolve(sanitize(name) + Project.Kind.CAMERA.ext), Project.Kind.CAMERA);
        if (Files.exists(f.path())) {
            throw new IOException("Файл уже существует");
        } else {
            CamProfile.Preset ready = CamProfile.preset(preset);
            this.write(f, (ready != null ? ready.profile() : new CamProfile()).toJson());
            return f;
        }
    }

    public Path postsDir() {
        return this.dir.resolve("posts");
    }

    public List<String> postNames() {
        List<String> list = new ArrayList<>();

        for (Project.FileRef f : this.files()) {
            if (f.kind() == Project.Kind.POST) {
                list.add(f.name());
            }
        }

        return list;
    }

    public Project.FileRef newPost(String name, String preset) throws IOException {
        Files.createDirectories(this.postsDir());
        Project.FileRef f = new Project.FileRef(this.postsDir().resolve(sanitize(name) + Project.Kind.POST.ext), Project.Kind.POST);
        if (Files.exists(f.path())) {
            throw new IOException("Файл уже существует");
        } else {
            PostPresets.Preset ready = PostPresets.get(preset);
            this.write(f, (ready != null ? ready.profile() : new PostProfile()).toJson());
            return f;
        }
    }

    public Path effectsDir() {
        return this.dir.resolve("effects");
    }

    public List<String> effectNames() {
        List<String> list = new ArrayList<>();

        for (Project.FileRef f : this.files()) {
            if (f.kind() == Project.Kind.EFFECT) {
                list.add(f.name());
            }
        }

        return list;
    }

    public Project.FileRef newEffect(String name, String preset) throws IOException {
        Files.createDirectories(this.effectsDir());
        Project.FileRef f = new Project.FileRef(this.effectsDir().resolve(sanitize(name) + Project.Kind.EFFECT.ext), Project.Kind.EFFECT);
        if (Files.exists(f.path())) {
            throw new IOException("Файл уже существует");
        } else {
            FxDef d = FxPresets.get(preset);
            this.write(f, (d != null ? d : new FxDef()).toJson());
            return f;
        }
    }

    public List<String> cutsceneNames() {
        List<String> list = new ArrayList<>();

        for (Project.FileRef f : this.files()) {
            if (f.kind() == Project.Kind.CUTSCENE) {
                list.add(f.name());
            }
        }

        return list;
    }

    public Project.FileRef newCutscene(String name) throws IOException {
        Files.createDirectories(this.cutscenesDir());
        Project.FileRef f = new Project.FileRef(this.cutscenesDir().resolve(sanitize(name) + Project.Kind.CUTSCENE.ext), Project.Kind.CUTSCENE);
        if (Files.exists(f.path())) {
            throw new IOException("Файл уже существует");
        } else {
            this.write(f, CutsceneDoc.example(name).toJson());
            return f;
        }
    }

    public Map<String, String> deploySources() {
        Map<String, String> map = this.guiSources();

        for (Project.FileRef f : this.files()) {
            if (f.kind() == Project.Kind.CUTSCENE
                || f.kind() == Project.Kind.EFFECT
                || f.kind() == Project.Kind.SHOP
                || f.kind() == Project.Kind.QUEST
                || f.kind() == Project.Kind.ZONE
                || f.kind() == Project.Kind.FIGHT
                || f.kind() == Project.Kind.STEALTH
                || f.kind() == Project.Kind.SKILLS
                || f.kind() == Project.Kind.PATH
                || f.kind() == Project.Kind.POST
                || f.kind() == Project.Kind.CAMERA
                || f.kind() == Project.Kind.STRUCTURE) {
                try {
                    map.put(
                        (
                                f.kind() == Project.Kind.EFFECT
                                    ? "fx:"
                                    : (
                                        f.kind() == Project.Kind.SHOP
                                            ? "shop:"
                                            : (
                                                f.kind() == Project.Kind.QUEST
                                                    ? "quest:"
                                                    : (
                                                        f.kind() == Project.Kind.ZONE
                                                            ? "zone:"
                                                            : (
                                                                f.kind() == Project.Kind.FIGHT
                                                                    ? "fight:"
                                                                    : (
                                                                        f.kind() == Project.Kind.STEALTH
                                                                            ? "stealth:"
                                                                            : (
                                                                                f.kind() == Project.Kind.SKILLS
                                                                                    ? "skills:"
                                                                                    : (
                                                                                        f.kind() == Project.Kind.PATH
                                                                                            ? "path:"
                                                                                            : (
                                                                                                f.kind() == Project.Kind.POST
                                                                                                    ? "post:"
                                                                                                    : (
                                                                                                        f.kind() == Project.Kind.CAMERA
                                                                                                            ? "cam:"
                                                                                                            : (
                                                                                                                f.kind() == Project.Kind.STRUCTURE
                                                                                                                    ? "build:"
                                                                                                                    : "cutscene:"
                                                                                                            )
                                                                                                    )
                                                                                            )
                                                                                    )
                                                                            )
                                                                    )
                                                            )
                                                    )
                                            )
                                    )
                            )
                            + f.name(),
                        this.read(f)
                    );
                } catch (IOException var5) {
                }
            }
        }

        return map;
    }

    public List<String> guiNames() {
        List<String> list = new ArrayList<>();

        for (Project.FileRef f : this.files()) {
            if (f.kind() == Project.Kind.GUI) {
                list.add(f.name());
            }
        }

        return list;
    }

    public Project.FileRef newGui(String name) throws IOException {
        Project.FileRef f = new Project.FileRef(this.guisDir().resolve(sanitize(name) + Project.Kind.GUI.ext), Project.Kind.GUI);
        if (Files.exists(f.path())) {
            throw new IOException("Файл уже существует");
        } else {
            this.write(f, GuiDoc.example(name).toJson());
            return f;
        }
    }

    public static Project create(Path dir, String name) throws IOException {
        Files.createDirectories(dir.resolve("scripts"));
        Files.createDirectories(dir.resolve("blocks"));
        Files.createDirectories(dir.resolve("sounds"));
        Project.Meta m = new Project.Meta();
        m.name = name;
        m.created = m.opened = System.currentTimeMillis();
        Project p = new Project(dir, m);
        p.saveMeta();
        return p;
    }

    public static Project open(Path dir) throws IOException {
        Path metaFile = dir.resolve("project.json");
        Project.Meta m = Files.exists(metaFile) ? GSON.fromJson(Files.readString(metaFile, StandardCharsets.UTF_8), Project.Meta.class) : null;
        if (m == null) {
            m = new Project.Meta();
            m.name = dir.getFileName().toString();
            m.created = System.currentTimeMillis();
        }

        if (m.openFiles == null) {
            m.openFiles = new ArrayList<>();
        }

        Files.createDirectories(dir.resolve("scripts"));
        Files.createDirectories(dir.resolve("blocks"));
        Files.createDirectories(dir.resolve("sounds"));
        m.opened = System.currentTimeMillis();
        Project p = new Project(dir, m);
        p.saveMeta();
        return p;
    }

    public static Project openReadOnly(Path dir) throws IOException {
        Path metaFile = dir.resolve("project.json");
        Project.Meta m = Files.exists(metaFile) ? GSON.fromJson(Files.readString(metaFile, StandardCharsets.UTF_8), Project.Meta.class) : null;
        if (m == null) {
            m = new Project.Meta();
            m.name = dir.getFileName().toString();
        }

        if (m.openFiles == null) {
            m.openFiles = new ArrayList<>();
        }

        return new Project(dir, m);
    }

    public void saveMeta() {
        try {
            Files.writeString(this.dir.resolve("project.json"), GSON.toJson(this.meta), StandardCharsets.UTF_8);
        } catch (IOException var2) {
            SprauteEngine.LOGGER.warn("Failed to save project meta", (Throwable)var2);
        }
    }

    public List<Project.FileRef> files() {
        List<Project.FileRef> list = new ArrayList<>();
        collect(this.scriptsDir(), ".sst", Project.Kind.SCRIPT, list);
        collect(this.blocksDir(), ".svs", Project.Kind.BLOCKS, list);
        collect(this.guisDir(), Project.Kind.GUI.ext, Project.Kind.GUI, list);
        collect(this.cutscenesDir(), Project.Kind.CUTSCENE.ext, Project.Kind.CUTSCENE, list);
        collect(this.effectsDir(), Project.Kind.EFFECT.ext, Project.Kind.EFFECT, list);
        collect(this.postsDir(), Project.Kind.POST.ext, Project.Kind.POST, list);
        collect(this.camerasDir(), Project.Kind.CAMERA.ext, Project.Kind.CAMERA, list);
        collect(this.structuresDir(), Project.Kind.STRUCTURE.ext, Project.Kind.STRUCTURE, list);
        collect(this.shopsDir(), Project.Kind.SHOP.ext, Project.Kind.SHOP, list);
        collect(this.questsDir(), Project.Kind.QUEST.ext, Project.Kind.QUEST, list);
        collect(this.zonesDir(), Project.Kind.ZONE.ext, Project.Kind.ZONE, list);
        collect(this.fightsDir(), Project.Kind.FIGHT.ext, Project.Kind.FIGHT, list);
        collect(this.stealthsDir(), Project.Kind.STEALTH.ext, Project.Kind.STEALTH, list);
        collect(this.skillsDir(), Project.Kind.SKILLS.ext, Project.Kind.SKILLS, list);
        collect(this.pathsDir(), Project.Kind.PATH.ext, Project.Kind.PATH, list);
        return list;
    }

    public Path entitiesDir() {
        return this.dir.resolve("entities");
    }

    public List<Project.EntityRef> entities() {
        List<Project.EntityRef> list = new ArrayList<>();
        if (!Files.isDirectory(this.entitiesDir())) {
            return list;
        } else {
            try (Stream<Path> s = Files.list(this.entitiesDir())) {
                s.filter(p -> p.toString().endsWith(".sent")).sorted().forEach(p -> list.add(new Project.EntityRef(p)));
            } catch (IOException var7) {
            }

            return list;
        }
    }

    public Project.EntityRef newEntity(String name, String model) throws IOException {
        Files.createDirectories(this.entitiesDir());
        Path f = this.entitiesDir().resolve(sanitize(name) + ".sent");
        if (Files.exists(f)) {
            throw new IOException("Такая сущность уже есть");
        } else {
            EntityLibrary.Def d = new EntityLibrary.Def();
            if (model != null) {
                d.model = model;
                d.texture = "model";
            }

            Files.writeString(f, d.toJson(), StandardCharsets.UTF_8);
            return new Project.EntityRef(f);
        }
    }

    public Path modelsDir() {
        return this.dir.resolve("models");
    }

    public Path animationsDir() {
        return this.dir.resolve("animations");
    }

    public List<Path> animationFiles() {
        List<Path> list = new ArrayList<>();
        if (!Files.isDirectory(this.animationsDir())) {
            return list;
        } else {
            try (Stream<Path> s = Files.list(this.animationsDir())) {
                s.filter(Project::isAnimationFile).sorted().forEach(list::add);
            } catch (IOException var7) {
            }

            return list;
        }
    }

    public static boolean isAnimationFile(Path p) {
        String n = p.getFileName().toString().toLowerCase();
        return n.endsWith(".animation.json") || n.endsWith(".animations.json");
    }

    public int importAnimations(List<Path> files) throws IOException {
        int done = 0;

        for (Path f : files) {
            if (isAnimationFile(f)) {
                try (InputStream in = Files.newInputStream(f)) {
                    SpAnimationParser.parse(in);
                } catch (RuntimeException var11) {
                    throw new IOException("«" + f.getFileName() + "» — не файл анимаций Blockbench: " + var11.getMessage(), var11);
                }

                String n = f.getFileName().toString();
                Path target = this.animationsDir();

                for (Project.ModelRef m : this.models()) {
                    if (n.toLowerCase().startsWith(m.name().toLowerCase() + ".")) {
                        target = m.dir();
                    }
                }

                Files.createDirectories(target);
                Files.copy(f, target.resolve(sanitize(n)), StandardCopyOption.REPLACE_EXISTING);
                done++;
            }
        }

        return done;
    }

    public List<Project.ModelRef> models() {
        List<Project.ModelRef> list = new ArrayList<>();
        if (!Files.isDirectory(this.modelsDir())) {
            return list;
        } else {
            try (Stream<Path> s = Files.list(this.modelsDir())) {
                s.filter(x$0 -> Files.isDirectory(x$0)).sorted().forEach(p -> list.add(new Project.ModelRef(p)));
            } catch (IOException var7) {
            }

            return list;
        }
    }

    public static boolean isModelFile(Path p) {
        String n = p.getFileName().toString().toLowerCase();
        return n.endsWith(".geo.json") || n.endsWith(".animation.json") || n.endsWith(".animations.json");
    }

    public List<Project.ModelRef> importModels(List<Path> files) throws IOException {
        List<Path> geos = new ArrayList<>();
        List<Path> others = new ArrayList<>();

        for (Path f : files) {
            String n = f.getFileName().toString().toLowerCase();
            if (n.endsWith(".geo.json")) {
                geos.add(f);
            } else if (isModelFile(f) || n.endsWith(".png")) {
                others.add(f);
            }
        }

        List<Project.ModelRef> made = new ArrayList<>();
        Map<String, Path> dirs = new LinkedHashMap<>();

        for (Path g : geos) {
            String base = g.getFileName().toString();
            base = base.substring(0, base.length() - ".geo.json".length());
            if (base.startsWith("geometry.")) {
                base = base.substring(9);
            }

            Path d = this.modelsDir().resolve(sanitize(base));
            Files.createDirectories(d);

            try (Stream<Path> old = Files.list(d)) {
                for (Path o : (Iterable<Path>) old::iterator) {
                    if (o.getFileName().toString().toLowerCase().endsWith(".geo.json")) {
                        Files.delete(o);
                    }
                }
            }

            Files.copy(g, d.resolve(g.getFileName()), StandardCopyOption.REPLACE_EXISTING);
            dirs.put(base.toLowerCase(), d);
            made.add(new Project.ModelRef(d));
        }

        if (dirs.isEmpty()) {
            for (Project.ModelRef m : this.models()) {
                dirs.put(m.name().toLowerCase(), m.dir());
            }
        }

        for (Path o : others) {
            String n = o.getFileName().toString().toLowerCase();
            Path target = null;

            for (Entry<String, Path> e : dirs.entrySet()) {
                if (n.startsWith(e.getKey())) {
                    target = e.getValue();
                }
            }

            if (target == null && geos.size() == 1) {
                target = dirs.values().iterator().next();
            }

            if (target != null) {
                if (n.endsWith(".png")) {
                    try (Stream<Path> old = Files.list(target)) {
                        for (Path p : (Iterable<Path>) old::iterator) {
                            if (p.getFileName().toString().toLowerCase().endsWith(".png")) {
                                Files.delete(p);
                            }
                        }
                    }
                }

                Files.copy(o, target.resolve(o.getFileName()), StandardCopyOption.REPLACE_EXISTING);
                if (made.stream().noneMatch(m -> m.dir().equals(o))) {
                    Path t = target;
                    if (made.stream().noneMatch(m -> m.dir().equals(t))) {
                        made.add(new Project.ModelRef(t));
                    }
                }
            }
        }

        return made;
    }

    public Path soundsDir() {
        return this.dir.resolve("sounds");
    }

    public static boolean isSound(Path p) {
        String n = p.getFileName().toString().toLowerCase();

        for (String ext : SOUND_EXTENSIONS) {
            if (n.endsWith(ext)) {
                return true;
            }
        }

        return false;
    }

    public List<Project.SoundRef> sounds() {
        List<Project.SoundRef> list = new ArrayList<>();
        if (!Files.isDirectory(this.soundsDir())) {
            return list;
        } else {
            try (Stream<Path> s = Files.list(this.soundsDir())) {
                s.filter(Project::isSound)
                    .sorted(Comparator.comparing(p -> p.getFileName().toString().toLowerCase()))
                    .forEach(p -> list.add(new Project.SoundRef(p)));
            } catch (IOException var7) {
                SprauteEngine.LOGGER.warn("Failed to list {}", this.soundsDir(), var7);
            }

            return list;
        }
    }

    public List<String> soundNames() {
        List<String> out = new ArrayList<>();

        for (Project.SoundRef r : this.sounds()) {
            out.add(r.name());
        }

        return out;
    }

    public Project.SoundRef importSound(Path source) throws IOException {
        Files.createDirectories(this.soundsDir());
        String file = source.getFileName().toString();
        int dot = file.lastIndexOf(46);
        String base = sanitize(dot > 0 ? file.substring(0, dot) : file);
        String ext = dot > 0 ? file.substring(dot).toLowerCase() : ".ogg";
        Path target = this.soundsDir().resolve(base + ext);
        int n = 2;

        while (Files.exists(target)) {
            target = this.soundsDir().resolve(base + " " + n++ + ext);
        }

        Files.copy(source, target);
        return new Project.SoundRef(target);
    }

    public Project.SoundRef renameSound(Project.SoundRef ref, String newName) throws IOException {
        String file = ref.path().getFileName().toString();
        String ext = file.substring(file.lastIndexOf(46));
        Path target = this.soundsDir().resolve(sanitize(newName) + ext);
        if (Files.exists(target)) {
            throw new IOException("Звук с таким именем уже есть");
        } else {
            Files.move(ref.path(), target);
            return new Project.SoundRef(target);
        }
    }

    public Path videosDir() {
        return this.dir.resolve("videos");
    }

    public static boolean isVideo(Path p) {
        String n = p.getFileName().toString().toLowerCase();

        for (String ext : VIDEO_EXTENSIONS) {
            if (n.endsWith(ext)) {
                return true;
            }
        }

        return false;
    }

    public List<Project.VideoRef> videos() {
        List<Project.VideoRef> list = new ArrayList<>();
        if (!Files.isDirectory(this.videosDir())) {
            return list;
        } else {
            try (Stream<Path> s = Files.list(this.videosDir())) {
                s.filter(Project::isVideo)
                    .sorted(Comparator.comparing(p -> p.getFileName().toString().toLowerCase()))
                    .forEach(p -> list.add(new Project.VideoRef(p)));
            } catch (IOException var7) {
                SprauteEngine.LOGGER.warn("Failed to list {}", this.videosDir(), var7);
            }

            return list;
        }
    }

    public static Path findVideo(Path folder, String name) {
        if (!Files.isDirectory(folder)) {
            return null;
        } else {
            try {
                Path var3;
                try (Stream<Path> s = Files.list(folder)) {
                    var3 = s.filter(Project::isVideo).filter(p -> new Project.VideoRef(p).name().equals(name)).findFirst().orElse(null);
                }

                return var3;
            } catch (IOException var7) {
                return null;
            }
        }
    }

    public Project.VideoRef importVideo(Path source) throws IOException {
        Files.createDirectories(this.videosDir());
        String file = source.getFileName().toString();
        int dot = file.lastIndexOf(46);
        String base = sanitize(dot > 0 ? file.substring(0, dot) : file);
        String ext = dot > 0 ? file.substring(dot).toLowerCase() : ".mp4";
        Path target = this.videosDir().resolve(base + ext);
        int n = 2;

        while (Files.exists(target)) {
            target = this.videosDir().resolve(base + " " + n++ + ext);
        }

        Files.copy(source, target);
        return new Project.VideoRef(target);
    }

    public Project.VideoRef renameVideo(Project.VideoRef ref, String newName) throws IOException {
        String file = ref.path().getFileName().toString();
        String ext = file.substring(file.lastIndexOf(46));
        Path target = this.videosDir().resolve(sanitize(newName) + ext);
        if (Files.exists(target)) {
            throw new IOException("Видео с таким именем уже есть");
        } else {
            Files.move(ref.path(), target);
            return new Project.VideoRef(target);
        }
    }

    public Path imagesDir() {
        return this.dir.resolve("images");
    }

    public static boolean isImage(Path p) {
        String n = p.getFileName().toString().toLowerCase();

        for (String ext : IMAGE_EXTENSIONS) {
            if (n.endsWith(ext)) {
                return true;
            }
        }

        return false;
    }

    public List<Project.ImageRef> images() {
        List<Project.ImageRef> list = new ArrayList<>();
        if (!Files.isDirectory(this.imagesDir())) {
            return list;
        } else {
            try (Stream<Path> s = Files.list(this.imagesDir())) {
                s.filter(Project::isImage)
                    .sorted(Comparator.comparing(p -> p.getFileName().toString().toLowerCase()))
                    .forEach(p -> list.add(new Project.ImageRef(p)));
            } catch (IOException var7) {
                SprauteEngine.LOGGER.warn("Failed to list {}", this.imagesDir(), var7);
            }

            return list;
        }
    }

    public Project.ImageRef image(String name) {
        for (Project.ImageRef r : this.images()) {
            if (r.name().equals(name)) {
                return r;
            }
        }

        return null;
    }

    public Project.ImageRef importImage(Path source) throws IOException {
        Files.createDirectories(this.imagesDir());
        String file = source.getFileName().toString();
        int dot = file.lastIndexOf(46);
        String base = sanitize(dot > 0 ? file.substring(0, dot) : file);
        String ext = dot > 0 ? file.substring(dot).toLowerCase() : ".png";
        Path target = this.imagesDir().resolve(base + ext);
        int n = 2;

        while (Files.exists(target)) {
            target = this.imagesDir().resolve(base + " " + n++ + ext);
        }

        Files.copy(source, target);
        return new Project.ImageRef(target);
    }

    public Project.ImageRef renameImage(Project.ImageRef ref, String newName) throws IOException {
        String file = ref.path().getFileName().toString();
        String ext = file.substring(file.lastIndexOf(46));
        Path target = this.imagesDir().resolve(sanitize(newName) + ext);
        if (Files.exists(target)) {
            throw new IOException("Картинка с таким именем уже есть");
        } else {
            Files.move(ref.path(), target);
            return new Project.ImageRef(target);
        }
    }

    private static void collect(Path dir, String ext, Project.Kind kind, List<Project.FileRef> out) {
        if (Files.isDirectory(dir)) {
            try (Stream<Path> s = Files.walk(dir)) {
                s.filter(p -> Files.isRegularFile(p) && p.toString().endsWith(ext))
                    .sorted(Comparator.comparing(p -> dir.relativize(p).toString().toLowerCase()))
                    .forEach(p -> out.add(new Project.FileRef(p, kind)));
            } catch (IOException var9) {
                SprauteEngine.LOGGER.warn("Failed to list {}", dir, var9);
            }
        }
    }

    public String read(Project.FileRef f) throws IOException {
        return Files.readString(f.path(), StandardCharsets.UTF_8);
    }

    public void write(Project.FileRef f, String content) throws IOException {
        Files.createDirectories(f.path().getParent());
        Files.writeString(f.path(), content, StandardCharsets.UTF_8);
    }

    public int nextFreeId() {
        List<Integer> used = new ArrayList<>();

        for (Project.FileRef f : this.files()) {
            if (f.kind() != Project.Kind.GUI
                && f.kind() != Project.Kind.CUTSCENE
                && f.kind() != Project.Kind.EFFECT
                && f.kind() != Project.Kind.SHOP
                && f.kind() != Project.Kind.QUEST
                && f.kind() != Project.Kind.ZONE
                && f.kind() != Project.Kind.FIGHT
                && f.kind() != Project.Kind.STEALTH
                && f.kind() != Project.Kind.SKILLS
                && f.kind() != Project.Kind.PATH
                && f.kind() != Project.Kind.POST
                && f.kind() != Project.Kind.CAMERA
                && f.kind() != Project.Kind.STRUCTURE) {
                try {
                    String text = this.read(f);
                    if (f.kind() == Project.Kind.SCRIPT) {
                        Parser.Result r = Parser.parse(text);
                        if (r.program.header != null) {
                            used.add(r.program.header.id);
                        }
                    } else {
                        VisualScript vs = VisualScript.fromJson(text);
                        VisualScript.Stack main = vs.mainStack();
                        if (main != null) {
                            used.add(Integer.parseInt(main.blocks.get(0).field("id").trim()));
                        }
                    }
                } catch (Exception var7) {
                }
            }
        }

        int id = this.meta.baseId;

        while (used.contains(id)) {
            id++;
        }

        return id;
    }

    public static String sanitize(String name) {
        String s = name.trim().replaceAll("[\\\\/:*?\"<>|]", "_");
        return s.isEmpty() ? "untitled" : s;
    }

    public Project.FileRef newScript(String name) throws IOException {
        int id = this.nextFreeId();
        Project.FileRef f = new Project.FileRef(this.scriptsDir().resolve(sanitize(name) + ".sst"), Project.Kind.SCRIPT);
        if (Files.exists(f.path())) {
            throw new IOException("Файл уже существует");
        } else {
            this.write(f, ScriptTemplates.newScript(id, name));
            return f;
        }
    }

    public Project.FileRef newGeneral(String name, boolean blocks) throws IOException {
        int id = this.nextFreeId();
        Project.FileRef f = blocks
            ? new Project.FileRef(this.blocksDir().resolve(sanitize(name) + ".svs"), Project.Kind.BLOCKS)
            : new Project.FileRef(this.scriptsDir().resolve(sanitize(name) + ".sst"), Project.Kind.SCRIPT);
        if (Files.exists(f.path())) {
            throw new IOException("Файл уже существует");
        } else {
            this.write(f, blocks ? VisualScript.createGeneral(id, name).toJson() : ScriptTemplates.general(id, name));
            return f;
        }
    }

    public boolean isGeneral(Project.FileRef f) {
        if (f.kind() != Project.Kind.SCRIPT && f.kind() != Project.Kind.BLOCKS) {
            return false;
        } else {
            try {
                String text = this.read(f);
                return f.kind() == Project.Kind.SCRIPT ? GeneralLang.isGeneral(text) : text.contains("общий: с запуска игры");
            } catch (IOException var3) {
                return false;
            }
        }
    }

    public Project.FileRef newBlocks(String name) throws IOException {
        int id = this.nextFreeId();
        Project.FileRef f = new Project.FileRef(this.blocksDir().resolve(sanitize(name) + ".svs"), Project.Kind.BLOCKS);
        if (Files.exists(f.path())) {
            throw new IOException("Файл уже существует");
        } else {
            this.write(f, VisualScript.create(id, name).toJson());
            return f;
        }
    }

    public Project.FileRef rename(Project.FileRef f, String newName) throws IOException {
        String ext = f.kind().ext;
        Path target = f.path().resolveSibling(sanitize(newName) + ext);
        if (Files.exists(target)) {
            throw new IOException("Файл с таким именем уже есть");
        } else {
            Files.move(f.path(), target);
            return new Project.FileRef(target, f.kind());
        }
    }

    public Project.FileRef duplicate(Project.FileRef f) throws IOException {
        String ext = f.kind().ext;
        int n = 2;

        Path target;
        do {
            target = f.path().resolveSibling(f.name() + " (" + n++ + ")" + ext);
        } while (Files.exists(target));

        Files.copy(f.path(), target);
        return new Project.FileRef(target, f.kind());
    }

    public void delete(Project.FileRef f) throws IOException {
        Files.deleteIfExists(f.path());
    }

    public record EntityRef(Path path) {
        public String name() {
            String n = this.path.getFileName().toString();
            return n.endsWith(".sent") ? n.substring(0, n.length() - 5) : n;
        }
    }

    public record FileRef(Path path, Project.Kind kind) {
        public String name() {
            String n = this.path.getFileName().toString();
            int dot = n.lastIndexOf(46);
            return dot > 0 ? n.substring(0, dot) : n;
        }

        public String fileName() {
            return this.path.getFileName().toString();
        }
    }

    public record ImageRef(Path path) {
        public String name() {
            String n = this.path.getFileName().toString();
            int dot = n.lastIndexOf(46);
            return dot > 0 ? n.substring(0, dot) : n;
        }
    }

    public static enum Kind {
        SCRIPT(".sst"),
        BLOCKS(".svs"),
        GUI(".sgui"),
        CUTSCENE(".scut"),
        EFFECT(".sfx"),
        SHOP(".sshop"),
        QUEST(".squest"),
        ZONE(".szone"),
        FIGHT(".sfight"),
        PATH(".spath"),
        POST(".spost"),
        CAMERA(".scam"),
        STRUCTURE(".sbuild"),
        STEALTH(".sstealth"),
        SKILLS(".sskills");

        public final String ext;

        private Kind(String ext) {
            this.ext = ext;
        }

        public static Project.Kind ofFile(String name) {
            for (Project.Kind k : values()) {
                if (name.endsWith(k.ext)) {
                    return k;
                }
            }

            return SCRIPT;
        }
    }

    public static final class Meta {
        public String name;
        public long created;
        public long opened;
        public int baseId = 5001;
        public List<String> openFiles = new ArrayList<>();
    }

    public record ModelRef(Path dir) {
        public String name() {
            return this.dir.getFileName().toString();
        }
    }

    public record SoundRef(Path path) {
        public String name() {
            String n = this.path.getFileName().toString();
            int dot = n.lastIndexOf(46);
            return dot > 0 ? n.substring(0, dot) : n;
        }
    }

    public record VideoRef(Path path) {
        public String name() {
            String n = this.path.getFileName().toString();
            int dot = n.lastIndexOf(46);
            return dot > 0 ? n.substring(0, dot) : n;
        }
    }
}
