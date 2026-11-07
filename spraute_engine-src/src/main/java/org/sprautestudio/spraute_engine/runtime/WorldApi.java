package org.sprautestudio.spraute_engine.runtime;

import com.mojang.authlib.GameProfile;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.function.IntFunction;
import net.minecraft.commands.arguments.EntityAnchorArgument.Anchor;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Direction.Axis;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.ButtonBlock;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.FenceGateBlock;
import net.minecraft.world.level.block.LeverBlock;
import net.minecraft.world.level.block.TrapDoorBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.sprautestudio.spraute_engine.compat.Mc;
import org.sprautestudio.spraute_engine.compat.Regs;
import org.sprautestudio.spraute_engine.core.entity.AnimatedEntity;
import org.sprautestudio.spraute_engine.core.entity.NpcChunks;
import org.sprautestudio.spraute_engine.core.world.Location;
import org.sprautestudio.spraute_engine.platform.Platform;
import org.sprautestudio.spraute_engine.player.PlayerRuleDefs;

public final class WorldApi {
    private static final GameProfile USER = new GameProfile(UUID.nameUUIDFromBytes("spraute:use".getBytes()), "[Spraute]");

    private WorldApi() {
    }

    public static ServerLevel level() {
        ServerPlayer p = SprauteRuntime.player();
        if (p != null) {
            return Mc.serverLevel(p);
        } else {
            MinecraftServer s = Platform.get().server();
            if (s == null) {
                throw new SprauteRuntimeException("Мир не загружен");
            } else {
                return s.overworld();
            }
        }
    }

    public static Vec3 vec(Object v) {
        if (v instanceof EntityRef r) {
            return r.require().position();
        } else if (v instanceof Vec3 l) {
            return l;
        } else if (v instanceof List<?> l && l.size() >= 3) {
            return new Vec3(Eval.num(l.get(0)), Eval.num(l.get(1)), Eval.num(l.get(2)));
        } else {
            throw new SprauteRuntimeException("Ожидалась точка (x, y, z) или сущность");
        }
    }

    public static List<Object> list(Vec3 v) {
        List<Object> out = new ArrayList<>();
        out.add(v.x);
        out.add(v.y);
        out.add(v.z);
        return out;
    }

    private static ResourceLocation id(String s) {
        String t = s.trim().replace("\"", "");
        ResourceLocation rl = ResourceLocation.tryParse(t.contains(":") ? t : "minecraft:" + t);
        if (rl == null) {
            throw new SprauteRuntimeException("Неверный id '" + s + "'");
        } else {
            return rl;
        }
    }

    public static EntityRef summon(String type, Object at) {
        ServerLevel level = level();
        EntityType<?> et = Regs.ENTITY_TYPE.getOptional(id(type)).orElseThrow(() -> new SprauteRuntimeException("Нет такой сущности: " + type));
        Vec3 p = vec(at);
        NpcChunks.load(level, p);
        Entity e = et.create(level);
        if (e == null) {
            throw new SprauteRuntimeException("Сущность " + type + " нельзя создать");
        } else {
            e.moveTo(p.x, p.y, p.z, level.random.nextFloat() * 360.0F, 0.0F);
            SprauteRuntime.markOwned(e);
            if (e instanceof Mob m) {
                m.finalizeSpawn(level, level.getCurrentDifficultyAt(Mc.blockPos(p)), MobSpawnType.COMMAND, null, null);
                m.setPersistenceRequired();
            }

            level.addFreshEntity(e);
            return new EntityRef(e);
        }
    }

    public static List<EntityRef> spawnItem(ServerLevel level, Vec3 at, ItemStack stack, Vec3 motion, int pickupDelay, boolean neverPickup) {
        List<EntityRef> made = new ArrayList<>();
        int left = stack.getCount();

        while (left > 0) {
            int n = Math.min(left, Math.max(1, stack.getMaxStackSize()));
            ItemEntity item = new ItemEntity(level, at.x, at.y, at.z, Mc.copyWithCount(stack, n), motion.x, motion.y, motion.z);
            if (neverPickup) {
                item.setNeverPickUp();
            } else {
                item.setPickUpDelay(pickupDelay);
            }

            SprauteRuntime.markOwned(item);
            level.addFreshEntity(item);
            made.add(new EntityRef(item));
            left -= n;
        }

        return made;
    }

