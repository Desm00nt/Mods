package org.sprautestudio.spraute_engine.custom;

import java.util.List;

public final class ModelLook {
    public static final List<String> HOLDS = List.of("auto", "block", "handheld");
    public static final List<String> RENDERS = List.of("game", "engine");
    public String name = "";
    public float scale = 1.0F;
    public int turn;
    public float x;
    public float y;
    public float z;
    public boolean fit = true;
    public String hold = "auto";
    public String render = "game";
    public String anim = "";

    public boolean active() {
        return this.name != null && !this.name.isBlank();
    }

    public boolean engine() {
        return this.active() && "engine".equals(this.render);
    }

    public float[] offset() {
        return new float[]{this.x, this.y, this.z};
    }

    public void fix() {
        if (this.name == null) {
            this.name = "";
        }

        if (!(this.scale > 0.0F)) {
            this.scale = 1.0F;
        }

        this.scale = Math.min(this.scale, 16.0F);
        this.turn = Math.floorMod(this.turn, 4);
        this.x = Math.max(-32.0F, Math.min(32.0F, this.x));
        this.y = Math.max(-32.0F, Math.min(32.0F, this.y));
        this.z = Math.max(-32.0F, Math.min(32.0F, this.z));
        if (!HOLDS.contains(this.hold)) {
            this.hold = "auto";
        }

        if (!RENDERS.contains(this.render)) {
            this.render = "game";
        }

        if (this.anim == null) {
            this.anim = "";
        }
    }
}
