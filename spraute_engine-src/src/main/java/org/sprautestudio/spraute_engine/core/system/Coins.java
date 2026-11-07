package org.sprautestudio.spraute_engine.core.system;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.saveddata.SavedData;
import org.sprautestudio.spraute_engine.engine.EngineServer;
import org.sprautestudio.spraute_engine.net.Packets;
import org.sprautestudio.spraute_engine.net.SprauteNetwork;
import org.sprautestudio.spraute_engine.runtime.ItemSpec;

public final class Coins {
    private static final String DATA = "spraute_coins";
    private static int shown;
    private static int shownNeed = -1;
    private static final Map<UUID, Integer> NEEDS = new HashMap<>();

    private Coins() {
    }

    private static Coins.Saved saved(MinecraftServer s) {
        return s.overworld().getDataStorage().computeIfAbsent(Coins.Saved::load, Coins.Saved::new, "spraute_coins");
    }

    public static String item() {
        String id = EngineServer.config().coinItem;
        return id != null && !id.isBlank() && !ItemSpec.stack(id.trim(), 1).isEmpty() ? id.trim() : null;
    }

    public static int get(ServerPlayer p) {
        if (p == null) {
            return 0;
        } else {
            String item = item();
            return item != null ? ItemSpec.count(p, item) : saved(p.server).purses.getOrDefault(p.getUUID(), 0);
        }
    }

    private static void setItems(ServerPlayer p, String item, int want) {
        int have = ItemSpec.count(p, item);
        if (want < have) {
            ItemSpec.take(p, item, have - want);
        }

        int left = want - have;

        while (left > 0) {
            ItemStack stack = ItemSpec.stack(item, 1);
            int part = Math.min(left, stack.getMaxStackSize());
            stack.setCount(part);
            if (!p.addItem(stack)) {
                p.drop(stack, false);
            }

            left -= part;
        }
    }

    public static void set(ServerPlayer p, int n) {
        if (p != null) {
            String item = item();
            if (item != null) {
                setItems(p, item, Math.max(0, n));
                sync(p, -2);
            } else {
                Coins.Saved d = saved(p.server);
                d.purses.put(p.getUUID(), Math.max(0, n));
                d.setDirty();
                sync(p, -2);
            }
        }
    }

    public static void add(ServerPlayer p, int n) {
        set(p, get(p) + n);
    }

    public static void need(ServerPlayer p, int n) {
        if (p != null) {
            sync(p, n);
        }
    }

    public static void sync(ServerPlayer p, int need) {
        if (need != -2) {
            NEEDS.put(p.getUUID(), need);
        }

        SprauteNetwork.sendTo(p, new Packets.CoinsSync(get(p), NEEDS.getOrDefault(p.getUUID(), -1)));
    }

    public static int shown() {
        return shown;
    }

    public static int shownNeed() {
        return shownNeed;
    }

    public static void receive(int n, int need) {
        shown = n;
        shownNeed = need;
    }

    private static final class Saved extends SavedData {
        final Map<UUID, Integer> purses = new HashMap<>();

        @Override
        public CompoundTag save(CompoundTag tag) {
            this.purses.forEach((id, n) -> tag.putInt(id.toString(), n));
            return tag;
        }

        static Coins.Saved load(CompoundTag tag) {
            Coins.Saved s = new Coins.Saved();

            for (String k : tag.getAllKeys()) {
                s.purses.put(UUID.fromString(k), tag.getInt(k));
            }

            return s;
        }
    }
}