    private static boolean typeMatches(Entity e, String type) {
        String t = type.trim().replace("\"", "");
        if (t.isEmpty() || t.equals("any")) {
            return true;
        } else if (t.equals("player")) {
            return e instanceof Player;
        } else if (!t.equals("mob")) {
            String full = t.contains(":") ? t : "minecraft:" + t;
            return String.valueOf(Regs.ENTITY_TYPE.getKey(e.getType())).equals(full);
        } else {
            return e instanceof LivingEntity && !(e instanceof Player);
        }
    }

    public static List<Object> entities(String type, Object center, double radius) {
        Vec3 c = vec(center);
        double r = Math.max(0.5, Math.min(256.0, radius));
        List<Entity> found = level()
            .getEntities((Entity)null, new AABB(c, c).inflate(r), ex -> ex.isAlive() && typeMatches(ex, type) && ex.position().distanceTo(c) <= r);
        found.sort((a, b) -> Double.compare(a.position().distanceToSqr(c), b.position().distanceToSqr(c)));
        List<Object> out = new ArrayList<>();

        for (Entity e : found) {
            out.add(new EntityRef(e));
        }

        return out;
    }

    public static Object nearest(String type, Object center, double radius) {
        List<Object> l = entities(type, center, radius);
        return l.isEmpty() ? "" : l.get(0);
    }

    public static Object get(EntityRef ref, String name) {
        if (name.equals("alive")) {
            return ref.alive();
        } else if (name.equals("type")) {
            return ref.type;
        } else if (name.equals("uuid")) {
            return ref.uuid.toString();
        } else {
            Entity e = ref.require();
            LivingEntity le = e instanceof LivingEntity l ? l : null;
            switch (name) {
                case "pos":
                case "position":
                    return list(e.position());
                case "x":
                    return e.getX();
                case "y":
                    return e.getY();
                case "z":
                    return e.getZ();
                case "eyes":
                    return list(e.getEyePosition());
                case "look":
                    return list(e.getLookAngle());
                case "yaw":
                    return (double)e.getYRot();
                case "pitch":
                    return (double)e.getXRot();
                case "speed":
                case "velocity":
                    return list(e.getDeltaMovement());
                case "inair":
                    return !Mc.onGround(e);
                case "flying": {
                    return e instanceof Player p ? p.getAbilities().flying : !Mc.onGround(e) && e.isNoGravity();
                }
                case "canfly": {
                    return e instanceof Player p && p.getAbilities().mayfly;
                }
                case "flyspeed": {
                    return e instanceof Player p ? p.getAbilities().getFlyingSpeed() : 0.0;
                }
                case "movespeed":
                    AttributeInstance ai = le != null ? le.getAttribute(Attributes.MOVEMENT_SPEED) : null;
                    return ai != null ? ai.getBaseValue() : 0.0;
                case "slotsize": {
                    return e instanceof Player p ? PlayerSlots.size(p) : 0.0;
                }
                case "falldistance":
                    return (double)e.fallDistance;
                case "fallsafe":
                    return Mechanics.fallSafeLeft(e);
                case "undying":
                    return Mechanics.undying(e);
                case "onground":
                    return Mc.onGround(e);
                case "name":
                    return e.getName().getString();
                case "id":
                    return (double)e.getId();
                case "health":
                case "hp":
                    return le != null ? le.getHealth() : 0.0;
                case "maxhealth":
                case "maxhp":
                    return le != null ? le.getMaxHealth() : 0.0;
                case "target":
                    return e instanceof Mob m && m.getTarget() != null ? new EntityRef(m.getTarget()) : "";
                case "item":
                case "hand":
                    return le != null ? String.valueOf(Regs.ITEM.getKey(le.getMainHandItem().getItem())) : "";
                case "sneaking":
                    return e.isShiftKeyDown();
                case "sprinting":
                    return e.isSprinting();
                case "level": {
                    return e instanceof Player p ? p.experienceLevel : 0.0;
                }
                case "food": {
                    return e instanceof Player p ? p.getFoodData().getFoodLevel() : 0.0;
                }
                case "dimension":
                    return Mc.level(e).dimension().location().toString();
                case "fire":
                    return e.isOnFire();
                case "glowing":
                    return e.hasGlowingTag();
                case "invisible":
                    return e.isInvisible();
                case "dist":
                case "distance": {
                    ServerPlayer p = SprauteRuntime.player();
                    return p != null ? e.position().distanceTo(p.position()) : -1.0;
                }
                default:
                    throw new SprauteRuntimeException(
                        "У сущности нет свойства ."
                            + name
                            + ". Есть: pos, x, y, z, eyes, look, yaw, pitch, speed, onground, name, type, health, maxhealth, target, item, alive, dist, sneaking, sprinting, level, food, dimension, fire, glowing, invisible"
                    );
            }
        }
    }

