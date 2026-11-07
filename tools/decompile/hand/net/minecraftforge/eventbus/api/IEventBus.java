package net.minecraftforge.eventbus.api;
import java.util.function.Consumer;
public interface IEventBus {
    void register(Object target);
    <T extends Event> void addListener(Consumer<T> consumer);
    <T extends Event> void addListener(EventPriority priority, Consumer<T> consumer);
    <T extends Event> void addListener(EventPriority priority, boolean receiveCancelled, Consumer<T> consumer);
    <T extends Event> void addListener(EventPriority priority, boolean receiveCancelled, Class<T> eventType, Consumer<T> consumer);
    <T extends GenericEvent<? extends F>, F> void addGenericListener(Class<F> genericClassFilter, Consumer<T> consumer);
    void unregister(Object object);
    boolean post(Event event);
}
