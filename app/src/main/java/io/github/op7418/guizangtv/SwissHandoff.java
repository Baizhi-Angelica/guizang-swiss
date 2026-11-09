package io.github.op7418.guizangtv;

import android.content.Context;

/** One-shot hand-off to the stock Xiaomi home, so its own window is not stolen back. */
final class SwissHandoff {
    private static final String PREFS = "swiss-handoff";
    private static final String KEY = "handoff_at";
    /** A hand-off older than this is stale (for example left from a previous session). */
    private static final long TTL_MS = 30_000L;

    static void toStockHome(Context context) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                .edit().putLong(KEY, System.currentTimeMillis()).apply();
    }

    /** True once after {@link #toStockHome(Context)}, then cleared. */
    static boolean take(Context context) {
        long at = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getLong(KEY, 0L);
        if (at <= 0L) return false;
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                .edit().remove(KEY).apply();
        return System.currentTimeMillis() - at < TTL_MS;
    }

    private SwissHandoff() { }
}
