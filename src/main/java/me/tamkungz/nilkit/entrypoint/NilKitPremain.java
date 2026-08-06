package me.tamkungz.nilkit.entrypoint;

/**
 * Standard NilKit entrypoint for the premain phase.
 *
 * This class is invoked by the loader during the premain stage
 * and delegates execution to the EntrypointDispatcher.
 */
public final class NilKitPremain implements Runnable {

    @Override
    public void run() {
        EntrypointDispatcher.dispatch("premain");
    }
}