package org.sprautestudio.spraute_engine.editor.path;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.PoseStack.Pose;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.MultiBufferSource.BufferSource;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.ClipContext.Block;
import net.minecraft.world.level.ClipContext.Fluid;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.HitResult.Type;
import org.joml.Matrix4f;
import org.joml.Vector4f;
import org.sprautestudio.spraute_engine.compat.Gfx;
import org.sprautestudio.spraute_engine.compat.Mc;
import org.sprautestudio.spraute_engine.cutscene.CutsceneDoc;
import org.sprautestudio.spraute_engine.cutscene.client.CameraDirector;
import org.sprautestudio.spraute_engine.editor.ClientEvents;
import org.sprautestudio.spraute_engine.editor.SprauteScreen;
import org.sprautestudio.spraute_engine.path.PathDef;
import org.sprautestudio.spraute_engine.platform.ClientHooks;
import org.sprautestudio.spraute_engine.platform.TickPhase;

public final class PathWorldEditor {
    private static PathTab tab;
    private static int hover = -1;
    private static int grabbed = -1;
    private static String toast = "";
    private static long toastAt;
    private static float[][] screen = new float[0][];

    public static void registerHooks() {
        ClientHooks.CLIENT_TICK.add(PathWorldEditor::onTick);
        ClientHooks.MOUSE_BUTTON.add(PathWorldEditor::onMouse);
        ClientHooks.KEY.add(PathWorldEditor::onKey);
        ClientHooks.MOUSE_SCROLL.add(PathWorldEditor::onScroll);
        ClientHooks.RENDER_LEVEL.add(PathWorldEditor::onRenderLevel);
        ClientHooks.RENDER_GUI.add(PathWorldEditor::onGui);
    }

    private PathWorldEditor() {
    }

    public static boolean active() {
        return tab != null;
    }

    static PathTab tab() {
        return tab;
    }

