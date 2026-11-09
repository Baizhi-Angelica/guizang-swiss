package io.github.op7418.guizangtv;

import android.content.Intent;
import android.os.SystemClock;
import android.service.notification.NotificationListenerService;
import android.service.notification.StatusBarNotification;
import android.util.Log;

/**
 * Second system-bound entry point, the same trick Aerial TV uses on this
 * firmware ("come back after a kill"): the notification listener is bound at
 * boot even when the app came from adb and is in the stopped state. It repairs
 * the guard and, shortly after boot, shows the Swiss home if the accessibility
 * service was dropped.
 */
public final class SwissRevivalListener extends NotificationListenerService {
    private static final String TAG = "SwissRevival";
    private static final long BOOT_GRACE_MS = 5 * 60_000L;

    @Override public void onListenerConnected() {
        Log.i(TAG, "connected at " + SystemClock.elapsedRealtime() + "ms");
        SwissGuard.ensure(this);
        if (SystemClock.elapsedRealtime() > BOOT_GRACE_MS) return;
        if (android.provider.Settings.System.getInt(getContentResolver(), "boot_auto_play", 0) == 1) return;
        try {
            startActivity(new Intent(this, SwissHomeActivity.class)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP));
            Log.i(TAG, "showed the Swiss home after boot");
        } catch (RuntimeException rejected) {
            Log.w(TAG, "boot start rejected", rejected);
        }
    }

    @Override public void onNotificationPosted(StatusBarNotification notification) {
        // Not needed: the listener only exists as a boot entry point.
    }

    @Override public void onNotificationRemoved(StatusBarNotification notification) {
        // Not needed.
    }
}
