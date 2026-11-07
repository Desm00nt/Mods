package org.sprautestudio.spraute_engine.engine.client;

import com.mojang.blaze3d.platform.Lighting;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.PoseStack.Pose;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.sprautestudio.spraute_engine.compat.Axes;
import org.sprautestudio.spraute_engine.compat.Gfx;
import org.sprautestudio.spraute_engine.compat.Mc;
import org.sprautestudio.spraute_engine.core.model.SpBone;
import org.sprautestudio.spraute_engine.core.model.SpCube;
import org.sprautestudio.spraute_engine.core.model.SpGeoModel;
import org.sprautestudio.spraute_engine.editor.project.Workspace;
import org.sprautestudio.spraute_engine.mcp.McpServer;
import org.sprautestudio.spraute_engine.platform.ClientHooks;
import org.sprautestudio.spraute_engine.runtime.Images;
import org.sprautestudio.spraute_engine.runtime.models.EntityLibrary;
import org.sprautestudio.spraute_engine.runtime.models.ModelLibrary;
import org.sprautestudio.spraute_engine.runtime.models.SprauteModelEntity;
import org.sprautestudio.spraute_engine.runtime.models.SprauteModelRenderer;
import org.sprautestudio.spraute_engine.runtime.orb.Orbs;

public final class ChatHeads {
    private static final String PREFIX = "textures/gui/chat/spraute/head/";
    private static final ResourceLocation FALLBACK = new ResourceLocation("spraute_engine", "textures/gui/chat/generic_man.png");
    private static final Map<String, ChatHeads.Bind> ENTITIES = new HashMap<>();
    private static final Map<String, LivingEntity> LAST = new HashMap<>();
    private static final Map<Integer, LivingEntity> COPIES = new HashMap<>();
    private static final Map<EntityType<?>, LivingEntity> BY_TYPE = new HashMap<>();
    private static final Map<String, LivingEntity> BY_SPRAUTE = new HashMap<>();

    private ChatHeads() {
    }

    public static void bind(int script, String character, int entityId, String model) {
        ENTITIES.put("s" + script + "/" + character, new ChatHeads.Bind(entityId, model == null ? "" : model));
    }

    private static LivingEntity remembered(Minecraft mc, String key) {
        LivingEntity c = LAST.get(key);
        if (c != null && Mc.level(c) == mc.level) {
            return c;
        } else {
            ChatHeads.Bind b = ENTITIES.get(key);
            if (b != null && !b.model().isEmpty() && mc.level != null) {
                try {
                    SprauteModelEntity made = Orbs.MODEL.get().create(mc.level);
                    if (made == null) {
                        return null;
                    } else {
                        made.setModel(b.model());
                        still(made);
                        LAST.put(key, made);
                        return made;
                    }
                } catch (Throwable var5) {
                    return null;
                }
            } else {
                return null;
            }
        }
    }

    private static int part(String[] p, int i, int def) {
        try {
            return i < p.length ? Integer.parseInt(p[i]) : def;
        } catch (NumberFormatException var4) {
            return def;
        }
    }

    public static void drawIcon(Gfx g, ResourceLocation tex, int x, int y, int w, int h) {
        if (!draw(g, tex, x, y, w, h)) {
            RenderSystem.enableBlend();
            g.blit(tex, x, y, 0.0F, 0.0F, w, h, w, h);
        }
    }

