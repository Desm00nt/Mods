package org.spongepowered.asm.mixin.injection.callback;
public class CallbackInfo {
    public CallbackInfo(String name, boolean cancellable) { }
    public String getId() { throw new RuntimeException(); }
    public boolean isCancellable() { throw new RuntimeException(); }
    public boolean isCancelled() { throw new RuntimeException(); }
    public void cancel() { throw new RuntimeException(); }
}
