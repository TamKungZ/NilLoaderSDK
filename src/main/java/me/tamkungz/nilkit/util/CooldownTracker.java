package me.tamkungz.nilkit.util;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * CooldownTracker — manages cooldowns for actions globally and per player.
 *
 * <p>The compound try-use operations are atomic, so multiple threads cannot
 * consume the same ready cooldown window at the same time.</p>
 */
public final class CooldownTracker {

    private volatile long nextGlobalMs = 0L;
    private final Map<String, Long> perPlayerMs = new ConcurrentHashMap<String, Long>();
    private final long cooldownMs;

    public CooldownTracker(long cooldownMs) {
        if (cooldownMs < 0L) {
            throw new IllegalArgumentException("cooldownMs must be >= 0");
        }
        this.cooldownMs = cooldownMs;
    }

    /** Returns true if the global cooldown has expired. */
    public boolean isGlobalReady() {
        return System.currentTimeMillis() >= nextGlobalMs;
    }

    /** Sets global cooldown to now + cooldownMs. */
    public synchronized void markGlobalUsed() {
        nextGlobalMs = deadlineFrom(System.currentTimeMillis());
    }

    /** Checks and applies global cooldown atomically. */
    public synchronized boolean tryUseGlobal() {
        long now = System.currentTimeMillis();
        if (now < nextGlobalMs) return false;
        nextGlobalMs = deadlineFrom(now);
        return true;
    }

    /** Returns remaining global cooldown in milliseconds, never negative. */
    public long getGlobalRemainingMs() {
        return remaining(nextGlobalMs, System.currentTimeMillis());
    }

    /** Returns true if the player's cooldown has expired. */
    public boolean isPlayerReady(String playerName) {
        String key = requirePlayerKey(playerName);
        Long next = perPlayerMs.get(key);
        return next == null || System.currentTimeMillis() >= next.longValue();
    }

    /** Sets cooldown for a specific player. */
    public void markPlayerUsed(String playerName) {
        String key = requirePlayerKey(playerName);
        perPlayerMs.put(key, Long.valueOf(deadlineFrom(System.currentTimeMillis())));
    }

    /** Checks and applies cooldown for a player atomically. */
    public boolean tryUsePlayer(String playerName) {
        String key = requirePlayerKey(playerName);
        synchronized (perPlayerMs) {
            long now = System.currentTimeMillis();
            Long next = perPlayerMs.get(key);
            if (next != null && now < next.longValue()) return false;
            perPlayerMs.put(key, Long.valueOf(deadlineFrom(now)));
            return true;
        }
    }

    /** Returns remaining cooldown for a player in milliseconds, never negative. */
    public long getPlayerRemainingMs(String playerName) {
        String key = requirePlayerKey(playerName);
        Long next = perPlayerMs.get(key);
        return next == null ? 0L : remaining(next.longValue(), System.currentTimeMillis());
    }

    /** Removes cooldown tracking for a player. */
    public void removePlayer(String playerName) {
        String key = requirePlayerKey(playerName);
        perPlayerMs.remove(key);
    }

    /** Removes expired per-player entries and returns the number removed. */
    public int clearExpiredPlayers() {
        int removed = 0;
        long now = System.currentTimeMillis();
        for (Map.Entry<String, Long> en : perPlayerMs.entrySet()) {
            Long deadline = en.getValue();
            if (deadline != null && now >= deadline.longValue()
                    && perPlayerMs.remove(en.getKey(), deadline)) {
                removed++;
            }
        }
        return removed;
    }

    /** Clears all cooldowns (global and per-player). */
    public synchronized void clearAll() {
        perPlayerMs.clear();
        nextGlobalMs = 0L;
    }

    public long getCooldownMs() {
        return cooldownMs;
    }

    private long deadlineFrom(long now) {
        if (cooldownMs == 0L) return now;
        if (Long.MAX_VALUE - now < cooldownMs) return Long.MAX_VALUE;
        return now + cooldownMs;
    }

    private static long remaining(long deadline, long now) {
        if (deadline <= now) return 0L;
        return deadline - now;
    }

    private static String requirePlayerKey(String playerName) {
        if (playerName == null || playerName.trim().isEmpty()) {
            throw new IllegalArgumentException("playerName must not be blank");
        }
        return playerName.trim();
    }
}
