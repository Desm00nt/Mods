package org.sprautestudio.spraute_engine.runtime;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import javax.script.Bindings;
import javax.script.ScriptEngine;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.storage.LevelResource;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.openjdk.nashorn.api.scripting.ClassFilter;
import org.openjdk.nashorn.api.scripting.NashornScriptEngineFactory;
import org.openjdk.nashorn.api.scripting.ScriptObjectMirror;
import org.sprautestudio.spraute_engine.compat.Mc;
import org.sprautestudio.spraute_engine.compat.Regs;
import org.sprautestudio.spraute_engine.core.script.Character;
import org.sprautestudio.spraute_engine.platform.Platform;

public final class JsEngine {
    private static final Map<Integer, ScriptEngine> ENGINES = new HashMap<>();
    private static Boolean unsafe;

    private JsEngine() {
    }

    private static Path settings() {
        MinecraftServer s = Platform.get().server();
        return s == null ? null : s.getWorldPath(LevelResource.ROOT).resolve("spraute").resolve("js_unsafe");
    }

    public static boolean unsafe() {
        if (unsafe == null) {
            Path p = settings();
            unsafe = p != null && Files.exists(p);
        }

        return unsafe;
    }

    public static void setUnsafe(boolean on) {
        unsafe = on;
        ENGINES.clear();
        Path p = settings();
        if (p != null) {
            try {
                if (on) {
                    Files.createDirectories(p.getParent());
                    Files.writeString(p, "JavaScript of stories may use Java classes", StandardCharsets.UTF_8);
                } else {
                    Files.deleteIfExists(p);
                }
            } catch (Exception var3) {
            }
        }
    }

    public static void unload() {
        ENGINES.clear();
        unsafe = null;
    }

    public static void forget(int scriptId) {
        ENGINES.remove(scriptId);
    }

    private static ScriptEngine engine(SprauteScriptInstance s) {
        return ENGINES.computeIfAbsent(
            s.getId(),
            id -> {
                boolean open = unsafe();
                ClassFilter filter = name -> open;
                ScriptEngine e = new NashornScriptEngineFactory().getScriptEngine(new String[]{"--language=es6"}, JsEngine.class.getClassLoader(), filter);
                Bindings b = e.getBindings(100);
                b.put("story", new JsEngine.Story(s));
                b.put("world", new JsEngine.World());
                if (!open) {
                    b.put("engine", null);
                    b.put("context", null);

                    try {
                        e.eval(
                            "delete this.Java; delete this.Packages; delete this.java; delete this.javax; delete this.com; delete this.org; delete this.net;delete this.JavaImporter; delete this.load; delete this.loadWithNewGlobal; delete this.exit; delete this.quit;Object.defineProperty(this,'engine',{value:undefined,writable:false,configurable:false});Object.defineProperty(this,'context',{value:undefined,writable:false,configurable:false});"
                        );
                    } catch (Exception var7) {
                        throw new SprauteRuntimeException("JS: не удалось закрыть песочницу — код не выполнен");
                    }
                }

                return e;
            }
        );
    }

    public static Object run(SprauteScriptInstance s, String code) {
        ScriptEngine e = engine(s);
        ServerPlayer p = SprauteRuntime.player();
        e.getBindings(100).put("player", p == null ? null : new JsEngine.Ent(p));
        String body = code.contains("return") ? "(function(){\n" + code + "\n})()" : code;

        try {
            return toStory(e.eval(body));
        } catch (Exception var7) {
            String m = var7.getMessage() == null ? var7.toString() : var7.getMessage();
            if (m.contains("ClassNotFoundException") || m.contains("is not defined") && (m.contains("Java") || m.contains("Packages"))) {
                m = m + " (классы Java выключены — оператор мира может разрешить: /sprautejs unsafe on)";
            }

            throw new SprauteRuntimeException("JS: " + m.replace("<eval>", "строка"));
        }
    }

