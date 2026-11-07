package org.sprautestudio.spraute_engine.platform.forge;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.Event;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.GenericEvent;
import net.minecraftforge.server.ServerLifecycleHooks;
import org.sprautestudio.spraute_engine.SprauteEngine;
import org.sprautestudio.spraute_engine.compat.Mc;
import org.sprautestudio.spraute_engine.compat.Regs;
import org.sprautestudio.spraute_engine.runtime.EntityRef;
import org.sprautestudio.spraute_engine.runtime.GameEvents;
import org.sprautestudio.spraute_engine.runtime.SprauteRuntimeException;

final class ForgeBridge {
    private static final Set<Class<?>> LISTENING = new HashSet<>();
    private static final Map<Class<?>, List<Method>> GETTERS = new HashMap<>();
    private static final String[] PACKAGES = new String[]{
        "net.minecraftforge.event.",
        "net.minecraftforge.event.entity.",
        "net.minecraftforge.event.entity.living.",
        "net.minecraftforge.event.entity.player.",
        "net.minecraftforge.event.entity.item.",
        "net.minecraftforge.event.level.",
        "net.minecraftforge.event.server.",
        "net.minecraftforge.event.village.",
        "net.minecraftforge.event.furnace.",
        "net.minecraftforge.event.brewing.",
        "net.minecraftforge.event.enchanting."
    };
    private static final String[] OUTER = new String[]{
        "net.minecraftforge.event.entity.living.LivingEvent",
        "net.minecraftforge.event.entity.player.PlayerEvent",
        "net.minecraftforge.event.entity.player.PlayerInteractEvent",
        "net.minecraftforge.event.entity.EntityEvent",
        "net.minecraftforge.event.level.BlockEvent",
        "net.minecraftforge.event.TickEvent",
        "net.minecraftforge.event.level.LevelEvent",
        "net.minecraftforge.event.entity.living.LivingEntityUseItemEvent",
        "net.minecraftforge.event.entity.player.PlayerXpEvent",
        "net.minecraftforge.event.entity.living.MobEffectEvent",
        "net.minecraftforge.event.level.ChunkEvent",
        "net.minecraftforge.event.entity.player.ItemTooltipEvent"
    };

    private ForgeBridge() {
    }

    public static Class<?> resolve(String name) {
        String n = name.trim();
        List<String> tries = new ArrayList<>();
        if (n.contains(".")) {
            tries.add(n);
            int i = n.lastIndexOf(46);
            tries.add(n.substring(0, i) + "$" + n.substring(i + 1));
        }

        for (String p : PACKAGES) {
            tries.add(p + n.replace('.', '$'));
        }

        for (String o : OUTER) {
            tries.add(o + "$" + n);
        }

        ClassLoader cl = ForgeBridge.class.getClassLoader();

        for (String t : tries) {
            try {
                Class<?> c = Class.forName(t, false, cl);
                if (Event.class.isAssignableFrom(c)) {
                    return c;
                }
            } catch (Throwable var7) {
            }
        }

        return null;
    }

    public static void listen(String name) {
        Class<?> c = resolve(name);
        if (c == null) {
            throw new SprauteRuntimeException(
                "Нет события Forge «"
                    + name
                    + "». Напишите полное имя класса, например \"net.minecraftforge.event.entity.living.LivingEvent$LivingJumpEvent\" или короткое: \"LivingJumpEvent\""
            );
        } else if (GenericEvent.class.isAssignableFrom(c)) {
            throw new SprauteRuntimeException("События с типом (" + c.getSimpleName() + ") пока не поддерживаются");
        } else if (Modifier.isAbstract(c.getModifiers())) {
            throw new SprauteRuntimeException("«" + c.getSimpleName() + "» — общий класс; нужно конкретное событие");
        } else {
            synchronized (LISTENING) {
                if (!LISTENING.add(c)) {
                    return;
                }
            }

            MinecraftForge.EVENT_BUS.addListener(EventPriority.LOW, true, (Class<Event>)c, e -> fired(e));
            SprauteEngine.LOGGER.info("Spraute: listening to {}", c.getName());
        }
    }

