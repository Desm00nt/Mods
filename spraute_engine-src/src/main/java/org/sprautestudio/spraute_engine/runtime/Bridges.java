package org.sprautestudio.spraute_engine.runtime;

import com.mojang.brigadier.StringReader;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import net.minecraft.commands.CommandSource;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.arguments.NbtPathArgument;
import net.minecraft.commands.arguments.NbtPathArgument.NbtPath;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.ByteTag;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.DoubleTag;
import net.minecraft.nbt.IntTag;
import net.minecraft.nbt.NumericTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.nbt.TagParser;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.scores.Objective;
import net.minecraft.world.scores.Score;
import net.minecraft.world.scores.Scoreboard;
import net.minecraft.world.scores.criteria.ObjectiveCriteria;
import net.minecraft.world.scores.criteria.ObjectiveCriteria.RenderType;
import org.sprautestudio.spraute_engine.compat.Mc;
import org.sprautestudio.spraute_engine.compat.Regs;
import org.sprautestudio.spraute_engine.platform.Platform;

public final class Bridges {
    private Bridges() {
    }

    private static NbtPath path(String p) {
        try {
            return new NbtPathArgument().parse(new StringReader(p));
        } catch (Exception var2) {
            throw new SprauteRuntimeException("Неверный путь NBT «" + p + "»: " + var2.getMessage());
        }
    }

    private static CompoundTag data(Object target) {
        if (target instanceof EntityRef r) {
            Entity e = r.require();
            return e.saveWithoutId(new CompoundTag());
        } else {
            BlockEntity be = blockEntity(target);
            return be.saveWithoutMetadata();
        }
    }

    private static BlockEntity blockEntity(Object target) {
        Vec3 v = WorldApi.vec(target);
        BlockPos pos = Mc.blockPos(v.x, v.y, v.z);
        BlockEntity be = WorldApi.level().getBlockEntity(pos);
        if (be == null) {
            throw new SprauteRuntimeException("В " + pos.toShortString() + " нет блока с данными (сундука, печи…)");
        } else {
            return be;
        }
    }

    public static Object nbt(Object target, String p) {
        CompoundTag tag = data(target);
        if (p != null && !p.isBlank()) {
            List<Tag> found;
            try {
                found = path(p).get(tag);
            } catch (Exception var5) {
                return "";
            }

            return found.isEmpty() ? "" : value(found.get(0));
        } else {
            return tag.toString();
        }
    }

    static Object value(Tag t) {
        if (t instanceof NumericTag n) {
            return n.getAsDouble();
        } else {
            return t instanceof StringTag s ? s.getAsString() : t.toString();
        }
    }

    static Tag tag(Object v) {
        if (v instanceof Boolean b) {
            return ByteTag.valueOf(b);
        } else if (!(v instanceof Number n)) {
            String s = Eval.str(v);

            try {
                CompoundTag wrap = TagParser.parseTag("{v:" + s + "}");
                Tag t = wrap.get("v");
                if (t != null) {
                    return t;
                }
            } catch (Exception var4) {
            }

            return StringTag.valueOf(s);
        } else {
            double d = n.doubleValue();
            return (Tag)(d == Math.rint(d) && Math.abs(d) < 2.147483647E9 ? IntTag.valueOf((int)d) : DoubleTag.valueOf(d));
        }
    }

    public static void setNbt(Object target, String p, Object value) {
        NbtPath path = path(p);
        if (target instanceof EntityRef r) {
            Entity e = r.require();
            if (e instanceof Player) {
                throw new SprauteRuntimeException("NBT игрока менять нельзя — используйте команды и функции (give, effect, health…)");
            } else {
                CompoundTag tag = e.saveWithoutId(new CompoundTag());

                try {
                    path.set(tag, tag(value));
                } catch (Exception var8) {
                    throw new SprauteRuntimeException("NBT: " + var8.getMessage());
                }

                UUID id = e.getUUID();
                e.load(tag);
                e.setUUID(id);
            }
        } else {
            BlockEntity be = blockEntity(target);
            CompoundTag tag = be.saveWithoutMetadata();

            try {
                path.set(tag, tag(value));
            } catch (Exception var9) {
                throw new SprauteRuntimeException("NBT: " + var9.getMessage());
            }

            be.load(tag);
            be.setChanged();
            ServerLevel lv = WorldApi.level();
            lv.sendBlockUpdated(be.getBlockPos(), be.getBlockState(), be.getBlockState(), 3);
        }
    }

