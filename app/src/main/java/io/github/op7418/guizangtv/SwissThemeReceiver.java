package io.github.op7418.guizangtv;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

/**
 * Palette switch for on-device review:
 *   auto (follow sunrise/sunset): --ei theme 2
 *   force paper:                  --ei theme 0
 *   force night:                  --ei theme 1
 * Without an extra the paper/night choice is toggled.
 */
public final class SwissThemeReceiver extends BroadcastReceiver {
    @Override public void onReceive(Context context, Intent intent) {
        if (!intent.hasExtra("theme")) {
            SwissPalette.save(context, SwissPalette.saved(context) == 0 ? 1 : 0);
            return;
        }
        int theme = intent.getIntExtra("theme", SwissPalette.AUTO);
        SwissPalette.save(context, theme == 0 || theme == 1 ? theme : SwissPalette.AUTO);
    }
}
