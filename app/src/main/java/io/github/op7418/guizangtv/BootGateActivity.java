package io.github.op7418.guizangtv;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;

/**
 * One-shot gate for Xiaomi's package-based startup hook (Settings.System
 * start_3rd_app). Whatever component the hook resolves, it ends up on the
 * Swiss home.
 */
public final class BootGateActivity extends Activity {
    @Override protected void onCreate(Bundle state) {
        super.onCreate(state);
        try {
            startActivity(new Intent(this, SwissHomeActivity.class)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP));
        } catch (RuntimeException ignored) {
            // Nothing else to do; the stock home stays in front.
        } finally {
            finish();
        }
    }
}
