package org.sprautestudio.spraute_engine.compat;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.VertexSorting;
import com.mojang.blaze3d.vertex.PoseStack.Pose;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import java.nio.file.Path;
import java.util.Set;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.Font.DisplayMode;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.screens.ConnectScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.client.multiplayer.resolver.ServerAddress;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.arguments.blocks.BlockStateParser;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.Position;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientboundAddEntityPacket;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.packs.PackResources;
import net.minecraft.server.packs.PathPackResources;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Item.Properties;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.Level.ExplosionInteraction;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;

public final class Mc {
    private Mc() {
    }

    public static Level level(Entity e) {
        return e.level();
    }

    public static ServerLevel serverLevel(ServerPlayer p) {
        return p.serverLevel();
    }

    public static boolean onGround(Entity e) {
        return e.onGround();
    }

    public static void guiProjection(int w, int h) {
        RenderSystem.setProjectionMatrix(new Matrix4f().setOrtho(0.0F, w, h, 0.0F, 1000.0F, 3000.0F), VertexSorting.ORTHOGRAPHIC_Z);
    }

    public static float maxUpStep(Entity e) {
        return e.maxUpStep();
    }

    public static void setMaxUpStep(Entity e, float step) {
        e.setMaxUpStep(step);
    }

    public static void addDeltaMovement(Entity e, Vec3 by) {
        e.addDeltaMovement(by);
    }

    public static void teleportTo(Entity e, ServerLevel level, double x, double y, double z, float yaw, float pitch) {
        e.teleportTo(level, x, y, z, Set.of(), yaw, pitch);
    }

    public static float walkSpeed(LivingEntity e) {
        return e.walkAnimation.speed();
    }

    public static void setWalkSpeed(LivingEntity e, float speed) {
        e.walkAnimation.setSpeed(speed);
    }

    public static boolean infinite(MobEffectInstance effect) {
        return effect.isInfiniteDuration();
    }

    public static Entity createEntity(EntityType<?> type, ServerLevel level, BlockPos at) {
        return type.create(level, null, null, at, MobSpawnType.COMMAND, false, false);
    }

    public static void showHurt(LivingEntity e, DamageSource source) {
        e.level().broadcastDamageEvent(e, source);
    }

    public static Packet<?> addEntityPacket(Entity e) {
        return new ClientboundAddEntityPacket(e);
    }

    public static DamageSource magic(Level level) {
        return level.damageSources().magic();
    }

    public static DamageSource indirectMagic(Level level, Entity by, Entity owner) {
        return level.damageSources().indirectMagic(by, owner);
    }

    public static DamageSource mobAttack(Level level, LivingEntity by) {
        return level.damageSources().mobAttack(by);
    }

    public static DamageSource generic(Level level) {
        return level.damageSources().generic();
    }

    public static boolean bypassesInvulnerability(DamageSource source) {
        return source.is(DamageTypeTags.BYPASSES_INVULNERABILITY);
    }

    public static BlockPos blockPos(double x, double y, double z) {
        return BlockPos.containing(x, y, z);
    }

    public static BlockPos blockPos(Position at) {
        return BlockPos.containing(at);
    }

    public static ItemStack copyWithCount(ItemStack stack, int count) {
        return stack.copyWithCount(count);
    }

    public static ResourceKey<Level> dimension(ResourceLocation id) {
        return ResourceKey.create(Registries.DIMENSION, id);
    }

    public static BlockState parseBlock(ServerLevel level, String text) throws CommandSyntaxException {
        return BlockStateParser.parseForBlock(level.registryAccess().lookupOrThrow(Registries.BLOCK), text, false).blockState();
    }

    public static void explode(Level level, Entity by, Vec3 at, float power, boolean fire, boolean breakBlocks) {
        level.explode(by, at.x, at.y, at.z, power, fire, breakBlocks ? ExplosionInteraction.TNT : ExplosionInteraction.NONE);
    }

    public static void success(CommandSourceStack source, Component text, boolean tellOps) {
        source.sendSuccess(() -> text, tellOps);
    }

    public static SoundEvent sound(SoundEvent sound) {
        return sound;
    }

    public static SoundEvent sound(Holder<SoundEvent> sound) {
        return sound.value();
    }

    public static SoundEvent soundEvent(ResourceLocation id) {
        return SoundEvent.createVariableRangeEvent(id);
    }

    public static int mapColor(BlockState state, Level level, BlockPos pos) {
        MapColor c = state.getMapColor(level, pos);
        return c == MapColor.NONE ? -1 : c.col;
    }

    public static boolean mapWater(BlockState state, Level level, BlockPos pos) {
        return state.getMapColor(level, pos) == MapColor.WATER;
    }

    public static DustParticleOptions dust(float r, float g, float b, float size) {
        return new DustParticleOptions(new Vector3f(r, g, b), size);
    }

    public static void faceCamera(PoseStack pose) {
        pose.mulPose(Minecraft.getInstance().getEntityRenderDispatcher().cameraOrientation());
    }

