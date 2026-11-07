package org.sprautestudio.spraute_engine;

import com.mojang.logging.LogUtils;
import java.util.HashMap;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import org.slf4j.Logger;
import org.sprautestudio.spraute_engine.addon.Addons;
import org.sprautestudio.spraute_engine.compat.Mc;
import org.sprautestudio.spraute_engine.content.ProjectContent;
import org.sprautestudio.spraute_engine.core.anim.AnimationSystem;
import org.sprautestudio.spraute_engine.core.script.Behaviours;
import org.sprautestudio.spraute_engine.core.script.ScriptCommand;
import org.sprautestudio.spraute_engine.core.script.ScriptEnvironment;
import org.sprautestudio.spraute_engine.core.system.CameraState;
import org.sprautestudio.spraute_engine.core.system.Coins;
import org.sprautestudio.spraute_engine.core.system.PlayerTexture;
import org.sprautestudio.spraute_engine.core.system.WorldClock;
import org.sprautestudio.spraute_engine.custom.CustomItems;
import org.sprautestudio.spraute_engine.custom.CustomMobs;
import org.sprautestudio.spraute_engine.engine.EngineServer;
import org.sprautestudio.spraute_engine.export.BundledProjects;
import org.sprautestudio.spraute_engine.fight.Fights;
import org.sprautestudio.spraute_engine.fx.Casts;
import org.sprautestudio.spraute_engine.hitbox.Hitboxes;
import org.sprautestudio.spraute_engine.net.SprauteNetwork;
import org.sprautestudio.spraute_engine.path.Walkers;
import org.sprautestudio.spraute_engine.platform.Hooks;
import org.sprautestudio.spraute_engine.platform.Platform;
import org.sprautestudio.spraute_engine.platform.Priority;
import org.sprautestudio.spraute_engine.platform.TickPhase;
import org.sprautestudio.spraute_engine.quest.Quests;
import org.sprautestudio.spraute_engine.runtime.Anims;
import org.sprautestudio.spraute_engine.runtime.GameEvents;
import org.sprautestudio.spraute_engine.runtime.Globals;
import org.sprautestudio.spraute_engine.runtime.ItemSpec;
import org.sprautestudio.spraute_engine.runtime.JsEngine;
import org.sprautestudio.spraute_engine.runtime.Mechanics;
import org.sprautestudio.spraute_engine.runtime.PlayerRules;
import org.sprautestudio.spraute_engine.runtime.PlayerSlots;
import org.sprautestudio.spraute_engine.runtime.PlayerVars;
import org.sprautestudio.spraute_engine.runtime.SprauteRuntime;
import org.sprautestudio.spraute_engine.runtime.models.ModelLibrary;
import org.sprautestudio.spraute_engine.runtime.orb.Orbs;
import org.sprautestudio.spraute_engine.shop.Shops;
import org.sprautestudio.spraute_engine.skills.Skills;
import org.sprautestudio.spraute_engine.stealth.Stealth;
import org.sprautestudio.spraute_engine.zone.Zones;

public final class SprauteEngine {
    public static final String MODID = "spraute_engine";
    public static final Logger LOGGER = LogUtils.getLogger();
    private static boolean tickEvents;

    private SprauteEngine() {
    }

    public static void init() {
        ItemSpec.install();
        AnimationSystem.setLibrary(Anims::resolve);
        Orbs.init();
        Hooks.COMMON_SETUP.add(e -> SprauteNetwork.register());
        Hooks.SERVER_ABOUT_TO_START.add(SprauteEngine::onServerAboutToStart);
        Hooks.SERVER_STOPPING.add(SprauteEngine::onServerStopping);
        Hooks.LEVEL_SAVE.add(SprauteEngine::onLevelSave);
        Hooks.SERVER_STOPPED.add(SprauteEngine::onServerStopped);
        Hooks.REGISTER_COMMANDS.add(Priority.LOWEST, SprauteEngine::onRegisterCommands);
        Hooks.SERVER_TICK.add(SprauteEngine::onServerTick);
        Hooks.PLAYER_LOGGED_IN.add(SprauteEngine::onPlayerLogin);
        Hooks.PLAYER_LOGGED_OUT.add(SprauteEngine::onPlayerLogout);
        BundledProjects.install();
        CustomItems.register();
        CustomMobs.register();
        Modules.common();
        if (Platform.get().isClient()) {
            ClientModules.client();
        }

        Addons.load();
    }

