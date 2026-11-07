package org.openjdk.nashorn.api.scripting;
public abstract class ScriptObjectMirror implements javax.script.Bindings {
    public boolean isArray() { throw new RuntimeException(); }
    public boolean isFunction() { throw new RuntimeException(); }
    public Object call(Object thiz, Object... args) { throw new RuntimeException(); }
    public Object getSlot(int index) { throw new RuntimeException(); }
    public <T> T to(Class<T> type) { throw new RuntimeException(); }
}
