package org.sprautestudio.spraute_engine.gui.client;

import com.mojang.blaze3d.platform.NativeImage;
import java.io.InputStream;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.texture.AbstractTexture;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.client.renderer.texture.PreloadedTexture;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.world.inventory.Slot;
import org.sprautestudio.spraute_engine.SprauteEngine;
import org.sprautestudio.spraute_engine.compat.Gfx;
import org.sprautestudio.spraute_engine.gui.GuiDoc;
import org.sprautestudio.spraute_engine.mixin.ContainerScreenAccess;
import org.sprautestudio.spraute_engine.platform.ClientPlatform;
import org.sprautestudio.spraute_engine.runtime.Images;

public final class MenuLook {
    private static final Map<String, MenuLook.Back> BACKS = new HashMap<>();
    private static final List<ResourceLocation> LOGOS = List.of(
        new ResourceLocation("textures/gui/title/minecraft.png"),
        new ResourceLocation("textures/gui/title/minceraft.png"),
        new ResourceLocation("textures/gui/title/edition.png")
    );
    public static boolean hideVersion;
    private static final Map<String, String> APPLIED = new HashMap<>();
    private static final Map<String, Set<String>> FLAGS = new HashMap<>();
    private static Screen slotsOf;
    private static String appliedBranding;
    private static boolean logoHidden;
    private static final Map<ResourceLocation, AbstractTexture> EMPTY_LOGOS = new HashMap<>();

    private MenuLook() {
    }

    private static void back(String key, String file, int w, int h) {
        BACKS.put(key, new MenuLook.Back(file, w, h));
    }

    public static void opened() {
        appliedBranding = null;
    }

    public static boolean hasBack(String key) {
        return BACKS.containsKey(key);
    }

    public static void slotsOf(Screen screen) {
        slotsOf = screen;
    }

    private static boolean on(GuiDoc doc, String prop) {
        return doc != null && "true".equals(doc.get(prop));
    }

    public static GuiDoc.Element copyright(GuiDoc doc) {
        for (GuiDoc.Element e : doc.elements) {
            if (e.type.equals("menu_widget") && e.get("baseText").startsWith("Copyright")) {
                return e;
            }
        }

        return null;
    }

    public static void apply(String key, GuiDoc doc, String project) {
        Set<String> flags = new HashSet<>();

        for (String prop : List.of("hideLabels", "hideDim")) {
            if (on(doc, prop)) {
                flags.add(prop);
            }
        }

        FLAGS.put(key, flags);
        if (key.equals("title")) {
            GuiDoc.Element copyright = doc != null ? copyright(doc) : null;
            hideVersion = on(doc, "hideVersion");
            String branding = hideVersion + "/" + (copyright != null && !copyright.b("visible"));
            if (!branding.equals(appliedBranding)) {
                appliedBranding = branding;
                ClientPlatform.get().hideBranding(hideVersion, copyright != null && !copyright.b("visible"));
            }
        }

        String image = doc != null ? doc.get("backImage") : "";
        String wanted = (on(doc, "hideLogo") ? "logo;" : "") + (on(doc, "hideBack") ? "back;" : "") + project + "/" + image;
        if (!wanted.equals(APPLIED.get(key))) {
            APPLIED.put(key, wanted);

            try {
                if (key.equals("title")) {
                    logo(on(doc, "hideLogo"));
                }

                MenuLook.Back back = BACKS.get(key);
                if (back != null) {
                    back(back, on(doc, "hideBack"), image, project);
                }
            } catch (Exception var7) {
                SprauteEngine.LOGGER.warn("Spraute: cannot change the look of the menu {}", key, var7);
            }
        }
    }

    private static void logo(boolean hide) {
        logoHidden = hide;
        if (hide) {
            keepLogoHidden();
        } else {
            TextureManager textures = Minecraft.getInstance().getTextureManager();

            for (ResourceLocation logo : LOGOS) {
                textures.release(logo);
            }

            EMPTY_LOGOS.clear();
        }
    }

