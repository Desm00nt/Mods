package org.sprautestudio.spraute_engine.skills;

import org.sprautestudio.spraute_engine.engine.EngineConfig;

public final class SkillLook {
    public int accent = 9137407;
    public int panel = -266987487;
    public int header = -14934229;
    public int tile = -14473675;
    public int border = -12959915;
    public int text = -1512718;
    public int muted = -6643531;
    public int good = -8658806;
    public int bad = -36752;
    public EngineConfig.Skin panelImage = new EngineConfig.Skin();
    public EngineConfig.Skin tileImage = new EngineConfig.Skin();
    public EngineConfig.Skin tileLearned = new EngineConfig.Skin();
    public EngineConfig.Skin tileLocked = new EngineConfig.Skin();
    public EngineConfig.Skin buttonImage = new EngineConfig.Skin();
    public EngineConfig.Skin tooltipImage = new EngineConfig.Skin();
    public EngineConfig.Skin linkImage = new EngineConfig.Skin();
    public EngineConfig.Skin linkLearned = new EngineConfig.Skin();
    public EngineConfig.Skin arrowImage = new EngineConfig.Skin();
    public EngineConfig.Skin arrowLearned = new EngineConfig.Skin();
    public int linkWidth = 2;

    public void fix() {
        if (this.panelImage == null) {
            this.panelImage = new EngineConfig.Skin();
        }

        if (this.tileImage == null) {
            this.tileImage = new EngineConfig.Skin();
        }

        if (this.tileLearned == null) {
            this.tileLearned = new EngineConfig.Skin();
        }

        if (this.tileLocked == null) {
            this.tileLocked = new EngineConfig.Skin();
        }

        if (this.buttonImage == null) {
            this.buttonImage = new EngineConfig.Skin();
        }

        if (this.tooltipImage == null) {
            this.tooltipImage = new EngineConfig.Skin();
        }

        if (this.linkImage == null) {
            this.linkImage = new EngineConfig.Skin();
        }

        if (this.linkLearned == null) {
            this.linkLearned = new EngineConfig.Skin();
        }

        if (this.arrowImage == null) {
            this.arrowImage = new EngineConfig.Skin();
        }

        if (this.arrowLearned == null) {
            this.arrowLearned = new EngineConfig.Skin();
        }

        this.linkWidth = Math.max(1, Math.min(32, this.linkWidth));
    }
}
