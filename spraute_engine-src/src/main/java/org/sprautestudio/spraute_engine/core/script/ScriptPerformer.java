package org.sprautestudio.spraute_engine.core.script;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.sprautestudio.spraute_engine.runtime.SprauteRuntime;

public final class ScriptPerformer {
    private static final int STEPS_PER_TICK = 4096;
    public static final String LABEL_START = "начало";
    public static final String LABEL_END = "конец";
    private final ScriptInstance script;
    final List<ScriptPerformerAction> actions = new ArrayList<>();
    final Map<Integer, String> words = new HashMap<>();
    final Map<String, Integer> labels = new LinkedHashMap<>();
    public final List<String> labelPointsNames = new ArrayList<>();
    private final SpeechState speech = new SpeechState();
    private final List<ScriptPerformerAction> passing = new ArrayList<>();
    private int index;
    private boolean entered;
    private int jump = -1;
    private String stage = "";
    public UUID asPlayer;
    private String asEnd;
    private final ArrayDeque<ScriptPerformer.AsFrame> asOuter = new ArrayDeque<>();
    private UUID baseAs;
    String goneLabel;

    ScriptPerformer(ScriptInstance script) {
        this.script = script;
    }

    public ScriptInstance getScript() {
        return this.script;
    }

    public SpeechState speech() {
        return this.speech;
    }

    private void stopPassing() {
        for (ScriptPerformerAction a : this.passing) {
            a.stop();
        }

        this.passing.clear();
    }

    void enterAs(UUID player, String end) {
        this.asOuter.push(new ScriptPerformer.AsFrame(this.asPlayer, this.asEnd));
        this.asPlayer = player;
        this.asEnd = end;
        SprauteRuntime.setAs(this.asPlayer);
    }

    void leaveAs(int blocks) {
        for (int i = 0; i < blocks; i++) {
            ScriptPerformer.AsFrame outer = this.asOuter.poll();
            if (outer == null) {
                this.resetAs();
                return;
            }

            this.asPlayer = outer.player();
            this.asEnd = outer.end();
        }

        SprauteRuntime.setAs(this.asPlayer);
    }

    void resetAs() {
        this.asOuter.clear();
        this.asPlayer = this.baseAs;
        this.asEnd = null;
        SprauteRuntime.setAs(this.asPlayer);
    }

    void inheritAs(UUID player) {
        this.baseAs = player;
        this.resetAs();
    }

    private boolean playerGone() {
        if (this.asPlayer != null && !SprauteRuntime.isOnline(this.asPlayer)) {
            String to = this.asEnd != null ? this.asEnd : this.goneLabel;
            if (this.asEnd != null) {
                this.leaveAs(1);
            } else {
                this.baseAs = null;
                this.resetAs();
            }

            Integer at = to != null ? this.labelAt(to) : null;
            if (at != null) {
                this.stopCurrent();
                this.index = at;
                this.jump = -1;
                this.entered = false;
            }

            return true;
        } else {
            return false;
        }
    }

    public void tick() {
        SprauteRuntime.setAs(this.asPlayer);

        while (this.playerGone()) {
        }

        this.passing.removeIf(ScriptPerformerAction::update);

        for (int i = 0; i < 4096 && this.index < this.actions.size(); i++) {
            while (this.playerGone()) {
            }

            if (this.index >= this.actions.size()) {
                return;
            }

            ScriptPerformerAction a = this.actions.get(this.index);
            if (!this.entered) {
                this.entered = true;
                a.start();
            }

            if (a.update()) {
                this.stopPassing();
            } else {
                if (a.blocking()) {
                    return;
                }

                this.passing.add(a);
            }

            this.entered = false;
            this.index = this.jump >= 0 ? this.jump : this.index + 1;
            this.jump = -1;
        }
    }

    public boolean isFinished() {
        return this.index >= this.actions.size();
    }

    private Integer labelAt(String label) {
        if ("начало".equals(label)) {
            return 0;
        } else {
            return "конец".equals(label) ? this.actions.size() : this.labels.get(label);
        }
    }

    public boolean goTo(String label) {
        Integer at = this.labelAt(label);
        if (at == null) {
            return false;
        } else {
            this.stage = label;
            this.stopCurrent();
            this.jump = -1;
            this.index = at;
            this.entered = false;
            this.resetAs();
            return true;
        }
    }

    void jumpTo(String label) {
        Integer at = this.labelAt(label);
        if (at != null) {
            this.stage = label;
            this.jump = at;
        }
    }

    private void stopCurrent() {
        this.stopPassing();
        if (this.entered && this.index < this.actions.size()) {
            this.actions.get(this.index).stop();
        }
    }

    public void jumpToIndex(int i) {
        this.stopCurrent();
        this.index = Math.max(0, Math.min(this.actions.size(), i));
        this.jump = -1;
        this.entered = false;
    }

    public void restart() {
        this.jumpToIndex(0);
        this.resetAs();
    }

    void stop() {
        this.stopCurrent();
        this.entered = false;
    }

    public String getStage() {
        return this.stage;
    }

    void setStage(String s) {
        this.stage = s;
    }

    public int getActionIndex() {
        return this.index;
    }

    public int getActionsAmount() {
        return this.actions.size();
    }

    public String describe() {
        if (this.index >= this.actions.size()) {
            return "Завершён";
        } else {
            String d = this.actions.get(this.index).describe();
            String w = this.words.get(this.index);
            if (d != null && (d.equals("Ждёт условие") || d.equals("Идёт параллельно"))) {
                d = null;
            }

            if (w != null) {
                return d != null ? w + " · " + d : w;
            } else {
                return d != null ? d : "Выполняет шаг " + (this.index + 1);
            }
        }
    }

    private record AsFrame(UUID player, String end) {
    }
}
