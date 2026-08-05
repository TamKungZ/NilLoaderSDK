package me.tamkungz.nilloadersdk;

import me.tamkungz.nilloadersdk.build.ModBuildInfo;
import me.tamkungz.nilloadersdk.event.Event;
import me.tamkungz.nilloadersdk.event.EventBus;
import me.tamkungz.nilloadersdk.event.EventPriority;
import me.tamkungz.nilloadersdk.log.Loggers;
import me.tamkungz.remapping.SimpleRemap;
import nilloader.api.NilLogger;

/**
 * Base class for NilLoader mods.
 *
 * Provides remap, logger, and buildInfo ready to use.
 * Override onLoad() instead of run() for mod initialization logic.
 */
public abstract class NilModBase implements Runnable {

    protected final NilLogger log;
    protected final SimpleRemap remap;
    protected final ModBuildInfo buildInfo;

    protected NilModBase(ModBuildInfo buildInfo, SimpleRemap remap) {
        this.buildInfo = buildInfo;
        this.remap     = remap;
        this.log       = Loggers.sdk();
    }

    @Override
    public final void run() {
        log.info("Entrypoint loaded. " + buildInfo.getBuildTag());
        try {
            onLoad();
        } catch (Throwable t) {
            log.error("Fatal error during onLoad", t);
        }
    }

    /**
     * Called automatically from run().
     * Override this to implement mod initialization logic.
     */
    protected abstract void onLoad() throws Throwable;

    /**
     * Registers annotated listener methods in this mod instance to the global SDK EventBus.
     */
    protected final void registerEvents() {
        NilLoaderSDK.registerEvents(this);
    }

    /**
     * Registers annotated listener methods from the provided instance to the global SDK EventBus.
     */
    protected final void registerEvents(Object listener) {
        NilLoaderSDK.registerEvents(listener);
    }

    /**
     * Registers a typed listener callback using NORMAL priority.
     */
    protected final <T extends Event> void listen(Class<T> eventType, EventBus.EventListener<T> listener) {
        NilLoaderSDK.listen(eventType, listener);
    }

    /**
     * Registers a typed listener callback with explicit delivery options.
     */
    protected final <T extends Event> void listen(Class<T> eventType, EventBus.EventListener<T> listener, EventPriority priority, boolean receiveCancelled) {
        NilLoaderSDK.listen(eventType, listener, priority, receiveCancelled);
    }

    /**
     * Removes a typed listener callback previously registered with listen().
     */
    protected final <T extends Event> void unlisten(Class<T> eventType, EventBus.EventListener<T> listener) {
        NilLoaderSDK.unlisten(eventType, listener);
    }

    /**
     * Posts an event to the global SDK EventBus.
     */
    protected final boolean post(Event event) {
        return NilLoaderSDK.post(event);
    }
}
