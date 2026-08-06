package me.tamkungz.nilkit.tooling;

import foo.zaaarf.geb.GEB;
import foo.zaaarf.geb.api.IEvent;
import foo.zaaarf.geb.api.IListener;

/**
 * Convenience bootstrap for the optional GEB event bus bundled in the
 * all-in-one JAR.
 *
 * <p>GEB listener dispatchers are generated at compile time. Mods using
 * {@code @Listen} must also put {@code foo.zaaarf.geb:processor} on their
 * annotationProcessor path; the processor is intentionally not shipped in the
 * runtime shadow JAR.</p>
 */
public final class GebHelper {

    private GebHelper() {
    }

    /** Creates a GEB bus and loads generated dispatchers from the context loader. */
    public static GEB createBus() {
        ClassLoader loader = Thread.currentThread().getContextClassLoader();
        if (loader == null) loader = GebHelper.class.getClassLoader();
        return createBus(loader);
    }

    /** Creates a GEB bus and loads generated dispatchers visible to the given loader. */
    public static GEB createBus(ClassLoader loader) {
        GEB bus = new GEB();
        bus.loadAndRegisterDispatchers(loader != null ? loader : GebHelper.class.getClassLoader());
        return bus;
    }

    public static void register(GEB bus, IListener listener) {
        if (bus == null) throw new IllegalArgumentException("bus must not be null");
        if (listener == null) throw new IllegalArgumentException("listener must not be null");
        bus.registerListener(listener);
    }

    public static void unregister(GEB bus, IListener listener) {
        if (bus == null) throw new IllegalArgumentException("bus must not be null");
        if (listener == null) throw new IllegalArgumentException("listener must not be null");
        bus.unregisterListener(listener);
    }

    /** Returns GEB's continuation/cancellation result for the event. */
    public static boolean post(GEB bus, IEvent event) {
        if (bus == null) throw new IllegalArgumentException("bus must not be null");
        if (event == null) throw new IllegalArgumentException("event must not be null");
        return bus.handleEvent(event);
    }
}
