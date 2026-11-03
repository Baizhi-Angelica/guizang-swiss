package io.github.op7418.guizangtv;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.util.Log;
import java.util.List;

/**
 * Device entry points used by the Swiss home: Xiaomi's external-source player
 * for the three HDMI ports, its signal-source panel as a fallback, and the
 * stock Xiaomi launcher behind the exit target.
 */
final class SwissLinks {
    private static final String TAG = "SwissHome";
    private static final String PLAYER = "com.xiaomi.mitv.tvplayer";
    private static final String PLAYER_ACTIVITY = "com.xiaomi.mitv.tvplayer.ExternalSourceActivity";

    private SwissLinks() { }

    /**
     * HDMI 1-3 map to the player's input extra 23/24/25 on the firmware this
     * project was checked against. The component is verified before the call
     * so a missing or unexported activity falls through to the input panel.
     */
    static boolean openHdmi(Context context, int port) {
        if (port < 1 || port > 3) return false;
        ComponentName player = new ComponentName(PLAYER, PLAYER_ACTIVITY);
        if (!callable(context, player)) return false;
        try {
            context.startActivity(new Intent("com.xiaomi.mitv.tvplayer.EXTSRC_PLAY")
                    .setComponent(player)
                    .putExtra("input", 22 + port)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
            Log.i(TAG, "HDMI " + port + " input=" + (22 + port));
            return true;
        } catch (RuntimeException unavailable) {
            Log.w(TAG, "HDMI " + port + " rejected", unavailable);
            return false;
        }
    }

    /** Xiaomi's own signal-source popup, used when a direct switch is unavailable. */
    static boolean showInputs(Context context) {
        ComponentName receiver = new ComponentName("com.xiaomi.mitv.settings",
                "com.xiaomi.mitv.settings.SettingsBroadcastReceiver");
        try {
            android.content.pm.ActivityInfo receiverInfo =
                    context.getPackageManager().getReceiverInfo(receiver, 0);
            if (!receiverInfo.exported || !receiverInfo.enabled) return false;
            if (receiverInfo.permission != null && context.checkSelfPermission(receiverInfo.permission)
                    != PackageManager.PERMISSION_GRANTED) return false;
            context.sendBroadcast(new Intent("com.xiaomi.mitv.settings.INPUTSOURCE_POPUP").setComponent(receiver));
            Log.i(TAG, "opened signal-source panel");
            return true;
        } catch (PackageManager.NameNotFoundException | SecurityException unavailable) {
            Log.w(TAG, "signal-source panel unavailable", unavailable);
            return false;
        }
    }

    /** Starts the stock Xiaomi launcher explicitly; any non-ours HOME is the fallback. */
    static boolean stockHome(Context context) {
        ComponentName home = findStockHome(context);
        if (home == null) {
            Log.w(TAG, "no stock home found");
            return false;
        }
        try {
            context.startActivity(new Intent(Intent.ACTION_MAIN)
                    .setComponent(home)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED));
            Log.i(TAG, "stock home " + home.flattenToShortString());
            return true;
        } catch (RuntimeException unavailable) {
            Log.w(TAG, "stock home rejected", unavailable);
            return false;
        }
    }

    private static ComponentName findStockHome(Context context) {
        ComponentName known = new ComponentName("com.mitv.tvhome", "com.mitv.tvhome.MainActivity");
        if (callable(context, known)) return known;
        Intent intent = new Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME);
        List<ResolveInfo> homes = context.getPackageManager().queryIntentActivities(intent, 0);
        ComponentName fallback = null;
        for (ResolveInfo info : homes) {
            if (info.activityInfo == null) continue;
            String pkg = info.activityInfo.packageName;
            if (context.getPackageName().equals(pkg) || "android".equals(pkg)) continue;
            ComponentName candidate = new ComponentName(pkg, info.activityInfo.name);
            if (pkg.contains("tvhome") || pkg.contains("mitv") || pkg.contains("xiaomi")) return candidate;
            if (fallback == null) fallback = candidate;
        }
        return fallback;
    }

    private static boolean callable(Context context, ComponentName component) {
        try {
            ActivityInfo info = context.getPackageManager().getActivityInfo(component, 0);
            if (!info.enabled || !info.exported) return false;
            return info.permission == null
                    || context.checkSelfPermission(info.permission) == PackageManager.PERMISSION_GRANTED;
        } catch (PackageManager.NameNotFoundException missing) {
            return false;
        }
    }
}
