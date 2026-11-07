package org.sprautestudio.spraute_engine.platform;

import com.mojang.blaze3d.platform.Window;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.client.Camera;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.player.Input;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import org.joml.Matrix4f;
import org.sprautestudio.spraute_engine.compat.Gfx;

public final class ClientHooks {
    public static final Hook<ClientHooks.ClientTick> CLIENT_TICK = new Hook<>();
    public static final Hook<ClientHooks.RenderTick> RENDER_TICK = new Hook<>();
    public static final Hook<ClientHooks.LoggingIn> LOGGING_IN = new Hook<>();
    public static final Hook<ClientHooks.LoggingOut> LOGGING_OUT = new Hook<>();
    public static final Hook<ClientHooks.ChatFromPlayer> CHAT_FROM_PLAYER = new Hook<>();
    public static final Hook<ClientHooks.InteractionKey> INTERACTION_KEY = new Hook<>();
    public static final Hook<ClientHooks.Key> KEY = new Hook<>();
    public static final Hook<ClientHooks.MouseButton> MOUSE_BUTTON = new Hook<>();
    public static final Hook<ClientHooks.MouseScroll> MOUSE_SCROLL = new Hook<>();
    public static final Hook<ClientHooks.MovementInput> MOVEMENT_INPUT = new Hook<>();
    public static final Hook<ClientHooks.ClientCommands> CLIENT_COMMANDS = new Hook<>();
    public static final Hook<ClientHooks.RenderGui> RENDER_GUI = new Hook<>();
    public static final Hook<ClientHooks.RenderHand> RENDER_HAND = new Hook<>();
    public static final Hook<ClientHooks.RenderPlayer> RENDER_PLAYER = new Hook<>();
    public static final Hook<ClientHooks.ScreenOpening> SCREEN_OPENING = new Hook<>();
    public static final Hook<ClientHooks.CameraAngles> CAMERA_ANGLES = new Hook<>();
    public static final Hook<ClientHooks.ComputeFov> COMPUTE_FOV = new Hook<>();
    public static final Hook<ClientHooks.LeftClickEmpty> LEFT_CLICK_EMPTY = new Hook<>();
    public static final Hook<ClientHooks.ClientSetup> CLIENT_SETUP = new Hook<>();
    public static final Hook<ClientHooks.RegisterKeys> REGISTER_KEYS = new Hook<>();
    public static final Hook<ClientHooks.RegisterHud> REGISTER_HUD = new Hook<>();
    public static final Hook<ClientHooks.RegisterRenderers> REGISTER_RENDERERS = new Hook<>();
    public static final Hook<ClientHooks.RenderHudPart> RENDER_HUD_PART = new Hook<>();
    public static final Hook<ClientHooks.RenderLevel> RENDER_LEVEL = new Hook<>();

    private ClientHooks() {
    }

    public static final class CameraAngles {
        private float yaw;
        private float pitch;
        private float roll;

        public CameraAngles(float yaw, float pitch, float roll) {
            this.yaw = yaw;
            this.pitch = pitch;
            this.roll = roll;
        }

        public float getYaw() {
            return this.yaw;
        }

        public void setYaw(float value) {
            this.yaw = value;
        }

        public float getPitch() {
            return this.pitch;
        }

        public void setPitch(float value) {
            this.pitch = value;
        }

        public float getRoll() {
            return this.roll;
        }

        public void setRoll(float value) {
            this.roll = value;
        }
    }

    public static final class ChatFromPlayer extends Cancellable {
    }

    public static final class ClientCommands {
        private final CommandDispatcher<CommandSourceStack> dispatcher;

        public ClientCommands(CommandDispatcher<CommandSourceStack> dispatcher) {
            this.dispatcher = dispatcher;
        }

        public CommandDispatcher<CommandSourceStack> getDispatcher() {
            return this.dispatcher;
        }
    }

    public static final class ClientSetup {
    }

    public static final class ClientTick {
        public final TickPhase phase;

        public ClientTick(TickPhase phase) {
            this.phase = phase;
        }
    }

    public static final class ComputeFov {
        private double fov;

        public ComputeFov(double fov) {
            this.fov = fov;
        }

        public double getFOV() {
            return this.fov;
        }

        public void setFOV(double value) {
            this.fov = value;
        }
    }

    @FunctionalInterface
    public interface HudLayer {
        void draw(Gfx var1, float var2, int var3, int var4);
    }

    public static final class HudParts {
        public static final ResourceLocation PLAYER_HEALTH = new ResourceLocation("minecraft", "player_health");
        public static final ResourceLocation ARMOR_LEVEL = new ResourceLocation("minecraft", "armor_level");
        public static final ResourceLocation FOOD_LEVEL = new ResourceLocation("minecraft", "food_level");
        public static final ResourceLocation AIR_LEVEL = new ResourceLocation("minecraft", "air_level");
        public static final ResourceLocation EXPERIENCE_BAR = new ResourceLocation("minecraft", "experience_bar");
        public static final ResourceLocation HOTBAR = new ResourceLocation("minecraft", "hotbar");
        public static final ResourceLocation CHAT_PANEL = new ResourceLocation("minecraft", "chat_panel");

        private HudParts() {
        }
    }

    public static final class InteractionKey extends Cancellable {
        private boolean swingHand;

