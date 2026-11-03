package io.github.op7418.guizangtv;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.SystemClock;
import android.provider.Settings;
import android.util.Log;

/**
 * Brings the Swiss home up once per full boot.
 *
 * Xiaomi's framework resolves the Home key to its own launcher and refuses to
 * change that home component, so boot is handled here instead: one alarm a few
 * seconds after BOOT_COMPLETED, which lands after the stock launcher has
 * finished its own boot flow. The launch needs the SYSTEM_ALERT_WINDOW app-op
 * on Android 10+ (granted once over adb, see docs/SWISS-HOME.md).
 */
public final class SwissBootReceiver extends BroadcastReceiver {
    static final String ACTION_SHOW = "io.github.op7418.guizangtv.SWISS_BOOT_SHOW";
    private static final String TAG = "SwissBoot";
    private static final String PREFS = "swiss-boot";
    private static final long DELAY_MS = 15_000L;

    @Override public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        if (ACTION_SHOW.equals(action)) {
            show(context);
            return;
        }
        if (!Intent.ACTION_BOOT_COMPLETED.equals(action)) return;

        // The accessibility bridge is the primary boot path; only fall back to
        // an alarm when it is not enabled (or the app is not allowed to be bound).
        if (accessibilityEnabled(context)) {
            Log.i(TAG, "accessibility bridge handles boot");
            return;
        }

        // Xiaomi's own "boot straight into a signal source" wins when it is on.
        int autoPlay = Settings.System.getInt(context.getContentResolver(), "boot_auto_play", 0);
        if (autoPlay == 1) {
            Log.i(TAG, "boot_auto_play=1, leaving the boot flow to Xiaomi");
            return;
        }

        int boot = Settings.Global.getInt(context.getContentResolver(), Settings.Global.BOOT_COUNT, -1);
        SharedPreferences prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        if (boot < 0) {
            Log.i(TAG, "no boot count, skipping");
            return;
        }
        if (prefs.getInt("boot", -1) == boot) {
            Log.i(TAG, "already handled boot " + boot);
            return;
        }
        prefs.edit().putInt("boot", boot).apply();

        AlarmManager alarms = context.getSystemService(AlarmManager.class);
        if (alarms == null) return;
        PendingIntent pending = PendingIntent.getBroadcast(context, 0,
                new Intent(ACTION_SHOW).setPackage(context.getPackageName()),
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        alarms.setExactAndAllowWhileIdle(AlarmManager.ELAPSED_REALTIME_WAKEUP,
                SystemClock.elapsedRealtime() + DELAY_MS, pending);
        Log.i(TAG, "boot " + boot + ": showing the Swiss home in " + DELAY_MS + " ms");
    }

    private boolean accessibilityEnabled(Context context) {
        String enabled = Settings.Secure.getString(context.getContentResolver(),
                Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES);
        if (enabled == null) return false;
        String self = context.getPackageName() + "/" + SwissAccessibilityService.class.getName();
        return enabled.contains(self);
    }

    private void show(Context context) {
        try {
            context.startActivity(new Intent(context, SwissHomeActivity.class)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP));
            Log.i(TAG, "Swiss home started");
        } catch (RuntimeException unavailable) {
            Log.w(TAG, "boot launch rejected", unavailable);
        }
    }
}
