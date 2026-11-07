package net.minecraftforge.eventbus.api;
public class Event {
    public Event() { }
    public boolean isCancelable() { throw new RuntimeException(); }
    public boolean isCanceled() { throw new RuntimeException(); }
    public void setCanceled(boolean cancel) { throw new RuntimeException(); }
    public boolean hasResult() { throw new RuntimeException(); }
    public Result getResult() { throw new RuntimeException(); }
    public void setResult(Result value) { throw new RuntimeException(); }
    public EventPriority getPhase() { throw new RuntimeException(); }
    public enum Result { DENY, DEFAULT, ALLOW }
}
