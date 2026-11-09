package io.github.op7418.guizangtv;

import android.content.ContentResolver;
import android.content.Context;
import android.content.pm.PackageManager;
import android.provider.Settings;
import android.util.Log;

/**
 * Self-healing for the firmware's favourite resets.
 *
 * MiTV-MFFU0 drops the accessibility service from the enabled list now and then
 * (the same thing that makes Home fall back to PatchWall) and turns ADB off on
 * every boot. With WRITE_SECURE_SETTINGS granted over adb (Aerial TV documents
 * the same grant on this model) the app can put both back whenever it runs.
 *
 * The standby whitelist is a private system setting: SettingsProvider rejects it
 * from apps even with WRITE_SECURE_SETTINGS, so the setup script writes it over
 * adb instead.
 */
final class SwissGuard {
    private static final String TAG = "SwissGuard";

    private SwissGuard() { }

    static void ensure(Context context) {
        if (!canWriteSecure(context)) return;
        ensureAccessibility(context);
        keepAdb(context);
    }

    static boolean canWriteSecure(Context context) {
        return context.checkSelfPermission(android.Manifest.permission.WRITE_SECURE_SETTINGS)
                == PackageManager.PERMISSION_GRANTED;
    }

    /** Put our service back at the end of the enabled list without touching the others. */
    private static void ensureAccessibility(Context context) {
        try {
            ContentResolver resolver = context.getContentResolver();
            String self = context.getPackageName() + "/" + SwissAccessibilityService.class.getName();
            String shortSelf = context.getPackageName() + "/.SwissAccessibilityService";
            String current = Settings.Secure.getString(resolver, Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES);
            if (current == null || "null".equals(current)) current = "";
            for (String part : current.split(":")) {
                if (part.equals(self) || part.equals(shortSelf)) return;
            }
            String next = current.isEmpty() ? self : current + ":" + self;
            if (Settings.Secure.putString(resolver, Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES, next)) {
                Settings.Secure.putInt(resolver, Settings.Secure.ACCESSIBILITY_ENABLED, 1);
                Log.i(TAG, "re-registered the accessibility service");
            }
        } catch (RuntimeException rejected) {
            Log.w(TAG, "accessibility repair rejected", rejected);
        }
    }

    /** Xiaomi switches adb off after a full reboot; this asks for it back. */
    private static void keepAdb(Context context) {
        try {
            if (Settings.Global.getInt(context.getContentResolver(), Settings.Global.ADB_ENABLED, 0) == 0) {
                Settings.Global.putInt(context.getContentResolver(), Settings.Global.ADB_ENABLED, 1);
                Log.i(TAG, "asked for adb back");
            }
        } catch (RuntimeException rejected) {
            Log.w(TAG, "adb repair rejected", rejected);
        }
    }
}
