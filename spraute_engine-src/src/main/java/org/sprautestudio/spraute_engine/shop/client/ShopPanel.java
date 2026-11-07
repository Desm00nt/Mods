package org.sprautestudio.spraute_engine.shop.client;

import com.mojang.blaze3d.systems.RenderSystem;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.Font;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.sprautestudio.spraute_engine.compat.Gfx;
import org.sprautestudio.spraute_engine.engine.EngineConfig;
import org.sprautestudio.spraute_engine.engine.client.ChatHeads;
import org.sprautestudio.spraute_engine.engine.client.EngineClient;
import org.sprautestudio.spraute_engine.engine.client.Skins;
import org.sprautestudio.spraute_engine.runtime.ItemSpec;
import org.sprautestudio.spraute_engine.shop.ShopDef;
import org.sprautestudio.spraute_engine.shop.ShopView;
import org.sprautestudio.spraute_engine.ui.GuiText;

public class ShopPanel {
    public static final int W = 445;
    public static final int H = 262;
    private static final int COLS = 5;
    private static final int STEP_X = 45;
    private static final int STEP_Y = 63;
    private static final int CELL_W = 40;
    private static final int CELL_H = 57;
    private static final int LOTS_X = 13;
    private static final int LOTS_Y = 38;
    private static final int CLIP_X = 10;
    private static final int CLIP_Y = 35;
    private static final int CLIP_W = 226;
    private static final int CLIP_H = 192;
    private static final float CARD = 1.3333334F;
    private static final ResourceLocation BASE_1 = tex("list");
    private static final ResourceLocation BASE_2 = tex("card");
    private static final ResourceLocation CELL = tex("cell");
    private static final ResourceLocation CELL_GLOW = tex("cell_hot");
    private static final ResourceLocation LOCKED = tex("cell_locked");
    private static final ResourceLocation SOLD = tex("cell_sold");
    private static final ResourceLocation BUY = tex("buy");
    private static final ResourceLocation BUY_LIGHT = tex("buy_hot");
    private static final ResourceLocation SLIDER = tex("slider");
    private static final int WHITE = -1;
    private static final int GREEN = -11141291;
    private static final int RED = -43691;
    private static final int TYPE = -1910844;
    public ShopView view;
    public int qty = 1;
    public double scroll;
    public String selected;
    public int x;
    public int y;
    public int bx;
    public int by;
    public EngineConfig.Shop style;
    public static final List<String> PARTS = List.of("window", "list", "card", "title", "money");
    public float scale = 1.0F;
    public long messageAt;
    public long openedAt = System.currentTimeMillis();
    public ItemStack hoveredStack;
    private static final int[][] ARROWS = new int[][]{{-5, 317, 102, 12, 15}, {-1, 342, 103, 9, 13}, {1, 365, 103, 9, 13}, {5, 387, 102, 12, 15}};

    private static ResourceLocation tex(String name) {
        return new ResourceLocation("spraute_engine", "textures/gui/store/" + name + ".png");
    }

    public static String partTitle(String part) {
        return switch (part) {
            case "window" -> "всё окно";
            case "list" -> "список товаров";
            case "card" -> "карточка товара";
            case "title" -> "название";
            default -> "деньги";
        };
    }

    public int[] off(String part) {
        int[] o = this.style != null ? this.style.offsets.get(part) : null;
        return o != null && o.length == 2 ? o : new int[]{0, 0};
    }

    private void at(String part) {
        int[] w = this.off("window");
        int[] o = part.equals("window") ? new int[]{0, 0} : this.off(part);
        this.x = this.bx + w[0] + o[0];
        this.y = this.by + w[1] + o[1];
    }

    public void place(int px, int py) {
        this.bx = px;
        this.by = py;
        this.at("window");
    }

    public int[] partRect(String part, Font font) {
        this.at(part);

        int[] r = switch (part) {
            case "list" -> new int[]{this.x, this.y + 25, 254, 212};
            case "card" -> new int[]{this.x + 269, this.y, 176, 262};
            case "title" -> new int[]{this.x + 14, this.y + 8, font.width(this.view.title) + 4, 12};
            case "money" -> {
                int bw = font.width(String.valueOf(this.view.balance));
                yield new int[]{this.x + 238 - bw - 15, this.y + 6, bw + 17, 13};
            }
            default -> new int[]{this.x, this.y, 445, 262};
        };
        this.at("window");
        return r;
    }

