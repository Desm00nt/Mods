package net.minecraftforge.network;
import java.util.concurrent.CompletableFuture;
import net.minecraft.server.level.ServerPlayer;
public class NetworkEvent extends net.minecraftforge.eventbus.api.Event {
    public static class Context {
        public CompletableFuture<Void> enqueueWork(Runnable r) { throw new RuntimeException(); }
        public ServerPlayer getSender() { throw new RuntimeException(); }
        public void setPacketHandled(boolean packetHandled) { throw new RuntimeException(); }
        public NetworkDirection getDirection() { throw new RuntimeException(); }
    }
}
