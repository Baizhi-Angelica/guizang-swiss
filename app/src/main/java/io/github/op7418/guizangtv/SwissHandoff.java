package io.github.op7418.guizangtv;

import android.content.Context;

/** One-shot hand-off to the stock Xiaomi home, so its own window is not stolen back. */
final class SwissHandoff {
    private static final String PREFS = "swiss-handoff";
    private static final String KEY = "handoff";

    static void toStockHome(Context context) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                .edit().putBoolean(KEY, true).apply();
    }

    /** True once after {@link #toStockHome(Context)}, then cleared. */
    static boolean take(Context context) {
        boolean pending = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                .getBoolean(KEY, false);
        if (pending) {
            context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                    .edit().putBoolean(KEY, false).apply();
        }
        return pending;
    }

    private SwissHandoff() { }
}
