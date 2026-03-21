package me.tamkungz.nilloadersdk.util;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * CooldownTracker — manages cooldowns for actions globally and per player.
 *
 * Helps prevent events from being triggered too frequently.
 */
public final class CooldownTracker {

    private volatile long nextGlobalMs = 0L;
    private final Map<String, Long> perPlayerMs = new ConcurrentHashMap<>();
    private final long cooldownMs;

    public CooldownTracker(long cooldownMs) {
        this.cooldownMs = cooldownMs;
    }

    // ─────────────────────────────────────────────
    // GLOBAL COOLDOWN
    // ─────────────────────────────────────────────

    /** Returns true if the global cooldown has expired. */
    public boolean isGlobalReady() {
        return System.currentTimeMillis() >= nextGlobalMs;
    }

    /** Sets global cooldown to now + cooldownMs. */
    public void markGlobalUsed() {
        nextGlobalMs = System.currentTimeMillis() + cooldownMs;
    }

    /** Checks and applies global cooldown in one step. */
    public boolean tryUseGlobal() {
        if (!isGlobalReady()) return false;
        markGlobalUsed();
        return true;
    }

    // ─────────────────────────────────────────────
    // PER-PLAYER COOLDOWN
    // ─────────────────────────────────────────────

    /** Returns true if the player's cooldown has expired. */
    public boolean isPlayerReady(String playerName) {
        Long next = perPlayerMs.get(playerName);
        return next == null || System.currentTimeMillis() >= next;
    }

    /** Sets cooldown for a specific player. */
    public void markPlayerUsed(String playerName) {
        perPlayerMs.put(playerName, System.currentTimeMillis() + cooldownMs);
    }

    /** Checks and applies cooldown for a player in one step. */
    public boolean tryUsePlayer(String playerName) {
        if (!isPlayerReady(playerName)) return false;
        markPlayerUsed(playerName);
        return true;
    }

    /** Removes cooldown tracking for a player. */
    public void removePlayer(String playerName) {
        perPlayerMs.remove(playerName);
    }

    /** Clears all cooldowns (global and per-player). */
    public void clearAll() {
        perPlayerMs.clear();
        nextGlobalMs = 0L;
    }

    public long getCooldownMs() { return cooldownMs; }
}