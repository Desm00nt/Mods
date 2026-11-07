package org.sprautestudio.spraute_engine.runtime;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import org.sprautestudio.spraute_engine.compat.Mc;
import org.sprautestudio.spraute_engine.compat.Regs;
import org.sprautestudio.spraute_engine.gui.GuiDoc;
import org.sprautestudio.spraute_engine.net.Packets;
import org.sprautestudio.spraute_engine.net.SprauteNetwork;

public final class GuiSlots {
    private GuiSlots() {
    }

    public static ItemStack stack(String id, int count) {
        return id != null && !id.isBlank() ? ItemSpec.stack(id, count) : ItemStack.EMPTY;
    }

    public static String id(ItemStack st) {
        return st != null && !st.isEmpty() ? Regs.ITEM.getKey(st.getItem()).toString() : "";
    }

    static void opened(SprauteScriptInstance s, GuiDoc doc, ServerPlayer p) {
        s.openGuiDoc = doc;

        for (GuiDoc.Element e : doc.all()) {
            if (e.type.equals("slot")) {
                if (!s.guiSlots.containsKey(e.id)) {
                    s.guiSlots.put(e.id, stack(e.get("item"), e.i("count")));
                }

                send(s, p, e.id);
            }
        }
    }

    static void send(SprauteScriptInstance s, ServerPlayer p, String slot) {
        if (p != null) {
            ItemStack st = s.guiSlots.getOrDefault(slot, ItemStack.EMPTY);
            SprauteNetwork.sendTo(p, new Packets.GuiUpdate(s.getId(), slot, "item", ItemSpec.of(st)));
            SprauteNetwork.sendTo(p, new Packets.GuiUpdate(s.getId(), slot, "count", String.valueOf(Math.max(1, st.getCount()))));
        }
    }

    static void set(SprauteScriptInstance s, String slot, ItemStack st) {
        s.guiSlots.put(slot, st.copy());
        send(s, SprauteRuntime.player(), slot);
    }

    private static void changed(SprauteScriptInstance s, ServerPlayer p, String slot) {
        s.lastSlot = slot;
        s.slotSeq++;
        send(s, p, slot);
    }

    static void put(SprauteScriptInstance s, ServerPlayer p, String slot, int inv, int count) {
        GuiDoc.Element e = s.openGuiDoc == null ? null : s.openGuiDoc.element(slot);
        if (e != null && e.type.equals("slot")) {
            String mode = e.get("mode");
            if (mode.equals("storage") || mode.equals("put")) {
                if (inv >= 0 && inv < p.getInventory().items.size()) {
                    ItemStack src = p.getInventory().getItem(inv);
                    if (!src.isEmpty()) {
                        String filter = e.get("filter").trim();
                        if (!filter.isEmpty() && !ItemSpec.matches(src, filter)) {
                            send(s, p, slot);
                        } else {
                            ItemStack cur = s.guiSlots.getOrDefault(slot, ItemStack.EMPTY);
                            if (cur.isEmpty() || ItemStack.isSameItemSameTags(cur, src)) {
                                int max = Math.max(1, Math.min(e.i("max") > 0 ? e.i("max") : 64, src.getMaxStackSize()));
                                int n = Math.min(Math.min(count, src.getCount()), max - cur.getCount());
                                if (n > 0) {
                                    ItemStack next = cur.isEmpty() ? Mc.copyWithCount(src, n) : Mc.copyWithCount(cur, cur.getCount() + n);
                                    src.shrink(n);
                                    p.getInventory().setChanged();
                                    s.guiSlots.put(slot, next);
                                    changed(s, p, slot);
                                }
                            } else if (mode.equals("storage")) {
                                ItemStack back = cur.copy();
                                s.guiSlots.put(slot, src.copy());
                                p.getInventory().setItem(inv, back);
                                changed(s, p, slot);
                            }
                        }
                    }
                }
            }
        }
    }

    static void take(SprauteScriptInstance s, ServerPlayer p, String slot, int count) {
        GuiDoc.Element e = s.openGuiDoc == null ? null : s.openGuiDoc.element(slot);
        if (e != null && e.type.equals("slot")) {
            String mode = e.get("mode");
            if (mode.equals("storage") || mode.equals("take")) {
                ItemStack cur = s.guiSlots.getOrDefault(slot, ItemStack.EMPTY);
                if (!cur.isEmpty()) {
                    int n = Math.min(count, cur.getCount());
                    ItemStack give = Mc.copyWithCount(cur, n);
                    ItemStack rest = Mc.copyWithCount(cur, cur.getCount() - n);
                    s.guiSlots.put(slot, rest.getCount() > 0 ? rest : ItemStack.EMPTY);
                    if (!p.getInventory().add(give)) {
                        p.drop(give, false);
                    }

                    p.getInventory().setChanged();
                    changed(s, p, slot);
                }
            }
        }
    }
}
