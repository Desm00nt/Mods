package org.sprautestudio.spraute_engine.core.anim;

public final class ConfiguredAnimation {
    private final Animation animation;
    private float transition = 0.25F;
    private float speed = 1.0F;
    private LoopMode loop;
    private float startTime;

    ConfiguredAnimation(Animation animation) {
        this.animation = animation;
        this.loop = animation.loops() ? LoopMode.LOOP : (animation.holdsLast() ? LoopMode.HOLD_ON_LAST_FRAME : LoopMode.ONCE);
    }

    public Animation animation() {
        return this.animation;
    }

    public ConfiguredAnimation transitionTime(float seconds) {
        this.transition = Math.max(0.0F, seconds);
        return this;
    }

    public float transitionTime() {
        return this.transition;
    }

    public ConfiguredAnimation startTime(float seconds) {
        this.startTime = Math.max(0.0F, seconds);
        return this;
    }

    public float startTime() {
        return this.startTime;
    }

    public ConfiguredAnimation speed(float s) {
        this.speed = s > 0.0F ? s : 1.0F;
        return this;
    }

    public float speed() {
        return this.speed;
    }

    public ConfiguredAnimation loopMode(LoopMode mode) {
        this.loop = mode;
        return this;
    }

    public LoopMode loopMode() {
        return this.loop;
    }
}