    public String partAt(double mx, double my, Font font) {
        for (String p : List.of("title", "money", "card", "list")) {
            if (in(this.partRect(p, font), mx, my)) {
                return p;
            }
        }

        return null;
    }

    public ShopPanel(ShopView view) {
        this.setView(view);
    }

    public void setView(ShopView v) {
        this.view = v;
        if (this.selected == null || this.line(this.selected) == null) {
            List<ShopView.Line> l = this.lines();
            this.selected = l.isEmpty() ? null : l.get(0).id;
        }

        if (!v.message.isEmpty()) {
            this.messageAt = System.currentTimeMillis();
        }

        this.scroll = Math.max(0.0, Math.min(this.scroll, this.maxScroll()));
    }

    public void layout(int sw, int sh, double guiScale) {
        this.scale = (float)Math.min(4.0 / Math.max(1.0, guiScale), Math.min((sw - 8) / 445.0, (sh - 8) / 262.0));
        this.place(Math.round((sw / this.scale - 445.0F) / 2.0F), Math.round((sh / this.scale - 262.0F) / 2.0F) + 5);
    }

    public void layout(int sw, int sh) {
        this.scale = 1.0F;
        this.place((sw - 445) / 2, (sh - 262) / 2 + 5);
    }

    public List<ShopView.Line> lines() {
        return this.view.lines;
    }

    public ShopView.Line line(String id) {
        if (id == null) {
            return null;
        } else {
            for (ShopView.Line l : this.view.lines) {
                if (l.id.equals(id)) {
                    return l;
                }
            }

            return null;
        }
    }

    static ItemStack stack(String id, int count) {
        return ItemSpec.stack(id, count);
    }

    private ItemStack money() {
        if (!this.view.currency.equals("coins")) {
            return stack(this.view.currency, 1);
        } else {
            ItemStack own = EngineClient.coinItem();
            if (!own.isEmpty()) {
                return own;
            } else {
                ItemStack s = stack("minecraft:gold_nugget", 1);
                return s.isEmpty() ? new ItemStack(Items.GOLD_NUGGET) : s;
            }
        }
    }

    public static String title(ShopView.Line l) {
        return l.name != null && !l.name.isEmpty() ? l.name : stack(l.item, 1).getHoverName().getString();
    }

    public int maxQty(ShopView.Line l) {
        int m = 99;
        if (l.stock >= 0) {
            m = Math.min(m, l.stock);
        }

        if (l.left >= 0) {
            m = Math.min(m, l.left);
        }

        return Math.max(1, m);
    }

    public boolean affordable(ShopView.Line l, int n) {
        return l.type.equals("sell") ? l.have >= l.count * n : this.view.balance >= l.price * n;
    }

    private double maxScroll() {
        int rows = (this.lines().size() + 5 - 1) / 5;
        return Math.max(0, rows * 63 - 192);
    }

    public int[] cellRect(int i) {
        return new int[]{this.x + 13 + i % 5 * 45, (int)Math.round(this.y + 38 + i / 5 * 63 - this.scroll), 40, 57};
    }

    private int[] arrowRect(int i) {
        int[] a = ARROWS[i];
        return new int[]{this.x + a[1], this.y + a[2], a[3], a[4]};
    }

    public int[] dealRect() {
        return new int[]{this.x + 313, this.y + 195, 88, 14};
    }

    private static boolean in(int[] r, double mx, double my) {
        return mx >= r[0] && mx < r[0] + r[2] && my >= r[1] && my < r[1] + r[3];
    }

    public double wx(double mx) {
        return mx / this.scale;
    }

    public double wy(double my) {
        return my / this.scale;
    }

