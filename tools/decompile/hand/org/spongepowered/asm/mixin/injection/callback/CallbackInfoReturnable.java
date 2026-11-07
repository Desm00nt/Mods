package org.spongepowered.asm.mixin.injection.callback;
public class CallbackInfoReturnable<R> extends CallbackInfo {
    public CallbackInfoReturnable(String name, boolean cancellable) { super(name, cancellable); }
    public void setReturnValue(R returnValue) { throw new RuntimeException(); }
    public R getReturnValue() { throw new RuntimeException(); }
    public byte getReturnValueB() { throw new RuntimeException(); }
    public char getReturnValueC() { throw new RuntimeException(); }
    public double getReturnValueD() { throw new RuntimeException(); }
    public float getReturnValueF() { throw new RuntimeException(); }
    public int getReturnValueI() { throw new RuntimeException(); }
    public long getReturnValueJ() { throw new RuntimeException(); }
    public short getReturnValueS() { throw new RuntimeException(); }
    public boolean getReturnValueZ() { throw new RuntimeException(); }
}
