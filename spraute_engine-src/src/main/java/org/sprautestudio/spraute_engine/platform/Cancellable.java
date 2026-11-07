package org.sprautestudio.spraute_engine.platform;

public abstract class Cancellable {
    private boolean canceled;

    public boolean isCanceled() {
        return this.canceled;
    }

    public void setCanceled(boolean canceled) {
        this.canceled = canceled;
    }
}