    public static void set(EntityRef ref, String name, Object v) {
        Entity e = ref.require();
        LivingEntity le = e instanceof LivingEntity l ? l : null;
        switch (name) {
            case "health":
            case "hp":
                if (le != null) {
                    le.setHealth((float)Math.max(0.0, Eval.num(v)));
                }
                break;
            case "lockbody":
                if (e instanceof AnimatedEntity ae) {
                    ae.setBodyLocked(Eval.truthy(v));
                }
                break;
            case "lockhead":
                if (e instanceof AnimatedEntity ae) {
                    ae.setHeadLocked(Eval.truthy(v));
                }
                break;
            case "noturn":
                if (e instanceof AnimatedEntity ae) {
                    ae.setNoTurn(Eval.truthy(v));
                }
                break;
            case "holdchunk":
                if (e instanceof AnimatedEntity ae) {
                    ae.setHoldsChunk(Eval.truthy(v));
                }
                break;
            case "blink":
                if (e instanceof AnimatedEntity ae) {
                    ae.blinks = Eval.truthy(v);
                }
                break;
            case "maxhealth":
            case "maxhp":
                if (e instanceof AnimatedEntity ae) {
                    ae.setMaxHealth((float)Math.max(1.0, Eval.num(v)));
                    return;
                }

                AttributeInstance a = le != null ? le.getAttribute(Attributes.MAX_HEALTH) : null;
                if (a != null) {
                    a.setBaseValue(Math.max(1.0, Eval.num(v)));
                    le.setHealth(le.getMaxHealth());
                }
                break;
            case "name":
                String s = Eval.str(v);
                e.setCustomName(s.isEmpty() ? null : Component.literal(s));
                e.setCustomNameVisible(!s.isEmpty());
                break;
            case "pos":
            case "position": {
                Vec3 p = vec(v);
                e.teleportTo(p.x, p.y, p.z);
                break;
            }
            case "x":
            case "y":
            case "z":
                double n = Eval.num(v);
                e.teleportTo(name.equals("x") ? n : e.getX(), name.equals("y") ? n : e.getY(), name.equals("z") ? n : e.getZ());
                break;
            case "yaw":
                e.setYRot((float)Eval.num(v));
                e.setYHeadRot((float)Eval.num(v));
                break;
            case "speed":
            case "velocity":
                e.setDeltaMovement(vec(v));
                e.hurtMarked = true;
                break;
            case "target":
                if (!(e instanceof Mob m)) {
                    throw new SprauteRuntimeException(".target — только у мобов");
                }

                m.setTarget(v instanceof EntityRef r && r.get() instanceof LivingEntity t ? t : null);
                break;
            case "ai":
                if (e instanceof Mob m) {
                    m.setNoAi(!Eval.truthy(v));
                }
                break;
            case "glowing":
                e.setGlowingTag(Eval.truthy(v));
                break;
            case "invisible":
                e.setInvisible(Eval.truthy(v));
                break;
            case "gravity":
                e.setNoGravity(!Eval.truthy(v));
                break;
            case "canfly": {
                if (!(e instanceof Player p)) {
                    throw new SprauteRuntimeException(".canfly — только у игрока");
                }

                p.getAbilities().mayfly = Eval.truthy(v);
                if (!Eval.truthy(v) && !p.isCreative() && !p.isSpectator()) {
                    p.getAbilities().flying = false;
                }

                p.onUpdateAbilities();
                break;
            }
            case "flying": {
                if (!(e instanceof Player p)) {
                    throw new SprauteRuntimeException(".flying — только у игрока");
                }

                boolean on = Eval.truthy(v);
                if (on) {
                    p.getAbilities().mayfly = true;
                }

                p.getAbilities().flying = on;
                p.onUpdateAbilities();
                break;
            }
            case "flyspeed": {
                if (!(e instanceof Player p)) {
                    throw new SprauteRuntimeException(".flyspeed — только у игрока");
                }

                p.getAbilities().setFlyingSpeed((float)Math.max(0.0, Math.min(1.0, Eval.num(v))));
                p.onUpdateAbilities();
                break;
            }
            case "movespeed":
                AttributeInstance ai = e instanceof LivingEntity l2 ? l2.getAttribute(Attributes.MOVEMENT_SPEED) : null;
                if (ai == null) {
                    throw new SprauteRuntimeException(".movespeed — только у живых");
                }

                ai.setBaseValue(Math.max(0.0, Eval.num(v)));
                break;
            case "falldistance":
                e.fallDistance = (float)Math.max(0.0, Eval.num(v));
                break;
            case "fallsafe":
                Mechanics.fallSafe(e, Eval.num(v));
                break;
            case "undying":
                Mechanics.setUndying(e, Eval.truthy(v));
                break;
            case "silent":
                e.setSilent(Eval.truthy(v));
                break;
            case "invulnerable":
                e.setInvulnerable(Eval.truthy(v));
                break;
            case "fire":
                e.setSecondsOnFire((int)Math.round(Eval.num(v)));
                break;
            case "item":
            case "hand":
                if (le != null) {
                    le.setItemSlot(EquipmentSlot.MAINHAND, stack(Eval.str(v), 1));
                }
                break;
            case "level": {
                if (e instanceof ServerPlayer p) {
                    p.setExperienceLevels((int)Eval.num(v));
                }
                break;
            }
            case "food": {
                if (e instanceof Player p) {
                    p.getFoodData().setFoodLevel((int)Eval.num(v));
                }
                break;
            }
            default:
                throw new SprauteRuntimeException(
                    "Свойство ."
                        + name
                        + " нельзя изменить. Можно: health, maxhealth, name, pos, x, y, z, yaw, speed, target, ai, glowing, invisible, gravity, silent, invulnerable, fire, item, level, food"
                );
        }
    }

