package me.tamkungz.nilloadersdk.event;

import me.tamkungz.nilloadersdk.log.Loggers;
import nilloader.api.NilLogger;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Simple Forge/Fabric-like event bus for NilLoaderSDK.
 */
public final class EventBus {

    private static final NilLogger LOG = Loggers.forClass(EventBus.class);

    private final Object lock = new Object();
    private final Map<Class<?>, List<Handler>> handlers = new HashMap<Class<?>, List<Handler>>();

    public void register(Object listener) {
        if (listener == null) return;

        Class<?> type = listener.getClass();
        Method[] methods = type.getDeclaredMethods();
        for (Method m : methods) {
            SubscribeEvent anno = m.getAnnotation(SubscribeEvent.class);
            if (anno == null) continue;

            if (Modifier.isStatic(m.getModifiers())) {
                LOG.warn("Skipping @SubscribeEvent static method: " + type.getName() + "#" + m.getName());
                continue;
            }

            Class<?>[] params = m.getParameterTypes();
            if (params.length != 1 || !Event.class.isAssignableFrom(params[0])) {
                LOG.warn("Skipping invalid @SubscribeEvent signature (must be one Event parameter): " + type.getName() + "#" + m.getName());
                continue;
            }

            if (m.getReturnType() != Void.TYPE) {
                LOG.warn("Skipping @SubscribeEvent non-void method: " + type.getName() + "#" + m.getName());
                continue;
            }

            if (!m.isAccessible()) {
                m.setAccessible(true);
            }

            registerHandler(params[0], new Handler(listener, m, anno.priority(), anno.receiveCancelled()));
        }
    }

    public <T extends Event> void register(Class<T> eventType, EventListener<T> listener) {
        register(eventType, listener, EventPriority.NORMAL, false);
    }

    public <T extends Event> void register(Class<T> eventType, EventListener<T> listener, EventPriority priority, boolean receiveCancelled) {
        if (eventType == null || listener == null) return;
        EventPriority p = priority == null ? EventPriority.NORMAL : priority;
        registerHandler(eventType, new Handler(null, eventType, listener, p, receiveCancelled));
    }

    public void unregister(Object listener) {
        if (listener == null) return;
        synchronized (lock) {
            for (Map.Entry<Class<?>, List<Handler>> en : handlers.entrySet()) {
                List<Handler> src = en.getValue();
                List<Handler> dst = new ArrayList<Handler>();
                for (Handler h : src) {
                    if (!h.isOwner(listener)) dst.add(h);
                }
                en.setValue(Collections.unmodifiableList(dst));
            }
        }
    }

    public boolean post(Event event) {
        if (event == null) return false;

        List<Handler> invoke = collectHandlers(event.getClass());
        for (Handler h : invoke) {
            if (event instanceof CancellableEvent) {
                CancellableEvent ce = (CancellableEvent) event;
                if (ce.isCancelled() && !h.receiveCancelled) {
                    continue;
                }
            }

            try {
                h.invoke(event);
            } catch (Throwable t) {
                LOG.error("Event handler failed for " + event.getClass().getName(), t);
            }
        }

        return !(event instanceof CancellableEvent) || !((CancellableEvent) event).isCancelled();
    }

    private void registerHandler(Class<?> eventType, Handler handler) {
        synchronized (lock) {
            List<Handler> old = handlers.get(eventType);
            List<Handler> next = old == null ? new ArrayList<Handler>() : new ArrayList<Handler>(old);
            next.add(handler);
            sortHandlers(next);
            handlers.put(eventType, Collections.unmodifiableList(next));
        }
    }

    private List<Handler> collectHandlers(Class<?> eventType) {
        synchronized (lock) {
            List<Handler> out = new ArrayList<Handler>();
            for (Map.Entry<Class<?>, List<Handler>> en : handlers.entrySet()) {
                if (en.getKey().isAssignableFrom(eventType)) {
                    out.addAll(en.getValue());
                }
            }
            sortHandlers(out);
            return out;
        }
    }

    private static void sortHandlers(List<Handler> list) {
        Collections.sort(list, (a, b) -> Integer.compare(a.priority.ordinal(), b.priority.ordinal()));
    }

    public interface EventListener<T extends Event> {
        void handle(T event) throws Exception;
    }

    private static final class Handler {
        private final Object owner;
        private final Method method;
        private final Class<?> lambdaEventType;
        private final EventListener listener;
        private final EventPriority priority;
        private final boolean receiveCancelled;

        Handler(Object owner, Method method, EventPriority priority, boolean receiveCancelled) {
            this.owner = owner;
            this.method = method;
            this.lambdaEventType = null;
            this.listener = null;
            this.priority = priority;
            this.receiveCancelled = receiveCancelled;
        }

        Handler(Object owner, Class<?> lambdaEventType, EventListener listener, EventPriority priority, boolean receiveCancelled) {
            this.owner = owner;
            this.method = null;
            this.lambdaEventType = lambdaEventType;
            this.listener = listener;
            this.priority = priority;
            this.receiveCancelled = receiveCancelled;
        }

        boolean isOwner(Object test) {
            return owner != null && owner == test;
        }

        @SuppressWarnings("unchecked")
        void invoke(Event event) throws Exception {
            if (method != null) {
                method.invoke(owner, event);
                return;
            }

            if (listener != null && lambdaEventType != null && lambdaEventType.isAssignableFrom(event.getClass())) {
                listener.handle(event);
            }
        }
    }
}

