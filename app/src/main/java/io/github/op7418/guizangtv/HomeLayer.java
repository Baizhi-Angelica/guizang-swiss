package io.github.op7418.guizangtv;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;
import java.util.ArrayList;
import java.util.List;

/**
 * Shared home chrome for every layout: wallpaper preview holder, clock, weather,
 * and the remote-control wiring helper. FrameHomeActivity and the Settings
 * preview hold a HomeLayer so list, dock and metro layouts can be swapped.
 */
abstract class HomeLayer extends FrameLayout {
    interface Actions { void open(String id); void control(int index); void selected(int index); }

    final List<View> apps = new ArrayList<>(), controls = new ArrayList<>();
    final ImageView previewPhoto;
    final TextView clock;
    final TextView weather;
    final HomeAppearance style;
    final boolean preview;
    final Actions actions;

    HomeLayer(Context c, HomeAppearance style, boolean preview, Actions actions) {
        super(c);
        this.style = style;
        this.preview = preview;
        this.actions = actions;
        previewPhoto = new ImageView(c);
        previewPhoto.setScaleType(ImageView.ScaleType.CENTER_CROP);
        clock = text("", 26);
        clock.setGravity(Gravity.CENTER_VERTICAL);
        weather = text("", 16);
        weather.setAlpha(.82f);
        weather.setGravity(Gravity.CENTER_VERTICAL);
        weather.setSingleLine(true);
        weather.setMaxWidth(dp(215));
        weather.setEllipsize(TextUtils.TruncateAt.END);
        weather.setVisibility(GONE);
    }

    /** Same reading renderer for list, dock and metro; hidden when weather is off. */
    void weather(HomeWeather.Reading reading) {
        weather.setVisibility(reading == null ? GONE : VISIBLE);
        if (reading == null) return;
        weather.setText(Math.round(reading.temperature) + "°  " + reading.city);
        Drawable icon = new WeatherIcon(reading.code, reading.day, style.ink());
        icon.setBounds(0, 0, dp(24), dp(24));
        weather.setCompoundDrawables(icon, null, null, null);
        weather.setCompoundDrawablePadding(dp(7));
        weather.setContentDescription(reading.city + "，" + WeatherPolicy.description(reading.code)
                + "，" + Math.round(reading.temperature) + "摄氏度");
    }

    /** Each layout applies the current wallpaper to its own surfaces. */
    abstract void wallpaper(Drawable drawable);

    /** Focus the remembered entry, clamped to the entries the layout actually shows. */
    abstract void focusApp();

    void wire(View v, Runnable run) {
        v.setId(View.generateViewId());
        v.setFocusable(!preview);
        v.setFocusableInTouchMode(!preview);
        v.setClickable(!preview);
        if (!preview) {
            v.setOnClickListener(w -> run.run());
            v.setOnKeyListener((w, key, e) -> (key == KeyEvent.KEYCODE_DPAD_CENTER || key == KeyEvent.KEYCODE_ENTER)
                    && e.getAction() == KeyEvent.ACTION_DOWN && e.getRepeatCount() > 0);
        }
    }

    TextView text(String s, float size) {
        TextView v = new TextView(getContext());
        v.setText(s);
        v.setTextSize(size);
        v.setTextColor(style.ink());
        v.setIncludeFontPadding(false);
        return v;
    }

    int dp(float value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }
}
