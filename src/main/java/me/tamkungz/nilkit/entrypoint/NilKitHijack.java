package me.tamkungz.nilkit.entrypoint;

/**
 * Standard NilKit entrypoint for the hijack phase.
 *
 * This class is invoked by the loader during the hijack stage
 * and delegates execution to the EntrypointDispatcher.
 */
public final class NilKitHijack implements Runnable {

    @Override
    public void run() {
        EntrypointDispatcher.dispatch("hijack");
    }
}