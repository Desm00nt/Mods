package org.sprautestudio.spraute_engine.ui.widgets;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;
import org.sprautestudio.spraute_engine.ui.Canvas;
import org.sprautestudio.spraute_engine.ui.Icon;
import org.sprautestudio.spraute_engine.ui.Theme;
import org.sprautestudio.spraute_engine.ui.UI;
import org.sprautestudio.spraute_engine.ui.Widget;

public class Dropdown<T> extends Widget implements UI.CursorProvider {
    private final Supplier<List<T>> options;
    private final Supplier<T> value;
    private final Consumer<T> onPick;
    private final Function<T, String> labeler;
    public String placeholder = "—";
    private boolean searchable;

    public Dropdown(Supplier<List<T>> options, Supplier<T> value, Function<T, String> labeler, Consumer<T> onPick) {
        this.options = options;
        this.value = value;
        this.labeler = labeler;
        this.onPick = onPick;
        this.prefH = 18;
    }

    public Dropdown<T> searchable() {
        this.searchable = true;
        return this;
    }

    @Override
    public int cursor(int mx, int my) {
        return 221188;
    }

    @Override
    protected void paint(Canvas c, int mx, int my, float dt) {
        c.roundBorder(this.x, this.y, this.w, this.h, 3, Theme.lerp(-15592678, -14210506, this.hoverAnim), -12762793);
        T v = this.value.get();
        String s = v == null ? this.placeholder : this.labeler.apply(v);
        c.textEllipsized(s, this.x + 6, this.y + (this.h - 8) / 2, this.w - 22, v == null ? -9933435 : -1841682);
        c.icon(Icon.CHEVRON_DOWN, this.x + this.w - 14, this.y + (this.h - 9) / 2, -6577995);
    }

    @Override
    public boolean mousePressed(double mx, double my, int button) {
        if (button != 0) {
            return false;
        } else {
            List<T> choices = this.options.get();
            if (!this.searchable && choices.size() <= 18) {
                PopupMenu pm = new PopupMenu();
                T current = this.value.get();

                for (T opt : choices) {
                    PopupMenu.Item it = pm.item(this.labeler.apply(opt), () -> this.onPick.accept(opt));
                    it.checked(() -> opt.equals(current));
                }

                if (pm.isEmpty()) {
                    pm.item("(пусто)", null).enabled(() -> false);
                }

                pm.show(this.x, this.y + this.h + 1, this);
                return true;
            } else {
                this.openSearch(choices);
                return true;
            }
        }
    }

    private void openSearch(List<T> choices) {
        UI ui = UI.get();
        if (ui != null) {
            Dialog dialog = new Dialog("Выбор из списка", Icon.SEARCH).width(Math.min(440, ui.vw - 30));
            TextField search = new TextField().placeholder("Поиск по названию или ID").icon(Icon.SEARCH);
            dialog.body.add(search);
            ListView<T> list = new ListView<>(
                (c, opt, x, y, w, h, selected, hovered) -> c.textEllipsized(
                    this.labeler.apply(opt), x + 8, y + (h - 8) / 2, w - 16, selected ? -1841682 : -6577995
                )
            );
            list.prefH = Math.max(100, Math.min(250, ui.vh - 130));
            list.background = -15592678;
            list.emptyText = "Совпадений нет";
            dialog.body.add(list);
            Runnable pick = () -> {
                T selected = list.selectedItem();
                if (selected != null) {
                    dialog.close();
                    this.onPick.accept(selected);
                }
            };
            list.onActivate = selected -> pick.run();
            search.onChange = query -> {
                List<T> found = new ArrayList<>();

                for (T opt : choices) {
                    String label = this.labeler.apply(opt);
                    String id = String.valueOf(opt);
                    if (matches(query, label, id)) {
                        found.add(opt);
                    }
                }

                list.setItems(found);
                if (list.selectedItem() == null && !found.isEmpty()) {
                    list.select(0);
                }
            };
            search.onChange.accept("");
            T current = this.value.get();
            if (current != null && list.items().contains(current)) {
                list.select(list.items().indexOf(current));
            }

            dialog.addButton("Отмена", Button.Style.DEFAULT, dialog::close);
            dialog.addButton("Выбрать", Button.Style.PRIMARY, pick);
            dialog.onEnter = pick;
            dialog.open();
        }
    }

    private static boolean matches(String query, String label, String id) {
        if (query != null && !query.isBlank()) {
            String hay = (label + " " + id).toLowerCase(Locale.ROOT).replace('_', ' ').replace('-', ' ');

            for (String word : query.toLowerCase(Locale.ROOT).trim().replace('_', ' ').replace('-', ' ').split("\\s+")) {
                if (!hay.contains(word)) {
                    return false;
                }
            }

            return true;
        } else {
            return true;
        }
    }
}
