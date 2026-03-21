package me.tamkungz.nilloadersdk.entrypoint;

/**
 * Standard NilLoaderSDK entrypoint for the hijack phase.
 *
 * This class is invoked by the loader during the hijack stage
 * and delegates execution to the EntrypointDispatcher.
 */
public final class NilLoaderSDKHijack implements Runnable {

    @Override
    public void run() {
        EntrypointDispatcher.dispatch("hijack");
    }
}