        public InteractionKey(boolean swingHand) {
            this.swingHand = swingHand;
        }

        public boolean shouldSwingHand() {
            return this.swingHand;
        }

        public void setSwingHand(boolean value) {
            this.swingHand = value;
        }
    }

    public static final class Key {
        private final int key;
        private final int scanCode;
        private final int action;
        private final int modifiers;

        public Key(int key, int scanCode, int action, int modifiers) {
            this.key = key;
            this.scanCode = scanCode;
            this.action = action;
            this.modifiers = modifiers;
        }

        public int getKey() {
            return this.key;
        }

        public int getScanCode() {
            return this.scanCode;
        }

        public int getAction() {
            return this.action;
        }

        public int getModifiers() {
            return this.modifiers;
        }
    }

    public static final class LeftClickEmpty {
    }

    public static enum LevelStage {
        AFTER_PARTICLES,
        AFTER_WEATHER;
    }

    public static final class LoggingIn {
    }

    public static final class LoggingOut {
    }

    public static final class MouseButton extends Cancellable {
        private final int button;
        private final int action;

        public MouseButton(int button, int action) {
            this.button = button;
            this.action = action;
        }

        public int getButton() {
            return this.button;
        }

        public int getAction() {
            return this.action;
        }
    }

    public static final class MouseScroll extends Cancellable {
        private final double scrollDelta;

        public MouseScroll(double scrollDelta) {
            this.scrollDelta = scrollDelta;
        }

        public double getScrollDelta() {
            return this.scrollDelta;
        }
    }

    public static final class MovementInput {
        private final Input input;

        public MovementInput(Input input) {
            this.input = input;
        }

        public Input getInput() {
            return this.input;
        }
    }

    public interface RegisterHud {
        void registerAbove(ResourceLocation var1, String var2, ClientHooks.HudLayer var3);
    }

    public interface RegisterKeys {
        void register(KeyMapping var1);
    }

    public interface RegisterRenderers {
        <T extends Entity> void registerEntityRenderer(EntityType<? extends T> var1, EntityRendererProvider<T> var2);

        <T extends BlockEntity> void registerBlockEntityRenderer(BlockEntityType<? extends T> var1, BlockEntityRendererProvider<T> var2);
    }

    public static final class RenderGui {
        private final Gfx guiGraphics;
        private final float partialTick;
        private final Window window;

        public RenderGui(Gfx guiGraphics, float partialTick, Window window) {
            this.guiGraphics = guiGraphics;
            this.partialTick = partialTick;
            this.window = window;
        }

        public Gfx getGuiGraphics() {
            return this.guiGraphics;
        }

        public float getPartialTick() {
            return this.partialTick;
        }

        public Window getWindow() {
            return this.window;
        }
    }

    public static final class RenderHand extends Cancellable {
    }

    public abstract static class RenderHudPart extends Cancellable {
        private final ResourceLocation id;
        private final Gfx guiGraphics;
        private final float partialTick;
        private final Window window;

        protected RenderHudPart(ResourceLocation id, Gfx guiGraphics, float partialTick, Window window) {
            this.id = id;
            this.guiGraphics = guiGraphics;
            this.partialTick = partialTick;
            this.window = window;
        }

        public ResourceLocation id() {
            return this.id;
        }

        public Gfx getGuiGraphics() {
            return this.guiGraphics;
        }

        public float getPartialTick() {
            return this.partialTick;
        }

        public Window getWindow() {
            return this.window;
        }

        public abstract boolean canDrawOriginal();

        public abstract void drawOriginal(int var1, int var2);
    }

    public static final class RenderLevel {
        private final ClientHooks.LevelStage stage;
        private final PoseStack poseStack;
        private final Matrix4f projectionMatrix;
        private final float partialTick;
        private final Camera camera;

        public RenderLevel(ClientHooks.LevelStage stage, PoseStack poseStack, Matrix4f projectionMatrix, float partialTick, Camera camera) {
            this.stage = stage;
            this.poseStack = poseStack;
            this.projectionMatrix = projectionMatrix;
            this.partialTick = partialTick;
            this.camera = camera;
        }

        public ClientHooks.LevelStage getStage() {
            return this.stage;
        }

        public PoseStack getPoseStack() {
            return this.poseStack;
        }

        public Matrix4f getProjectionMatrix() {
            return this.projectionMatrix;
        }

        public float getPartialTick() {
            return this.partialTick;
        }

        public Camera getCamera() {
            return this.camera;
        }
    }

    public static final class RenderPlayer extends Cancellable {
        private final Player entity;
        private final float partialTick;

        public RenderPlayer(Player entity, float partialTick) {
            this.entity = entity;
            this.partialTick = partialTick;
        }

        public Player getEntity() {
            return this.entity;
        }

        public float getPartialTick() {
            return this.partialTick;
        }
    }

    public static final class RenderTick {
        public final TickPhase phase;
        public final float renderTickTime;

        public RenderTick(TickPhase phase, float renderTickTime) {
            this.phase = phase;
            this.renderTickTime = renderTickTime;
        }
    }

    public static final class ScreenOpening {
        private Screen newScreen;

        public ScreenOpening(Screen newScreen) {
            this.newScreen = newScreen;
        }

        public Screen getNewScreen() {
            return this.newScreen;
        }

        public void setNewScreen(Screen value) {
            this.newScreen = value;
        }
    }
}
