package org.sprautestudio.spraute_engine.zone.client;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.PoseStack.Pose;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.function.Supplier;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.MultiBufferSource.BufferSource;
import net.minecraft.network.chat.Component;
import net.minecraft.world.phys.Vec3;
import org.sprautestudio.spraute_engine.compat.Gfx;
import org.sprautestudio.spraute_engine.compat.Mc;
import org.sprautestudio.spraute_engine.editor.zone.ZoneWorldEditor;
import org.sprautestudio.spraute_engine.engine.EngineConfig;
import org.sprautestudio.spraute_engine.engine.client.EngineClient;
import org.sprautestudio.spraute_engine.engine.client.ProjectSounds;
import org.sprautestudio.spraute_engine.engine.client.Skins;
import org.sprautestudio.spraute_engine.gui.client.GuiView;
import org.sprautestudio.spraute_engine.hitbox.Hitboxes;
import org.sprautestudio.spraute_engine.platform.ClientHooks;
import org.sprautestudio.spraute_engine.platform.Hooks;
import org.sprautestudio.spraute_engine.platform.TickPhase;
import org.sprautestudio.spraute_engine.zone.ZoneDef;
import org.sprautestudio.spraute_engine.zone.ZoneShape;

public final class ZoneClient {
    private static String title = "";
    private static String subtitle = "";
    private static long titleAt;
    private static Vec3 lastPos;
    public static Supplier<Map<String, String>> projectZones = Map::of;
    public static final Map<String, ZoneDef> LIVE = new LinkedHashMap<>();
    public static String highlighted = "";
    private static Map<String, ZoneDef> cached = new LinkedHashMap<>();
    private static long cachedAt;
    private static Map<String, ZoneDef> shown = new LinkedHashMap<>();

    public static void registerHooks() {
        Hooks.PLAYER_TICK.add(ZoneClient::wallTick);
        ClientHooks.LOGGING_OUT.add(ZoneClient::logout);
        ClientHooks.RENDER_GUI.add(ZoneClient::render);
        ClientHooks.RENDER_LEVEL.add(ZoneClient::renderLevel);
    }

    private ZoneClient() {
    }

    public static void showTitle(String t, String sub) {
        title = t;
        subtitle = sub;
        titleAt = System.currentTimeMillis();
        GuiView lay = EngineClient.window("zone");
        if (lay != null) {
            lay.restartAppear();
        }

        String snd = EngineClient.config().zoneTitle.sound;
        ProjectSounds.ui(EngineClient.config().project, snd);
    }

    public static void wallTick(Hooks.PlayerTick e) {
        Minecraft mc = Minecraft.getInstance();
        if (e.phase == TickPhase.END && e.player == mc.player && mc.player != null && mc.level != null) {
            LocalPlayer p = mc.player;
            Vec3 now = p.position();
            if (lastPos != null && !(lastPos.distanceToSqr(now) > 9.0) && !p.isSpectator()) {
                String dim = mc.level.dimension().location().toString();

                for (ZoneDef d : shown.values()) {
                    if (!d.wall.equals("none") && (d.dimension.isEmpty() || d.dimension.equals(dim))) {
                        boolean keepIn = d.wall.equals("in");
                        boolean was = d.holds(lastPos.x, lastPos.y, lastPos.z);
                        boolean in = d.holds(now.x, now.y, now.z);
                        if (keepIn ? was && !in : !was && in) {
                            Vec3[] tries = new Vec3[]{
                                new Vec3(lastPos.x, now.y, now.z),
                                new Vec3(now.x, now.y, lastPos.z),
                                new Vec3(lastPos.x, now.y, lastPos.z),
                                new Vec3(now.x, lastPos.y, now.z),
                                lastPos
                            };
                            Vec3 fix = lastPos;

                            for (Vec3 t : tries) {
                                if (d.holds(t.x, t.y, t.z) == keepIn) {
                                    fix = t;
                                    break;
                                }
                            }

                            Vec3 v = p.getDeltaMovement();
                            p.setDeltaMovement(fix.x != now.x ? 0.0 : v.x, fix.y != now.y ? Math.min(0.0, v.y) : v.y, fix.z != now.z ? 0.0 : v.z);
                            p.setPos(fix.x, fix.y, fix.z);
                            now = fix;
                        }
                    }
                }

                lastPos = now;
            } else {
                lastPos = now;
            }
        }
    }

