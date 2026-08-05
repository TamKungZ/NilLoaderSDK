package me.tamkungz.nilloadersdk.entrypoint;

/**
 * Standard NilLoaderSDK entrypoint for the premain phase.
 *
 * This class is invoked by the loader during the premain stage
 * and delegates execution to the EntrypointDispatcher.
 */
public final class NilLoaderSDKPremain implements Runnable {

    @Override
    public void run() {
        EntrypointDispatcher.dispatch("premain");
    }
}