package me.tamkungz.nilloadersdk;

import me.tamkungz.nilloadersdk.build.ModBuildInfo;
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
}