    public static void logout(ClientHooks.LoggingOut e) {
        lastPos = null;
        titleAt = 0L;
        shown = new LinkedHashMap<>();
        Hitboxes.CLIENT.clear();
    }

    public static void render(ClientHooks.RenderGui e) {
        if (titleAt != 0L) {
            EngineConfig cfg = EngineClient.config();
            long t = System.currentTimeMillis() - titleAt;
            if ((float)t > cfg.zoneTitle.seconds * 1000.0F) {
                titleAt = 0L;
            } else {
                Minecraft mc = Minecraft.getInstance();
                if (!mc.options.hideGui) {
                    Gfx g = e.getGuiGraphics();
                    GuiView lay = EngineClient.window("zone");
                    if (lay != null) {
                        float total = Math.max(0.5F, cfg.zoneTitle.seconds) * 1000.0F;
                        lay.vars.put("zone", title);
                        lay.vars.put("subtitle", subtitle);
                        lay.opacity = Math.min(1.0F, (total - (float)t) / Math.min(900.0F, total * 0.25F));
                        EngineClient.drawWindow(g, lay, -10000, -10000);
                    } else {
                        drawTitle(g, mc.font, g.guiWidth(), g.guiHeight(), cfg.zoneTitle, cfg.project, title, subtitle, t);
                    }
                }
            }
        }
    }