    private static void onPlayerLogin(Hooks.PlayerLoggedIn event) {
        if (event.getEntity() instanceof ServerPlayer sp) {
            EngineServer.sync(sp);
            Coins.sync(sp, -2);
            PlayerTexture.sendTo(sp);
            SprauteRuntime.env().ifPresent(env -> env.sendTo(sp));
        }
    }

    private static void onPlayerLogout(Hooks.PlayerLoggedOut event) {
        if (event.getEntity() instanceof ServerPlayer sp) {
            CameraState.left(sp);
        }
    }

    private static void onServerAboutToStart(Hooks.ServerAboutToStart event) {
        SprauteRuntime.installErrorHook();
        ModelLibrary.scan();
        EngineServer.load(event.getServer());
        SprauteRuntime.get().loadWorld();
        SprauteRuntime.get().openEnvironment(event.getServer());
        SprauteRuntime.env().ifPresent(SprauteRuntime.get()::registerInEnv);
        BundledProjects.worldOpened();
    }

    private static void onServerStopping(Hooks.ServerStopping event) {
        SprauteRuntime.env().ifPresent(ScriptEnvironment::save);
    }

    private static void onLevelSave(Hooks.LevelSave event) {
        if (event.getLevel() instanceof ServerLevel l && l.dimension() == Level.OVERWORLD) {
            SprauteRuntime.env().ifPresent(ScriptEnvironment::save);
        }
    }

    private static void onServerStopped(Hooks.ServerStopped event) {
        SprauteRuntime.get().clear();
        SprauteRuntime.get().closeEnvironment();
        Behaviours.unload();
        WorldClock.unload();
        CameraState.unload();
        ProjectContent.unload();
        EngineServer.unload();
        Shops.unload();
        Quests.unload();
        Globals.unload();
        PlayerVars.unload();
        PlayerRules.unload();
        PlayerSlots.unload();
        Walkers.unload();
        Stealth.unload();
        Skills.unload();
        JsEngine.unload();
        Zones.unload();
        Fights.unload();
        Casts.unload();
        Mechanics.unload();
        Hitboxes.clearServer();
        GameEvents.reset();
    }

    private static void onRegisterCommands(Hooks.RegisterCommands event) {
        ScriptCommand.register(event.getDispatcher());
        event.getDispatcher()
            .register(
                Commands.literal("sprautejs")
                    .requires(s -> s.hasPermission(4))
                    .then(
                        Commands.literal("unsafe")
                            .then(
                                Commands.literal("on")
                                    .executes(
                                        ctx -> {
                                            JsEngine.setUnsafe(true);
                                            Mc.success(
                                                ctx.getSource(),
                                                Component.literal(
                                                    "Сюжеты этого мира могут использовать классы Java (java(), jcall… и JavaScript). Включайте только для сюжетов, которым доверяете."
                                                ),
                                                true
                                            );
                                            return 1;
                                        }
                                    )
                            )
                            .then(Commands.literal("off").executes(ctx -> {
                                JsEngine.setUnsafe(false);
                                Mc.success(ctx.getSource(), Component.literal("Сюжеты снова в песочнице: без Java в коде и в JavaScript."), true);
                                return 1;
                            }))
                            .executes(
                                ctx -> {
                                    boolean on = JsEngine.unsafe();
                                    Mc.success(
                                        ctx.getSource(),
                                        Component.literal(
                                            on
                                                ? "Java в сюжетах и их JavaScript: разрешена (/sprautejs unsafe off — выключить)"
                                                : "Java в сюжетах и их JavaScript: запрещена (/sprautejs unsafe on — разрешить)"
                                        ),
                                        false
                                    );
                                    return 1;
                                }
                            )
                    )
            );
    }

    private static void onServerTick(Hooks.ServerTick event) {
        if (event.phase == TickPhase.END) {
            SprauteRuntime.env().ifPresent(ScriptEnvironment::tick);
            ProjectContent.tick(event.getServer());
            BundledProjects.tick(event.getServer());
            if (event.getServer().getTickCount() % 100 == 0) {
                tickEvents = false;

                for (SprauteRuntime.Deployed d : SprauteRuntime.get().all().values()) {
                    if (d.source.contains("servertick") || d.source.contains("playertick")) {
                        tickEvents = true;
                    }
                }
            }

            if (tickEvents) {
                GameEvents.fire("servertick", null, new HashMap<>());

                for (ServerPlayer p : event.getServer().getPlayerList().getPlayers()) {
                    GameEvents.fire("playertick", p, new HashMap<>());
                }
            }
        }
    }
}