    static void start(PathTab t) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player != null) {
            tab = t;
            grabbed = -1;
            hover = -1;
            mc.setScreen(null);
            Vec3 e = mc.player.getEyePosition(1.0F);
            CutsceneDoc.Pose start = new CutsceneDoc.Pose();
            PathDef d = t.doc();
            Vec3 c = center(d);
            if (c != null && c.distanceTo(e) > 48.0) {
                start.x = c.x;
                start.y = c.y + 14.0;
                start.z = c.z + 14.0;
                Vec3 to = c.subtract(start.x, start.y, start.z);
                start.yaw = (float)Math.toDegrees(Math.atan2(-to.x, to.z));
                start.pitch = (float)Math.toDegrees(-Math.atan2(to.y, Math.hypot(to.x, to.z)));
            } else {
                start.x = e.x;
                start.y = e.y;
                start.z = e.z;
                start.yaw = mc.player.getYRot();
                start.pitch = mc.player.getXRot();
            }

            CameraDirector.startFly(start);
            say("Редактор пути в мире");
        }
    }

    static void stopEditing() {
        if (tab != null) {
            CameraDirector.stopFly();
            tab = null;
            grabbed = -1;
        }
    }

    private static Vec3 center(PathDef d) {
        if (d.points.isEmpty()) {
            return null;
        } else {
            double x = 0.0;
            double y = 0.0;
            double z = 0.0;

            for (PathDef.Point p : d.points) {
                x += p.x;
                y += p.y;
                z += p.z;
            }

            int n = d.points.size();
            return new Vec3(x / n, y / n, z / n);
        }
    }

    private static void say(String s) {
        toast = s;
        toastAt = System.currentTimeMillis();
    }

    private static void click() {
        LocalPlayer p = Minecraft.getInstance().player;
        if (p != null) {
            p.playSound(Mc.sound(SoundEvents.UI_BUTTON_CLICK), 0.4F, 1.6F);
        }
    }

    private static Vec3 eye() {
        CutsceneDoc.Pose p = CameraDirector.pose();
        return new Vec3(p.x, p.y, p.z);
    }

    private static Vec3 dir() {
        CutsceneDoc.Pose p = CameraDirector.pose();
        double ya = Math.toRadians(p.yaw);
        double pa = Math.toRadians(p.pitch);
        return new Vec3(-Math.sin(ya) * Math.cos(pa), -Math.sin(pa), Math.cos(ya) * Math.cos(pa));
    }

    private static BlockHitResult aimBlock() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level != null && mc.player != null) {
            Vec3 o = eye();
            Vec3 to = o.add(dir().scale(160.0));
            BlockHitResult h = mc.level.clip(new ClipContext(o, to, Block.COLLIDER, Fluid.NONE, mc.player));
            return h.getType() == Type.BLOCK ? h : null;
        } else {
            return null;
        }
    }

    private static Vec3 aimSpot() {
        BlockHitResult h = aimBlock();
        if (h == null) {
            return null;
        } else {
            BlockPos b = h.getBlockPos().relative(h.getDirection());
            return new Vec3(b.getX() + 0.5, b.getY(), b.getZ() + 0.5);
        }
    }

    private static int aimedPoint(PathDef d) {
        Vec3 e = eye();
        Vec3 f = dir();
        int best = -1;
        double bestAng = Math.toRadians(6.0);

        for (int i = 0; i < d.points.size(); i++) {
            Vec3 at = d.points.get(i).vec().add(0.0, 0.9, 0.0);
            Vec3 to = at.subtract(e);
            double len = to.length();
            if (!(len < 0.3) && !(len > 200.0)) {
                double ang = Math.acos(Math.max(-1.0, Math.min(1.0, to.normalize().dot(f))));
                double allow = Math.max(Math.toRadians(2.5), Math.atan(0.9 / len));
                if (ang < allow && ang < bestAng) {
                    best = i;
                    bestAng = ang;
                }
            }
        }

        return best;
    }

    public static void onTick(ClientHooks.ClientTick e) {
        if (e.phase == TickPhase.END && tab != null) {
            Minecraft mc = Minecraft.getInstance();
            if (mc.player != null && !(mc.screen instanceof SprauteScreen) && CameraDirector.flying()) {
                PathDef d = tab.doc();
                if (grabbed >= 0 && grabbed < d.points.size()) {
                    Vec3 s = aimSpot();
                    if (s != null) {
                        PathDef.Point p = d.points.get(grabbed);
                        p.x = s.x;
                        p.y = s.y;
                        p.z = s.z;
                    }
                }

                hover = grabbed >= 0 ? grabbed : aimedPoint(d);
            } else {
                stopEditing();
            }
        }
    }

    public static void onMouse(ClientHooks.MouseButton e) {
        if (tab != null && Minecraft.getInstance().screen == null) {
            PathDef d = tab.doc();
            if (e.getButton() == 0) {
                e.setCanceled(true);
                if (e.getAction() != 1) {
                    return;
                }

                if (grabbed >= 0) {
                    grabbed = -1;
                    tab.edited(true);
                    say("Поставлено");
                } else if (hover >= 0) {
                    tab.snapshot();
                    grabbed = hover;
                    tab.sel = hover;
                    click();
                    say("Точка " + (hover + 1) + " — ведите прицелом, ЛКМ — поставить");
                }
            } else if (e.getButton() == 1) {
                e.setCanceled(true);
                if (e.getAction() != 1) {
                    return;
                }

                if (hover >= 0 && grabbed < 0) {
                    tab.sel = hover;
                    tab.edited(true);
                    say("Выбрана точка " + (hover + 1) + " — новые встанут после неё");
                    return;
                }

                Vec3 s = aimSpot();
                if (s == null) {
                    say("Наведите прицел на блок");
                    return;
                }

                tab.addPoint(s);
                click();
                say("Точка " + (tab.sel + 1) + " из " + d.points.size());
            }
        }
    }

    public static void onKey(ClientHooks.Key e) {
        if (tab != null && e.getAction() == 1 && Minecraft.getInstance().screen == null) {
            boolean ctrl = (e.getModifiers() & 2) != 0;
            PathDef d = tab.doc();
            switch (e.getKey()) {
                case 67: {
                    Vec3 c = center(d);
                    if (c == null) {
                        return;
                    }

                    CutsceneDoc.Pose p = new CutsceneDoc.Pose();
                    p.x = c.x;
                    p.y = c.y + 14.0;
                    p.z = c.z + 14.0;
                    Vec3 to = c.subtract(p.x, p.y, p.z);
                    p.yaw = (float)Math.toDegrees(Math.atan2(-to.x, to.z));
                    p.pitch = (float)Math.toDegrees(-Math.atan2(to.y, Math.hypot(to.x, to.z)));
                    CameraDirector.flyTo(p);
                    say("К пути");
                    break;
                }
                case 77:
                    tab.snapshot();
                    d.mode = switch (d.mode) {
                        case "once" -> "loop";
                        case "loop" -> "pingpong";
                        default -> "once";
                    };
                    tab.edited(true);
                    say("Как идти: " + modeName(d.mode));
                    break;
                case 80: {
                    if (tab.sel < 0 || tab.sel >= d.points.size()) {
                        return;
                    }

                    tab.snapshot();
                    PathDef.Point p = d.points.get(tab.sel);
                    double[] steps = new double[]{0.0, 1.0, 2.0, 3.0, 5.0};
                    int k = 0;

                    for (int ix = 0; ix < steps.length; ix++) {
                        if (Math.abs(steps[ix] - p.wait) < 0.01) {
                            k = ix;
                        }
                    }

                    p.wait = steps[(k + 1) % steps.length];
                    tab.edited(true);
                    say("Точка " + (tab.sel + 1) + ": постоять " + PathTab.fmt(p.wait) + " с");
                    break;
                }
                case 82:
                    if (tab.sel < 0 || tab.sel >= d.points.size()) {
                        return;
                    }

                    tab.snapshot();
                    d.points.get(tab.sel).yaw = (double)Math.round(CameraDirector.pose().yaw);
                    tab.edited(true);
                    say("Точка " + (tab.sel + 1) + ": смотреть туда же, куда камера");
                    break;
                case 90:
                    if (ctrl && tab.undoPath()) {
                        say("Отменено");
                    }
                    break;
                case 257:
                case 335:
                    tab.saveNow();
                    say("Сохранено");
                    break;
                case 258:
                    if (ClientEvents.isEditorKey(e.getKey()) || d.points.isEmpty()) {
                        return;
                    }

                    tab.sel = (tab.sel + 1) % d.points.size();
                    tab.edited(true);
                    say("Выбрана точка " + (tab.sel + 1));
                    break;
                case 259:
                case 261:
                    int i = hover >= 0 ? hover : tab.sel;
                    if (i >= 0) {
                        grabbed = -1;
                        tab.removePoint(i);
                        say("Точка убрана — осталось " + d.points.size());
                    }
            }
        }
    }

    private static String modeName(String m) {
        return switch (m) {
            case "loop" -> "по кругу";
            case "pingpong" -> "туда-обратно";
            default -> "один раз";
        };
    }

    public static void onScroll(ClientHooks.MouseScroll e) {
        if (tab != null && Minecraft.getInstance().screen == null) {
            CameraDirector.flySpeed = Math.max(0.5, Math.min(64.0, CameraDirector.flySpeed * (e.getScrollDelta() > 0.0 ? 1.25 : 0.8)));
            say("Скорость полёта: " + Math.round(CameraDirector.flySpeed * 10.0) / 10.0);
            e.setCanceled(true);
        }
    }

    public static void onRenderLevel(ClientHooks.RenderLevel e) {
        if (tab != null && e.getStage() == ClientHooks.LevelStage.AFTER_PARTICLES) {
            Minecraft mc = Minecraft.getInstance();
            PathDef d = tab.doc();
            Vec3 cam = e.getCamera().getPosition();
            PoseStack ps = e.getPoseStack();
            BufferSource buf = mc.renderBuffers().bufferSource();
            VertexConsumer vc = buf.getBuffer(RenderType.lines());
            ps.pushPose();
            ps.translate(-cam.x, -cam.y, -cam.z);
            Pose last = ps.last();
            List<PathDef.Point> pts = d.points;
            float r0 = (d.color >> 16 & 0xFF) / 255.0F;
            float g0 = (d.color >> 8 & 0xFF) / 255.0F;
            float b0 = (d.color & 0xFF) / 255.0F;

            for (int i = 1; i < pts.size(); i++) {
                Vec3 a = pts.get(i - 1).vec().add(0.0, 0.1, 0.0);
                Vec3 b = pts.get(i).vec().add(0.0, 0.1, 0.0);
                line(vc, last, a, b, r0, g0, b0, 1.0F);
                Vec3 m = a.add(b).scale(0.5);
                Vec3 dirv = b.subtract(a).normalize();
                Vec3 side = new Vec3(-dirv.z, 0.0, dirv.x).scale(0.35);
                line(vc, last, m, m.subtract(dirv.scale(0.5)).add(side), r0, g0, b0, 1.0F);
                line(vc, last, m, m.subtract(dirv.scale(0.5)).subtract(side), r0, g0, b0, 1.0F);
            }

            if (d.mode.equals("loop") && pts.size() > 2) {
                line(vc, last, pts.get(pts.size() - 1).vec().add(0.0, 0.1, 0.0), pts.get(0).vec().add(0.0, 0.1, 0.0), r0, g0, b0, 0.4F);
            }

            for (int i = 0; i < pts.size(); i++) {
                PathDef.Point p = pts.get(i);
                boolean on = i == hover || i == grabbed;
                boolean chosen = i == tab.sel;
                float r = on ? 1.0F : (i == 0 ? 0.48F : (i == pts.size() - 1 ? 1.0F : r0));
                float g = on ? 0.88F : (i == 0 ? 0.88F : (i == pts.size() - 1 ? 0.42F : g0));
                float b = on ? 0.3F : (i == 0 ? 0.54F : (i == pts.size() - 1 ? 0.42F : b0));
                double s = chosen ? 0.32 : 0.22;
                line(vc, last, p.vec(), p.vec().add(0.0, 1.8, 0.0), r, g, b, 1.0F);
                box(vc, last, p.x - s, p.y + 1.8 - s, p.z - s, p.x + s, p.y + 1.8 + s, p.z + s, r, g, b, 1.0F);
                box(vc, last, p.x - 0.5, p.y, p.z - 0.5, p.x + 0.5, p.y + 0.02, p.z + 0.5, r, g, b, chosen ? 1.0F : 0.5F);
                if (p.yaw != null) {
                    double ya = Math.toRadians(p.yaw);
                    Vec3 f = new Vec3(-Math.sin(ya), 0.0, Math.cos(ya)).scale(0.8);
                    line(vc, last, p.vec().add(0.0, 1.5, 0.0), p.vec().add(0.0, 1.5, 0.0).add(f), 0.6F, 0.9F, 1.0F, 1.0F);
                }
            }

            Vec3 s = grabbed < 0 && hover < 0 ? aimSpot() : null;
            if (s != null) {
                box(vc, last, s.x - 0.5, s.y, s.z - 0.5, s.x + 0.5, s.y + 0.05, s.z + 0.5, 0.4F, 1.0F, 0.6F, 1.0F);
                if (tab.sel >= 0 && tab.sel < pts.size()) {
                    line(vc, last, pts.get(tab.sel).vec().add(0.0, 0.1, 0.0), s.add(0.0, 0.1, 0.0), 0.4F, 1.0F, 0.6F, 0.5F);
                }
            }

            ps.popPose();
            buf.endBatch(RenderType.lines());
            Matrix4f proj = e.getProjectionMatrix();
            Matrix4f view = Mc.pose(e.getPoseStack().last());
            float[][] sc = new float[pts.size()][];
            int sw = mc.getWindow().getGuiScaledWidth();
            int sh = mc.getWindow().getGuiScaledHeight();

            for (int ix = 0; ix < pts.size(); ix++) {
                PathDef.Point p = pts.get(ix);
                Vector4f v = new Vector4f((float)(p.x - cam.x), (float)(p.y + 2.3 - cam.y), (float)(p.z - cam.z), 1.0F);
                view.transform(v);
                proj.transform(v);
                if (!(v.w <= 0.05F)) {
                    sc[ix] = new float[]{(v.x / v.w * 0.5F + 0.5F) * sw, (1.0F - (v.y / v.w * 0.5F + 0.5F)) * sh};
                }
            }

            screen = sc;
        }
    }

    private static void line(VertexConsumer vc, Pose p, Vec3 a, Vec3 b, float r, float g, float bl, float al) {
        float dx = (float)(b.x - a.x);
        float dy = (float)(b.y - a.y);
        float dz = (float)(b.z - a.z);
        float len = (float)Math.sqrt(dx * dx + dy * dy + dz * dz);
        if (!(len < 1.0E-5)) {
            dx /= len;
            dy /= len;
            dz /= len;
            vc.vertex(p.pose(), (float)a.x, (float)a.y, (float)a.z).color(r, g, bl, al).normal(p.normal(), dx, dy, dz).endVertex();
            vc.vertex(p.pose(), (float)b.x, (float)b.y, (float)b.z).color(r, g, bl, al).normal(p.normal(), dx, dy, dz).endVertex();
        }
    }

    private static void box(VertexConsumer vc, Pose p, double x0, double y0, double z0, double x1, double y1, double z1, float r, float g, float b, float a) {
        double[][] e = new double[][]{
            {x0, y0, z0, x1, y0, z0},
            {x0, y1, z0, x1, y1, z0},
            {x0, y0, z1, x1, y0, z1},
            {x0, y1, z1, x1, y1, z1},
            {x0, y0, z0, x0, y1, z0},
            {x1, y0, z0, x1, y1, z0},
            {x0, y0, z1, x0, y1, z1},
            {x1, y0, z1, x1, y1, z1},
            {x0, y0, z0, x0, y0, z1},
            {x1, y0, z0, x1, y0, z1},
            {x0, y1, z0, x0, y1, z1},
            {x1, y1, z0, x1, y1, z1}
        };

        for (double[] l : e) {
            line(vc, p, new Vec3(l[0], l[1], l[2]), new Vec3(l[3], l[4], l[5]), r, g, b, a);
        }
    }

    public static void onGui(ClientHooks.RenderGui e) {
        if (tab != null) {
            Minecraft mc = Minecraft.getInstance();
            Gfx g = e.getGuiGraphics();
            PathDef d = tab.doc();
            float[][] sc = screen;

            for (int i = 0; i < sc.length && i < d.points.size(); i++) {
                if (sc[i] != null) {
                    PathDef.Point p = d.points.get(i);
                    String s = i + 1 + (p.wait > 0.0 ? " ⏸" + PathTab.fmt(p.wait) : "");
                    int w = mc.font.width(s);
                    int x = Math.round(sc[i][0]) - w / 2;
                    int y = Math.round(sc[i][1]) - 10;
                    g.fill(x - 2, y - 1, x + w + 2, y + 9, i == tab.sel ? -863277825 : -1728053248);
                    g.drawString(mc.font, s, x, y, -1, false);
                }
            }

            String back = ClientEvents.OPEN_EDITOR.getTranslatedKeyMessage().getString();
            String[] lines = new String[]{
                "ПУТЬ «"
                    + tab.title()
                    + "» — "
                    + d.points.size()
                    + " точек, "
                    + PathTab.fmt(d.length())
                    + " бл., "
                    + modeName(d.mode)
                    + (tab.isDirty() ? "  •  не сохранено" : ""),
                "WASD, пробел, Shift — полёт · Ctrl — быстрее · колёсико — скорость (" + Math.round(CameraDirector.flySpeed * 10.0) / 10.0 + ")",
                "ПКМ по блоку — новая точка (после выбранной) · ПКМ по точке — выбрать · ЛКМ по точке — перенести",
                "Tab — следующая · Delete — убрать · P — постоять 0/1/2/3/5 с · R — смотреть как камера · M — один раз / круг / туда-обратно",
                "C — к пути · Ctrl+Z — отменить · Enter — сохранить · [" + back + "] — в редактор"
            };
            int y = 6;

            for (int ix = 0; ix < lines.length; ix++) {
                int w = mc.font.width(lines[ix]);
                g.fill(4, y - 2, 10 + w, y + 9, -1728053248);
                g.drawString(mc.font, lines[ix], 7, y, ix == 0 ? d.color | 0xFF000000 : -2039584, false);
                y += 12;
            }

            int sw = e.getWindow().getGuiScaledWidth();
            int sh = e.getWindow().getGuiScaledHeight();
            g.fill(sw / 2 - 4, sh / 2, sw / 2 + 5, sh / 2 + 1, -855638017);
            g.fill(sw / 2, sh / 2 - 4, sw / 2 + 1, sh / 2 + 5, -855638017);
            if (!toast.isEmpty() && System.currentTimeMillis() - toastAt < 2000L) {
                int tw = mc.font.width(toast);
                g.fill((sw - tw) / 2 - 6, 86, (sw + tw) / 2 + 6, 100, -1442840576);
                g.drawString(mc.font, toast, (sw - tw) / 2, 89, -1, false);
            }
        }
    }
}
