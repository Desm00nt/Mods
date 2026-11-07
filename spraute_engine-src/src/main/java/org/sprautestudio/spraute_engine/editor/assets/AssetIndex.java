package org.sprautestudio.spraute_engine.editor.assets;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Supplier;
import net.minecraft.client.Minecraft;
import net.minecraft.client.server.IntegratedServer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import org.sprautestudio.spraute_engine.build.Builds;
import org.sprautestudio.spraute_engine.camera.CamProfile;
import org.sprautestudio.spraute_engine.compat.Regs;
import org.sprautestudio.spraute_engine.fight.FightDef;
import org.sprautestudio.spraute_engine.fx.FxPresets;
import org.sprautestudio.spraute_engine.lang.Specs;
import org.sprautestudio.spraute_engine.post.PostPresets;
import org.sprautestudio.spraute_engine.runtime.Anims;
import org.sprautestudio.spraute_engine.stealth.StealthDef;

public final class AssetIndex {
    public static Supplier<List<String>> projectShops = List::of;
    public static Supplier<List<String>> projectQuests = List::of;
    public static Function<String, Map<String, String>> projectQuestTasks = n -> Map.of();
    public static Supplier<List<String>> projectCharacters = List::of;
    private static final long LOOKUP_MS = 2000L;
    private static final Map<String, Map<String, String>> questTasks = new HashMap<>();
    private static long questTasksAt;
    private static List<String> characters = List.of();
    private static long charactersAt;
    public static Supplier<List<String>> projectZones = List::of;
    public static Supplier<List<String>> projectPaths = List::of;
    public static Supplier<List<String>> projectFights = List::of;
    public static Supplier<List<String>> projectSkills = List::of;
    public static Supplier<List<String>> projectSkillNodes = List::of;
    public static Supplier<List<String>> projectSkillMenus = List::of;
    public static Supplier<List<String>> projectStealths = List::of;
    public static Supplier<List<String>> projectEffects = List::of;
    public static Supplier<List<String>> projectStructures = List::of;
    public static Supplier<List<String>> projectCams = List::of;
    public static Supplier<List<String>> projectPosts = List::of;
    public static Supplier<Map<String, List<String>>> projectGuis = Map::of;
    public static Supplier<List<String>> projectCutscenes = List::of;
    public static Supplier<List<String>> projectImages = List::of;
    public static Supplier<String> projectName = () -> "";
    public static Supplier<List<String>> projectSounds = List::of;
    private static List<AssetIndex.Model> models;
    private static List<AssetIndex.Sound> sounds;

    private AssetIndex() {
    }

    public static Map<String, String> questTasks(String quest) {
        long now = System.currentTimeMillis();
        if (now - questTasksAt > 2000L) {
            questTasks.clear();
            questTasksAt = now;
        }

        return questTasks.computeIfAbsent(quest, projectQuestTasks);
    }

    public static List<String> characters() {
        long now = System.currentTimeMillis();
        if (now - charactersAt > 2000L) {
            characters = projectCharacters.get();
            charactersAt = now;
        }

        return characters;
    }

    public static List<String> stealthChoices() {
        List<String> out = new ArrayList<>(projectStealths.get());

        for (String p : StealthDef.PRESET_TITLES.keySet()) {
            if (!out.contains(p)) {
                out.add(p);
            }
        }

        return out;
    }

    public static List<String> fightChoices() {
        List<String> out = new ArrayList<>(projectFights.get());

        for (String p : FightDef.PRESET_TITLES.keySet()) {
            if (!out.contains(p)) {
                out.add(p);
            }
        }

        return out;
    }

    public static List<String> vfxChoices() {
        List<String> out = new ArrayList<>(List.of());

        for (String n : FxPresets.names()) {
            if (!out.contains(n)) {
                out.add(n);
            }
        }

        return out;
    }

    public static List<String> structureChoices() {
        Set<String> out = new LinkedHashSet<>(projectStructures.get());

        for (Path f : Builds.schematicFiles()) {
            String n = f.getFileName().toString();
            out.add(n.substring(0, n.lastIndexOf(46)));
        }

        IntegratedServer server = Minecraft.getInstance().getSingleplayerServer();
        if (server != null) {
            server.getStructureManager().listTemplates().map(Object::toString).sorted().limit(2000L).forEach(out::add);
        }

        return new ArrayList<>(out);
    }

