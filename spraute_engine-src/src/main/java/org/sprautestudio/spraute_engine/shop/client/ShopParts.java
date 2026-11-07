package org.sprautestudio.spraute_engine.shop.client;

import java.util.ArrayList;
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
import org.sprautestudio.spraute_engine.engine.client.EngineClient;
import org.sprautestudio.spraute_engine.engine.client.LiveDraw;
import org.sprautestudio.spraute_engine.gui.GuiDoc;
import org.sprautestudio.spraute_engine.gui.client.GuiView;
import org.sprautestudio.spraute_engine.shop.ShopDef;
import org.sprautestudio.spraute_engine.shop.ShopView;
import org.sprautestudio.spraute_engine.ui.Canvas;

public final class ShopParts {
    private static final ResourceLocation CELL = tex("cell");
    private static final ResourceLocation CELL_GLOW = tex("cell_hot");
    private static final ResourceLocation LOCKED = tex("cell_locked");
    private static final ResourceLocation SOLD = tex("cell_sold");
    private static final ResourceLocation BUY = tex("buy");
    private static final ResourceLocation BUY_LIGHT = tex("buy_hot");
    private static final ResourceLocation SLIDER = tex("slider");
    private static final int[] STEPS = new int[]{-5, -1, 1, 5};
    private static ShopParts.State sample;

    private ShopParts() {
    }

    private static ResourceLocation tex(String name) {
        return new ResourceLocation("spraute_engine", "textures/gui/store/" + name + ".png");
    }

    public static ShopParts.State sample() {
        if (sample == null) {
            sample = new ShopParts.State(new ShopPanel(ShopView.sample()));
        }

        return sample;
    }

    static ShopParts.State state(GuiView v) {
        return v.liveState instanceof ShopParts.State s ? s : sample();
    }

    public static void vars(GuiView v) {
        ShopParts.State s = state(v);
        v.vars.put("title", s.panel.view.title);
        v.vars.put("balance", String.valueOf(s.panel.view.balance));
        v.vars.put("currency", s.panel.view.currencyName);
    }

    private static EngineConfig.Shop st() {
        return EngineClient.config().shop;
    }

    private static ItemStack coin(ShopView view) {
        if (!view.currency.equals("coins")) {
            return ShopPanel.stack(view.currency, 1);
        } else {
            ItemStack own = EngineClient.coinItem();
            if (!own.isEmpty()) {
                return own;
            } else {
                ItemStack s = ShopPanel.stack("minecraft:gold_nugget", 1);
                return s.isEmpty() ? new ItemStack(Items.GOLD_NUGGET) : s;
            }
        }
    }

    public static void draw(GuiView v, Canvas c, GuiDoc.Element e, int[] r, float op) {
        ShopParts.State s = state(v);
        c.flush();
        float mx = v.mouseDX();
        float my = v.mouseDY();
        if (e.type.equals("shop_goods")) {
            s.hovered = null;
        }

        String var8 = e.type;
        switch (var8) {
            case "shop_goods":
                goods(v, c.g, c.font, s, e, r, op, mx, my);
                break;
            case "shop_slider":
                slider(v, c.g, s, e, r, op);
                break;
            case "shop_item":
                item(c.g, c.font, s, e, r, op, mx, my);
                break;
            case "shop_info":
                info(c.g, c.font, s, e, r, op);
                break;
            case "shop_arrows":
                arrows(v, c.g, s, e, r, op, mx, my);
                break;
            case "shop_deal":
                deal(v, c.g, c.font, s, e, r, op, mx, my);
                break;
            case "shop_desc":
                desc(c.g, c.font, s, e, r, op);
                break;
            case "shop_money":
                money(c.g, c.font, s, e, r, op);
        }

        c.flush();
    }

