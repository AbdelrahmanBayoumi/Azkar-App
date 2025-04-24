package com.bayoumi.util.web;


import com.bayoumi.util.Logger;

import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Supplier;

public final class RetryUtil {

    /**
     * Synchronously retry a boolean action up to maxRetries times
     * with exponential backoff (and optional jitter).
     *
     * @param action           a Supplier that returns true on success, false on failure
     * @param maxRetries       how many attempts before giving up
     * @param initialBackoffMs starting backoff in milliseconds
     * @param addJitter        if true, adds ±20% jitter to each sleep interval
     */
    public static void retry(
            Supplier<Boolean> action,
            int maxRetries,
            long initialBackoffMs,
            boolean addJitter
    ) {
        int attempt = 1;
        long backoff = initialBackoffMs;

        while (attempt <= maxRetries) {
            Logger.debug(String.format("[RetryUtil] Attempt %d/%d...", attempt, maxRetries));
            try {
                if (action.get()) return;
            } catch (Exception ex) {
                Logger.error("[RetryUtil] Exception on attempt " + attempt, ex, RetryUtil.class.getName() + ".retry()");
            }

            if (attempt == maxRetries) break;

            long sleepMs = backoff;
            if (addJitter) {
                long delta = backoff / 5; // 20%
                sleepMs += ThreadLocalRandom.current().nextLong(-delta, delta);
            }

            try {
                Thread.sleep(sleepMs);
            } catch (InterruptedException ignored) {
                Thread.currentThread().interrupt();
                break;
            }

            backoff *= 2;
            attempt++;
        }

        Logger.error("[RetryUtil] All retry attempts failed", null, RetryUtil.class.getName() + ".retry()");
    }

    /**
     * Asynchronously retries the given action in its own daemon thread.
     */
    public static void retryAsync(
            Supplier<Boolean> action,
            int maxRetries,
            long initialBackoffMs,
            boolean addJitter,
            String threadName
    ) {
        Thread t = new Thread(() -> retry(action, maxRetries, initialBackoffMs, addJitter), threadName);
        t.setDaemon(true);
        t.setUncaughtExceptionHandler((th, ex) ->
                Logger.error("Uncaught in " + th.getName(), ex, RetryUtil.class.getName())
        );
        t.start();
    }
}
