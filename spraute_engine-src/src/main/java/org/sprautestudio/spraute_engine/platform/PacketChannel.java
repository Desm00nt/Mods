package org.sprautestudio.spraute_engine.platform;

import java.util.function.BiConsumer;
import java.util.function.Function;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;

public interface PacketChannel {
    <T> void register(
        Class<T> var1,
        BiConsumer<T, FriendlyByteBuf> var2,
        Function<FriendlyByteBuf, T> var3,
        BiConsumer<T, PacketChannel.Context> var4,
        PacketChannel.Direction var5
    );

    void sendToServer(Object var1);

    void sendTo(ServerPlayer var1, Object var2);

    void sendToEveryone(Object var1);

    public interface Context {
        ServerPlayer sender();

        void enqueue(Runnable var1);
    }

    public static enum Direction {
        TO_SERVER,
        TO_CLIENT,
        BOTH;
    }
}
