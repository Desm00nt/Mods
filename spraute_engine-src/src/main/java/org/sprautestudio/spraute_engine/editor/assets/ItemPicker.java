package org.sprautestudio.spraute_engine.editor.assets;

import java.util.ArrayList;
import java.util.List;
import java.util.TreeSet;
import java.util.function.Consumer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.sprautestudio.spraute_engine.compat.Regs;
import org.sprautestudio.spraute_engine.ui.Canvas;
import org.sprautestudio.spraute_engine.ui.Icon;
import org.sprautestudio.spraute_engine.ui.UI;
import org.sprautestudio.spraute_engine.ui.Widget;
import org.sprautestudio.spraute_engine.ui.widgets.Button;
import org.sprautestudio.spraute_engine.ui.widgets.Dialog;
import org.sprautestudio.spraute_engine.ui.widgets.Dropdown;
import org.sprautestudio.spraute_engine.ui.widgets.TextField;

public final class ItemPicker {
    private static List<ItemPicker.Entry> cache;

    private ItemPicker() {
    }

    private static List<ItemPicker.Entry> all() {
        if (cache != null) {
            return cache;
        } else {
            List<ItemPicker.Entry> list = new ArrayList<>();

            for (Item it : Regs.ITEM) {
                if (it != Items.AIR) {
                    ResourceLocation key = Regs.ITEM.getKey(it);

                    String title;
                    try {
                        title = it.getDescription().getString();
                    } catch (RuntimeException var6) {
                        title = key.toString();
                    }

                    list.add(new ItemPicker.Entry(key.toString(), title, key.getNamespace(), new ItemStack(it)));
                }
            }

            cache = list;
            return list;
        }
    }

    public static void open(String current, Consumer<String> onPick) {
        UI ui = UI.get();
        if (ui != null) {
            Dialog d = new Dialog("Выбор предмета", Icon.CUBE).width(Math.min(640, ui.vw - 30));
            ItemPicker.Grid grid = new ItemPicker.Grid(current, id -> {
                d.close();
                onPick.accept(id);
            });
            grid.prefH = Math.min(360, ui.vh - 120);
            d.body.add(grid);
            d.addButton("Отмена", Button.Style.DEFAULT, d::close);
            Runnable ok = () -> {
                if (grid.selected != null) {
                    d.close();
                    onPick.accept(grid.selected.id);
                }
            };
            d.addButton("Выбрать", Button.Style.PRIMARY, ok);
            d.onEnter = ok;
            d.open();
            grid.search.requestFocus();
        }
    }

    private record Entry(String id, String title, String mod, ItemStack stack) {
    }

    private static final class Grid extends Widget {
        static final int CELL = 20;
        final TextField search = new TextField().placeholder("Поиск по названию или id").icon(Icon.SEARCH);
        final Dropdown<String> mod;
        final Consumer<String> choose;
        String modFilter = "";
        List<ItemPicker.Entry> shown = new ArrayList<>();
        ItemPicker.Entry selected;
        ItemPicker.Entry hovered;
        double scroll;
        long lastClick;
        ItemPicker.Entry lastClicked;

        Grid(String current, Consumer<String> choose) {
            this.choose = choose;
            this.add(this.search);
            TreeSet<String> mods = new TreeSet<>();

            for (ItemPicker.Entry e : ItemPicker.all()) {
                mods.add(e.mod);
            }

            List<String> modList = new ArrayList<>();
            modList.add("");
            modList.addAll(mods);
            this.mod = this.add(new Dropdown<>(() -> modList, () -> this.modFilter, m -> m.isEmpty() ? "Все моды" : m, m -> {
                this.modFilter = m;
                this.refresh();
            }));
            this.search.onChange = s -> this.refresh();
            this.refresh();
            String cur = current == null ? "" : current.replace("\"", "").trim();
            if (!cur.isEmpty() && !cur.contains(":")) {
                cur = "minecraft:" + cur;
            }

            for (ItemPicker.Entry e : this.shown) {
                if (e.id.equals(cur)) {
                    this.selected = e;
                    break;
                }
            }
        }

