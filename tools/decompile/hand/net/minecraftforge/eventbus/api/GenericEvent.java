package net.minecraftforge.eventbus.api;
import java.lang.reflect.Type;
public class GenericEvent<T> extends Event {
    public GenericEvent() { }
    public GenericEvent(Class<T> type) { }
    public Type getGenericType() { throw new RuntimeException(); }
}
