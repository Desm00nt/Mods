package net.minecraftforge.network;
public class ServerStatusPing {
    public record ChannelData(net.minecraft.resources.ResourceLocation name, String version, boolean required) { }
}