    static Object toStory(Object v) {
        if (v == null) {
            return "";
        } else if (v instanceof Number n) {
            return n.doubleValue();
        } else if (v instanceof Boolean || v instanceof String) {
            return v;
        } else if (v instanceof JsEngine.Ent en) {
            return new EntityRef(en.e);
        } else if (v instanceof ScriptObjectMirror m) {
            if (m.isArray()) {
                List<Object> out = new ArrayList<>();

                for (Object x : m.values()) {
                    out.add(toStory(x));
                }

                return out;
            } else {
                Map<String, Object> out = new LinkedHashMap<>();

                for (String k : m.keySet()) {
                    out.put(k, toStory(m.get(k)));
                }

                return out;
            }
        } else if (v instanceof Entity en) {
            return new EntityRef(en);
        } else if (!(v instanceof List<?> l)) {
            return String.valueOf(v);
        } else {
            List<Object> out = new ArrayList<>();

            for (Object x : l) {
                out.add(toStory(x));
            }

            return out;
        }
    }

    static Object toJs(Object v) {
        if (v instanceof EntityRef r) {
            Entity e = r.get();
            return e == null ? null : new JsEngine.Ent(e);
        } else if (!(v instanceof List<?> l)) {
            return v;
        } else {
            List<Object> out = new ArrayList<>();

            for (Object x : l) {
                out.add(toJs(x));
            }

            return out;
        }
    }

    public static final class Ent {
        final Entity e;

        Ent(Entity e) {
            this.e = e;
        }

        public double getX() {
            return this.e.getX();
        }

        public double getY() {
            return this.e.getY();
        }

        public double getZ() {
            return this.e.getZ();
        }

        public String getName() {
            return this.e.getName().getString();
        }

        public String getType() {
            return String.valueOf(Regs.ENTITY_TYPE.getKey(this.e.getType()));
        }

        public boolean isAlive() {
            return this.e.isAlive();
        }

        public double getHealth() {
            return this.e instanceof LivingEntity le ? le.getHealth() : 0.0;
        }

        public void setHealth(double v) {
            if (this.e instanceof LivingEntity le) {
                le.setHealth((float)v);
            }
        }

        public double getMaxHealth() {
            return this.e instanceof LivingEntity le ? le.getMaxHealth() : 0.0;
        }

        public float getYaw() {
            return this.e.getYRot();
        }

        public void tp(double x, double y, double z) {
            this.e.teleportTo(x, y, z);
        }

        public void push(double x, double y, double z) {
            this.e.push(x, y, z);
            this.e.hurtMarked = true;
        }

        public void setGlowing(boolean on) {
            this.e.setGlowingTag(on);
        }

        public void kill() {
            this.e.kill();
        }

        public Object nbt(String path) {
            return JsEngine.toJs(Bridges.nbt(new EntityRef(this.e), path));
        }

        public void setNbt(String path, Object value) {
            Bridges.setNbt(new EntityRef(this.e), path, JsEngine.toStory(value));
        }

        public double attr(String id) {
            return Bridges.attr(new EntityRef(this.e), id);
        }

        public void setAttr(String id, double v) {
            Bridges.setAttr(new EntityRef(this.e), id, v);
        }

        public void effect(String id, double seconds, int level) {
            if (this.e instanceof LivingEntity le) {
                MobEffect eff = Regs.MOB_EFFECT.get(new ResourceLocation(id.contains(":") ? id : "minecraft:" + id));
                if (eff != null) {
                    le.addEffect(new MobEffectInstance(eff, (int)Math.round(seconds * 20.0), Math.max(0, level - 1)));
                }
            }
        }

        public void give(String item, int count) {
            if (this.e instanceof Player p) {
                p.getInventory().add(ItemSpec.stack(item, count));
            }
        }

        public String getHand() {
            return this.e instanceof LivingEntity le ? String.valueOf(Regs.ITEM.getKey(le.getMainHandItem().getItem())) : "";
        }

        public double distance(JsEngine.Ent other) {
            return this.e.distanceTo(other.e);
        }