    private static Attribute attribute(String id) {
        ResourceLocation rl = ResourceLocation.tryParse(id.contains(":") ? id : "minecraft:" + id);
        Attribute a = rl == null ? null : Regs.ATTRIBUTE.get(rl);
        if (a == null) {
            throw new SprauteRuntimeException("Нет атрибута «" + id + "» (например minecraft:generic.max_health, generic.attack_damage)");
        } else {
            return a;
        }
    }

    private static LivingEntity living(Object target) {
        if (target instanceof EntityRef r && r.require() instanceof LivingEntity le) {
            return le;
        } else {
            throw new SprauteRuntimeException("Атрибуты есть только у живых существ и игрока");
        }
    }

    public static double attr(Object target, String id) {
        AttributeInstance inst = living(target).getAttribute(attribute(id));
        return inst == null ? 0.0 : inst.getValue();
    }

    public static void setAttr(Object target, String id, double value) {
        AttributeInstance inst = living(target).getAttribute(attribute(id));
        if (inst == null) {
            throw new SprauteRuntimeException("У этого существа нет атрибута «" + id + "»");
        } else {
            inst.setBaseValue(value);
        }
    }

    private static String holder(Object target) {
        return target instanceof EntityRef r ? r.require().getScoreboardName() : Eval.str(target);
    }

    private static Objective objective(String name, boolean create) {
        Scoreboard sb = server().getScoreboard();
        Objective o = sb.getObjective(name);
        if (o == null && create) {
            o = sb.addObjective(name, ObjectiveCriteria.DUMMY, Component.literal(name), RenderType.INTEGER);
        }

        return o;
    }

    public static double score(Object target, String objective) {
        Objective o = objective(objective, false);
        if (o == null) {
            return 0.0;
        } else {
            Scoreboard sb = server().getScoreboard();
            String h = holder(target);
            return sb.hasPlayerScore(h, o) ? sb.getOrCreatePlayerScore(h, o).getScore() : 0.0;
        }
    }

    public static void setScore(Object target, String objective, double value, boolean add) {
        Objective o = objective(objective, true);
        Score s = server().getScoreboard().getOrCreatePlayerScore(holder(target), o);
        int v = (int)Math.round(value);
        if (add) {
            s.add(v);
        } else {
            s.setScore(v);
        }
    }

    private static MinecraftServer server() {
        MinecraftServer s = Platform.get().server();
        if (s == null) {
            throw new SprauteRuntimeException("Мир не запущен");
        } else {
            return s;
        }
    }

    public static Bridges.CommandResult run(String command) {
        MinecraftServer server = server();
        String line = command.startsWith("/") ? command.substring(1) : command;
        final List<String> out = new ArrayList<>();
        CommandSource capture = new CommandSource() {
            @Override
            public void sendSystemMessage(Component message) {
                out.add(message.getString());
            }

            @Override
            public boolean acceptsSuccess() {
                return true;
            }

            @Override
            public boolean acceptsFailure() {
                return true;
            }

            @Override
            public boolean shouldInformAdmins() {
                return false;
            }
        };
        ServerPlayer p = SprauteRuntime.player();
        CommandSourceStack src = (p != null ? p.createCommandSourceStack() : server.createCommandSourceStack()).withSource(capture).withPermission(4);
        int[] result = new int[]{0};
        src = src.withCallback((ctx, ok, value) -> result[0] = ok ? value : 0);
        int r = server.getCommands().performPrefixedCommand(src, line);
        return new Bridges.CommandResult(result[0] != 0 ? result[0] : r, String.join("\n", out));
    }

    public static boolean hasMod(String id) {
        return Platform.get().isModLoaded(id.trim().toLowerCase());
    }

    public record CommandResult(int value, String output) {
    }
}