    private static boolean draw(Gfx g, ResourceLocation tex, int x, int y, int w, int h) {
        if (tex.getNamespace().equals("spraute_engine") && tex.getPath().startsWith("textures/gui/chat/spraute/head/")) {
            String[] p = tex.getPath().substring("textures/gui/chat/spraute/head/".length()).replace(".png", "").split("/");
            boolean player = p[0].equals("player");
            int base = player ? 1 : 2;
            ChatHeads.Params params = new ChatHeads.Params(
                Math.max(0.2F, Math.min(8.0F, part(p, base, 100) / 100.0F)),
                part(p, base + 1, 0),
                part(p, base + 2, 0),
                part(p, base + 3, 0),
                base + 4 < p.length ? p[base + 4] : "auto"
            );
            float alpha = RenderSystem.getShaderColor()[3];
            if (alpha < 0.05F) {
                return true;
            } else {
                Minecraft mc = Minecraft.getInstance();
                Entity en = null;
                String key = p.length >= 2 ? p[0] + "/" + p[1] : "";
                if (player) {
                    en = mc.player;
                } else if (p.length >= 2 && mc.level != null) {
                    ChatHeads.Bind b = ENTITIES.get(key);
                    if (b != null && b.entity() >= 0) {
                        en = mc.level.getEntity(b.entity());
                    }
                }

                int[] clip = screenRect(g, x, y, w, h);
                g.enableScissor(clip[0], clip[1], clip[2], clip[3]);

                try {
                    if (en instanceof AbstractClientPlayer pl) {
                        face(g, pl.getSkinTextureLocation(), x, y, w, h, params);
                    } else if (en instanceof LivingEntity le) {
                        LivingEntity copy = copyOf(le);
                        still(copy);
                        if (!key.isEmpty()) {
                            LAST.put(key, copy);
                        }

                        render(g, copy, x, y, w, h, params);
                    } else {
                        LivingEntity copy = player ? null : remembered(mc, key);
                        if (copy != null) {
                            still(copy);
                            render(g, copy, x, y, w, h, params);
                        } else {
                            g.blit(FALLBACK, x, y, 0.0F, 0.0F, w, h, w, h);
                        }
                    }
                } finally {
                    g.disableScissor();
                }

                return true;
            }
        } else {
            return false;
        }
    }

    public static int[] screenRect(Gfx g, int x, int y, int w, int h) {
        Matrix4f m = Mc.pose(g.pose().last());
        Vector3f a = m.transformPosition(new Vector3f(x, y, 0.0F));
        Vector3f b = m.transformPosition(new Vector3f(x + w, y + h, 0.0F));
        return new int[]{
            (int)Math.floor(Math.min(a.x, b.x)), (int)Math.floor(Math.min(a.y, b.y)), (int)Math.ceil(Math.max(a.x, b.x)), (int)Math.ceil(Math.max(a.y, b.y))
        };
    }

    public static void face(Gfx g, ResourceLocation skin, int x, int y, int w, int h, ChatHeads.Params p) {
        int s = Math.round(Math.min(w, h) * p.scale());
        int fx = x + (w - s) / 2 + Math.round(w * p.dx() / 100.0F);
        int fy = y + (h - s) / 2 + Math.round(h * p.dy() / 100.0F);
        RenderSystem.enableBlend();
        g.blit(skin, fx, fy, s, s, 8.0F, 8.0F, 8, 8, 64, 64);
        float hs = s * 9 / 8.0F;
        float off = (hs - s) / 2.0F;
        g.pose().pushPose();
        g.pose().translate(fx - off, fy - off, 0.0F);
        g.pose().scale(hs / s, hs / s, 1.0F);
        g.blit(skin, 0, 0, s, s, 40.0F, 8.0F, 8, 8, 64, 64);
        g.pose().popPose();
    }

    private static LivingEntity copyOf(LivingEntity live) {
        LivingEntity c = COPIES.get(live.getId());
        if (c != null && c.getType() == live.getType() && Mc.level(c) == Mc.level(live)) {
            if (c instanceof SprauteModelEntity cm && live instanceof SprauteModelEntity lm && !cm.model().equals(lm.model())) {
                cm.setModel(lm.model());
            }

            return c;
        } else {
            Entity made;
            try {
                made = live.getType().create(Mc.level(live));
            } catch (Throwable var6) {
                made = null;
            }

            if (made instanceof LivingEntity le) {
                try {
                    CompoundTag tag = live.saveWithoutId(new CompoundTag());
                    tag.remove("UUID");
                    tag.remove("Motion");
                    tag.remove("Rotation");
                    le.load(tag);
                } catch (Throwable var5) {
                }

                still(le);
                COPIES.put(live.getId(), le);
                return le;
            } else {
                return live;
            }
        }
    }

