package org.sprautestudio.spraute_engine.addon;

import com.google.gson.JsonObject;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.IntConsumer;
import java.util.function.Supplier;
import net.minecraft.resources.ResourceLocation;
import org.sprautestudio.spraute_engine.build.BuildStyles;
import org.sprautestudio.spraute_engine.camera.CamProfile;
import org.sprautestudio.spraute_engine.fx.FxDef;
import org.sprautestudio.spraute_engine.gui.GuiSchema;
import org.sprautestudio.spraute_engine.lang.ParamType;
import org.sprautestudio.spraute_engine.platform.PacketChannel;
import org.sprautestudio.spraute_engine.platform.Platform;
import org.sprautestudio.spraute_engine.post.PostProfile;

public final class AddonContext {
    private final AddonManifest manifest;
    final Map<String, Integer> added = new TreeMap<>();

    AddonContext(AddonManifest manifest) {
        this.manifest = manifest;
    }

    public AddonManifest manifest() {
        return this.manifest;
    }

    void count(String what) {
        this.added.merge(what, 1, Integer::sum);
    }

    public void command(String key, String category, String syntax, String doc, String blockText, AddonContext.Command run) {
        AddonContent.command(key, category, syntax, doc, blockText);
        Addons.COMMANDS.put(key, run);
        this.count("команд");
    }

    public void function(String name, String signature, String returns, String doc, AddonContext.StoryFunction run) {
        AddonContent.function(name, signature, returns, doc);
        Addons.FUNCTIONS.put(name, run);
        this.count("функций");
    }

    public void event(String name, String title, String doc, ParamType filter, String filterLabel, List<String> matches, String... fields) {
        AddonContent.event(name, title, doc, filter, filterLabel, matches, fields);
        this.count("событий");
    }

    public void playerEvent(String name) {
        AddonContent.playerEvent(name);
    }

    public void choices(String commandKey, String field, List<String> choices) {
        AddonContent.choices(commandKey, field, choices);
    }

    public void onStoryStop(IntConsumer listener) {
        Addons.STORY_STOP.add(listener);
    }

    public void ruleSwitch(String name, String title, String doc) {
        AddonContent.ruleSwitch(name, title, doc);
        this.count("правил");
    }

    public void ruleNumber(String name, String title, String doc, double def, double min, double max) {
        AddonContent.ruleNumber(name, title, doc, def, min, max);
        this.count("правил");
    }

    public void buildStyle(
        String name, String title, String doc, BuildStyles.Ease ease, double flight, boolean spinning, double swirl, BuildStyles.Planner planner
    ) {
        AddonContent.buildStyle(name, title, doc, ease, flight, spinning, swirl, planner);
        this.count("анимаций построек");
    }

    public void fxPreset(String name, String title, String doc, Supplier<FxDef> make) {
        AddonContent.fxPreset(name, title, doc, make);
        this.count("эффектов");
    }

    public void postPreset(String group, String name, String title, PostProfile profile) {
        AddonContent.postPreset(group, name, title, profile);
        this.count("пост-эффектов");
    }

    public void cameraPreset(String name, String title, String doc, CamProfile profile) {
        AddonContent.cameraPreset(name, title, doc, profile);
        this.count("камер");
    }

    public void guiType(String key, String title, String doc, int width, int height, Consumer<GuiSchema.B> properties) {
        AddonContent.guiType(key, title, doc, width, height, properties);
        this.count("элементов интерфейса");
    }

    public PacketChannel channel(String path, String protocol) {
        return Platform.get().channel(new ResourceLocation(this.manifest.id(), path), protocol);
    }

    public void mcpTool(String name, String description, JsonObject schema, Function<JsonObject, Object> run) {
        this.count("инструментов MCP");
    }

    @FunctionalInterface
    public interface Command {
        BooleanSupplier run(StoryCall var1);
    }

    @FunctionalInterface
    public interface StoryFunction {
        Object call(List<Object> var1);
    }
}
