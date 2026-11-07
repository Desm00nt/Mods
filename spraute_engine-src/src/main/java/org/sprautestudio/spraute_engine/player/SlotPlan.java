package org.sprautestudio.spraute_engine.player;

import java.util.List;
import java.util.Locale;

public final class SlotPlan {
    public static final int HOTBAR = 9;
    public static final int MAIN = 36;
    public static final int OFFHAND = 40;
    public static final int ARMOR_FROM = 36;
    public static final int EXTRA_MAX = 54;
    public static final int SIZE_MAX = 90;
    public static final int EXTRA_CELL = 1000;
    public static final int CRAFT_CELL = 2000;
    public static final int CRAFT_RESULT = 2004;
    public static final int MENU_EXTRA = 46;
    private static final int MENU_HOTBAR = 36;
    private static final int MENU_FEET = 8;
    private static final int MENU_OFFHAND = 45;
    private static final int MENU_RESULT = 0;
    private static final int MENU_CRAFT = 1;
    public static final String TOP = "top";
    public static final String BOTTOM = "bottom";
    public static final String COLUMNS = "columns";
    public static final String HOTBAR_FIRST = "hotbar";
    public static final String LIST = "list";
    public static final List<String> ORDERS = List.of("top", "bottom", "columns", "hotbar", "list");
    private static final int BACKPACK = 27;
    private static final int ROWS = 3;
    public long locked;
    public int size = 36;
    public String order = "";

    public static int menuSlot(int cell) {
        if (cell == 2004) {
            return 0;
        } else if (cell >= 2000) {
            return 1 + cell - 2000;
        } else if (cell >= 1000) {
            return 46 + cell - 1000;
        } else if (cell < 9) {
            return 36 + cell;
        } else if (cell < 36) {
            return cell;
        } else {
            return cell == 40 ? 45 : 8 - (cell - 36);
        }
    }

    public static String orderTitle(String order) {
        return switch (order) {
            case "bottom" -> "Рюкзак снизу вверх, хотбар последним";
            case "columns" -> "Рюкзак столбцами справа, хотбар последним";
            case "hotbar" -> "Сначала хотбар, потом рюкзак сверху вниз";
            case "list" -> "Свой список ячеек";
            default -> "Рюкзак сверху вниз, хотбар последним";
        };
    }

    private static int place(int cell, String order) {
        int n = cell - 9;
        int column = n % 9;
        int row = n / 9;

        return switch (order) {
            case "bottom" -> 26 - n;
            case "columns" -> (8 - column) * 3 + row;
            default -> n;
        };
    }

    public static boolean closedByCount(int cell, int open, String order) {
        if (cell >= 0 && cell < 36) {
            int closed = 36 - Math.max(0, Math.min(36, open));
            int backpack = "hotbar".equals(order) ? Math.max(0, closed - 9) : Math.min(27, closed);
            return cell < 9 ? cell >= 9 - (closed - backpack) : place(cell, order) < backpack;
        } else {
            return false;
        }
    }

    public boolean isLocked(int cell) {
        return this.isLocked(cell, "top");
    }

    public boolean isLocked(int cell, String fallbackOrder) {
        return cell >= 0 && cell <= 40
            ? (this.locked >> cell & 1L) != 0L || closedByCount(cell, this.size, this.order.isEmpty() ? fallbackOrder : this.order)
            : false;
    }

    public int extra() {
        return Math.max(0, this.size - 36);
    }

    public static int clampSize(double n) {
        return (int)Math.max(0L, Math.min(90L, Math.round(n)));
    }

    public static long parse(String text) {
        long bits = 0L;

        for (String raw : text.split(",")) {
            String part = raw.trim().toLowerCase(Locale.ROOT).replaceAll("\\.0+$", "");
            switch (part) {
                case "hotbar":
                    bits |= range(0, 8);
                    break;
                case "main":
                    bits |= range(9, 35);
                    break;
                case "armor":
                    bits |= range(36, 39);
                    break;
                case "offhand":
                    bits |= 1099511627776L;
                    break;
                case "all":
                    bits |= range(0, 40);
                    break;
                default:
                    int dash = part.indexOf(45);
                    int from = number(dash > 0 ? part.substring(0, dash) : part, text);
                    int to = dash > 0 ? number(part.substring(dash + 1), text) : from;
                    if (to < from) {
                        throw new IllegalArgumentException("Ячейки идут по порядку: " + text);
                    }

                    bits |= range(from - 1, to - 1);
            }
        }

        return bits;
    }

    public static String problem(String text) {
        try {
            parse(text);
            return null;
        } catch (IllegalArgumentException var2) {
            return var2.getMessage();
        }
    }

    private static int number(String part, String whole) {
        try {
            int n = Integer.parseInt(part.trim());
            if (n >= 1 && n <= 36) {
                return n;
            }
        } catch (NumberFormatException var3) {
        }

        throw new IllegalArgumentException(
            "Ячейки: числа от 1 до 36 (1–9 — хотбар), диапазон \"3-7\", или hotbar, main, armor, offhand, all. Не понятно: " + whole
        );
    }

    private static long range(int from, int to) {
        long bits = 0L;

        for (int i = from; i <= to; i++) {
            bits |= 1L << i;
        }

        return bits;
    }
}
