package org.sprautestudio.spraute_engine.ui;

public final class Theme {
    public static final int BG = -15329507;
    public static final int BG_DEEP = -15724266;
    public static final int PANEL = -14934490;
    public static final int PANEL_ALT = -14605523;
    public static final int RAISED = -14210506;
    public static final int RAISED_HOVER = -13618102;
    public static final int INPUT = -15592678;
    public static final int BORDER = -13749952;
    public static final int BORDER_STRONG = -12762793;
    public static final int SHADOW = 1711276032;
    public static final int OVERLAY_DIM = -1728053248;
    public static final int TEXT = -1841682;
    public static final int TEXT_DIM = -6577995;
    public static final int TEXT_MUTED = -9933435;
    public static final int TEXT_ON_ACCENT = -1;
    public static final int ACCENT = -7639809;
    public static final int ACCENT_HOVER = -6255873;
    public static final int ACCENT_DIM = 1435200767;
    public static final int ACCENT_2 = -12988235;
    public static final int SUCCESS = -11746422;
    public static final int WARNING = -870325;
    public static final int ERROR = -1023878;
    public static final int INFO = -10638858;
    public static final int SELECTION = 1430088664;
    public static final int SELECTION_INACTIVE = 859663320;
    public static final int HOVER = 352321535;
    public static final int PRESSED = 587202559;
    public static final int CODE_BG = -15461093;
    public static final int CODE_LINE = -15000282;
    public static final int CODE_GUTTER = -15461093;
    public static final int CODE_GUTTER_TEXT = -11775898;
    public static final int CODE_GUTTER_ACTIVE = -4933428;
    public static final int CODE_CARET = -1;
    public static final int CODE_KEYWORD = -3697942;
    public static final int CODE_COMMAND = -8213761;
    public static final int CODE_STRING = -3938163;
    public static final int CODE_INTERP = -13461;
    public static final int CODE_NUMBER = -553876;
    public static final int CODE_COMMENT = -9998699;
    public static final int CODE_OPERATOR = -7741953;
    public static final int CODE_IDENT = -2696470;
    public static final int CODE_CHARACTER = -11544898;
    public static final int CODE_LOCATION = -13461;
    public static final int CODE_LABEL = -33098;
    public static final int CODE_VARIABLE = -8531969;
    public static final int CODE_FUNCTION = -10880;
    public static final int CODE_PARAM_KW = -6642226;
    public static final int CODE_ERROR = -44176;
    public static final int CODE_MATCH = 872401771;
    public static final int CODE_OCCURRENCE = 712888319;
    public static final int CODE_BRACKET = 1435098623;

    private Theme() {
    }

    public static int category(String category) {
        if (category == null) {
            return -10722448;
        } else {
            return switch (category) {
                case "Речь" -> -10638858;
                case "Ожидание" -> -870325;
                case "Персонажи" -> -12988235;
                case "Анимации" -> -28838;
                case "Функции" -> -2069345;
                case "Интерфейс" -> -10638858;
                case "Камера" -> -2075026;
                case "Эффекты" -> -14006;
                case "События" -> -1660928;
                case "Переменные" -> -29670;
                case "Операторы" -> -11554737;
                case "Значения" -> -12606776;
                case "Игрок и мир" -> -11746422;
                case "Рецепты и предметы" -> -6501275;
                case "Экран" -> -1737768;
                case "Логика" -> -7639809;
                case "Поток" -> -1023878;
                case "Объявления" -> -5195831;
                default -> -10722448;
            };
        }
    }

    public static int alpha(int color, float a) {
        int base = color >>> 24 & 0xFF;
        return (int)(base * Math.max(0.0F, Math.min(1.0F, a))) << 24 | color & 16777215;
    }

    public static int lerp(int a, int b, float t) {
        t = Math.max(0.0F, Math.min(1.0F, t));
        int aa = a >>> 24 & 0xFF;
        int ar = a >> 16 & 0xFF;
        int ag = a >> 8 & 0xFF;
        int ab = a & 0xFF;
        int ba = b >>> 24 & 0xFF;
        int br = b >> 16 & 0xFF;
        int bg = b >> 8 & 0xFF;
        int bb = b & 0xFF;
        return (int)(aa + (ba - aa) * t) << 24 | (int)(ar + (br - ar) * t) << 16 | (int)(ag + (bg - ag) * t) << 8 | (int)(ab + (bb - ab) * t);
    }

    public static int darker(int color, float f) {
        int r = (int)((color >> 16 & 0xFF) * f);
        int g = (int)((color >> 8 & 0xFF) * f);
        int b = (int)((color & 0xFF) * f);
        return color & 0xFF000000 | r << 16 | g << 8 | b;
    }
}