    public static void keepLogoHidden() {
        if (logoHidden) {
            TextureManager textures = Minecraft.getInstance().getTextureManager();

            for (ResourceLocation logo : LOGOS) {
                AbstractTexture now = textures.getTexture(logo);
                if (now != EMPTY_LOGOS.get(logo) && !(now instanceof PreloadedTexture loading && !loading.getFuture().isDone())) {
                    NativeImage empty = new NativeImage(1, 1, true);
                    empty.setPixelRGBA(0, 0, 0);
                    DynamicTexture own = new DynamicTexture(empty);
                    textures.register(logo, own);
                    EMPTY_LOGOS.put(logo, own);
                    SprauteEngine.LOGGER.debug("Spraute: the game's logo is hidden: {}", logo);
                }
            }
        }
    }

    private static void back(MenuLook.Back back, boolean hide, String image, String project) throws Exception {
        ResourceLocation id = new ResourceLocation("textures/gui/container/" + back.file() + ".png");
        Minecraft mc = Minecraft.getInstance();
        if (!hide && image.isEmpty()) {
            mc.getTextureManager().release(id);
        } else {
            Optional<Resource> resource = mc.getResourceManager().getResource(id);
            if (!resource.isEmpty()) {
                NativeImage sheet;
                try (InputStream in = resource.get().open()) {
                    sheet = NativeImage.read(in);
                }

                float scale = sheet.getWidth() / 256.0F;
                int w = Math.min(sheet.getWidth(), Math.round(back.w() * scale));
                int h = Math.min(sheet.getHeight(), Math.round(back.h() * scale));

                for (int y = 0; y < h; y++) {
                    for (int x = 0; x < w; x++) {
                        sheet.setPixelRGBA(x, y, 0);
                    }
                }

                if (!image.isEmpty() && project != null) {
                    NativeImage from = mc.getTextureManager().getTexture(Images.texture(project, image)) instanceof DynamicTexture d ? d.getPixels() : null;
                    if (from != null) {
                        for (int y = 0; y < h; y++) {
                            for (int x = 0; x < w; x++) {
                                sheet.setPixelRGBA(x, y, from.getPixelRGBA(x * from.getWidth() / w, y * from.getHeight() / h));
                            }
                        }
                    }
                }

                mc.getTextureManager().register(id, new DynamicTexture(sheet));
            }
        }
    }

    public static boolean flag(String key, String prop) {
        Set<String> flags = FLAGS.get(key);
        return flags != null && flags.contains(prop);
    }

    public static void drawSlots(Gfx g, int cell, int border, float opacity) {
        if (slotsOf instanceof AbstractContainerScreen<?> screen) {
            ContainerScreenAccess var15 = (ContainerScreenAccess)screen;
            int alpha = Math.round((cell >>> 24) * opacity) << 24;
            int line = Math.round((border >>> 24) * opacity) << 24;
            int fill = alpha | cell & 16777215;
            int edge = line | border & 16777215;
            int light = line | 16777215;

            for (Slot slot : screen.getMenu().slots) {
                int x = var15.spraute$left() + slot.x;
                int y = var15.spraute$top() + slot.y;
                g.fill(x - 1, y - 1, x + 17, y, edge);
                g.fill(x - 1, y, x, y + 16, edge);
                g.fill(x + 16, y, x + 17, y + 17, light);
                g.fill(x, y + 16, x + 16, y + 17, light);
                g.fill(x, y, x + 16, y + 16, fill);
            }
        }
    }

    static {
        back("inventory", "inventory", 176, 166);
        back("crafting", "crafting_table", 176, 166);
        back("furnace", "furnace", 176, 166);
        back("smoker", "smoker", 176, 166);
        back("blast", "blast_furnace", 176, 166);
        back("chest", "generic_54", 176, 222);
        back("shulker", "shulker_box", 176, 166);
        back("hopper", "hopper", 176, 133);
        back("dispenser", "dispenser", 176, 166);
        back("brewing", "brewing_stand", 176, 166);
        back("enchanting", "enchanting_table", 176, 166);
        back("anvil", "anvil", 176, 166);
        back("smithing", "smithing", 176, 166);
        back("grindstone", "grindstone", 176, 166);
        back("stonecutter", "stonecutter", 176, 166);
        back("loom", "loom", 176, 166);
        back("cartography", "cartography_table", 176, 166);
        back("beacon", "beacon", 230, 219);
    }

    private record Back(String file, int w, int h) {
    }
}
