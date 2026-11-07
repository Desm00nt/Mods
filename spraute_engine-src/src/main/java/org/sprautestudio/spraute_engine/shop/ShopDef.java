package org.sprautestudio.spraute_engine.shop;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import java.util.ArrayList;
import java.util.List;

public class ShopDef {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();
    public String title = "Лавка";
    public String currency = "coins";
    public String currencyName = "";
    public List<ShopDef.Tab> tabs = new ArrayList<>();
    public List<ShopDef.Offer> offers = new ArrayList<>();

    public String toJson() {
        return GSON.toJson(this);
    }

    public static ShopDef fromJson(String json) {
        ShopDef d = null;

        try {
            d = GSON.fromJson(json, ShopDef.class);
        } catch (Exception var3) {
        }

        if (d == null) {
            d = new ShopDef();
        }

        d.fix();
        return d;
    }

    public void fix() {
        if (this.title == null) {
            this.title = "Лавка";
        }

        if (this.currency == null || this.currency.isBlank()) {
            this.currency = "coins";
        }

        if (this.currencyName == null) {
            this.currencyName = "";
        }

        if (this.tabs == null) {
            this.tabs = new ArrayList<>();
        }

        if (this.offers == null) {
            this.offers = new ArrayList<>();
        }

        if (this.tabs.isEmpty()) {
            this.tabs.add(new ShopDef.Tab());
        }

        for (ShopDef.Offer o : this.offers) {
            if (o.id == null || o.id.isBlank()) {
                o.id = "offer";
            }

            if (o.tab == null || o.tab.isBlank()) {
                o.tab = this.tabs.get(0).id;
            }

            if (o.type == null) {
                o.type = "buy";
            }

            if (o.item == null) {
                o.item = "minecraft:air";
            }

            if (o.name == null) {
                o.name = "";
            }

            if (o.description == null) {
                o.description = "";
            }

            if (o.costs == null) {
                o.costs = new ArrayList<>();
            }

            if (o.restock == null) {
                o.restock = "never";
            }

            if (o.condition == null) {
                o.condition = "";
            }

            if (o.unlock == null) {
                o.unlock = "";
            }

            if (o.lockText == null) {
                o.lockText = "";
            }

            if (o.discount <= 0.0F) {
                o.discount = 1.0F;
            }

            o.count = Math.max(1, o.count);
            o.price = Math.max(0, o.price);
        }
    }

    public ShopDef.Offer offer(String id) {
        for (ShopDef.Offer o : this.offers) {
            if (o.id.equals(id)) {
                return o;
            }
        }

        return null;
    }

    public boolean coins() {
        return this.currency.equals("coins");
    }

    public static int finalPrice(ShopDef.Offer o, int bought) {
        double p = o.price * o.discount * Math.pow(1.0F + Math.max(0.0F, o.priceGrowth), Math.max(0, bought));
        return (int)Math.max(0L, Math.round(p));
    }

    public static ShopDef example(String title) {
        ShopDef d = new ShopDef();
        d.title = title;
        d.tabs.add(new ShopDef.Tab("main", "Еда", "minecraft:bread"));
        d.tabs.add(new ShopDef.Tab("gear", "Снаряжение", "minecraft:iron_sword"));
        d.tabs.add(new ShopDef.Tab("buyout", "Скупка", "minecraft:gold_ingot"));
        ShopDef.Offer bread = offer("bread", "main", "buy", "minecraft:bread", 3, 4);
        bread.description = "Свежий хлеб из печи";
        ShopDef.Offer apple = offer("apple", "main", "buy", "minecraft:golden_apple", 1, 40);
        apple.stock = 2;
        apple.restock = "daily";
        apple.description = "Всего две штуки в день";
        ShopDef.Offer sword = offer("sword", "gear", "buy", "minecraft:iron_sword", 1, 25);
        sword.priceGrowth = 0.25F;
        sword.description = "Каждый следующий дороже на 25%";
        ShopDef.Offer armor = offer("chest", "gear", "buy", "minecraft:iron_chestplate", 1, 60);
        armor.unlock = "flag(\"trusted\")";
        armor.lockText = "Торговец вам ещё не доверяет";
        ShopDef.Offer wheat = offer("wheat", "buyout", "sell", "minecraft:wheat", 8, 3);
        wheat.description = "Принимаю пшеницу пачками по 8";
        ShopDef.Offer trade = offer("emeralds", "buyout", "trade", "minecraft:emerald", 1, 0);
        trade.costs.add(new ShopDef.Stack("minecraft:diamond", 1));
        trade.description = "Алмаз на изумруд";
        d.offers.addAll(List.of(bread, apple, sword, armor, wheat, trade));
        d.fix();
        return d;
    }

    private static ShopDef.Offer offer(String id, String tab, String type, String item, int count, int price) {
        ShopDef.Offer o = new ShopDef.Offer();
        o.id = id;
        o.tab = tab;
        o.type = type;
        o.item = item;
        o.count = count;
        o.price = price;
        return o;
    }

    public static class Offer {
        public String id = "offer";
        public String tab = "main";
        public String type = "buy";
        public String item = "minecraft:bread";
        public int count = 1;
        public String name = "";
        public String description = "";
        public int price = 5;
        public List<ShopDef.Stack> costs = new ArrayList<>();
        public int stock = -1;
        public String restock = "never";
        public int limit = -1;
        public String condition = "";
        public String unlock = "";
        public String lockText = "";
        public float priceGrowth;
        public float discount = 1.0F;
    }

    public static class Stack {
        public String item = "minecraft:emerald";
        public int count = 1;

        public Stack() {
        }

        public Stack(String item, int count) {
            this.item = item;
            this.count = count;
        }
    }

    public static class Tab {
        public String id = "main";
        public String name = "Товары";
        public String icon = "minecraft:chest";

        public Tab() {
        }

        public Tab(String id, String name, String icon) {
            this.id = id;
            this.name = name;
            this.icon = icon;
        }
    }
}