    private static void goods(GuiView v, Gfx g, Font font, ShopParts.State s, GuiDoc.Element e, int[] r, float op, float mx, float my) {
        s.goodsRect = (int[])r.clone();
        s.cellRects.clear();
        s.cellIds.clear();
        EngineConfig.Shop st = st();
        ShopPanel p = s.panel;
        List<ShopView.Line> ls = p.lines();
        int cols = Math.max(1, e.i("columns"));
        int cw = Math.max(8, e.i("cellWidth"));
        int ch = Math.max(8, e.i("cellHeight"));
        int sx = cw + e.i("gapX");
        int sy = ch + e.i("gapY");
        int px = e.i("padX");
        int py = e.i("padY");
        int rows = (ls.size() + cols - 1) / cols;
        s.maxScroll = Math.max(0, rows * sy + py - r[3]);
        p.scroll = Math.max(0.0, Math.min(s.maxScroll, p.scroll));
        ItemStack coin = coin(p.view);
        LiveDraw.clip(g, r[0], r[1], r[2], r[3]);

        for (int i = 0; i < ls.size(); i++) {
            ShopView.Line l = ls.get(i);
            int[] cr = new int[]{r[0] + px + i % cols * sx, (int)Math.round(r[1] + py + i / cols * sy - p.scroll), cw, ch};
            s.cellRects.add(cr);
            s.cellIds.add(l.id);
            if (cr[1] + ch >= r[1] && cr[1] <= r[1] + r[3]) {
                boolean sel = l.id.equals(p.selected);
                boolean ok = l.state.equals("ok") || l.state.equals("limit");
                ResourceLocation def = l.state.equals("locked") ? LOCKED : (l.state.equals("soldout") ? SOLD : (sel ? CELL_GLOW : CELL));
                String prop = ok ? (sel ? "cellOn" : "cell") : (l.state.equals("locked") ? "locked" : "sold");
                EngineConfig.Skin skin = ok ? (sel ? st.cellOn : st.cell) : (l.state.equals("locked") ? st.locked : st.sold);
                LiveDraw.image(g, e, LiveDraw.tex(v.project, e, prop, skin, def), cr[0], cr[1], cw, ch, op);
                ItemStack it = ShopPanel.stack(l.item, l.count);
                float isz = e.i("itemSize") > 0 ? e.i("itemSize") : cw * 16 / 30.0F;
                LiveDraw.item(g, it, cr[0] + (cw - isz) / 2.0F, cr[1] + ch * 8 / 43.0F, isz / 16.0F);
                if (l.count > 1) {
                    g.pose().pushPose();
                    g.pose().translate(cr[0] + (cw - isz) / 2.0F, cr[1] + ch * 8 / 43.0F, 0.0F);
                    g.pose().scale(isz / 16.0F, isz / 16.0F, 1.0F);
                    g.renderItemDecorations(font, it, 0, 0);
                    g.pose().popPose();
                }

                boolean trade = l.type.equals("trade") && l.price == 0;
                String pr = trade ? "ОБМЕН" : String.valueOf(l.price);
                int col = l.type.equals("sell") ? e.color("sellColor") : e.color("priceColor");
                float k = cw / 30.0F;
                float ty = cr[1] + ch - 7.0F * k;
                if (trade) {
                    LiveDraw.text(g, font, pr, cr[0] + cw / 2.0F - font.width(pr) * 0.25F * k, ty, 0.5F * k, LiveDraw.a(col, op), true);
                } else {
                    float pw = (font.width(pr) * 0.5F + 6.0F) * k;
                    LiveDraw.item(g, coin, cr[0] + cw / 2.0F - pw / 2.0F - k, ty - 1.5F * k, 0.375F * k);
                    LiveDraw.text(g, font, pr, cr[0] + cw / 2.0F - pw / 2.0F + 6.0F * k, ty, 0.5F * k, LiveDraw.a(col, op), true);
                }

                if (LiveDraw.in(cr, mx, my) && LiveDraw.in(r, mx, my)) {
                    s.hovered = it;
                }
            }
        }

        g.disableScissor();
        if (ls.isEmpty()) {
            LiveDraw.text(g, font, "Здесь пока пусто", (float)(r[0] + px), (float)(r[1] + py + 4), 0.75F, LiveDraw.a(e.color("mutedColor"), op), false);
        }
    }

    private static void slider(GuiView v, Gfx g, ShopParts.State s, GuiDoc.Element e, int[] r, float op) {
        s.sliderRect = (int[])r.clone();
        if (!(s.maxScroll <= 0.0) || e.b("always")) {
            int th = Math.max(2, e.i("thumbHeight"));
            double share = s.maxScroll <= 0.0 ? 0.0 : s.panel.scroll / s.maxScroll;
            LiveDraw.image(g, e, LiveDraw.tex(v.project, e, "image", st().slider, SLIDER), r[0], (int)Math.round(r[1] + share * (r[3] - th)), r[2], th, op);
        }
    }

    private static void item(Gfx g, Font font, ShopParts.State s, GuiDoc.Element e, int[] r, float op, float mx, float my) {
        ShopPanel p = s.panel;
        ShopView.Line l = p.line(p.selected);
        if (l != null) {
            int size = e.i("itemSize") > 0 ? e.i("itemSize") : Math.min(r[2], r[3]);
            LiveDraw.item(g, ShopPanel.stack(l.item, 1), r[0], r[1], size / 16.0F);
            if (LiveDraw.in(new int[]{r[0], r[1], size, size}, mx, my)) {
                s.hovered = ShopPanel.stack(l.item, l.count);
            }

            if (e.b("amount")) {
                LiveDraw.text(g, font, "x" + p.qty, r[0] + size * 0.6F, r[1] + size * 0.62F, size / 21.3F, LiveDraw.a(e.color("textColor"), op), true);
            }
        }
    }