    private static void fired(Event e) {
        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        if (server != null && server.isSameThread()) {
            Map<String, Object> d;
            try {
                d = data(e);
            } catch (Throwable var9) {
                return;
            }

            if (d != null) {
                Player player = null;

                for (Object v : d.values()) {
                    if (v instanceof EntityRef r && r.get() instanceof Player p) {
                        player = p;
                    }
                }

                GameEvents.post("forge", player instanceof ServerPlayer ? player : null, d);
            }
        }
    }

    private static Map<String, Object> data(Event e) {
        Class<?> c = e.getClass();
        Map<String, Object> d = new LinkedHashMap<>();
        String full = c.getName();
        d.put("event", full.substring(full.lastIndexOf(46) + 1).replaceAll("^.*\\$", ""));
        d.put("class", full);
        boolean first = true;

        for (Method m : getters(c)) {
            Object v;
            try {
                v = m.invoke(e);
            } catch (Throwable var10) {
                continue;
            }

            String key = key(m.getName());
            if (v instanceof Entity en) {
                if (Mc.level(en).isClientSide) {
                    return null;
                }

                if (first) {
                    d.put("__entity", en);
                    first = false;
                }

                d.put(key, new EntityRef(en));
                d.putIfAbsent(key + "type", String.valueOf(Regs.ENTITY_TYPE.getKey(en.getType())));
            } else {
                if (v instanceof Level lv && lv.isClientSide) {
                    return null;
                }

                Object conv = convert(v);
                if (conv != null) {
                    d.put(key, conv);
                }
            }
        }

        if (e.isCancelable()) {
            d.put("canceled", e.isCanceled());
        }

        return d;
    }

    private static Object convert(Object v) {
        if (v == null) {
            return null;
        } else if (v instanceof Number n) {
            return n.doubleValue();
        } else if (v instanceof Boolean || v instanceof String) {
            return v;
        } else if (v instanceof Enum<?> en) {
            return en.name().toLowerCase();
        } else if (v instanceof BlockPos p) {
            return List.of((double)p.getX(), (double)p.getY(), (double)p.getZ());
        } else if (v instanceof Vec3 p) {
            return List.of(p.x, p.y, p.z);
        } else if (v instanceof ItemStack st) {
            return st.isEmpty() ? "" : String.valueOf(Regs.ITEM.getKey(st.getItem()));
        } else if (v instanceof BlockState st) {
            return String.valueOf(Regs.BLOCK.getKey(st.getBlock()));
        } else if (v instanceof Level lv) {
            return lv.dimension().location().toString();
        } else if (v instanceof Component cm) {
            return cm.getString();
        } else if (v instanceof DamageSource ds) {
            return ds.getMsgId();
        } else {
            return v instanceof ResourceLocation rl ? rl.toString() : null;
        }
    }

    private static String key(String method) {
        String s = method.startsWith("get") ? method.substring(3) : (method.startsWith("is") ? method.substring(2) : method);
        return s.isEmpty() ? method : Character.toLowerCase(s.charAt(0)) + s.substring(1);
    }

    private static List<Method> getters(Class<?> c) {
        synchronized (GETTERS) {
            return GETTERS.computeIfAbsent(
                c,
                k -> {
                    List<Method> out = new ArrayList<>();

                    for (Method m : k.getMethods()) {
                        if (m.getParameterCount() == 0 && !Modifier.isStatic(m.getModifiers()) && m.getDeclaringClass() != Object.class) {
                            String n = m.getName();
                            if ((n.startsWith("get") || n.startsWith("is"))
                                && m.getReturnType() != void.class
                                && !Set.of("getClass", "getListenerList", "getResult", "getPhase", "isCancelable", "hasResult", "isCanceled").contains(n)) {
                                m.setAccessible(true);
                                out.add(m);
                            }
                        }
                    }

                    out.sort(Comparator.comparing(Method::getName));
                    out.sort(Comparator.comparing(mx -> !mx.getName().equals("getEntity")));
                    return out;
                }
            );
        }
    }
}
