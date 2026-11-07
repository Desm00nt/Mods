package org.sprautestudio.spraute_engine.shop;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.storage.LevelResource;
import org.sprautestudio.spraute_engine.SprauteEngine;
import org.sprautestudio.spraute_engine.core.system.Coins;
import org.sprautestudio.spraute_engine.lang.Ast;
import org.sprautestudio.spraute_engine.lang.Parser;
import org.sprautestudio.spraute_engine.net.Packets;
import org.sprautestudio.spraute_engine.net.SprauteNetwork;
import org.sprautestudio.spraute_engine.platform.Platform;
import org.sprautestudio.spraute_engine.runtime.Eval;
import org.sprautestudio.spraute_engine.runtime.GameEvents;
import org.sprautestudio.spraute_engine.runtime.ItemSpec;
import org.sprautestudio.spraute_engine.runtime.SprauteRuntime;
import org.sprautestudio.spraute_engine.runtime.SprauteScriptInstance;

public final class Shops {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static Map<String, Shops.ShopState> states;
    private static final Map<String, Ast.Expr> PARSED = new HashMap<>();

    private Shops() {
    }

    public static String key(String project, String name) {
        return project + "/" + name;
    }

    public static ShopDef def(String project, String name) {
        String json = SprauteRuntime.get().gui(project, "shop:" + name);
        return json == null ? null : ShopDef.fromJson(json);
    }

    private static Path file() {
        MinecraftServer s = Platform.get().server();
        return s == null ? null : s.getWorldPath(LevelResource.ROOT).resolve("spraute").resolve("shops.json");
    }

    private static Map<String, Shops.ShopState> states() {
        if (states != null) {
            return states;
        } else {
            states = new LinkedHashMap<>();
            Path f = file();
            if (f != null && Files.exists(f)) {
                try {
                    Map<String, Shops.ShopState> m = GSON.fromJson(
                        Files.readString(f, StandardCharsets.UTF_8), (new TypeToken<Map<String, Shops.ShopState>>() {}).getType()
                    );
                    if (m != null) {
                        states.putAll(m);
                    }
                } catch (Exception var2) {
                    SprauteEngine.LOGGER.warn("Spraute: cannot read shops", (Throwable)var2);
                }
            }

            return states;
        }
    }

    private static void save() {
        Path f = file();
        if (f != null && states != null) {
            try {
                Files.createDirectories(f.getParent());
                Files.writeString(f, GSON.toJson(states), StandardCharsets.UTF_8);
            } catch (IOException var2) {
                SprauteEngine.LOGGER.warn("Spraute: cannot save shops", (Throwable)var2);
            }
        }
    }

    public static void unload() {
        states = null;
    }

    private static long today() {
        MinecraftServer s = Platform.get().server();
        return s == null ? 0L : Math.floorDiv(s.overworld().getDayTime(), 24000L);
    }

    private static Shops.ShopState state(String key, ShopDef def) {
        Shops.ShopState st = states().computeIfAbsent(key, k -> new Shops.ShopState());
        long day = today();
        boolean newDay = st.day != day;

        for (ShopDef.Offer o : def.offers) {
            Shops.OfferState os = st.offers.computeIfAbsent(o.id, k -> new Shops.OfferState());
            if (os.stock == Integer.MIN_VALUE || newDay && st.day != Long.MIN_VALUE && o.restock.equals("daily")) {
                os.stock = o.stock;
            }
        }

        st.day = day;
        return st;
    }

    public static void restock(String project, String name) {
        ShopDef def = def(project, name);
        if (def != null) {
            Shops.ShopState st = state(key(project, name), def);

            for (ShopDef.Offer o : def.offers) {
                st.offers.get(o.id).stock = o.stock;
            }

            save();
        }
    }

    public static int bought(String project, String name, String offer) {
        ShopDef def = def(project, name);
        if (def == null) {
            return 0;
        } else {
            Shops.OfferState os = state(key(project, name), def).offers.get(offer);
            return os == null ? 0 : os.total;
        }
    }

    public static int stock(String project, String name, String offer) {
        ShopDef def = def(project, name);
        if (def == null) {
            return 0;
        } else {
            Shops.OfferState os = state(key(project, name), def).offers.get(offer);
            return os == null ? 0 : os.stock;
        }
    }

    public static void setStock(String project, String name, String offer, int stock) {
        ShopDef def = def(project, name);
        if (def != null) {
            Shops.OfferState os = state(key(project, name), def).offers.get(offer);
            if (os != null) {
                os.stock = stock;
                save();
            }
        }
    }

