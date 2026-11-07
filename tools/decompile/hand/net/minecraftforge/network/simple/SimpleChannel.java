package net.minecraftforge.network.simple;
import java.util.Optional;
import java.util.function.*;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.PacketDistributor;
public class SimpleChannel {
    public <MSG> IndexedMessageCodec.MessageHandler<MSG> registerMessage(int index, Class<MSG> messageType, BiConsumer<MSG, FriendlyByteBuf> encoder, Function<FriendlyByteBuf, MSG> decoder, BiConsumer<MSG, Supplier<NetworkEvent.Context>> messageConsumer) { throw new RuntimeException(); }
    public <MSG> IndexedMessageCodec.MessageHandler<MSG> registerMessage(int index, Class<MSG> messageType, BiConsumer<MSG, FriendlyByteBuf> encoder, Function<FriendlyByteBuf, MSG> decoder, BiConsumer<MSG, Supplier<NetworkEvent.Context>> messageConsumer, Optional<NetworkDirection> networkDirection) { throw new RuntimeException(); }
    public <MSG> void sendToServer(MSG message) { throw new RuntimeException(); }
    public <MSG> void send(PacketDistributor.PacketTarget target, MSG message) { throw new RuntimeException(); }
}
