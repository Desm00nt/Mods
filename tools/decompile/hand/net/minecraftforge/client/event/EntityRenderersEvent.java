package net.minecraftforge.client.event;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
public class EntityRenderersEvent extends net.minecraftforge.eventbus.api.Event {
    public static class RegisterRenderers extends EntityRenderersEvent {
        public <T extends Entity> void registerEntityRenderer(EntityType<? extends T> entityType, EntityRendererProvider<T> entityRendererProvider) { throw new RuntimeException(); }
        public <T extends BlockEntity> void registerBlockEntityRenderer(BlockEntityType<? extends T> blockEntityType, BlockEntityRendererProvider<T> blockEntityRendererProvider) { throw new RuntimeException(); }
    }
}
