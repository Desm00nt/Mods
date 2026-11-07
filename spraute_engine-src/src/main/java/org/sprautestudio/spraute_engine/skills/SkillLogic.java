package org.sprautestudio.spraute_engine.skills;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class SkillLogic {
    private SkillLogic() {
    }

    public static long xpToNext(SkillTree t, int level) {
        String var4 = t.curve;

        double need = switch (var4) {
            case "flat" -> t.xpBase;
            case "linear" -> t.xpBase + t.xpGrowth * (level - 1);
            default -> t.xpBase * Math.pow(t.xpGrowth, level - 1);
        };
        return Math.max(1L, Math.round(Math.min(need, 2.305843E18F)));
    }

    public static List<Integer> addXp(SkillTree t, SkillLogic.State s, long amount) {
        List<Integer> reached = new ArrayList<>();
        if (!t.levels || amount <= 0L) {
            return reached;
        } else if (s.level >= t.maxLevel) {
            return reached;
        } else {
            s.xp += amount;

            while (s.level < t.maxLevel) {
                long need = xpToNext(t, s.level);
                if (s.xp < need) {
                    break;
                }

                s.xp -= need;
                s.level++;
                payLevel(t, s, s.level);
                reached.add(s.level);
            }

            if (s.level >= t.maxLevel) {
                s.xp = 0L;
            }

            return reached;
        }
    }

    private static void payLevel(SkillTree t, SkillLogic.State s, int level) {
        for (SkillTree.LevelReward r : t.levelRewards) {
            if (level >= r.from && (level - r.from) % Math.max(1, r.every) == 0) {
                s.points.merge(r.currency, r.amount, Integer::sum);
            }
        }
    }

    public static int costOf(SkillTree.Node n, SkillTree.Cost c, int rank) {
        return c.amount + n.costStep * rank;
    }

    public static String refusal(SkillTree t, SkillLogic.State s, SkillTree.Node n) {
        int rank = s.rank(n.id);
        if (rank >= n.maxRank) {
            return "Изучено до конца";
        } else if (t.levels && s.level < n.level) {
            return "Нужен уровень " + n.level;
        } else if (!parentsMet(t, s, n)) {
            List<String> names = new ArrayList<>();

            for (String p : n.parents) {
                SkillTree.Node pn = t.node(p);
                if (pn != null && s.rank(p) == 0) {
                    names.add(pn.title);
                }
            }

            return (n.parentMode.equals("all") ? "Сначала: " : "Сначала что-то из: ") + String.join(", ", names);
        } else {
            if (!n.group.isEmpty()) {
                for (SkillTree.Node o : t.nodes) {
                    if (o != n && o.group.equals(n.group) && s.rank(o.id) > 0) {
                        return "Уже выбрано: " + o.title;
                    }
                }
            }

            for (SkillTree.Cost c : n.costs) {
                if (s.points(c.currency) < costOf(n, c, rank)) {
                    SkillTree.Currency cur = t.currency(c.currency);
                    return "Не хватает: " + (cur != null ? cur.name : c.currency);
                }
            }

            return null;
        }
    }

    public static boolean parentsMet(SkillTree t, SkillLogic.State s, SkillTree.Node n) {
        if (n.parents.isEmpty()) {
            return true;
        } else {
            boolean all = n.parentMode.equals("all");

            for (String p : n.parents) {
                boolean has = s.rank(p) > 0;
                if (all && !has) {
                    return false;
                }

                if (!all && has) {
                    return true;
                }
            }

            return all;
        }
    }

    public static boolean learn(SkillTree t, SkillLogic.State s, SkillTree.Node n) {
        if (refusal(t, s, n) != null) {
            return false;
        } else {
            int rank = s.rank(n.id);

            for (SkillTree.Cost c : n.costs) {
                s.points.merge(c.currency, -costOf(n, c, rank), Integer::sum);
            }

            s.ranks.put(n.id, rank + 1);
            return true;
        }
    }

    public static void grant(SkillTree.Node n, SkillLogic.State s) {
        s.ranks.put(n.id, Math.min(n.maxRank, s.rank(n.id) + 1));
    }

    public static void reset(SkillTree t, SkillLogic.State s) {
        for (SkillTree.Node n : t.nodes) {
            int rank = s.rank(n.id);

            for (int r = 0; r < rank; r++) {
                for (SkillTree.Cost c : n.costs) {
                    s.points.merge(c.currency, (int)Math.floor(costOf(n, c, r) * t.refund), Integer::sum);
                }
            }
        }

        s.ranks.clear();
    }

    public static boolean visible(SkillTree t, SkillLogic.State s, SkillTree.Node n) {
        return visible(t.view, s, n);
    }

    public static boolean visible(String view, SkillLogic.State s, SkillTree.Node n) {
        if (!view.equals("fog")) {
            return true;
        } else if (s.rank(n.id) <= 0 && !n.parents.isEmpty()) {
            for (String p : n.parents) {
                if (s.rank(p) > 0) {
                    return true;
                }
            }

            return false;
        } else {
            return true;
        }
    }

    public static Map<String, Float> stats(SkillTree t, SkillLogic.State s) {
        Map<String, Float> out = new HashMap<>();

        for (SkillTree.Node n : t.nodes) {
            int rank = s.rank(n.id);
            if (rank != 0) {
                for (SkillTree.Effect e : n.effects) {
                    if (e.kind.equals("stat")) {
                        out.merge(e.rule, e.amount * rank, Float::sum);
                    }
                }
            }
        }

        return out;
    }

    public static int[] freeSpot(SkillTree t) {
        int x = 0;
        int y = 0;

        for (SkillTree.Node n : t.nodes) {
            if (n.x > x || n.x == x && n.y > y) {
                x = n.x;
                y = n.y;
            }
        }

        return t.nodes.isEmpty() ? new int[]{0, 0} : new int[]{x + 90, y};
    }

    public static final class State {
        public int level = 1;
        public long xp;
        public Map<String, Integer> points = new HashMap<>();
        public Map<String, Integer> ranks = new HashMap<>();
        public boolean off;

        public int rank(String node) {
            return this.ranks.getOrDefault(node, 0);
        }

        public int points(String currency) {
            return this.points.getOrDefault(currency, 0);
        }
    }
}
