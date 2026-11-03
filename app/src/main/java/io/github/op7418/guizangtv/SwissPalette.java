package io.github.op7418.guizangtv;

import android.content.Context;
import android.content.SharedPreferences;

/** Two flat palettes in the Swiss poster tradition: warm paper and deep night. */
final class SwissPalette {
    /** Preference value: follow sunrise and sunset. */
    static final int AUTO = -1;

    final int top, bottom, ink, muted, hair, accent, accentInk;

    private SwissPalette(int top, int bottom, int ink, int muted, int hair, int accent, int accentInk) {
        this.top = top;
        this.bottom = bottom;
        this.ink = ink;
        this.muted = muted;
        this.hair = hair;
        this.accent = accent;
        this.accentInk = accentInk;
    }

    static final SwissPalette PAPER = new SwissPalette(
            0xFFF4EFE2, 0xFFE9E2D1, 0xFF19140D, 0xFF7B7264, 0xFFD6CEBB,
            0xFFD92318, 0xFFF8F4EA);

    static final SwissPalette NIGHT = new SwissPalette(
            0xFF131315, 0xFF0A0A0B, 0xFFF1EEE5, 0xFF8B8578, 0xFF2C2C2E,
            0xFFE02418, 0xFFFFFFFF);

    private static final String PREFS = "swiss-home";
    private static final String THEME = "theme";

    /** -1 follows the sun; 0 forces paper; 1 forces night. */
    static int saved(Context context) {
        int value = prefs(context).getInt(THEME, AUTO);
        return value == 0 || value == 1 ? value : AUTO;
    }

    static void save(Context context, int theme) {
        int value = theme == 0 || theme == 1 ? theme : AUTO;
        prefs(context).edit().putInt(THEME, value).apply();
    }

    static SwissPalette of(int theme) {
        return theme == 1 ? NIGHT : PAPER;
    }

    private static SharedPreferences prefs(Context context) {
        return context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }
}
