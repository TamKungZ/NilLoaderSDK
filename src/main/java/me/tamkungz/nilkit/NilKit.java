package me.tamkungz.nilkit;

import me.tamkungz.nilkit.event.Event;
import me.tamkungz.nilkit.event.EventBus;
import me.tamkungz.nilkit.event.EventPriority;

/**
 * Global SDK access point for high-level systems.
 */
public final class NilKit {

    private static final EventBus EVENT_BUS = new EventBus();

    private NilKit() {
    }

    public static EventBus eventBus() {
        return EVENT_BUS;
    }

    public static void registerEvents(Object listener) {
        EVENT_BUS.register(listener);
    }

    public static void unregisterEvents(Object listener) {
        EVENT_BUS.unregister(listener);
    }

    public static <T extends Event> void listen(Class<T> eventType, EventBus.EventListener<T> listener) {
        EVENT_BUS.register(eventType, listener);
    }

    public static <T extends Event> void listen(Class<T> eventType, EventBus.EventListener<T> listener, EventPriority priority, boolean receiveCancelled) {
        EVENT_BUS.register(eventType, listener, priority, receiveCancelled);
    }

    public static <T extends Event> void unlisten(Class<T> eventType, EventBus.EventListener<T> listener) {
        EVENT_BUS.unregister(eventType, listener);
    }

    public static boolean post(Event event) {
        return EVENT_BUS.post(event);
    }
}

