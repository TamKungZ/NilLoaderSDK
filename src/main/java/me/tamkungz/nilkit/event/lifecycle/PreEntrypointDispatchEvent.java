package me.tamkungz.nilkit.event.lifecycle;

import me.tamkungz.nilkit.event.CancellableEvent;

/**
 * Fired before SDK entrypoint targets/modules are dispatched for a phase.
 * Can be cancelled to skip dispatch body.
 */
public final class PreEntrypointDispatchEvent extends CancellableEvent {

    private final String phase;

    public PreEntrypointDispatchEvent(String phase) {
        this.phase = phase;
    }

    public String getPhase() {
        return phase;
    }
}

