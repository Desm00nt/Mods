package org.sprautestudio.spraute_engine.platform;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;

public final class Hook<E> {
    private final Map<Priority, List<Consumer<E>>> listeners = new EnumMap<>(Priority.class);
    private final boolean receivesCanceled;

    public Hook() {
        this(false);
    }

    public Hook(boolean receivesCanceled) {
        this.receivesCanceled = receivesCanceled;
    }

    public void add(Consumer<E> listener) {
        this.add(Priority.NORMAL, listener);
    }

    public void add(Priority priority, Consumer<E> listener) {
        this.listeners.computeIfAbsent(priority, p -> new ArrayList<>()).add(listener);
    }

    public Set<Priority> priorities() {
        return this.listeners.keySet();
    }

    public boolean receivesCanceled() {
        return this.receivesCanceled;
    }

    public void fire(Priority priority, E event) {
        List<Consumer<E>> list = this.listeners.get(priority);
        if (list != null) {
            for (Consumer<E> l : list) {
                if (!this.receivesCanceled && event instanceof Cancellable c && c.isCanceled()) {
                    return;
                }

                l.accept(event);
            }
        }
    }

    public void fire(E event) {
        for (Priority p : Priority.values()) {
            this.fire(p, event);
        }
    }
}