    public static Vector3f cameraLeft(Camera cam) {
        return new Vector3f(cam.getLeftVector());
    }

    public static Vector3f cameraUp(Camera cam) {
        return new Vector3f(cam.getUpVector());
    }

    public static void renderScreen(Screen screen, Gfx g, int mouseX, int mouseY, float partial) {
        screen.render(g.raw, mouseX, mouseY, partial);
    }

    public static String gameVersion() {
        return "1.20.1";
    }

    public static void connect(Screen from, String address) {
        ServerAddress server = ServerAddress.parseString(address);
        ConnectScreen.startConnecting(from, Minecraft.getInstance(), server, new ServerData(address, address, false), false);
    }

    public static int widgetX(AbstractWidget w) {
        return w.getX();
    }

    public static int widgetY(AbstractWidget w) {
        return w.getY();
    }

    public static void widgetMove(AbstractWidget w, int x, int y) {
        w.setX(x);
        w.setY(y);
    }

    public static void mulPose(PoseStack pose, Quaternionf q) {
        pose.mulPose(q);
    }

    public static void mulPoseMatrix(PoseStack pose, Matrix4f m) {
        pose.mulPoseMatrix(m);
    }

    public static Matrix4f pose(Pose p) {
        return p.pose();
    }

    public static Matrix3f normal(Pose p) {
        return p.normal();
    }

    public static VertexConsumer vertex(VertexConsumer vc, Matrix4f m, float x, float y, float z) {
        return vc.vertex(m, x, y, z);
    }

    public static VertexConsumer normal(VertexConsumer vc, Matrix3f m, float x, float y, float z) {
        return vc.normal(m, x, y, z);
    }

    private static DisplayMode mode(boolean seeThrough) {
        return seeThrough ? DisplayMode.SEE_THROUGH : DisplayMode.NORMAL;
    }

    public static void text(
        Font font, String s, float x, float y, int color, boolean shadow, Pose p, MultiBufferSource buffers, boolean seeThrough, int back, int light
    ) {
        font.drawInBatch(s, x, y, color, shadow, p.pose(), buffers, mode(seeThrough), back, light, font.isBidirectional());
    }

    public static void text(
        Font font,
        FormattedCharSequence s,
        float x,
        float y,
        int color,
        boolean shadow,
        Pose p,
        MultiBufferSource buffers,
        boolean seeThrough,
        int back,
        int light
    ) {
        font.drawInBatch(s, x, y, color, shadow, p.pose(), buffers, mode(seeThrough), back, light);
    }

    public static void text(
        Font font, Component s, float x, float y, int color, boolean shadow, Pose p, MultiBufferSource buffers, boolean seeThrough, int back, int light
    ) {
        font.drawInBatch(s, x, y, color, shadow, p.pose(), buffers, mode(seeThrough), back, light);
    }

    public static void heldItem(ItemStack stack, boolean leftHand, LivingEntity holder, PoseStack pose, MultiBufferSource buffers, int light) {
        ItemRenderer items = Minecraft.getInstance().getItemRenderer();
        ItemDisplayContext ctx = leftHand ? ItemDisplayContext.THIRD_PERSON_LEFT_HAND : ItemDisplayContext.THIRD_PERSON_RIGHT_HAND;
        items.render(stack, ctx, leftHand, pose, buffers, light, OverlayTexture.NO_OVERLAY, items.getModel(stack, holder.level(), holder, 0));
    }

    public static void groundItem(ItemStack stack, int light, PoseStack pose, MultiBufferSource buffers, Level level, int seed) {
        Minecraft.getInstance().getItemRenderer().renderStatic(stack, ItemDisplayContext.GROUND, light, OverlayTexture.NO_OVERLAY, pose, buffers, level, seed);
    }

    public static void inventoryEntity(Gfx g, int x, int y, int scale, float lookX, float lookY, LivingEntity e) {
        InventoryScreen.renderEntityInInventoryFollowsMouse(g.raw, x, y, scale, lookX, lookY, e);
    }

    public static ItemStack recipeResult(Recipe<?> recipe, Level level) {
        return recipe.getResultItem(level.registryAccess());
    }

    public static void creativeTab(Properties properties, String key) {
    }

    public static ResourceKey<CreativeModeTab> tabKey(String key) {
        String tab = switch (key) {
            case "building" -> "building_blocks";
            case "decorations" -> "functional_blocks";
            case "redstone", "transport" -> "redstone_blocks";
            case "food", "brewing" -> "food_and_drinks";
            case "tools" -> "tools_and_utilities";
            case "combat" -> "combat";
            default -> "ingredients";
        };
        return ResourceKey.create(Registries.CREATIVE_MODE_TAB, new ResourceLocation("minecraft", tab));
    }

    public static PackResources folderPack(Path dir) {
        return new PathPackResources("spraute_items", dir, true);
    }

    public static void ownTab(Properties properties, Object tab) {
    }
}
