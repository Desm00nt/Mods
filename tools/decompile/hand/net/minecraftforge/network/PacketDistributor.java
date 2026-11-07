package net.minecraftforge.network;
import java.util.function.Supplier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.resources.ResourceKey;
public class PacketDistributor<T> {
    public static PacketDistributor<ServerPlayer> PLAYER;
    public static PacketDistributor<ResourceKey<Level>> DIMENSION;
    public static PacketDistributor<Void> ALL;
    public static PacketDistributor<Void> SERVER;
    public static PacketDistributor<Entity> TRACKING_ENTITY;
    public static PacketDistributor<Entity> TRACKING_ENTITY_AND_SELF;
    public PacketTarget with(Supplier<T> input) { throw new RuntimeException(); }
    public PacketTarget noArg() { throw new RuntimeException(); }
    public static class PacketTarget { }
}
