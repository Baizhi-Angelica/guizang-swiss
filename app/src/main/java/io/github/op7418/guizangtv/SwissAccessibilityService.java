package io.github.op7418.guizangtv;

import android.accessibilityservice.AccessibilityService;
import android.content.Intent;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.util.Log;
import android.view.accessibility.AccessibilityEvent;

/**
 * Keeps the Swiss home in front of the stock launcher.
 *
 * Xiaomi's framework resolves the Home key to com.mitv.tvhome at the system
 * level and refuses to change that component, so this service watches for the
 * two stock home activities and brings the Swiss home forward. It reads only
 * the package and class name of window changes, never window content.
 *
 * The exit target sets a one-shot hand-off so the stock home is reachable
 * deliberately; the next Home press after that is taken back.
 */
public final class SwissAccessibilityService extends AccessibilityService {
    private static final String TAG = "SwissA11y";
    private static final String STOCK = "com.mitv.tvhome";
    private static final long SETTLE_MS = 350L;
    /** Any service connect inside this window after boot counts as the boot launch. */
    private static final long BOOT_GRACE_MS = 5 * 60_000L;
    /** Inside this window every Xiaomi launcher page is covered, including the
     *  "my apps" page the launcher restores after a reboot. */
    private static final long BOOT_WINDOW_MS = 3 * 60_000L;

    private final Handler handler = new Handler(Looper.getMainLooper());
    private Runnable pending;

    @Override public void onAccessibilityEvent(AccessibilityEvent event) {
        if (event.getEventType() != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) return;
        CharSequence pkg = event.getPackageName();
        CharSequence cls = event.getClassName();
        if (pkg == null || cls == null) return;
        if (!STOCK.equals(pkg.toString())) return;
        String name = cls.toString();
        Log.i(TAG, "stock window " + name);
        boolean home = name.endsWith(".MainActivityUserMode") || name.equals("com.mitv.tvhome.MainActivity");
        boolean bootWindow = SystemClock.elapsedRealtime() < BOOT_WINDOW_MS;
        if (!home && !(bootWindow && name.startsWith("com.mitv.tvhome."))) return;
        if (SwissHandoff.take(this)) {
            Log.i(TAG, "stock home allowed once");
            return;
        }
        if (pending != null) handler.removeCallbacks(pending);
        pending = this::showHome;
        handler.postDelayed(pending, SETTLE_MS);
    }

    private void showHome() {
        pending = null;
        try {
            startActivity(new Intent(this, SwissHomeActivity.class)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP));
            Log.i(TAG, "stock home replaced");
        } catch (RuntimeException rejected) {
            Log.w(TAG, "home start rejected", rejected);
        }
    }

    @Override protected void onServiceConnected() {
        Log.i(TAG, "connected at " + SystemClock.elapsedRealtime() + "ms");
        SwissGuard.ensure(this);
        // The service is bound a little after the stock launcher has already
        // drawn its first window, so a boot that misses the window event is
        // covered by these two attempts.
        if (SystemClock.elapsedRealtime() < BOOT_GRACE_MS && !bootAutoPlay()) {
            handler.postDelayed(this::showHome, 1500L);
            handler.postDelayed(this::showHome, 9000L);
        }
    }

    private boolean bootAutoPlay() {
        return android.provider.Settings.System.getInt(getContentResolver(), "boot_auto_play", 0) == 1;
    }

    @Override public void onInterrupt() {
        if (pending != null) handler.removeCallbacks(pending);
        pending = null;
    }

    @Override public void onDestroy() {
        if (pending != null) handler.removeCallbacks(pending);
        pending = null;
        super.onDestroy();
    }
}