    public static List<String> camChoices() {
        List<String> out = new ArrayList<>(projectCams.get());

        for (String n : CamProfile.presetNames()) {
            if (!out.contains(n)) {
                out.add(n);
            }
        }

        return out;
    }

    public static List<String> postChoices() {
        List<String> out = new ArrayList<>(projectPosts.get());

        for (String n : PostPresets.names()) {
            if (!out.contains(n)) {
                out.add(n);
            }
        }

        return out;
    }

    public static List<String> imageChoices() {
        List<String> list = new ArrayList<>(projectImages.get());

        for (String s : Specs.SPEECH_ICONS) {
            if (!list.contains(s)) {
                list.add(s);
            }
        }

        return list;
    }

    public static List<AssetIndex.Model> models() {
        if (models != null) {
            return models;
        } else {
            List<AssetIndex.Model> list = new ArrayList<>();

            for (EntityType<?> t : Regs.ENTITY_TYPE) {
                ResourceLocation id = Regs.ENTITY_TYPE.getKey(t);
                if ((
                        t.getCategory() != MobCategory.MISC
                            || id.getNamespace().equals("spraute_engine")
                            || id.getPath().equals("player")
                            || id.getPath().equals("villager")
                            || id.getPath().equals("iron_golem")
                            || id.getPath().equals("armor_stand")
                    )
                    && !id.getPath().equals("player")) {
                    String title;
                    try {
                        title = t.getDescription().getString();
                    } catch (RuntimeException var6) {
                        title = id.getPath();
                    }

                    list.add(new AssetIndex.Model(id, t, title, id.getNamespace().equals("spraute_engine")));
                }
            }

            list.sort((a, b) -> {
                boolean ka = Specs.ST_ENTITIES.contains(a.idString());
                boolean kb = Specs.ST_ENTITIES.contains(b.idString());
                if (ka != kb) {
                    return ka ? -1 : 1;
                } else if (a.builtin != b.builtin) {
                    return a.builtin ? -1 : 1;
                } else {
                    return a.idString().compareTo(b.idString());
                }
            });
            models = Collections.unmodifiableList(list);
            return models;
        }
    }

    public static AssetIndex.Model model(String id) {
        for (AssetIndex.Model m : models()) {
            if (m.idString().equals(id)) {
                return m;
            }
        }

        return null;
    }

    public static List<AssetIndex.Sound> sounds() {
        if (sounds != null) {
            return sounds;
        } else {
            List<AssetIndex.Sound> list = new ArrayList<>();

            for (ResourceLocation rl : Regs.SOUND_EVENT.keySet()) {
                list.add(new AssetIndex.Sound(rl));
            }

            list.sort((a, b) -> {
                boolean sa = a.id.getNamespace().equals("spraute_engine");
                boolean sb = b.id.getNamespace().equals("spraute_engine");
                if (sa != sb) {
                    return sa ? -1 : 1;
                } else {
                    return a.id.toString().compareTo(b.id.toString());
                }
            });
            sounds = Collections.unmodifiableList(list);
            return sounds;
        }
    }

    public static List<Anims.Entry> animations() {
        return Anims.all();
    }

    public static boolean matches(String query, String... fields) {
        if (query != null && !query.isBlank()) {
            StringBuilder sb = new StringBuilder();

            for (String f : fields) {
                if (f != null) {
                    sb.append(f.toLowerCase(Locale.ROOT).replace('_', ' ').replace('-', ' ')).append(' ');
                }
            }

            String hay = sb.toString();

            for (String word : query.toLowerCase(Locale.ROOT).trim().replace('_', ' ').replace('-', ' ').split("\\s+")) {
                if (!hay.contains(word)) {
                    return false;
                }
            }

            return true;
        } else {
            return true;
        }
    }

    public static AssetIndex.Model defaultPreviewModel() {
        for (String id : List.of("spraute_engine:model")) {
            AssetIndex.Model m = model(id);
            if (m != null) {
                return m;
            }
        }

        return models().isEmpty() ? null : models().get(0);
    }

    public record Model(ResourceLocation id, EntityType<?> type, String title, boolean builtin) {
        public String idString() {
            return this.id.toString();
        }
    }

    public record Sound(ResourceLocation id) {
    }
}