        void refresh() {
            String q = this.search.text();
            List<ItemPicker.Entry> out = new ArrayList<>();

            for (ItemPicker.Entry e : ItemPicker.all()) {
                if ((this.modFilter.isEmpty() || e.mod.equals(this.modFilter)) && AssetIndex.matches(q, e.id, e.title)) {
                    out.add(e);
                }
            }

            this.shown = out;
            this.scroll = 0.0;
        }

        int cols() {
            return Math.max(1, (this.w - 8) / 20);
        }

        int gridY() {
            return this.y + 24;
        }

        int gridH() {
            return this.h - 24 - 30;
        }

        int maxScroll() {
            int rows = (this.shown.size() + this.cols() - 1) / this.cols();
            return Math.max(0, rows * 20 - this.gridH());
        }

        @Override
        public void layout() {
            int mw = Math.min(150, this.w / 3);
            this.search.bounds(this.x, this.y, this.w - mw - 6, 18);
            this.mod.bounds(this.x + this.w - mw, this.y, mw, 18);
            super.layout();
        }

        ItemPicker.Entry at(double mx, double my) {
            int gy = this.gridY();
            if (!(mx < this.x) && !(mx >= this.x + this.cols() * 20) && !(my < gy) && !(my >= gy + this.gridH())) {
                int col = (int)((mx - this.x) / 20.0);
                int row = (int)((my - gy + this.scroll) / 20.0);
                int i = row * this.cols() + col;
                return i >= 0 && i < this.shown.size() ? this.shown.get(i) : null;
            } else {
                return null;
            }
        }

        @Override
        protected void paint(Canvas c, int mx, int my, float dt) {
            int gy = this.gridY();
            int gh = this.gridH();
            int cols = this.cols();
            c.fill(this.x, gy, this.w, gh, -15592678);
            c.outline(this.x, gy, this.w, gh, -13749952);
            this.hovered = this.at(mx, my);
            c.pushClip(this.x, gy, this.w, gh);
            int first = (int)(this.scroll / 20.0) * cols;
            int last = Math.min(this.shown.size(), first + (gh / 20 + 2) * cols);

            for (int i = first; i < last; i++) {
                ItemPicker.Entry e = this.shown.get(i);
                int cx = this.x + i % cols * 20;
                int cy = gy + i / cols * 20 - (int)this.scroll;
                if (e == this.selected) {
                    c.round(cx, cy, 20, 20, 3, -7639809);
                } else if (e == this.hovered) {
                    c.round(cx, cy, 20, 20, 3, -14210506);
                }

                c.flush();
                c.g.renderItem(e.stack, cx + 2, cy + 2);
            }

            c.popClip();
            if (this.shown.isEmpty()) {
                c.textCentered("Ничего не найдено", this.x + this.w / 2, gy + gh / 2 - 4, -9933435);
            }

            ItemPicker.Entry info = this.hovered != null ? this.hovered : this.selected;
            int iy = gy + gh + 6;
            if (info != null) {
                c.flush();
                c.g.renderItem(info.stack, this.x + 2, iy + 2);
                c.text(info.title, this.x + 24, iy + 2, -1841682);
                c.text(info.id, this.x + 24, iy + 12, -9933435);
            }

            String count = this.shown.size() + " предметов";
            c.text(count, this.x + this.w - c.width(count), iy + 7, -9933435);
        }

        @Override
        public boolean mousePressed(double mx, double my, int button) {
            if (super.mousePressed(mx, my, button)) {
                return true;
            } else {
                ItemPicker.Entry e = this.at(mx, my);
                if (e == null) {
                    return false;
                } else {
                    long now = System.currentTimeMillis();
                    if (e == this.lastClicked && now - this.lastClick < 350L) {
                        this.choose.accept(e.id);
                        return true;
                    } else {
                        this.selected = e;
                        this.lastClicked = e;
                        this.lastClick = now;
                        return true;
                    }
                }
            }
        }

        @Override
        public boolean mouseScrolled(double mx, double my, double delta) {
            if (my >= this.gridY()) {
                this.scroll = Math.max(0.0, Math.min((double)this.maxScroll(), this.scroll - delta * 20.0 * 2.0));
                return true;
            } else {
                return super.mouseScrolled(mx, my, delta);
            }
        }

        @Override
        public String tooltipText(int mx, int my) {
            ItemPicker.Entry e = this.at(mx, my);
            return e != null ? e.title + "\n" + e.id : super.tooltipText(mx, my);
        }
    }
}
