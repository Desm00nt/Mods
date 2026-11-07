package org.sprautestudio.spraute_engine.core.anim;

import org.sprautestudio.spraute_engine.core.parser.SpAnimationParser;

public final class Animation {
    public static final Animation NULL = new Animation("", null);
    private final String id;
    private final SpAnimationParser.AnimationClip clip;

    public Animation(String id, SpAnimationParser.AnimationClip clip) {
        this.id = id;
        this.clip = clip;
    }

    public String id() {
        return this.id;
    }

    public SpAnimationParser.AnimationClip clip() {
        return this.clip;
    }

    public boolean isNull() {
        return this.clip == null;
    }

    public double lengthSeconds() {
        return this.clip != null ? this.clip.getLengthSec() : 0.0;
    }

    public boolean loops() {
        return this.clip != null && this.clip.isLoop();
    }

    public boolean holdsLast() {
        return this.clip != null && this.clip.holdsLast;
    }

    public ConfiguredAnimation configure() {
        return new ConfiguredAnimation(this);
    }
}
