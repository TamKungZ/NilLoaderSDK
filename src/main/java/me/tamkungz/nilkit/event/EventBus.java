package me.tamkungz.nilkit.event;

import me.tamkungz.nilkit.log.Loggers;
import nilloader.api.NilLogger;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Simple Forge/Fabric-like event bus for NilKit.
 */
public final class EventBus {

    private static final NilLogger LOG = Loggers.forClass(EventBus.class);

    private final Object lock = new Object();
    private final Map<Class<?>, List<Handler>> handlers = new HashMap<Class<?>, List<Handler>>();
    private long nextSequence;

    /**
     * Registers all valid {@link SubscribeEvent} methods on a listener instance.
     * Inherited listener methods are supported. Registering the same listener twice is idempotent.
     */
    public void register(Object listener) {
        if (listener == null) return;

        Set<String> seenSignatures = new HashSet<String>();
        for (Class<?> type = listener.getClass(); type != null && type != Object.class; type = type.getSuperclass()) {
            Method[] methods = type.getDeclaredMethods();
            for (Method m : methods) {
                int modifiers = m.getModifiers();
                String signature = signatureOf(m);
                // A non-private subclass method overrides/hides the inherited signature even
                // when the subclass deliberately omits @SubscribeEvent. Private methods do not
                // override, so annotated private handlers in a superclass remain independent.
                if (!Modifier.isPrivate(modifiers) && !seenSignatures.add(signature)) {
                    continue;
                }

                SubscribeEvent anno = m.getAnnotation(SubscribeEvent.class);
                if (anno == null) continue;

                if (Modifier.isStatic(modifiers)) {
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

                try {
                    if (!m.isAccessible()) {
                        m.setAccessible(true);
                    }
                } catch (Throwable t) {
                    LOG.warn("Skipping inaccessible @SubscribeEvent method: " + type.getName() + "#" + m.getName(), t);
                    continue;
                }

                registerHandler(params[0], new Handler(listener, m, anno.priority(), anno.receiveCancelled()));
            }
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

    /** Removes all annotated handlers owned by the listener instance. */
    public void unregister(Object listener) {
        if (listener == null) return;
        synchronized (lock) {
            removeMatchingHandlers(new HandlerMatcher() {
                @Override
                public boolean matches(Handler handler) {
                    return handler.isOwner(listener);
                }
            });
        }
    }

    /** Removes a typed callback previously registered with {@link #register(Class, EventListener)}. */
    public <T extends Event> void unregister(final Class<T> eventType, final EventListener<T> listener) {
        if (eventType == null || listener == null) return;
        synchronized (lock) {
            removeMatchingHandlers(new HandlerMatcher() {
                @Override
                public boolean matches(Handler handler) {
                    return handler.isTypedListener(eventType, listener);
                }
            });
        }
    }

    /** Removes all listeners from this bus. */
    public void clear() {
        synchronized (lock) {
            handlers.clear();
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
            if (old != null) {
                for (Handler existing : old) {
                    if (existing.sameRegistration(handler)) {
                        return;
                    }
                }
            }

            List<Handler> next = old == null ? new ArrayList<Handler>() : new ArrayList<Handler>(old);
            handler.sequence = nextSequence++;
            next.add(handler);
            sortHandlers(next);
            handlers.put(eventType, Collections.unmodifiableList(next));
        }
    }

    private void removeMatchingHandlers(HandlerMatcher matcher) {
        List<Class<?>> emptyKeys = new ArrayList<Class<?>>();
        for (Map.Entry<Class<?>, List<Handler>> en : handlers.entrySet()) {
            List<Handler> src = en.getValue();
            List<Handler> dst = new ArrayList<Handler>();
            for (Handler h : src) {
                if (!matcher.matches(h)) dst.add(h);
            }
            if (dst.isEmpty()) {
                emptyKeys.add(en.getKey());
            } else if (dst.size() != src.size()) {
                en.setValue(Collections.unmodifiableList(dst));
            }
        }
        for (Class<?> key : emptyKeys) {
            handlers.remove(key);
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
        Collections.sort(list, (a, b) -> {
            int byPriority = Integer.compare(a.priority.ordinal(), b.priority.ordinal());
            return byPriority != 0 ? byPriority : Long.compare(a.sequence, b.sequence);
        });
    }

    private static String signatureOf(Method method) {
        StringBuilder sb = new StringBuilder(method.getName()).append('(');
        Class<?>[] params = method.getParameterTypes();
        for (Class<?> param : params) {
            sb.append(param.getName()).append(';');
        }
        return sb.append(')').toString();
    }

    public interface EventListener<T extends Event> {
        void handle(T event) throws Exception;
    }

    private interface HandlerMatcher {
        boolean matches(Handler handler);
    }

    private static final class Handler {
        private final Object owner;
        private final Method method;
        private final Class<?> lambdaEventType;
        private final EventListener listener;
        private final EventPriority priority;
        private final boolean receiveCancelled;
        private long sequence;

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

        boolean isTypedListener(Class<?> eventType, EventListener test) {
            return method == null && lambdaEventType == eventType && listener == test;
        }

        boolean sameRegistration(Handler other) {
            if (method != null || other.method != null) {
                return owner == other.owner && method != null && method.equals(other.method);
            }
            return lambdaEventType == other.lambdaEventType && listener == other.listener;
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