    private static void info(Gfx g, Font font, ShopParts.State s, GuiDoc.Element e, int[] r, float op) {
        ShopPanel p = s.panel;
        ShopView.Line l = p.line(p.selected);
        if (l != null) {
            int white = LiveDraw.a(e.color("textColor"), op);
            ItemStack coin = coin(p.view);
            int n = Math.max(1, p.qty);
            boolean sell = l.type.equals("sell");
            int priceCol = LiveDraw.a(sell ? e.color("sellColor") : e.color("priceColor"), op);
            int x = r[0];
            int y = r[1];
            String name = ShopPanel.title(l);
            float ts = Math.max(0.5F, Math.min(1.0F, r[2] / 0.75F / Math.max(1, font.width(name)))) * 0.75F;
            LiveDraw.text(g, font, name, (float)x, (float)y, ts, white, false);
            LiveDraw.text(g, font, sell ? "Торговец заплатит: " : "Стоимость товара: ", (float)x, (float)(y + 9), 0.75F, white, false);
            price(g, font, coin, l, x, y + 19, 1, priceCol, white);
            LiveDraw.text(g, font, "Количество: " + n + " по " + l.count + " (" + n * l.count + " шт.)", (float)x, (float)(y + 34), 0.75F, white, false);
            LiveDraw.text(g, font, sell ? "Итого получите: " : "Итоговая стоимость: ", (float)x, (float)(y + 43), 0.75F, white, false);
            price(g, font, coin, l, x, y + 53, n, priceCol, white);
            String status;
            boolean good;
            if (!p.view.message.isEmpty() && System.currentTimeMillis() - p.messageAt < 3000L) {
                status = p.view.message;
                good = p.view.ok;
            } else if (l.state.equals("locked")) {
                status = l.lockText.isEmpty() ? "Товар заблокирован!" : l.lockText;
                good = false;
            } else if (l.state.equals("soldout")) {
                status = sell ? "Товар не принимается!" : "Товар закончился!";
                good = false;
            } else if (l.state.equals("limit")) {
                status = "Больше нельзя!";
                good = false;
            } else {
                good = p.affordable(l, n);
                status = sell
                    ? (good ? "Готово к продаже!" : "Недостаточно товара!")
                    : (good ? (l.type.equals("trade") ? "Готово к обмену!" : "Готово к покупке!") : "Недостаточно средств!");
            }

            LiveDraw.text(
                g,
                font,
                font.plainSubstrByWidth(status, (int)(r[2] / 0.75F)),
                (float)x,
                (float)(y + 67),
                0.75F,
                LiveDraw.a(good ? e.color("okColor") : e.color("badColor"), op),
                false
            );
        }
    }

    private static void price(Gfx g, Font font, ItemStack coin, ShopView.Line l, float x, float y, int n, int priceCol, int white) {
        float end = x;
        if (l.price > 0 || l.costs.isEmpty()) {
            LiveDraw.item(g, coin, x, y - 3.0F, 0.6F);
            String t = "x" + l.price * n;
            LiveDraw.text(g, font, t, x + 11.0F, y, 0.75F, priceCol, false);
            end = x + 11.0F + font.width(t) * 0.75F;
        }

        for (ShopDef.Stack cs : l.costs) {
            LiveDraw.item(g, ShopPanel.stack(cs.item, 1), end + 3.0F, y - 3.0F, 0.6F);
            String t = "x" + cs.count * n;
            LiveDraw.text(g, font, t, end + 14.0F, y, 0.75F, white, true);
            end += 14.0F + font.width(t) * 0.75F;
        }
    }

    private static void arrows(GuiView v, Gfx g, ShopParts.State s, GuiDoc.Element e, int[] r, float op, float mx, float my) {
        EngineConfig.Shop st = st();
        int[][] at = new int[][]{{0, 0, 12, 15}, {25, 1, 9, 13}, {48, 1, 9, 13}, {70, 0, 12, 15}};
        float kx = r[2] / 82.0F;
        float ky = r[3] / 15.0F;

        for (int i = 0; i < 4; i++) {
            int[] a = at[i];
            int[] ar = new int[]{r[0] + Math.round(a[0] * kx), r[1] + Math.round(a[1] * ky), Math.round(a[2] * kx), Math.round(a[3] * ky)};
            s.arrowRects[i] = ar;
            int step = STEPS[i];
            boolean hov = LiveDraw.in(ar, mx, my);
            String name = (step > 0 ? "plus" : "minus") + Math.abs(step) + (hov ? "Hover" : "");
            LiveDraw.image(
                g, e, LiveDraw.tex(v.project, e, name, st.arrow(step, hov), tex("arrow_" + step + (hov ? "_glow" : ""))), ar[0], ar[1], ar[2], ar[3], op
            );
        }
    }

