package me.tamkungz.nilkit.util;

import org.junit.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.Assert.*;

public class CooldownTrackerTest {

    @Test
    public void tryUseGlobalIsAtomic() throws Exception {
        final CooldownTracker tracker = new CooldownTracker(60_000L);
        final int threadCount = 24;
        final CountDownLatch ready = new CountDownLatch(threadCount);
        final CountDownLatch go = new CountDownLatch(1);
        final CountDownLatch done = new CountDownLatch(threadCount);
        final AtomicInteger winners = new AtomicInteger();

        for (int i = 0; i < threadCount; i++) {
            new Thread(new Runnable() {
                @Override
                public void run() {
                    ready.countDown();
                    try {
                        go.await();
                        if (tracker.tryUseGlobal()) winners.incrementAndGet();
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    } finally {
                        done.countDown();
                    }
                }
            }).start();
        }

        ready.await();
        go.countDown();
        done.await();
        assertEquals(1, winners.get());
        assertTrue(tracker.getGlobalRemainingMs() > 0L);
    }

    @Test
    public void playerKeysAreNormalizedAndCleanupWorks() {
        CooldownTracker tracker = new CooldownTracker(0L);
        tracker.markPlayerUsed(" Alice ");
        assertTrue(tracker.isPlayerReady("Alice"));
        assertEquals(1, tracker.clearExpiredPlayers());
        assertEquals(0L, tracker.getPlayerRemainingMs("Alice"));
    }

    @Test(expected = IllegalArgumentException.class)
    public void rejectsNegativeCooldown() {
        new CooldownTracker(-1L);
    }
}