    private static ItemStack stack(String item, int count) {
        if (item.isBlank() || item.equals("air")) {
            return ItemStack.EMPTY;
        } else if (ItemSpec.error(item) != null) {
            throw new SprauteRuntimeException("Предмет " + item + ": ошибка в NBT: " + ItemSpec.error(item));
        } else {
            ItemStack s = ItemSpec.stack(item, count);
            if (s.isEmpty()) {
                throw new SprauteRuntimeException("Нет такого предмета: " + item);
            } else {
                return s;
            }
        }
    }

    private static EquipmentSlot slot(String s) {
        String var1 = s.trim().toLowerCase();

        return switch (var1) {
            case "offhand", "left" -> EquipmentSlot.OFFHAND;
            case "head", "helmet" -> EquipmentSlot.HEAD;
            case "chest", "chestplate" -> EquipmentSlot.CHEST;
            case "legs", "leggings" -> EquipmentSlot.LEGS;
            case "feet", "boots" -> EquipmentSlot.FEET;
            default -> EquipmentSlot.MAINHAND;
        };
    }

    private static Object arg(List<Object> a, int i, Object def) {
        return i < a.size() ? a.get(i) : def;
    }

    public static Object call(EntityRef ref, String name, List<Object> a) {
        if (name.equals("alive")) {
            return ref.alive();
        } else {
            Entity e = ref.require();
            LivingEntity le = e instanceof LivingEntity l ? l : null;
            switch (name) {
                case "slotlocked": {
                    return e instanceof Player p && PlayerSlots.locked(p, (int)Eval.num(arg(a, 0, 1.0)) - 1);
                }
                case "can": {
                    String what = Eval.str(arg(a, 0, ""));
                    PlayerRuleDefs.Rule rule = PlayerRuleDefs.get(what);
                    if (rule != null && rule.isSwitch()) {
                        if (e instanceof Player px) {
                            return what.trim().equals("fly") ? px.getAbilities().mayfly : PlayerRules.can(px, rule.name());
                        }

                        return true;
                    }

                    throw new SprauteRuntimeException("Нет такого разрешения игрока: " + what);
                }
                case "rule": {
                    PlayerRuleDefs.Rule rule = PlayerRuleDefs.get(Eval.str(arg(a, 0, "")));
                    if (rule == null) {
                        throw new SprauteRuntimeException("Нет такого правила игрока: " + arg(a, 0, ""));
                    }

                    return e instanceof Player p ? PlayerRules.number(p, rule.name(), rule.def()) : rule.def();
                }
                case "kill":
                    e.kill();
                    break;
                case "remove":
                case "despawn":
                    if (e instanceof Player) {
                        throw new SprauteRuntimeException("Игрока нельзя убрать из мира");
                    }

                    e.discard();
                    break;
                case "tp":
                case "teleport": {
                    Vec3 p = vec(arg(a, 0, ""));
                    e.teleportTo(p.x, p.y, p.z);
                    break;
                }
                case "damage":
                case "hurt":
                    e.hurt(Mc.generic(Mc.level(e)), (float)Eval.num(arg(a, 0, 1.0)));
                    break;
                case "heal":
                    if (le != null) {
                        le.heal((float)Eval.num(arg(a, 0, 1.0)));
                    }
                    break;
                case "effect":
                    if (le != null) {
                        MobEffect eff = Regs.MOB_EFFECT
                            .getOptional(id(Eval.str(arg(a, 0, ""))))
                            .orElseThrow(() -> new SprauteRuntimeException("Нет такого эффекта: " + arg(a, 0, "")));
                        int seconds = (int)Eval.num(arg(a, 1, 10.0));
                        int lvl = (int)Eval.num(arg(a, 2, 1.0));
                        le.addEffect(new MobEffectInstance(eff, Math.max(1, seconds * 20), Math.max(0, lvl - 1)));
                    }
                    break;
                case "cleareffects":
                    if (le != null) {
                        le.removeAllEffects();
                    }
                    break;
                case "push":
                    Mc.addDeltaMovement(e, vec(arg(a, 0, "")));
                    e.hurtMarked = true;
                    break;
                case "dash": {
                    double p = Eval.num(arg(a, 0, 1.0));
                    Vec3 look = e.getViewVector(1.0F);
                    Vec3 flat = new Vec3(look.x, 0.0, look.z);
                    if (flat.lengthSqr() > 1.0E-6) {
                        flat = flat.normalize();
                    }

                    e.setDeltaMovement(flat.x * p, Math.max(e.getDeltaMovement().y, 0.15), flat.z * p);
                    e.hurtMarked = true;
                    break;
                }
                case "launch":
                    Vec3 m = e.getDeltaMovement();
                    e.setDeltaMovement(m.x, Eval.num(arg(a, 0, 1.0)), m.z);
                    e.hurtMarked = true;
                    break;
                case "knockback": {
                    Object from = arg(a, 0, "");
                    Vec3 c = from instanceof EntityRef r ? r.require().position() : vec(from);
                    Vec3 d = e.position().subtract(c);
                    d = new Vec3(d.x, 0.0, d.z);
                    if (d.lengthSqr() < 1.0E-6) {
                        d = new Vec3(0.0, 0.0, 1.0);
                    }

                    double p = Eval.num(arg(a, 1, 1.0));
                    d = d.normalize().scale(p);
                    e.push(d.x, 0.3 * p + 0.1, d.z);
                    e.hurtMarked = true;
                    break;
                }
                case "pull": {
                    Object to = arg(a, 0, "");
                    Vec3 c = to instanceof EntityRef r ? r.require().position() : vec(to);
                    Vec3 d = c.subtract(e.position());
                    if (d.lengthSqr() < 1.0E-6) {
                        return ref;
                    }

                    double p = Eval.num(arg(a, 1, 1.0));
                    d = d.normalize().scale(p);
                    e.push(d.x, d.y * 0.5 + 0.15, d.z);
                    e.hurtMarked = true;
                    break;
                }
                case "stop":
                    e.setDeltaMovement(Vec3.ZERO);
                    e.fallDistance = 0.0F;
                    e.hurtMarked = true;
                    break;
                case "lookat": {
                    e.lookAt(
                        Anchor.EYES, a.isEmpty() ? e.position() : (arg(a, 0, "") instanceof EntityRef r ? r.require().getEyePosition() : vec(arg(a, 0, "")))
                    );
                    break;
                }
                case "give": {
                    if (!(e instanceof Player p)) {
                        throw new SprauteRuntimeException(".give() — только у игрока");
                    }

                    ItemStack var25 = stack(Eval.str(arg(a, 0, "")), (int)Eval.num(arg(a, 1, 1.0)));
                    if (!p.addItem(var25)) {
                        p.drop(var25, false);
                    }
                    break;
                }
                case "equip":
                    if (le != null) {
                        le.setItemSlot(slot(Eval.str(arg(a, 0, "mainhand"))), stack(Eval.str(arg(a, 1, "")), 1));
                    }
                    break;
                case "distance":
                case "dist":
                    return e.position().distanceTo(vec(arg(a, 0, "")));
                case "has": {
                    if (e instanceof Player p) {
                        String item = Eval.str(arg(a, 0, ""));
                        stack(item, 1);
                        return ItemSpec.count(p, item) >= (int)Eval.num(arg(a, 1, 1.0));
                    }

                    return false;
                }
                case "count": {
                    if (e instanceof Player p) {
                        String item = Eval.str(arg(a, 0, ""));
                        stack(item, 1);
                        return (double)ItemSpec.count(p, item);
                    }

                    return 0.0;
                }
                default:
                    throw new SprauteRuntimeException(
                        "У сущности нет метода ."
                            + name
                            + "(). Есть: kill, remove, tp, damage, heal, effect, cleareffects, push, dash, launch, knockback, pull, stop, lookat, give, equip, distance, has, count"
                    );
            }

            return ref;
        }
    }

