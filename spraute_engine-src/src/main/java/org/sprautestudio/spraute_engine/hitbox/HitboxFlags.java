package org.sprautestudio.spraute_engine.hitbox;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class HitboxFlags {
    public static final int SOLID = 1;
    public static final int FIXED = 2;
    public static final int GHOST = 4;
    public static final int NOPICK = 8;
    public static final int DAMAGE = 16;
    public static final int NODAMAGE = 32;
    public static final int MORTAL = 64;

    private HitboxFlags() {
    }

    public static int parse(int flags, List<String> words) {
        for (String raw : words) {
            for (String w : raw.toLowerCase(Locale.ROOT).trim().split("[\\s,]+")) {
                if (!w.isEmpty()) {
                    switch (w) {
                        case "normal":
                        case "обычный":
                        case "reset":
                            flags &= -16;
                            break;
                        case "solid":
                        case "твёрдый":
                        case "твердый":
                        case "стена":
                            flags = flags & -7 | 1;
                            break;
                        case "fixed":
                        case "неподвижный":
                        case "nopush":
                            flags = flags & -6 | 2;
                            break;
                        case "push":
                        case "pushable":
                        case "толкается":
                            flags &= -8;
                            break;
                        case "ghost":
                        case "призрак":
                        case "сквозной":
                        case "passthrough":
                            flags = flags & -4 | 4;
                            break;
                        case "pick":
                        case "clickable":
                            flags &= -9;
                            break;
                        case "nopick":
                        case "unclickable":
                        case "невыделяемый":
                            flags |= 8;
                            break;
                        case "damage":
                        case "урон":
                        case "vulnerable":
                            flags = flags & -33 | 16;
                            break;
                        case "nodamage":
                        case "безурона":
                        case "invulnerable":
                        case "неуязвимый":
                            flags = flags & -81 | 32;
                            break;
                        case "mortal":
                        case "смертный":
                        case "candie":
                            flags = flags & -33 | 16 | 64;
                            break;
                        case "immortal":
                        case "бессмертный":
                            flags &= -65;
                            break;
                        default:
                            throw new IllegalArgumentException(
                                "Хитбокс: неизвестное слово «"
                                    + w
                                    + "» (есть: normal, solid, fixed, push, ghost, nopick, pick, damage, nodamage, mortal, immortal)"
                            );
                    }
                }
            }
        }

        return flags;
    }

    public static String describe(int f) {
        List<String> out = new ArrayList<>();
        out.add((f & 1) != 0 ? "solid" : ((f & 2) != 0 ? "fixed" : ((f & 4) != 0 ? "ghost" : "normal")));
        if ((f & 8) != 0) {
            out.add("nopick");
        }

        if ((f & 32) != 0) {
            out.add("nodamage");
        } else if ((f & 64) != 0) {
            out.add("mortal");
        } else if ((f & 16) != 0) {
            out.add("damage");
        }

        return String.join(" ", out);
    }
}