    public String hit(double mx, double my) {
        try {
            this.at("list");
            if (mx >= this.x + 10 && mx < this.x + 10 + 226 && my >= this.y + 35 && my < this.y + 35 + 192) {
                List<ShopView.Line> ls = this.lines();

                for (int i = 0; i < ls.size(); i++) {
                    if (in(this.cellRect(i), mx, my)) {
                        return "cell:" + ls.get(i).id;
                    }
                }
            }

            this.at("card");

            for (int ix = 0; ix < ARROWS.length; ix++) {
                if (in(this.arrowRect(ix), mx, my)) {
                    return "arrow:" + ARROWS[ix][0];
                }
            }

            return !in(this.dealRect(), mx, my) ? null : "deal";
        } finally {
            this.at("window");
        }
    }

    public boolean click(double mx, double my) {
        String h = this.hit(mx, my);
        if (h == null) {
            return false;
        } else {
            ShopView.Line l = this.line(this.selected);
            if (h.startsWith("cell:")) {
                String id = h.substring(5);
                if (!id.equals(this.selected)) {
                    this.qty = 1;
                }

                this.selected = id;
            } else if (h.startsWith("arrow:") && l != null) {
                this.qty = Math.max(1, Math.min(this.maxQty(l), this.qty + Integer.parseInt(h.substring(6))));
            } else if (h.equals("deal") && l != null) {
                return l.state.equals("ok") && this.affordable(l, this.qty);
            }

            return false;
        }
    }

    public void scroll(double delta) {
        this.scroll = Math.max(0.0, Math.min(this.maxScroll(), this.scroll - delta * 20.0));
    }

    private static void blit(Gfx g, String project, EngineConfig.Skin skin, ResourceLocation def, int x, int y, int w, int h) {
        if (skin != null && skin.isSet()) {
            Skins.draw(g, project, skin, x, y, w, h);
        } else {
            g.blit(def, x, y, 0.0F, 0.0F, w, h, w, h);
        }
    }

    private static void text(Gfx g, Font font, String s, float x, float y, float scale, int color, boolean shadow) {
        g.pose().pushPose();
        g.pose().scale(scale, scale, 1.0F);
        GuiText.draw(g, font, s, x / scale, y / scale, color, shadow);
        g.pose().popPose();
    }

    private static void item(Gfx g, ItemStack s, float x, float y, float scale) {
        g.pose().pushPose();
        g.pose().translate(x, y, 0.0F);
        g.pose().scale(scale, scale, 1.0F);
        g.renderItem(s, 0, 0);
        g.pose().popPose();
    }

    private float price(Gfx g, Font font, ItemStack coin, float px, float py, int amount, int color) {
        item(g, coin, px, py - 3.0F, 0.6F);
        String s = "x" + amount;
        text(g, font, s, px + 11.0F, py, 0.75F, color, false);
        return px + 11.0F + font.width(s) * 0.75F;
    }

    public void render(Gfx g, Font font, EngineConfig cfg, double mx, double my) {
        EngineConfig.Shop c = cfg.shop;
        this.style = c;
        String project = cfg.project;
        float k = Math.min(1.0F, (float)(System.currentTimeMillis() - this.openedAt) / 500.0F);
        float ease = 1.0F - (1.0F - k) * (1.0F - k);
        g.pose().pushPose();
        g.pose().scale(this.scale, this.scale, 1.0F);
        g.pose().translate(0.0F, -(1.0F - ease) * 30.0F, 0.0F);
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, ease);
        this.at("list");
        blit(g, project, c.panel, BASE_1, this.x, this.y + 25, 254, 212);
        this.at("card");
        blit(g, project, c.detail, BASE_2, this.x + 254 + 15, this.y, 176, 262);
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        this.at("title");
        text(g, font, this.view.title, this.x + 16, this.y + 11, 1.0F, c.titleColor, true);
        this.at("money");
        ItemStack coin = this.money();
        String bal = String.valueOf(this.view.balance);
        float bw = font.width(bal);
        item(g, coin, this.x + 238 - bw - 13.0F, this.y + 7, 0.75F);
        text(g, font, bal, this.x + 238 - bw, this.y + 11, 1.0F, c.priceColor, true);
        this.at("list");
        List<ShopView.Line> ls = this.lines();
        int[] clip = ChatHeads.screenRect(g, this.x + 10, this.y + 35, 226, 192);
        g.enableScissor(clip[0], clip[1], clip[2], clip[3]);
        this.hoveredStack = null;