    public static void drawTitle(Gfx g, Font font, int sw, int sh, EngineConfig.ZoneTitle c, String project, String title, String subtitle, long t) {
        float total = Math.max(0.5F, c.seconds) * 1000.0F;
        float fadeIn = Math.min(700.0F, total * 0.2F);
        float fadeOut = Math.min(900.0F, total * 0.25F);
        float a = Math.min(Math.min(1.0F, (float)t / fadeIn), Math.min(1.0F, (total - (float)t) / fadeOut));
        if (!(a <= 0.02F)) {
            RenderSystem.enableBlend();
            RenderSystem.defaultBlendFunc();
            int alpha = Math.max(4, (int)(a * 255.0F)) << 24;
            Component big = c.bold ? Component.literal(title).withStyle(ChatFormatting.BOLD) : Component.literal(title);
            float scale = Math.min(c.scale, (float)(sw - 40) / Math.max(1, font.width(big)));
            float cx = sw * c.x;
            float cy = sh * c.y;
            float grow = Math.min(1.0F, (float)t / 900.0F);
            float spread = c.spread ? (1.0F - grow) * 0.15F + 1.0F : 1.0F;
            float tw = font.width(big) * scale;
            float th = 9.0F * scale;
            float subW = subtitle.isEmpty() ? 0.0F : font.width(subtitle) * c.subScale;
            float bottom = cy + th / 2.0F + 4.0F + (subtitle.isEmpty() ? 0.0F : 6.0F + 9.0F * c.subScale);
            if (c.plate) {
                int pw = (int)(Math.max(tw, subW) + c.padding * 2);
                int top = (int)(cy - th / 2.0F - c.padding);
                int bot = (int)(bottom + c.padding);
                int px = (int)(cx - pw / 2.0F);
                if (c.plateSkin.isSet()) {
                    RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, a);
                    Skins.draw(g, project, c.plateSkin, px, top, pw, bot - top);
                    RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
                } else {
                    int pa = (int)((c.plateColor >>> 24 & 0xFF) * a);
                    g.fill(px, top, px + pw, bot, pa << 24 | c.plateColor & 16777215);
                }
            }

            g.pose().pushPose();
            g.pose().translate(cx, cy, 0.0F);
            g.pose().scale(scale * spread, scale, 1.0F);
            g.drawString(font, big, -font.width(big) / 2, -9 / 2, alpha | c.titleColor & 16777215, c.shadow);
            g.pose().popPose();
            int lineY = (int)(cy + th / 2.0F + 4.0F);
            if (c.lines) {
                int lw = (int)(grow * Math.min(sw * 0.35F, tw * 0.7F + 30.0F));
                int la = (int)(a * (c.lineColor >>> 24 & 0xFF)) << 24;
                int lc = c.lineColor & 16777215;
                g.fillGradient((int)cx - lw, lineY, (int)cx, lineY + 1, lc, la | lc);
                g.fillGradient((int)cx, lineY, (int)cx + lw, lineY + 1, la | lc, lc);
            }

            if (!subtitle.isEmpty()) {
                g.pose().pushPose();
                g.pose().translate(cx, (float)(lineY + 6), 0.0F);
                g.pose().scale(c.subScale, c.subScale, 1.0F);
                g.drawString(font, subtitle, -font.width(subtitle) / 2, 0, alpha | c.subColor & 16777215, c.shadow);
                g.pose().popPose();
            }
        }
    }

    public static void receiveShown(String json) {
        Map<String, ZoneDef> m = new LinkedHashMap<>();

        try {
            JsonObject o = JsonParser.parseString(json).getAsJsonObject();

            for (Entry<String, JsonElement> e : o.entrySet()) {
                m.put(e.getKey(), ZoneDef.fromJson(e.getValue().toString()));
            }
        } catch (Exception var5) {
        }

        shown = m;
    }

    private static Map<String, ZoneDef> zones() {
        if (!ZoneWorldEditor.active()) {
            Map<String, ZoneDef> vis = new LinkedHashMap<>();

            for (Entry<String, ZoneDef> e : shown.entrySet()) {
                if (!e.getValue().hideInGame) {
                    vis.put(e.getKey(), e.getValue());
                }
            }

            return vis;
        } else {
            long now = System.currentTimeMillis();
            if (now - cachedAt > 1000L) {
                cachedAt = now;
                Map<String, ZoneDef> m = new LinkedHashMap<>();

                try {
                    for (Entry<String, String> ex : projectZones.get().entrySet()) {
                        m.put(ex.getKey(), ZoneDef.fromJson(ex.getValue()));
                    }
                } catch (Exception var5) {
                }

                cached = m;
            }

            Map<String, ZoneDef> out = new LinkedHashMap<>(cached);
            out.putAll(LIVE);
            return out;
        }
    }

    public static boolean visible() {
        if (ZoneWorldEditor.active()) {
            return true;
        } else {
            for (ZoneDef d : shown.values()) {
                if (!d.hideInGame) {
                    return true;
                }
            }

            return false;
        }
    }

    public static void renderLevel(ClientHooks.RenderLevel e) {
        if (e.getStage() == ClientHooks.LevelStage.AFTER_PARTICLES && visible()) {
            boolean editing = ZoneWorldEditor.active();
            if (editing || !Minecraft.getInstance().options.hideGui) {
                Map<String, ZoneDef> zones = zones();
                if (!zones.isEmpty()) {
                    Minecraft mc = Minecraft.getInstance();
                    if (mc.level != null) {
                        String dim = mc.level.dimension().location().toString();
                        Vec3 cam = e.getCamera().getPosition();
                        PoseStack ps = e.getPoseStack();
                        BufferSource buf = mc.renderBuffers().bufferSource();
                        VertexConsumer vc = buf.getBuffer(RenderType.lines());
                        ps.pushPose();
                        ps.translate(-cam.x, -cam.y, -cam.z);
                        Pose last = ps.last();

                        for (Entry<String, ZoneDef> en : zones.entrySet()) {
                            ZoneDef d = en.getValue();
                            if ((d.dimension.isEmpty() || d.dimension.equals(dim))
                                && !(d.center().distanceTo(cam) > 160.0 + Math.max(d.uMaxX() - d.uMinX(), d.uMaxZ() - d.uMinZ()))) {
                                boolean hl = en.getKey().equals(highlighted);
                                int c = d.color;
                                float r = (c >> 16 & 0xFF) / 255.0F;
                                float g = (c >> 8 & 0xFF) / 255.0F;
                                float b = (c & 0xFF) / 255.0F;
                                float a = d.enabled ? 1.0F : 0.4F;
                                if (hl) {
                                    float pulse = 0.75F + 0.25F * (float)Math.sin(System.currentTimeMillis() / 200.0);
                                    r = Math.min(1.0F, r * pulse + 0.3F);
                                    g = Math.min(1.0F, g * pulse + 0.3F);
                                    b = Math.min(1.0F, b * pulse + 0.3F);
                                }

                                outline(vc, last, d.primary(), r, g, b, a);

                                for (ZoneShape sh : d.more) {
                                    outline(vc, last, sh, r, g, b, editing ? a * 0.55F : a);
                                }
                            }
                        }

                        ps.popPose();
                        buf.endBatch(RenderType.lines());
                        Font font = mc.font;

                        for (Entry<String, ZoneDef> enx : zones.entrySet()) {
                            ZoneDef d = enx.getValue();
                            if (d.dimension.isEmpty() || d.dimension.equals(dim)) {
                                Vec3 top = new Vec3((d.uMinX() + d.uMaxX()) / 2.0, d.uMaxY() + 0.4, (d.uMinZ() + d.uMaxZ()) / 2.0);
                                if (!(top.distanceTo(cam) > 96.0)) {
                                    Vec3 p = top.subtract(cam);
                                    ps.pushPose();
                                    ps.translate(p.x, p.y, p.z);
                                    Mc.faceCamera(ps);
                                    float s = 0.03F * (float)Math.max(1.0, top.distanceTo(cam) / 12.0);
                                    ps.scale(-s, -s, s);
                                    if (!editing && d.title.isEmpty()) {
                                        ps.popPose();
                                    } else {
                                        String label = !editing
                                            ? d.title
                                            : "◆ "
                                                + enx.getKey()
                                                + (d.enabled ? "" : "  (выкл)")
                                                + (d.wall.equals("none") ? "" : (d.wall.equals("in") ? "  [не выйти]" : "  [не войти]"))
                                                + (d.hideInGame ? "" : "  [видна в игре]");
                                        int col = enx.getKey().equals(highlighted) ? -8070 : 0xFF000000 | d.color;
                                        Mc.text(font, label, -font.width(label) / 2.0F, 0.0F, col, false, ps.last(), buf, true, 1711276032, 15728880);
                                        ps.popPose();
                                    }
                                }
                            }
                        }

                        buf.endBatch();
                    }
                }
            }
        }
    }

    private static void outline(VertexConsumer vc, Pose p, ZoneShape d, float r, float g, float b, float a) {
        switch (d.shape) {
            case "sphere": {
                int n = 48;

                for (int ring = 0; ring < 3; ring++) {
                    circle(vc, p, d.cx, d.cy, d.cz, d.radius, ring, n, r, g, b, a);
                }

                for (double lat : new double[]{-0.6, 0.6}) {
                    double rr = d.radius * Math.sqrt(1.0 - lat * lat);
                    circle(vc, p, d.cx, d.cy + d.radius * lat, d.cz, rr, 0, n, r, g, b, a * 0.6F);
                }
                break;
            }
            case "cylinder": {
                int n = 48;
                circle(vc, p, d.cx, d.cy, d.cz, d.radius, 0, n, r, g, b, a);
                circle(vc, p, d.cx, d.cy + d.height, d.cz, d.radius, 0, n, r, g, b, a);

                for (int i = 0; i < 8; i++) {
                    double ang = (Math.PI * 2) * i / 8.0;
                    double x = d.cx + Math.cos(ang) * d.radius;
                    double z = d.cz + Math.sin(ang) * d.radius;
                    line(vc, p, x, d.cy, z, x, d.cy + d.height, z, r, g, b, a);
                }
                break;
            }
            default:
                double x0 = d.minX();
                double y0 = d.minY();
                double z0 = d.minZ();
                double x1 = d.maxX();
                double y1 = d.maxY();
                double z1 = d.maxZ();
                line(vc, p, x0, y0, z0, x1, y0, z0, r, g, b, a);
                line(vc, p, x1, y0, z0, x1, y0, z1, r, g, b, a);
                line(vc, p, x1, y0, z1, x0, y0, z1, r, g, b, a);
                line(vc, p, x0, y0, z1, x0, y0, z0, r, g, b, a);
                line(vc, p, x0, y1, z0, x1, y1, z0, r, g, b, a);
                line(vc, p, x1, y1, z0, x1, y1, z1, r, g, b, a);
                line(vc, p, x1, y1, z1, x0, y1, z1, r, g, b, a);
                line(vc, p, x0, y1, z1, x0, y1, z0, r, g, b, a);
                line(vc, p, x0, y0, z0, x0, y1, z0, r, g, b, a);
                line(vc, p, x1, y0, z0, x1, y1, z0, r, g, b, a);
                line(vc, p, x1, y0, z1, x1, y1, z1, r, g, b, a);
                line(vc, p, x0, y0, z1, x0, y1, z1, r, g, b, a);

                for (double x = Math.ceil(x0 / 4.0) * 4.0; x < x1; x += 4.0) {
                    line(vc, p, x, y0 + 0.02, z0, x, y0 + 0.02, z1, r, g, b, a * 0.25F);
                }

                for (double z = Math.ceil(z0 / 4.0) * 4.0; z < z1; z += 4.0) {
                    line(vc, p, x0, y0 + 0.02, z, x1, y0 + 0.02, z, r, g, b, a * 0.25F);
                }
        }
    }

    private static void circle(VertexConsumer vc, Pose p, double cx, double cy, double cz, double rad, int axis, int n, float r, float g, float b, float a) {
        double px = 0.0;
        double py = 0.0;
        double pz = 0.0;

        for (int i = 0; i <= n; i++) {
            double ang = (Math.PI * 2) * i / n;
            double u = Math.cos(ang) * rad;
            double v = Math.sin(ang) * rad;
            double x = cx + (axis == 2 ? 0.0 : u);
            double y = cy + (axis == 0 ? 0.0 : v);
            double z = cz + (axis == 0 ? v : (axis == 2 ? u : 0.0));
            if (i > 0) {
                line(vc, p, px, py, pz, x, y, z, r, g, b, a);
            }

            px = x;
            py = y;
            pz = z;
        }
    }

    private static void line(VertexConsumer vc, Pose p, double x1, double y1, double z1, double x2, double y2, double z2, float r, float g, float b, float a) {
        float dx = (float)(x2 - x1);
        float dy = (float)(y2 - y1);
        float dz = (float)(z2 - z1);
        float len = (float)Math.sqrt(dx * dx + dy * dy + dz * dz);
        if (!(len < 1.0E-5)) {
            dx /= len;
            dy /= len;
            dz /= len;
            vc.vertex(p.pose(), (float)x1, (float)y1, (float)z1).color(r, g, b, a).normal(p.normal(), dx, dy, dz).endVertex();
            vc.vertex(p.pose(), (float)x2, (float)y2, (float)z2).color(r, g, b, a).normal(p.normal(), dx, dy, dz).endVertex();
        }
    }
}
