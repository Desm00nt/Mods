package net.minecraftforge.network;
import java.util.function.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.network.simple.SimpleChannel;
public class NetworkRegistry {
    public static String ABSENT_STR;
    public static ServerStatusPing.ChannelData ABSENT;
    public static String ACCEPTVANILLA;
    public static SimpleChannel newSimpleChannel(ResourceLocation name, Supplier<String> networkProtocolVersion, Predicate<String> clientAcceptedVersions, Predicate<String> serverAcceptedVersions) { throw new RuntimeException(); }
}
