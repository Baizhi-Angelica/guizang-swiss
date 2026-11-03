package io.github.op7418.guizangtv;

import android.app.Activity;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.KeyEvent;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Toast;
import java.time.LocalDateTime;
import java.util.Locale;

/**
 * Swiss home: three HDMI sources and a small exit target. This is the activity
 * declared as the device HOME; it deliberately offers no settings, app list or
 * Xiaomi features of its own.
 */
public final class SwissHomeActivity extends Activity implements SwissBoard.Listener {
    private static final String[] WEEKDAYS = {"星期一", "星期二", "星期三", "星期四", "星期五", "星期六", "星期日"};

    private final Handler handler = new Handler(Looper.getMainLooper());
    private final Runnable tick = new Runnable() {
        @Override public void run() {
            updateClock();
            if (refreshTheme()) return;
            schedule();
        }
    };

    private SwissBoard board;
    /** Effective palette: 0 paper, 1 night. */
    private int theme;

    @Override protected void onCreate(Bundle state) {
        theme = resolveTheme(SwissPalette.saved(this));
        setTheme(theme == 1 ? R.style.Theme_GuiZangTV_Night : R.style.Theme_GuiZangTV_Paper);
        super.onCreate(state);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
        immersive();
        board = new SwissBoard(this, SwissPalette.of(theme), this);
        setContentView(board);
        board.requestFocus();
    }

    @Override protected void onResume() {
        super.onResume();
        if (refreshTheme()) return;
        immersive();
        updateClock();
        handler.removeCallbacks(tick);
        schedule();
        if (board != null) {
            board.palette(SwissPalette.of(theme));
            board.requestFocus();
            board.playIntro();
        }
    }

    /** Paper while the sun is up, night after sunset; an explicit choice wins. */
    private int resolveTheme(int preference) {
        if (preference == 0 || preference == 1) return preference;
        return SwissSun.daylight(System.currentTimeMillis()) ? 0 : 1;
    }

    private boolean refreshTheme() {
        int wanted = resolveTheme(SwissPalette.saved(this));
        if (wanted == theme) return false;
        theme = wanted;
        recreate();
        return true;
    }

    @Override protected void onPause() {
        handler.removeCallbacks(tick);
        super.onPause();
    }

    @Override public void onWindowFocusChanged(boolean focused) {
        super.onWindowFocusChanged(focused);
        if (focused) immersive();
    }

    private void immersive() {
        Window window = getWindow();
        window.getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                | View.SYSTEM_UI_FLAG_FULLSCREEN
                | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);
    }

    private void updateClock() {
        LocalDateTime now = LocalDateTime.now();
        String hour = String.format(Locale.CHINA, "%02d", now.getHour());
        String minute = String.format(Locale.CHINA, "%02d", now.getMinute());
        String date = now.getYear() + "年" + now.getMonthValue() + "月" + now.getDayOfMonth() + "日";
        String weekday = WEEKDAYS[now.getDayOfWeek().getValue() - 1];
        if (board != null) board.time(hour, minute, date, weekday);
    }

    private void schedule() {
        long now = System.currentTimeMillis();
        long delay = 60_000L - now % 60_000L + 150L;
        handler.postDelayed(tick, delay);
    }

    @Override public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK) return true;
        if (board != null && board.key(event)) return true;
        return super.onKeyDown(keyCode, event);
    }

    @Override public boolean onKeyUp(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK) return true;
        if (board != null && board.key(event)) return true;
        return super.onKeyUp(keyCode, event);
    }

    @Override public void openSource(int port) {
        if (SwissLinks.openHdmi(this, port)) return;
        if (SwissLinks.showInputs(this)) {
            toast("请在信号源面板中选择 HDMI " + port);
            return;
        }
        toast("暂时无法切换信号源");
    }

    @Override public void exitHome() {
        // One-shot hand-off: the accessibility bridge must not steal this one back.
        SwissHandoff.toStockHome(this);
        if (!SwissLinks.stockHome(this)) toast("暂时无法打开小米桌面");
    }

    private void toast(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }
}
