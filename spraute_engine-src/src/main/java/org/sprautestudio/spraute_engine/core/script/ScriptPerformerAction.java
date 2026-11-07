package org.sprautestudio.spraute_engine.core.script;

public abstract class ScriptPerformerAction {
    private final ScriptPerformer performer;

    protected ScriptPerformerAction(ScriptPerformer performer) {
        this.performer = performer;
    }

    public ScriptPerformer getPerformer() {
        return this.performer;
    }

    public void start() {
    }

    public abstract boolean update();

    public boolean blocking() {
        return true;
    }

    public void stop() {
    }

    public String describe() {
        return null;
    }
}