        for (int i = 0; i < ls.size(); i++) {
            ShopView.Line l = ls.get(i);
            int[] r = this.cellRect(i);
            if (r[1] + r[3] >= this.y + 35 && r[1] <= this.y + 35 + 192) {
                boolean sel = l.id.equals(this.selected);
                ResourceLocation t = l.state.equals("locked") ? LOCKED : (l.state.equals("soldout") ? SOLD : (sel ? CELL_GLOW : CELL));
                EngineConfig.Skin skin = !l.state.equals("ok") && !l.state.equals("limit")
                    ? (l.state.equals("locked") ? c.locked : c.sold)
                    : (sel ? c.cellOn : c.cell);
                blit(g, project, skin, t, r[0], r[1], 40, 57);
                ItemStack s = stack(l.item, l.count);
                g.pose().pushPose();
                g.pose().translate(r[0] + 9.333334F, r[1] + 10.666667F, 0.0F);
                g.pose().scale(1.3333334F, 1.3333334F, 1.0F);
                g.renderItem(s, 0, 0);
                if (l.count > 1) {
                    g.renderItemDecorations(font, s, 0, 0);
                }

                g.pose().popPose();
                String p = l.type.equals("trade") && l.price == 0 ? "ОБМЕН" : String.valueOf(l.price);
                int col = l.type.equals("sell") ? c.sellColor : -1910844;
                float mid = r[0] + 20.0F;
                float py = r[1] + 48.0F;
                if (l.type.equals("trade") && l.price == 0) {
                    text(g, font, p, mid - font.width(p) * 0.25F * 1.3333334F, py, 0.6666667F, col, true);
                } else {
                    float pw = (font.width(p) * 0.5F + 6.0F) * 1.3333334F;
                    item(g, coin, mid - pw / 2.0F - 1.3333334F, py - 2.0F, 0.5F);
                    text(g, font, p, mid - pw / 2.0F + 8.0F, py, 0.6666667F, col, true);
                }

                if (in(r, mx, my) && my >= this.y + 35 && my < this.y + 35 + 192) {
                    this.hoveredStack = s;
                }
            }
        }

        g.disableScissor();
        if (ls.isEmpty()) {
            text(g, font, "Здесь пока пусто", this.x + 13, this.y + 38 + 4, 0.75F, c.mutedColor, false);
        }

        if (this.maxScroll() > 0.0) {
            double share = this.scroll / this.maxScroll();
            blit(g, project, c.slider, SLIDER, this.x + 239, (int)Math.round(this.y + 35 + share * 182.0), 9, 10);
        }