    public static double itemCount(String item) {
        ServerPlayer p = SprauteRuntime.player();
        if (p == null) {
            return 0.0;
        } else {
            stack(item, 1);
            return ItemSpec.count(p, item);
        }
    }

    public static double give(String item, int count) {
        ServerPlayer p = SprauteRuntime.player();
        if (p != null && count > 0) {
            ItemStack s = stack(item, count);
            if (!p.getInventory().add(s.copy())) {
                p.drop(s.copy(), false);
            }

            return count;
        } else {
            return 0.0;
        }
    }

    public static double take(String item, int count) {
        ServerPlayer p = SprauteRuntime.player();
        if (p != null && count > 0) {
            stack(item, 1);
            return ItemSpec.take(p, item, count);
        } else {
            return 0.0;
        }
    }

    public static String heldItem(Object who) {
        return who instanceof EntityRef r && r.require() instanceof LivingEntity le
            ? String.valueOf(Regs.ITEM.getKey(le.getMainHandItem().getItem()))
            : "minecraft:air";
    }

    public static MobEffectInstance effect(Object who, String effect) {
        if (who instanceof EntityRef r && r.alive() && r.require() instanceof LivingEntity le) {
            MobEffect var5 = Regs.MOB_EFFECT.getOptional(id(effect)).orElseThrow(() -> new SprauteRuntimeException("Нет такого эффекта: " + effect));
            return le.getEffect(var5);
        } else {
            return null;
        }
    }

