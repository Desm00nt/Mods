package org.sprautestudio.spraute_engine.platform.forge;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraftforge.client.event.MovementInputUpdateEvent;
import net.minecraftforge.client.event.RegisterClientCommandsEvent;
import net.minecraftforge.client.event.RenderHandEvent;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent.LoggingIn;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent.LoggingOut;
import net.minecraftforge.client.event.InputEvent.InteractionKeyMappingTriggered;
import net.minecraftforge.client.event.InputEvent.Key;
import net.minecraftforge.client.event.InputEvent.MouseScrollingEvent;
import net.minecraftforge.client.event.InputEvent.MouseButton.Pre;
import net.minecraftforge.client.event.RenderGuiEvent.Post;
import net.minecraftforge.client.event.RenderLevelStageEvent.Stage;
import net.minecraftforge.client.event.ScreenEvent.Opening;
import net.minecraftforge.client.event.ViewportEvent.ComputeCameraAngles;
import net.minecraftforge.client.event.ViewportEvent.ComputeFov;
import net.minecraftforge.client.gui.overlay.ForgeGui;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent.ClientTickEvent;
import net.minecraftforge.event.TickEvent.RenderTickEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent.LeftClickEmpty;
import net.minecraftforge.eventbus.api.IEventBus;
import org.sprautestudio.spraute_engine.compat.Gfx;
import org.sprautestudio.spraute_engine.platform.ClientHooks;
import org.sprautestudio.spraute_engine.platform.Priority;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.client.event.RegisterGuiOverlaysEvent;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;

final class ForgeClientEvents {
    private ForgeClientEvents() {
    }