        this.at("card");
        ShopView.Line l = this.line(this.selected);
        if (l != null) {
            ItemStack sx = stack(l.item, 1);
            item(g, sx, this.x + 357 - 16, this.y + 71 - 16, 2.0F);
            if (in(new int[]{this.x + 341, this.y + 55, 32, 32}, mx, my)) {
                this.hoveredStack = stack(l.item, l.count);
            }

            text(g, font, "x" + this.qty, this.x + 360, this.y + 75, 1.5F, -1, true);
            int n = Math.max(1, this.qty);
            int yOff = 10;
            String name = title(l);
            float ts = Math.max(0.5F, Math.min(1.0F, 155.0F / Math.max(1, font.width(name)))) * 0.75F;
            text(g, font, name, this.x + 296, this.y + yOff + 108, ts, -1, false);
            boolean sell = l.type.equals("sell");
            text(g, font, sell ? "Торговец заплатит: " : "Стоимость товара: ", this.x + 296, this.y + yOff + 117, 0.75F, -1, false);
            float end = l.price <= 0 && !l.costs.isEmpty()
                ? this.x + 296
                : this.price(g, font, coin, this.x + 296, this.y + 137, l.price, sell ? c.sellColor : c.priceColor);

            for (ShopDef.Stack cs : l.costs) {
                item(g, stack(cs.item, 1), end + 3.0F, this.y + 134, 0.6F);
                text(g, font, "x" + cs.count, end + 14.0F, this.y + 137, 0.75F, -1, true);
                end += 14.0F + font.width("x" + cs.count) * 0.75F;
            }

            text(g, font, "Количество: " + n + " по " + l.count + " (" + n * l.count + " шт.)", this.x + 296, this.y + yOff + 126 + 16, 0.75F, -1, false);
            text(g, font, sell ? "Итого получите: " : "Итоговая стоимость: ", this.x + 296, this.y + yOff + 135 + 16, 0.75F, -1, false);
            float end2 = l.price <= 0 && !l.costs.isEmpty()
                ? this.x + 296
                : this.price(g, font, coin, this.x + 296, this.y + 171, l.price * n, sell ? c.sellColor : c.priceColor);

            for (ShopDef.Stack cs : l.costs) {
                item(g, stack(cs.item, 1), end2 + 3.0F, this.y + 168, 0.6F);
                text(g, font, "x" + cs.count * n, end2 + 14.0F, this.y + 171, 0.75F, -1, true);
                end2 += 14.0F + font.width("x" + cs.count * n) * 0.75F;
            }

            String status;
            int sc;
            if (!this.view.message.isEmpty() && System.currentTimeMillis() - this.messageAt < 3000L) {
                status = this.view.message;
                sc = this.view.ok ? -11141291 : -43691;
            } else if (l.state.equals("locked")) {
                status = l.lockText.isEmpty() ? "Товар заблокирован!" : l.lockText;
                sc = -43691;
            } else if (l.state.equals("soldout")) {
                status = sell ? "Товар не принимается!" : "Товар закончился!";
                sc = -43691;
            } else if (l.state.equals("limit")) {
                status = "Больше нельзя!";
                sc = -43691;
            } else if (sell) {
                status = this.affordable(l, n) ? "Готово к продаже!" : "Недостаточно товара!";
                sc = this.affordable(l, n) ? -11141291 : -43691;
            } else {
                status = this.affordable(l, n) ? (l.type.equals("trade") ? "Готово к обмену!" : "Готово к покупке!") : "Недостаточно средств!";
                sc = this.affordable(l, n) ? -11141291 : -43691;
            }

            text(g, font, font.plainSubstrByWidth(status, 200), this.x + 296, this.y + yOff + 175, 0.75F, sc, false);

            for (int ix = 0; ix < ARROWS.length; ix++) {
                int[] r = this.arrowRect(ix);
                int step = ARROWS[ix][0];
                boolean hov = in(r, mx, my);
                blit(g, project, c.arrow(step, hov), tex("arrow_" + step + (hov ? "_glow" : "")), r[0], r[1], r[2], r[3]);
            }

            if (!l.description.isEmpty()) {
                List<FormattedCharSequence> lines = font.split(Component.literal(l.description), 176);
                float dy = this.y + 220;

                for (int ix = 0; ix < Math.min(3, lines.size()); ix++) {
                    g.pose().pushPose();
                    g.pose().scale(0.6F, 0.6F, 1.0F);
                    GuiText.draw(g, font, lines.get(ix), (this.x + 304) / 0.6F, dy / 0.6F, c.mutedColor, false);
                    g.pose().popPose();
                    dy += 6.5F;
                }
            }
        }

        int[] d = this.dealRect();
        boolean hov = l != null && in(d, mx, my);
        EngineConfig.Skin bskin = hov && c.buttonHover.isSet() ? c.buttonHover : (c.dealButton.isSet() ? c.dealButton : c.button);
        blit(g, project, bskin, hov ? BUY_LIGHT : BUY, d[0], d[1], d[2], d[3]);
        String var10000;
        if (l == null) {
            var10000 = "ВЫБЕРИТЕ ТОВАР";
        } else {
            String var43 = l.type;
            switch (var43) {
                case "sell":
                    var10000 = "ПРОДАТЬ";
                    break;
                case "trade":
                    var10000 = "ОБМЕНЯТЬ";
                    break;
                default:
                    var10000 = "КУПИТЬ";
            }
        }

        String action = var10000;
        Component act = Component.literal(action).withStyle(ChatFormatting.BOLD);
        g.pose().pushPose();
        g.pose().scale(0.75F, 0.75F, 1.0F);
        g.drawString(font, act, (int)((d[0] + d[2] / 2.0F) / 0.75F - font.width(act) / 2.0F), (int)((d[1] + 4) / 0.75F) + 1, -1, false);
        g.pose().popPose();
        g.pose().popPose();
        this.at("window");
    }
}
