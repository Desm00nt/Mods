package org.sprautestudio.spraute_engine.engine;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.GameRules;
import org.sprautestudio.spraute_engine.platform.Hooks;
import org.sprautestudio.spraute_engine.platform.Priority;
import org.sprautestudio.spraute_engine.platform.TickPhase;

public final class BodyRules {
    private static final Map<UUID, Integer> HURT = new HashMap<>();
    private static Boolean regenRule;

    private BodyRules() {
    }

    public static void registerHooks() {
        Hooks.SERVER_TICK.add(e -> {
            if (e.phase == TickPhase.END) {
                tick(e.getServer());
            }
        });
        Hooks.LIVING_ATTACK.add(Priority.HIGH, BodyRules::onAttack);
        Hooks.PLAYER_LOGGED_OUT.add(e -> HURT.remove(e.getEntity().getUUID()));
    }

    private static void onAttack(Hooks.LivingAttack ev) {
        if (ev.getEntity() instanceof ServerPlayer p) {
            EngineConfig.Body b = EngineServer.config().body;
            if (!b.fallDamage && "fall".equals(ev.getSource().getMsgId())) {
                ev.setCanceled(true);
            } else {
                HURT.put(p.getUUID(), p.getServer() != null ? p.getServer().getTickCount() : 0);
            }
        }
    }

    private static void tick(MinecraftServer server) {
        EngineConfig.Body b = EngineServer.config().body;
        boolean natural = b.regen.equals("game");
        if (!natural && regenRule != Boolean.FALSE) {
            regenRule = false;
            server.getGameRules().getRule(GameRules.RULE_NATURAL_REGENERATION).set(false, server);
        } else if (natural && regenRule == Boolean.FALSE) {
            regenRule = true;
            server.getGameRules().getRule(GameRules.RULE_NATURAL_REGENERATION).set(true, server);
        }

        int now = server.getTickCount();

        for (ServerPlayer p : server.getPlayerList().getPlayers()) {
            if (!p.isSpectator() && !p.isCreative() && p.isAlive()) {
                if (!b.hunger && now % 20 == 0) {
                    p.getFoodData().setFoodLevel(20);
                    p.getFoodData().setSaturation(5.0F);
                }

                if (!b.drowning && p.getAirSupply() < p.getMaxAirSupply()) {
                    p.setAirSupply(p.getMaxAirSupply());
                }

                if (!b.exp && now % 20 == 0 && (p.experienceLevel > 0 || p.experienceProgress > 0.0F)) {
                    p.setExperienceLevels(0);
                    p.setExperiencePoints(0);
                }

                if (b.regen.equals("engine") && p.getHealth() < p.getMaxHealth()) {
                    int every = Math.max(1, Math.round(b.regenEvery * 20.0F));
                    int since = now - HURT.getOrDefault(p.getUUID(), -1073741824);
                    if (since >= b.regenDelay * 20.0F && now % every == 0 && (b.regenFood <= 0 || p.getFoodData().getFoodLevel() >= b.regenFood)) {
                        p.heal(b.regenAmount);
                    }
                }
            }
        }
    }

    public static void unload() {
        regenRule = null;
        HURT.clear();
    }
}
