package me.tamkungz.nilloadersdk.event.lifecycle;

import me.tamkungz.nilloadersdk.event.Event;

/**
 * Base lifecycle event with current phase value.
 */
public class PhaseEvent extends Event {

    private final String phase;

    public PhaseEvent(String phase) {
        this.phase = phase;
    }

    public String getPhase() {
        return phase;
    }
}

