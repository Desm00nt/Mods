package org.sprautestudio.spraute_engine.addon;

import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import java.util.function.Supplier;
import org.sprautestudio.spraute_engine.build.BuildStyles;
import org.sprautestudio.spraute_engine.camera.CamProfile;
import org.sprautestudio.spraute_engine.editor.visual.BlockTypes;
import org.sprautestudio.spraute_engine.fx.FxDef;
import org.sprautestudio.spraute_engine.fx.FxPresets;
import org.sprautestudio.spraute_engine.gui.GuiSchema;
import org.sprautestudio.spraute_engine.lang.CommandSpec;
import org.sprautestudio.spraute_engine.lang.EventDefs;
import org.sprautestudio.spraute_engine.lang.ParamType;
import org.sprautestudio.spraute_engine.lang.Specs;
import org.sprautestudio.spraute_engine.player.PlayerRuleDefs;
import org.sprautestudio.spraute_engine.post.PostPresets;
import org.sprautestudio.spraute_engine.post.PostProfile;

public final class AddonContent {
    private static final Set<String> PLAYER_EVENTS = ConcurrentHashMap.newKeySet();

    private AddonContent() {
    }

    public static void playerEvent(String name) {
        if (EventDefs.get(name) == null) {
            throw new IllegalArgumentException("Нет события «" + name + "»");
        } else {
            PLAYER_EVENTS.add(name);
        }
    }

    public static boolean isPlayerEvent(String name) {
        return PLAYER_EVENTS.contains(name);
    }

    private static void free(boolean taken, String what) {
        if (taken) {
            throw new IllegalArgumentException(what + " уже есть в движке или другом аддоне");
        }
    }

    public static void command(String key, String category, String syntax, String doc, String blockText) {
        free(Specs.command(key) != null, "Команда «" + key + "»");
        Specs.addCommand(key, category, syntax, doc);
        BlockTypes.addCommand(Specs.command(key), blockText);
    }

    public static void function(String name, String signature, String returns, String doc) {
        free(Specs.function(name) != null, "Функция «" + name + "»");
        Specs.addFunction(name, signature, returns, doc);
        BlockTypes.addFunction(Specs.function(name));
    }

    public static void event(String name, String title, String doc, ParamType filter, String filterLabel, List<String> matches, String... fields) {
        free(EventDefs.get(name) != null || Specs.command("wait " + name) != null, "Событие «" + name + "»");
        EventDefs.add(name, title, doc, filter, filterLabel, matches, fields);
        CommandSpec spec = Specs.addEventCommand(EventDefs.get(name));
        BlockTypes.addCommand(spec, "ждать: " + title + (filter != null ? " %what" : ""));
    }

    public static void choices(String commandKey, String field, List<String> choices) {
        BlockTypes.addChoices("cmd:" + commandKey, field, choices);
    }

    public static void ruleSwitch(String name, String title, String doc) {
        free(PlayerRuleDefs.get(name) != null, "Правило «" + name + "»");
        PlayerRuleDefs.addSwitch(name, title, doc);
    }

    public static void ruleNumber(String name, String title, String doc, double def, double min, double max) {
        free(PlayerRuleDefs.get(name) != null, "Правило «" + name + "»");
        PlayerRuleDefs.addNumber(name, title, doc, def, min, max);
    }

    public static void buildStyle(
        String name, String title, String doc, BuildStyles.Ease ease, double flight, boolean spinning, double swirl, BuildStyles.Planner planner
    ) {
        free(BuildStyles.get(name) != null, "Анимация постройки «" + name + "»");
        BuildStyles.add(name, title, doc, ease, flight, spinning, swirl, planner);
    }

    public static void fxPreset(String name, String title, String doc, Supplier<FxDef> make) {
        free(FxPresets.ALL.containsKey(name), "Эффект «" + name + "»");
        FxPresets.add(name, title, doc, make);
    }

    public static void postPreset(String group, String name, String title, PostProfile profile) {
        free(PostPresets.get(name) != null, "Пост-эффект «" + name + "»");
        PostPresets.add(group, name, title, profile);
    }

    public static void cameraPreset(String name, String title, String doc, CamProfile profile) {
        free(CamProfile.preset(name) != null, "Камера «" + name + "»");
        CamProfile.addPreset(name, title, doc, profile);
    }

    public static void guiType(String key, String title, String doc, int width, int height, Consumer<GuiSchema.B> properties) {
        free(GuiSchema.type(key) != null, "Элемент интерфейса «" + key + "»");
        GuiSchema.addType(key, title, doc, width, height, properties);
    }
}
