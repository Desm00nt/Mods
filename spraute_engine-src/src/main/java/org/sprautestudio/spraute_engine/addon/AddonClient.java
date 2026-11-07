package org.sprautestudio.spraute_engine.addon;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;
import org.sprautestudio.spraute_engine.engine.EngineConfig;
import org.sprautestudio.spraute_engine.gui.GuiDoc;
import org.sprautestudio.spraute_engine.gui.client.GuiView;
import org.sprautestudio.spraute_engine.ui.Canvas;

public final class AddonClient {
    static final Map<String, AddonClient.GuiPart> PARTS = new LinkedHashMap<>();
    static final Map<String, AddonClient.Window> WINDOWS = new LinkedHashMap<>();
    private final AddonContext context;

    AddonClient(AddonContext context) {
        this.context = context;
    }

    public AddonManifest manifest() {
        return this.context.manifest();
    }

    public void guiPart(String type, AddonClient.GuiPart draw) {
        PARTS.put(type, draw);
    }

    public void window(String role, String title, String help, Supplier<GuiDoc> standard) {
        if (!WINDOWS.containsKey(role) && !EngineConfig.WINDOWS.contains(role)) {
            EngineConfig.addWindow(role, title);
            WINDOWS.put(role, new AddonClient.Window(role, title, help, standard));
            this.context.count("окон");
        } else {
            throw new IllegalArgumentException("Окно «" + role + "» уже есть");
        }
    }

    public static AddonClient.GuiPart part(String type) {
        return PARTS.get(type);
    }

    public static AddonClient.Window windowOf(String role) {
        return WINDOWS.get(role);
    }

    public static List<AddonClient.Window> windows() {
        return List.copyOf(WINDOWS.values());
    }

    @FunctionalInterface
    public interface GuiPart {
        void draw(Canvas var1, GuiDoc.Element var2, int[] var3, float var4, float var5, GuiView var6);
    }

    public record Window(String role, String title, String help, Supplier<GuiDoc> standard) {
    }
}