    private static void deal(GuiView v, Gfx g, Font font, ShopParts.State s, GuiDoc.Element e, int[] r, float op, float mx, float my) {
        s.dealRect = (int[])r.clone();
        EngineConfig.Shop st = st();
        ShopView.Line l = s.panel.line(s.panel.selected);
        boolean hov = l != null && LiveDraw.in(r, mx, my);
        EngineConfig.Skin skin = hov && st.buttonHover.isSet() ? st.buttonHover : (st.dealButton.isSet() ? st.dealButton : st.button);
        String own = hov && !e.get("hoverImage").isEmpty() ? e.get("hoverImage") : e.get("image");
        LiveDraw.image(g, e, LiveDraw.tex(v.project, own, skin, hov ? BUY_LIGHT : BUY), r[0], r[1], r[2], r[3], op);
        String var10000;
        if (l == null) {
            var10000 = "ВЫБЕРИТЕ ТОВАР";
        } else {
            String act = l.type;
            switch (act) {
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
        float sc = Math.max(0.3F, e.f("textScale"));
        LiveDraw.text(
            g,
            font,
            act,
            r[0] + r[2] / 2.0F - font.width(act) * sc / 2.0F,
            r[1] + (r[3] - 8.0F * sc) / 2.0F + 0.5F,
            sc,
            LiveDraw.a(e.color("textColor"), op),
            false
        );
    }

    private static void desc(Gfx g, Font font, ShopParts.State s, GuiDoc.Element e, int[] r, float op) {
        ShopView.Line l = s.panel.line(s.panel.selected);
        if (l != null && !l.description.isEmpty()) {
            float sc = Math.max(0.3F, e.f("textScale"));
            List<FormattedCharSequence> lines = font.split(Component.literal(l.description), (int)(r[2] / sc));
            float dy = r[1];
            int max = Math.max(1, (int)(r[3] / (10.8F * sc)));

            for (int i = 0; i < Math.min(max, lines.size()); i++) {
                LiveDraw.text(g, font, lines.get(i), (float)r[0], dy, sc, LiveDraw.a(e.color("textColor"), op), false);
                dy += 10.8F * sc;
            }
        }
    }

    private static void money(Gfx g, Font font, ShopParts.State s, GuiDoc.Element e, int[] r, float op) {
        String bal = String.valueOf(s.panel.view.balance);
        float bw = font.width(bal);
        float x = r[0] + r[2] - bw;
        LiveDraw.item(g, coin(s.panel.view), x - 13.0F, r[1] + (r[3] - 12) / 2.0F, 0.75F);
        LiveDraw.text(g, font, bal, x, r[1] + (r[3] - 8) / 2.0F, 1.0F, LiveDraw.a(e.color("textColor"), op), true);
    }

    public static String hit(ShopParts.State s, float mx, float my) {
        if (LiveDraw.in(s.goodsRect, mx, my)) {
            for (int i = 0; i < s.cellRects.size(); i++) {
                if (LiveDraw.in(s.cellRects.get(i), mx, my)) {
                    return "cell:" + s.cellIds.get(i);
                }
            }
        }

        for (int ix = 0; ix < 4; ix++) {
            if (LiveDraw.in(s.arrowRects[ix], mx, my)) {
                return "arrow:" + STEPS[ix];
            }
        }

        return LiveDraw.in(s.dealRect, mx, my) ? "deal" : null;
    }

    public static boolean click(ShopParts.State s, float mx, float my) {
        String h = hit(s, mx, my);
        if (h == null) {
            return false;
        } else {
            ShopPanel p = s.panel;
            ShopView.Line l = p.line(p.selected);
            if (h.startsWith("cell:")) {
                String id = h.substring(5);
                if (!id.equals(p.selected)) {
                    p.qty = 1;
                }

                p.selected = id;
            } else if (h.startsWith("arrow:") && l != null) {
                p.qty = Math.max(1, Math.min(p.maxQty(l), p.qty + Integer.parseInt(h.substring(6))));
            } else if (h.equals("deal") && l != null) {
                return l.state.equals("ok") && p.affordable(l, p.qty);
            }

            return false;
        }
    }

    public static void scroll(ShopParts.State s, double delta) {
        s.panel.scroll = Math.max(0.0, Math.min(s.maxScroll, s.panel.scroll - delta * 20.0));
    }

    public static final class State {
        public final ShopPanel panel;
        public double maxScroll;
        final List<int[]> cellRects = new ArrayList<>();
        final List<String> cellIds = new ArrayList<>();
        final int[][] arrowRects = new int[4][];
        int[] goodsRect;
        int[] dealRect;
        int[] sliderRect;
        public ItemStack hovered;

        public State(ShopPanel panel) {
            this.panel = panel;
        }
    }
}