    public static String weather() {
        ServerLevel l = level();
        return l.isThundering() ? "thunder" : (l.isRaining() ? "rain" : "clear");
    }

    public static String biome(Object at) {
        return level().getBiome(pos(at)).unwrapKey().map(k -> k.location().toString()).orElse("");
    }

    public static double light(Object at) {
        return level().getMaxLocalRawBrightness(pos(at));
    }

    public static boolean sky(Object at) {
        return level().canSeeSky(pos(at));
    }

    private static BlockPos pos(Object at) {
        return Mc.blockPos(vec(at));
    }

    public static String block(Object at) {
        return String.valueOf(Regs.BLOCK.getKey(level().getBlockState(pos(at)).getBlock()));
    }

    public static Object blockProp(Object at, String prop) {
        BlockState s = level().getBlockState(pos(at));

        for (Property<?> p : s.getProperties()) {
            if (p.getName().equals(prop)) {
                return value(s, p);
            }
        }

        return "";
    }

    private static <T extends Comparable<T>> Object value(BlockState s, Property<T> p) {
        T v = s.getValue(p);
        if (v instanceof Boolean b) {
            return b;
        } else {
            return v instanceof Integer i ? (double)i.intValue() : p.getName(v);
        }
    }

    public static void setBlock(Object at, String block) {
        ServerLevel level = level();
        String b = block.trim().replace("\"", "");
        if (!b.contains(":") && !b.startsWith("[")) {
            b = "minecraft:" + b;
        }

        BlockState state;
        try {
            state = Mc.parseBlock(level, b);
        } catch (Exception var6) {
            throw new SprauteRuntimeException("Не получилось разобрать блок '" + block + "': " + var6.getMessage());
        }

        level.setBlock(pos(at), state, 3);
    }