    private static boolean check(SprauteScriptInstance s, String expr) {
        if (expr == null || expr.isBlank()) {
            return true;
        } else if (s == null) {
            return true;
        } else {
            Ast.Expr e = PARSED.computeIfAbsent(expr, Parser::parseExpression);
            if (e == null) {
                SprauteRuntime.get().report(s.getId(), "Магазин: не понимаю условие «" + expr + "»");
                return false;
            } else {
                try {
                    return Eval.truthy(s.eval().eval(e));
                } catch (RuntimeException var4) {
                    SprauteRuntime.get().report(s.getId(), "Магазин, условие «" + expr + "»: " + var4.getMessage());
                    return false;
                }
            }
        }
    }

    private static SprauteScriptInstance script(int id) {
        return SprauteRuntime.env().map(env -> env.scriptInstances.get(id) instanceof SprauteScriptInstance s ? s : null).orElse(null);
    }

    static Item item(String id) {
        return ItemSpec.item(id);
    }

    private static int count(ServerPlayer p, String item) {
        return ItemSpec.count(p, item);
    }

    private static void take(ServerPlayer p, String item, int n) {
        ItemSpec.take(p, item, n);
    }

    private static void give(ServerPlayer p, String item, int n) {
        int most = item(item).getMaxStackSize();

        while (n > 0) {
            int k = Math.min(n, most);
            ItemStack s = ItemSpec.stack(item, k);
            if (!p.addItem(s)) {
                p.drop(s, false);
            }

            n -= k;
        }
    }

    private static int balance(ServerPlayer p, ShopDef def) {
        return def.coins() ? Coins.get(p) : count(p, def.currency);
    }

    private static void pay(ServerPlayer p, ShopDef def, int n) {
        if (def.coins()) {
            Coins.set(p, Math.max(0, Coins.get(p) - n));
        } else {
            take(p, def.currency, n);
        }
    }

    private static void earn(ServerPlayer p, ShopDef def, int n) {
        if (def.coins()) {
            Coins.add(p, n);
        } else {
            give(p, def.currency, n);
        }
    }

    public static ShopView view(ServerPlayer p, int scriptId, String project, String name) {
        ShopDef def = def(project, name);
        if (def == null) {
            return null;
        } else {
            SprauteScriptInstance s = script(scriptId);
            Shops.ShopState st = state(key(project, name), def);
            ShopView v = new ShopView();
            v.key = key(project, name);
            v.scriptId = scriptId;
            v.title = def.title;
            v.currency = def.currency;
            v.currencyName = def.currencyName;
            v.balance = balance(p, def);
            v.tabs = def.tabs;
            String who = p.getStringUUID();

            for (ShopDef.Offer o : def.offers) {
                if (check(s, o.condition)) {
                    Shops.OfferState os = st.offers.get(o.id);
                    ShopView.Line l = new ShopView.Line();
                    l.id = o.id;
                    l.tab = o.tab;
                    l.type = o.type;
                    l.item = o.item;
                    l.name = o.name;
                    l.description = o.description;
                    l.count = o.count;
                    l.price = ShopDef.finalPrice(o, os.total);
                    l.costs = o.costs;
                    l.stock = os.stock;
                    int mine = os.bought.getOrDefault(who, 0);
                    l.left = o.limit < 0 ? -1 : Math.max(0, o.limit - mine);
                    l.have = count(p, o.item);
                    if (!check(s, o.unlock)) {
                        l.state = "locked";
                        l.lockText = o.lockText;
                    } else if (os.stock == 0) {
                        l.state = "soldout";
                    } else if (l.left == 0) {
                        l.state = "limit";
                    }

                    v.lines.add(l);
                }
            }

            return v;
        }
    }

    public static boolean open(ServerPlayer p, int scriptId, String project, String name, Entity trader) {
        ShopView v = view(p, scriptId, project, name);
        if (v == null) {
            return false;
        } else {
            v.entityId = trader != null ? trader.getId() : -1;
            SprauteNetwork.sendTo(p, new Packets.ShopOpen(v.toJson(), false));
            return true;
        }
    }