    public static LivingEntity ofSprauteType(String id) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) {
            return null;
        } else {
            EntityLibrary.Type t = EntityLibrary.find(null, id);
            String key;
            if (id.equals("spraute:npc")) {
                key = "builtin/humanoid";
            } else if (t != null) {
                key = t.key();
            } else {
                ModelLibrary.Model m = ModelLibrary.find(null, id.substring(8));
                if (m == null) {
                    return null;
                }

                key = m.key();
            }

            LivingEntity c = BY_SPRAUTE.get(key);
            if (c != null && Mc.level(c) == mc.level) {
                return c;
            } else {
                try {
                    if ((Object)Orbs.MODEL.get().create(mc.level) instanceof SprauteModelEntity me) {
                        me.setModel(key);
                        still(me);
                        BY_SPRAUTE.put(key, me);
                        return me;
                    } else {
                        return null;
                    }
                } catch (Throwable var7) {
                    return null;
                }
            }
        }
    }

    public static LivingEntity ofType(EntityType<?> type) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level != null && type != null) {
            LivingEntity c = BY_TYPE.get(type);
            if (c != null && Mc.level(c) == mc.level) {
                return c;
            } else {
                try {
                    if (type.create(mc.level) instanceof LivingEntity le) {
                        still(le);
                        BY_TYPE.put(type, le);
                        return le;
                    } else {
                        return null;
                    }
                } catch (Throwable var5) {
                    return null;
                }
            }
        } else {
            return null;
        }
    }

    private static void still(LivingEntity le) {
        le.tickCount = 0;
        Mc.setWalkSpeed(le, 0.0F);
    }

    private static SpGeoModel geometry(LivingEntity e) {
        if (e instanceof SprauteModelEntity me) {
            EntityLibrary.Look look = me.look();
            return look != null ? look.geometry() : null;
        } else {
            return null;
        }
    }

    private static Map<String, ModelPart> vanillaParts(LivingEntity e) {
        Map<String, ModelPart> out = new LinkedHashMap<>();

        try {
            if (!(Minecraft.getInstance().getEntityRenderDispatcher().getRenderer(e) instanceof LivingEntityRenderer<?, ?> lr)) {
                return out;
            }

            EntityModel<?> m = lr.getModel();

            for (Class<?> c = m.getClass(); c != null && c != Object.class; c = c.getSuperclass()) {
                for (Field f : c.getDeclaredFields()) {
                    if (f.getType() == ModelPart.class && !Modifier.isStatic(f.getModifiers())) {
                        f.setAccessible(true);
                        ModelPart part = (ModelPart)f.get(m);
                        if (part != null && !out.containsValue(part)) {
                            out.put(f.getName(), part);
                        }
                    }
                }
            }
        } catch (Throwable var11) {
        }

        return out;
    }

    public static List<String> bones(LivingEntity e) {
        List<String> names = new ArrayList<>();
        SpGeoModel gm = geometry(e);
        if (gm != null) {
            names.addAll(gm.boneMap.keySet());
        } else {
            names.addAll(vanillaParts(e).keySet());
        }

        names.sort((a, b) -> Boolean.compare(!a.toLowerCase().contains("head"), !b.toLowerCase().contains("head")));
        return names;
    }

    private static String pick(List<String> names, String want) {
        if (want != null && !want.isEmpty() && !want.equals("auto")) {
            if (want.equals("all")) {
                return null;
            } else {
                for (String n : names) {
                    if (n.equals(want) || Images.slug(n).equals(want)) {
                        return n;
                    }
                }

                return null;
            }
        } else {
            for (String nx : names) {
                if (nx.equalsIgnoreCase("head")) {
                    return nx;
                }
            }

            for (String nxx : names) {
                if (nxx.toLowerCase().contains("head") && !nxx.toLowerCase().contains("wear")) {
                    return nxx;
                }
            }

            return null;
        }
    }

    public static void render(Gfx g, LivingEntity e, int x, int y, int w, int h, ChatHeads.Params p) {
        if (e != null) {
            int size = Math.min(w, h);
            float px = size * 1.6F * p.scale();
            float focus = e.getEyeHeight();
            float sideShift = 0.0F;
            SpGeoModel gm = geometry(e);
            Map<Object, boolean[]> saved = new IdentityHashMap<>();
            String bone = null;
            float walk = Mc.walkSpeed(e);
            Mc.setWalkSpeed(e, 0.0F);
            if (gm != null) {
                bone = pick(new ArrayList<>(gm.boneMap.keySet()), p.bone());
                if (bone != null) {
                    float[] bb = bounds(gm.boneMap.get(bone));
                    float k = e instanceof SprauteModelEntity sme && sme.look() != null ? sme.look().scale() : 1.0F;
                    if (bb != null) {
                        focus = (bb[2] + bb[3]) / 2.0F * k;
                        sideShift = (bb[0] + bb[1]) / 2.0F * k;
                        float extent = Math.max(bb[1] - bb[0], bb[3] - bb[2]) * k;
                        if (extent > 0.05F) {
                            px = size * 0.8F / extent * p.scale();
                        }
                    }
                }
            } else {
                Map<String, ModelPart> parts = vanillaParts(e);
                bone = pick(new ArrayList<>(parts.keySet()), p.bone());
                if (bone != null) {
                    ModelPart keep = parts.get(bone);

                    for (Entry<String, ModelPart> en : parts.entrySet()) {
                        ModelPart mp = en.getValue();
                        saved.put(mp, new boolean[]{mp.visible});
                        boolean show = mp == keep || en.getKey().equals("hat") && bone.equals("head");
                        mp.visible = show;
                    }

                    focus = (24.0F - keep.y) / 16.0F + 0.25F;
                }
            }

            float cx = x + w / 2.0F + w * p.dx() / 100.0F - sideShift * px;
            float cy = y + h * 0.5F + h * p.dy() / 100.0F;
            float feetY = cy + focus * px;
            PoseStack ps = g.pose();
            g.flush();
            Pose outer = ps.last();
            ps.pushPose();
            try {
                ps.translate(cx, feetY, 200.0F);
                Mc.mulPoseMatrix(ps, new Matrix4f().scaling(px, px, -px));
                Mc.mulPose(ps, new Quaternionf().rotateZ((float) Math.PI));
                Mc.mulPose(ps, Axes.YP.rotationDegrees(180 + p.yaw()));
                e.setYRot(0.0F);
                e.yRotO = 0.0F;
                e.setXRot(0.0F);
                e.xRotO = 0.0F;
                e.yBodyRot = e.yBodyRotO = 0.0F;
                e.yHeadRot = e.yHeadRotO = 0.0F;
                Lighting.setupForEntityInInventory();
                EntityRenderDispatcher var42 = Minecraft.getInstance().getEntityRenderDispatcher();
                var42.setRenderShadow(false);
                SprauteModelRenderer.withPortrait(
                    bone, () -> RenderSystem.runAsFancy(() -> var42.render(e, 0.0, 0.0, 0.0, 0.0F, 0.0F, ps, g.bufferSource(), 15728880))
                );
                g.flush();
                var42.setRenderShadow(true);
            } catch (Throwable var29) {
                try {
                    g.flush();
                } catch (Throwable var28) {
                }
            } finally {
                for (int i = 0; i < 64 && ps.last() != outer; i++) {
                    ps.popPose();
                }

                Lighting.setupFor3DItems();
                Mc.setWalkSpeed(e, walk);
                saved.forEach((k, v) -> {
                    if (k instanceof ModelPart mp) {
                        mp.visible = v[0];
                    }
                });
            }
        }
    }

    private static float[] bounds(SpBone top) {
        float[] b = new float[]{Float.MAX_VALUE, -Float.MAX_VALUE, Float.MAX_VALUE, -Float.MAX_VALUE};
        ArrayDeque<SpBone> todo = new ArrayDeque<>();
        todo.push(top);
        boolean any = false;

        while (!todo.isEmpty()) {
            SpBone bone = todo.pop();
            todo.addAll(bone.children);

            for (SpCube c : bone.cubes) {
                any = true;
                b[0] = Math.min(b[0], -(c.origin.x + c.size.x) / 16.0F);
                b[1] = Math.max(b[1], -c.origin.x / 16.0F);
                b[2] = Math.min(b[2], c.origin.y / 16.0F);
                b[3] = Math.max(b[3], (c.origin.y + c.size.y) / 16.0F);
            }
        }

        return any ? b : null;
    }

    private record Bind(int entity, String model) {
    }

    public record Params(float scale, int dx, int dy, int yaw, String bone) {
        public static final ChatHeads.Params DEFAULT = new ChatHeads.Params(1.0F, 0, 0, 0, "auto");
    }

    public static final class Setup {
        public static void registerHooks() {
            ClientHooks.CLIENT_SETUP.add(ChatHeads.Setup::clientSetup);
        }

        public static void clientSetup(ClientHooks.ClientSetup e) {
            Workspace.Settings ws = Workspace.settings();
            if (ws.mcp) {
                McpServer.start(ws.mcpPort);
            }
        }
    }
}