    public static void useBlock(Entity who, Object at) {
        ServerLevel level = Mc.level(who) instanceof ServerLevel l ? l : level();
        BlockPos p = pos(at);
        BlockState s = level.getBlockState(p);
        if (!s.isAir()) {
            Vec3 from = who.getEyePosition();
            Vec3 middle = Vec3.atCenterOf(p);
            Direction face = Direction.getNearest(from.x - middle.x, from.y - middle.y, from.z - middle.z);
            BlockHitResult hit = new BlockHitResult(middle, face, p, false);
            InteractionHand hand = InteractionHand.MAIN_HAND;
            if (who instanceof LivingEntity le) {
                le.swing(hand, true);
            }

            Block block = s.getBlock();
            boolean worldOnly = block instanceof DoorBlock
                || block instanceof TrapDoorBlock
                || block instanceof FenceGateBlock
                || block instanceof ButtonBlock
                || block instanceof LeverBlock;
            if (who instanceof ServerPlayer player && !worldOnly) {
                s.use(level, player, hand, hit);
            } else {
                ServerPlayer user = Platform.get().fakePlayer(level, USER);
                user.moveTo(who.getX(), who.getY(), who.getZ(), who.getYRot(), who.getXRot());
                user.setItemInHand(hand, who instanceof LivingEntity le ? le.getMainHandItem().copy() : ItemStack.EMPTY);

                try {
                    s.use(level, user, hand, hit);
                } finally {
                    user.setItemInHand(hand, ItemStack.EMPTY);
                }
            }
        }
    }

    public static void setProp(Object at, String prop, Object value) {
        ServerLevel level = level();
        BlockPos p = pos(at);
        BlockState s = level.getBlockState(p);

        for (Property<?> pr : s.getProperties()) {
            if (pr.getName().equals(prop)) {
                level.setBlock(p, with(s, pr, value), 3);
                return;
            }
        }

        throw new SprauteRuntimeException("У блока " + block(at) + " нет свойства '" + prop + "'");
    }

    private static <T extends Comparable<T>> BlockState with(BlockState s, Property<T> p, Object value) {
        String v = value instanceof Double d && d == Math.rint(d) ? String.valueOf(d.longValue()) : Eval.str(value);
        Optional<T> parsed = p.getValue(v.toLowerCase());
        if (!parsed.isEmpty()) {
            return s.setValue(p, parsed.get());
        } else {
            List<String> ok = new ArrayList<>();

            for (T t : p.getPossibleValues()) {
                ok.add(p.getName(t));
            }

            throw new SprauteRuntimeException("Свойство '" + p.getName() + "' не может быть '" + v + "'. Можно: " + String.join(", ", ok));
        }
    }

    public static boolean inBox(Object at, Object a, Object b) {
        Vec3 p = vec(at);
        Vec3 x = vec(a);
        Vec3 y = vec(b);
        return p.x >= Math.min(x.x, y.x)
            && p.x <= Math.max(x.x, y.x) + 1.0
            && p.y >= Math.min(x.y, y.y)
            && p.y <= Math.max(x.y, y.y) + 1.0
            && p.z >= Math.min(x.z, y.z)
            && p.z <= Math.max(x.z, y.z) + 1.0;
    }

    public static Vec3 ground(Vec3 p) {
        ServerLevel lv = level();
        int x = (int)Math.floor(p.x);
        int z = (int)Math.floor(p.z);
        int y0 = (int)Math.floor(p.y);
        IntFunction<Double> top = y -> {
            BlockPos at = new BlockPos(x, y, z);
            VoxelShape shape = lv.getBlockState(at).getCollisionShape(lv, at);
            if (shape.isEmpty()) {
                return null;
            } else {
                double tx = y + shape.max(Axis.Y);

                for (int k = 0; k < 2; k++) {
                    BlockPos above = new BlockPos(x, (int)Math.floor(tx + 0.01) + k, z);
                    if (above.getY() != y && !lv.getBlockState(above).getCollisionShape(lv, above).isEmpty()) {
                        return null;
                    }
                }

                return tx;
            }
        };

        for (int dy = 0; dy <= 27; dy++) {
            Double t = top.apply(y0 + 2 - dy);
            if (t != null) {
                return new Vec3(x + 0.5, t, z + 0.5);
            }
        }

        for (int dyx = 3; dyx <= 24; dyx++) {
            Double t = top.apply(y0 + dyx);
            if (t != null) {
                return new Vec3(x + 0.5, t, z + 0.5);
            }
        }

        return new Vec3(x + 0.5, p.y, z + 0.5);
    }

    public static Vec3 of(Location l) {
        return l;
    }
}
