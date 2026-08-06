package me.tamkungz.nilkit.event.lifecycle;

/**
 * Fired after SDK entrypoint dispatch completes for a phase.
 */
public final class PostEntrypointDispatchEvent extends PhaseEvent {

    private final int executedCount;

    public PostEntrypointDispatchEvent(String phase, int executedCount) {
        super(phase);
        this.executedCount = executedCount;
    }

    public int getExecutedCount() {
        return executedCount;
    }
}