        public Object getRaw() {
            if (!JsEngine.unsafe()) {
                throw new SecurityException("entity.raw — только при /sprautejs unsafe on");
            } else {
                return this.e;
            }
        }

        @Override
        public String toString() {
            return this.getType() + " «" + this.getName() + "»";
        }
    }

    public static final class Story {
        private final SprauteScriptInstance s;

        Story(SprauteScriptInstance s) {
            this.s = s;
        }

        public Object get(String name) {
            return JsEngine.toJs(this.s.vars.get(name));
        }

        public void set(String name, Object value) {
            this.s.vars.put(name, JsEngine.toStory(value));
        }

        public JsEngine.Ent character(String name) {
            Character<?> ch = this.s.characters.get(name);
            if (ch == null) {
                return null;
            } else {
                Entity e = (Entity)this.s.optional(ch).orElse(null);
                return e == null ? null : new JsEngine.Ent(e);
            }
        }

        public void log(Object text) {
            SprauteRuntime.get().log(this.s.getId(), String.valueOf(text));
        }

        public Object global(String name) {
            return JsEngine.toJs(Globals.get(name, null));
        }

        public void setGlobal(String name, Object value) {
            Globals.set(name, JsEngine.toStory(value));
        }

        public int getId() {
            return this.s.getId();
        }
    }

    public static final class World {
        public String command(String cmd) {
            return Bridges.run(cmd).output();
        }

        public int commandValue(String cmd) {
            return Bridges.run(cmd).value();
        }

        public String block(double x, double y, double z) {
            return String.valueOf(Regs.BLOCK.getKey(WorldApi.level().getBlockState(Mc.blockPos(x, y, z)).getBlock()));
        }

        public void setBlock(double x, double y, double z, String id) {
            Block b = Regs.BLOCK.get(new ResourceLocation(id.contains(":") ? id : "minecraft:" + id));
            WorldApi.level().setBlockAndUpdate(Mc.blockPos(x, y, z), b.defaultBlockState());
        }

        public long getTime() {
            return WorldApi.level().getDayTime();
        }

        public void setTime(long t) {
            WorldApi.level().setDayTime(t);
        }

        public String getDimension() {
            return WorldApi.level().dimension().location().toString();
        }

        public List<JsEngine.Ent> entities(String type, double x, double y, double z, double r) {
            List<JsEngine.Ent> out = new ArrayList<>();
            AABB box = new AABB(x - r, y - r, z - r, x + r, y + r, z + r);

            for (Entity e : WorldApi.level().getEntities((Entity)null, box, en -> true)) {
                String t = String.valueOf(Regs.ENTITY_TYPE.getKey(e.getType()));
                if (type == null || type.isEmpty() || t.equals(type) || t.equals("minecraft:" + type)) {
                    out.add(new JsEngine.Ent(e));
                }
            }

            return out;
        }

        public JsEngine.Ent summon(String type, double x, double y, double z) {
            return (Object)WorldApi.summon(type, WorldApi.list(new Vec3(x, y, z))) instanceof EntityRef er && er.get() != null ? new JsEngine.Ent(er.get()) : null;
        }

        public boolean hasMod(String id) {
            return Bridges.hasMod(id);
        }

        public double score(Object who, String objective) {
            return Bridges.score(who instanceof JsEngine.Ent en ? new EntityRef(en.e) : who, objective);
        }

        public void setScore(Object who, String objective, double v) {
            Bridges.setScore(who instanceof JsEngine.Ent en ? new EntityRef(en.e) : who, objective, v, false);
        }

        public void say(String text) {
            ServerPlayer p = SprauteRuntime.player();
            if (p != null) {
                p.sendSystemMessage(Component.literal(text));
            }
        }

        public Object getServer() {
            if (!JsEngine.unsafe()) {
                throw new SecurityException("world.server — только при /sprautejs unsafe on");
            } else {
                return Platform.get().server();
            }
        }
    }
}