    static void install(IEventBus modBus) {
        ForgeEvents.bridge(ClientTickEvent.class, ClientHooks.CLIENT_TICK, f -> new ClientHooks.ClientTick(ForgeEvents.phase(f.phase)));
        ForgeEvents.bridge(RenderTickEvent.class, ClientHooks.RENDER_TICK, f -> new ClientHooks.RenderTick(ForgeEvents.phase(f.phase), f.renderTickTime));
        ForgeEvents.bridge(LoggingIn.class, ClientHooks.LOGGING_IN, f -> new ClientHooks.LoggingIn());
        ForgeEvents.bridge(LoggingOut.class, ClientHooks.LOGGING_OUT, f -> new ClientHooks.LoggingOut());
        ForgeClientVersion.registerChat();
        ForgeEvents.bridge(
            InteractionKeyMappingTriggered.class,
            ClientHooks.INTERACTION_KEY,
            f -> new ClientHooks.InteractionKey(f.shouldSwingHand()),
            (e, f) -> f.setSwingHand(e.shouldSwingHand())
        );
        ForgeEvents.bridge(Key.class, ClientHooks.KEY, f -> new ClientHooks.Key(f.getKey(), f.getScanCode(), f.getAction(), f.getModifiers()));
        ForgeEvents.bridge(Pre.class, ClientHooks.MOUSE_BUTTON, f -> new ClientHooks.MouseButton(f.getButton(), f.getAction()));
        ForgeEvents.bridge(MouseScrollingEvent.class, ClientHooks.MOUSE_SCROLL, f -> new ClientHooks.MouseScroll(f.getScrollDelta()));
        ForgeEvents.bridge(MovementInputUpdateEvent.class, ClientHooks.MOVEMENT_INPUT, f -> new ClientHooks.MovementInput(f.getInput()));
        ForgeEvents.bridge(RegisterClientCommandsEvent.class, ClientHooks.CLIENT_COMMANDS, f -> new ClientHooks.ClientCommands(f.getDispatcher()));
        ForgeEvents.bridge(Post.class, ClientHooks.RENDER_GUI, f -> new ClientHooks.RenderGui(ForgeClientVersion.gfx(f), f.getPartialTick(), f.getWindow()));
        ForgeEvents.bridge(RenderHandEvent.class, ClientHooks.RENDER_HAND, f -> new ClientHooks.RenderHand());
        ForgeEvents.bridge(
            net.minecraftforge.client.event.RenderPlayerEvent.Pre.class,
            ClientHooks.RENDER_PLAYER,
            f -> new ClientHooks.RenderPlayer(f.getEntity(), f.getPartialTick())
        );
        ForgeEvents.bridge(
            Opening.class, ClientHooks.SCREEN_OPENING, f -> new ClientHooks.ScreenOpening(f.getNewScreen()), (e, f) -> f.setNewScreen(e.getNewScreen())
        );
        ForgeEvents.bridge(
            ComputeCameraAngles.class, ClientHooks.CAMERA_ANGLES, f -> new ClientHooks.CameraAngles(f.getYaw(), f.getPitch(), f.getRoll()), (e, f) -> {
                f.setYaw(e.getYaw());
                f.setPitch(e.getPitch());
                f.setRoll(e.getRoll());
            }
        );
        ForgeEvents.bridge(ComputeFov.class, ClientHooks.COMPUTE_FOV, f -> new ClientHooks.ComputeFov(f.getFOV()), (e, f) -> f.setFOV(e.getFOV()));
        ForgeEvents.bridge(LeftClickEmpty.class, ClientHooks.LEFT_CLICK_EMPTY, f -> new ClientHooks.LeftClickEmpty());

        for (Priority p : ClientHooks.RENDER_HUD_PART.priorities()) {
            MinecraftForge.EVENT_BUS
                .addListener(
                    ForgeEvents.priority(p),
                    false,
                    net.minecraftforge.client.event.RenderGuiOverlayEvent.Pre.class,
                    f -> ForgeEvents.fire(p, ClientHooks.RENDER_HUD_PART, ForgeClientEvents::hudPart, null, f)
                );
        }

        for (Priority p : ClientHooks.RENDER_LEVEL.priorities()) {
            MinecraftForge.EVENT_BUS
                .addListener(
                    ForgeEvents.priority(p),
                    false,
                    RenderLevelStageEvent.class,
                    f -> {
                        ClientHooks.LevelStage stage = f.getStage() == Stage.AFTER_PARTICLES
                            ? ClientHooks.LevelStage.AFTER_PARTICLES
                            : (f.getStage() == Stage.AFTER_WEATHER ? ClientHooks.LevelStage.AFTER_WEATHER : null);
                        if (stage != null) {
                            ClientHooks.RENDER_LEVEL
                                .fire(
                                    p,
                                    new ClientHooks.RenderLevel(stage, f.getPoseStack(), ForgeClientVersion.projection(f), f.getPartialTick(), f.getCamera())
                                );
                        }
                    }
                );
        }

        modBus.addListener((FMLClientSetupEvent f) -> ClientHooks.CLIENT_SETUP.fire(new ClientHooks.ClientSetup()));
        modBus.addListener((RegisterKeyMappingsEvent f) -> ClientHooks.REGISTER_KEYS.fire(f::register));
        modBus.addListener(
            (RegisterGuiOverlaysEvent f) -> ClientHooks.REGISTER_HUD
                .fire((above, name, layer) -> f.registerAbove(above, name, (gui, g, partial, w, h) -> layer.draw(Gfx.of(g), partial, w, h)))
        );
        modBus.addListener((EntityRenderersEvent.RegisterRenderers f) -> ClientHooks.REGISTER_RENDERERS.fire(new ClientHooks.RegisterRenderers() {
            @Override
            public <T extends Entity> void registerEntityRenderer(EntityType<? extends T> type, EntityRendererProvider<T> renderer) {
                f.registerEntityRenderer(type, renderer);
            }

            @Override
            public <T extends BlockEntity> void registerBlockEntityRenderer(BlockEntityType<? extends T> type, BlockEntityRendererProvider<T> renderer) {
                f.registerBlockEntityRenderer(type, renderer);
            }
        }));
    }

    private static ClientHooks.RenderHudPart hudPart(final net.minecraftforge.client.event.RenderGuiOverlayEvent.Pre f) {
        return new ClientHooks.RenderHudPart(f.getOverlay().id(), ForgeClientVersion.gfx(f), f.getPartialTick(), f.getWindow()) {
            @Override
            public boolean canDrawOriginal() {
                return Minecraft.getInstance().gui instanceof ForgeGui;
            }

            @Override
            public void drawOriginal(int width, int height) {
                ForgeClientVersion.drawOverlay(f, this.getGuiGraphics(), this.getPartialTick(), width, height);
            }
        };
    }
}
