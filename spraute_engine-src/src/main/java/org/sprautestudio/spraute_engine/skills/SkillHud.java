package org.sprautestudio.spraute_engine.skills;

import org.sprautestudio.spraute_engine.engine.EngineConfig;

public final class SkillHud {
    public SkillHud.Abilities abilities = new SkillHud.Abilities();
    public SkillHud.Level level = new SkillHud.Level();

    public void fix() {
        if (this.abilities == null) {
            this.abilities = new SkillHud.Abilities();
        }

        if (this.level == null) {
            this.level = new SkillHud.Level();
        }

        if (this.abilities.plateImage == null) {
            this.abilities.plateImage = new EngineConfig.Skin();
        }

        if (this.abilities.frameImage == null) {
            this.abilities.frameImage = new EngineConfig.Skin();
        }

        if (this.abilities.cooldownImage == null) {
            this.abilities.cooldownImage = new EngineConfig.Skin();
        }

        if (this.abilities.anchor == null) {
            this.abilities.anchor = "B";
        }

        if (this.level.anchor == null) {
            this.level.anchor = "TL";
        }

        if (this.level.direction == null) {
            this.level.direction = "right";
        }

        this.abilities.size = Math.max(4, this.abilities.size);
    }

    public static int[] place(String anchor, int x, int y, int w, int h, int screenW, int screenH) {
        float fx = anchor.endsWith("L") ? 0.0F : (anchor.endsWith("R") ? 1.0F : 0.5F);
        float fy = anchor.startsWith("T") ? 0.0F : (anchor.startsWith("B") ? 1.0F : 0.5F);
        return new int[]{Math.round((screenW - w) * fx) + x, Math.round((screenH - h) * fy) + y, w, h};
    }

    public static String elementAnchor(String anchor) {
        return switch (anchor) {
            case "TL" -> "top_left";
            case "T" -> "top";
            case "TR" -> "top_right";
            case "L" -> "left";
            case "R" -> "right";
            case "BL" -> "bottom_left";
            case "B" -> "bottom";
            case "BR" -> "bottom_right";
            default -> "center";
        };
    }

    public static final class Abilities {
        public boolean visible = true;
        public String anchor = "B";
        public int x = 0;
        public int y = -62;
        public int size = 20;
        public int gap = 2;
        public int pad = 2;
        public boolean vertical;
        public boolean keys = true;
        public boolean seconds = true;
        public int plate = -1609559528;
        public int keyColor = -11686;
        public int cooldown = -1342177280;
        public int textColor = -1;
        public EngineConfig.Skin plateImage = new EngineConfig.Skin();
        public EngineConfig.Skin frameImage = new EngineConfig.Skin();
        public EngineConfig.Skin cooldownImage = new EngineConfig.Skin();
    }

    public static final class Level {
        public boolean visible = true;
        public String anchor = "TL";
        public int x = 4;
        public int y = 4;
        public int width = 80;
        public int height = 2;
        public int gap = 5;
        public boolean text = true;
        public String direction = "right";
        public int back = Integer.MIN_VALUE;
        public int textColor = -1;
    }
}
