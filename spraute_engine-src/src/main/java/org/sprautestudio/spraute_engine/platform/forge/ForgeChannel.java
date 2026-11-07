package org.sprautestudio.spraute_engine.platform.forge;

import java.util.Optional;
import java.util.function.BiConsumer;
import java.util.function.Function;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;
import org.sprautestudio.spraute_engine.platform.PacketChannel;

final class ForgeChannel implements PacketChannel {
    private final SimpleChannel channel;
    private int nextId;

    ForgeChannel(ResourceLocation id, String protocol) {
        this.channel = NetworkRegistry.newSimpleChannel(id, () -> protocol, v -> ForgeVersion.accepted(v, protocol), v -> ForgeVersion.accepted(v, protocol));
    }

    @Override
    public <T> void register(
        Class<T> type,
        BiConsumer<T, FriendlyByteBuf> write,
        Function<FriendlyByteBuf, T> read,
        BiConsumer<T, PacketChannel.Context> handler,
        PacketChannel.Direction direction
    ) {
        this.channel
            .registerMessage(
                this.nextId++,
                type,
                write,
                read,
                (msg, ctx) -> {
                    final NetworkEvent.Context c = ctx.get();
                    handler.accept(msg, new PacketChannel.Context() {
                        @Override
                        public ServerPlayer sender() {
                            return c.getSender();
                        }

                        @Override
                        public void enqueue(Runnable work) {
                            c.enqueueWork(work);
                        }
                    });
                    c.setPacketHandled(true);
                },
                direction == PacketChannel.Direction.BOTH
                    ? Optional.empty()
                    : Optional.of(direction == PacketChannel.Direction.TO_SERVER ? NetworkDirection.PLAY_TO_SERVER : NetworkDirection.PLAY_TO_CLIENT)
            );
    }

    @Override
    public void sendToServer(Object message) {
        this.channel.sendToServer(message);
    }

    @Override
    public void sendTo(ServerPlayer player, Object message) {
        this.channel.send(PacketDistributor.PLAYER.with(() -> player), message);
    }

    @Override
    public void sendToEveryone(Object message) {
        this.channel.send(PacketDistributor.ALL.noArg(), message);
    }
}