    public static void deal(ServerPlayer p, Packets.ShopDeal msg) {
        int slash = msg.key().indexOf(47);
        if (slash >= 0) {
            String project = msg.key().substring(0, slash);
            String name = msg.key().substring(slash + 1);
            ShopDef def = def(project, name);
            if (def != null) {
                ShopDef.Offer o = def.offer(msg.offer());
                boolean ok = false;
                int n = Math.max(1, Math.min(999, msg.count()));
                SprauteScriptInstance s = script(msg.scriptId());
                Shops.ShopState st = state(key(project, name), def);
                String result;
                if (o == null || !check(s, o.condition)) {
                    result = "Этого больше нет";
                } else if (!check(s, o.unlock)) {
                    result = o.lockText.isEmpty() ? "Пока недоступно" : o.lockText;
                } else {
                    Shops.OfferState os = st.offers.get(o.id);
                    String who = p.getStringUUID();
                    int mine = os.bought.getOrDefault(who, 0);
                    if (os.stock >= 0) {
                        n = Math.min(n, os.stock);
                    }

                    if (o.limit >= 0) {
                        n = Math.min(n, o.limit - mine);
                    }

                    if (n <= 0) {
                        result = os.stock == 0 ? "Закончилось" : "Больше нельзя";
                    } else {
                        result = exchange(p, def, o, os, n);
                    }

                    if (result == null) {
                        ok = true;
                        if (os.stock >= 0) {
                            os.stock -= n;
                        }

                        os.total += n;
                        os.bought.merge(who, n, Integer::sum);
                        save();
                        Map<String, Object> data = new LinkedHashMap<>();
                        data.put("shop", name);
                        data.put("offer", o.id);
                        data.put("item", o.item);
                        data.put("count", (double)(n * o.count));
                        data.put("amount", (double)n);
                        data.put("type", o.type);
                        GameEvents.post("purchase", p, data);
                        String what = (o.name.isEmpty() ? ItemSpec.stack(o.item, 1).getHoverName().getString() : o.name) + " ×" + n * o.count;
                        String var17 = o.type;

                        result = switch (var17) {
                            case "sell" -> "Продано: " + what;
                            case "trade" -> "Обменяно: " + what;
                            default -> "Куплено: " + what;
                        };
                    }
                }

                ShopView v = view(p, msg.scriptId(), project, name);
                if (v != null) {
                    v.entityId = msg.entityId();
                    v.message = result;
                    v.ok = ok;
                    SprauteNetwork.sendTo(p, new Packets.ShopOpen(v.toJson(), true));
                }
            }
        }
    }

    private static String exchange(ServerPlayer p, ShopDef def, ShopDef.Offer o, Shops.OfferState os, int n) {
        Item it = item(o.item);
        if (it == Items.AIR) {
            return "Неизвестный предмет " + o.item;
        } else {
            int price = 0;

            for (int i = 0; i < n; i++) {
                price += ShopDef.finalPrice(o, os.total + i);
            }

            for (ShopDef.Stack c : o.costs) {
                if (count(p, c.item) < c.count * n) {
                    return "Не хватает: " + ItemSpec.stack(c.item, 1).getHoverName().getString() + " ×" + c.count * n;
                }
            }

            String var12 = o.type;
            switch (var12) {
                case "sell":
                    if (count(p, o.item) < o.count * n) {
                        return "У вас нет столько: нужно " + o.count * n;
                    }

                    take(p, o.item, o.count * n);

                    for (ShopDef.Stack cx : o.costs) {
                        take(p, cx.item, cx.count * n);
                    }

                    earn(p, def, price);
                    break;
                case "trade":
                    if (price > 0 && balance(p, def) < price) {
                        return "Не хватает денег: нужно " + price;
                    }

                    for (ShopDef.Stack cx : o.costs) {
                        take(p, cx.item, cx.count * n);
                    }

                    if (price > 0) {
                        pay(p, def, price);
                    }

                    give(p, o.item, o.count * n);
                    break;
                default:
                    if (balance(p, def) < price) {
                        return "Не хватает денег: нужно " + price;
                    }

                    pay(p, def, price);

                    for (ShopDef.Stack cx : o.costs) {
                        take(p, cx.item, cx.count * n);
                    }

                    give(p, o.item, o.count * n);
            }

            return null;
        }
    }

    static final class OfferState {
        int stock = Integer.MIN_VALUE;
        int total;
        Map<String, Integer> bought = new HashMap<>();
    }

    static final class ShopState {
        long day = Long.MIN_VALUE;
        Map<String, Shops.OfferState> offers = new LinkedHashMap<>();
    }
}
