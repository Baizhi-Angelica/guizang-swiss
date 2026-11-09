package io.github.op7418.guizangtv;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Shader;
import android.graphics.Typeface;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.animation.DecelerateInterpolator;

/**
 * The Swiss home board: a typographic grid on a flat field, three signal-source
 * rows and a small exit target. Everything is drawn in a 1920 x 1080 design
 * space and scaled to the window, so 1080p panels and 4K panels share one layout.
 */
final class SwissBoard extends View {
    interface Listener {
        /** HDMI port 1-3. */
        void openSource(int port);

        /** Back to the Xiaomi home screen. */
        void exitHome();
    }

    private static final String[] NUMBER = {"01", "02", "03"};
    private static final String[] TITLE = {"Switch", "有线电视", "Apple TV"};
    private static final String[] PORT = {"HDMI 1", "HDMI 2", "HDMI 3"};

    private final Listener listener;
    private SwissPalette palette;

    private final Typeface light = Typeface.create("sans-serif-light", Typeface.NORMAL);
    private final Typeface regular = Typeface.create("sans-serif", Typeface.NORMAL);
    private final Typeface medium = Typeface.create("sans-serif-medium", Typeface.NORMAL);
    private final Typeface bold = Typeface.create("sans-serif", Typeface.BOLD);

    private final Paint brush = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.SUBPIXEL_TEXT_FLAG);
    private final Paint plate = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF scrub = new RectF();
    private final Rect ink = new Rect();
    private Shader wash;

    private final float[] focusMix = new float[SwissFrame.EXIT + 1];
    private int focus;
    private int lastSource;
    private ValueAnimator focusAnim;
    private ValueAnimator introAnim;
    private float intro = 1f;
    private boolean introDone;

    private String hour = "", minute = "", date = "", weekday = "";

    SwissBoard(Context context, SwissPalette palette, Listener listener) {
        super(context);
        this.palette = palette;
        this.listener = listener;
        setFocusable(true);
        setFocusableInTouchMode(true);
        setBackgroundColor(palette.bottom);
        focusMix[0] = 1f;
    }

    void palette(SwissPalette value) {
        if (value == null || value == palette) return;
        palette = value;
        wash = null;
        setBackgroundColor(palette.bottom);
        invalidate();
    }

    void time(String hour, String minute, String date, String weekday) {
        this.hour = hour;
        this.minute = minute;
        this.date = date;
        this.weekday = weekday;
        invalidate();
    }

    void playIntro() {
        if (introDone) return;
        introDone = true;
        intro = 0f;
        if (introAnim != null) introAnim.cancel();
        introAnim = ValueAnimator.ofFloat(0f, 1f);
        introAnim.setDuration(900);
        introAnim.setInterpolator(new DecelerateInterpolator(1.4f));
        introAnim.addUpdateListener(animation -> {
            intro = (float) animation.getAnimatedValue();
            invalidate();
        });
        introAnim.start();
        focusAt(0, false);
    }

    void focusAt(int target, boolean animate) {
        int next = Math.max(0, Math.min(SwissFrame.EXIT, target));
        if (next < SwissFrame.SOURCE_COUNT) lastSource = next;
        focus = next;
        float[] from = focusMix.clone();
        float[] to = new float[SwissFrame.EXIT + 1];
        to[next] = 1f;
        if (!animate) {
            System.arraycopy(to, 0, focusMix, 0, to.length);
            invalidate();
            return;
        }
        if (focusAnim != null) focusAnim.cancel();
        focusAnim = ValueAnimator.ofFloat(0f, 1f);
        focusAnim.setDuration(170);
        focusAnim.setInterpolator(new DecelerateInterpolator(1.8f));
        focusAnim.addUpdateListener(animation -> {
            float t = (float) animation.getAnimatedValue();
            for (int i = 0; i < focusMix.length; i++) focusMix[i] = from[i] + (to[i] - from[i]) * t;
            invalidate();
        });
        focusAnim.start();
    }

    /** Coming back from the stock home should not leave OK pointing at "exit". */
    void resumeFocus() {
        if (focus == SwissFrame.EXIT) {
            focusAt(Math.max(0, Math.min(SwissFrame.SOURCE_COUNT - 1, lastSource)), false);
        }
    }

    boolean key(KeyEvent event) {
        int code = event.getKeyCode();
        if (event.getAction() == KeyEvent.ACTION_UP) return consumes(code);
        int direction = 0;
        if (code == KeyEvent.KEYCODE_DPAD_UP) direction = SwissFrame.UP;
        else if (code == KeyEvent.KEYCODE_DPAD_DOWN) direction = SwissFrame.DOWN;
        else if (code == KeyEvent.KEYCODE_DPAD_LEFT) direction = SwissFrame.LEFT;
        else if (code == KeyEvent.KEYCODE_DPAD_RIGHT) direction = SwissFrame.RIGHT;
        if (direction != 0) {
            focusAt(SwissFrame.move(focus, direction), true);
            return true;
        }
        if (code == KeyEvent.KEYCODE_DPAD_CENTER || code == KeyEvent.KEYCODE_ENTER
                || code == KeyEvent.KEYCODE_NUMPAD_ENTER || code == KeyEvent.KEYCODE_BUTTON_A
                || code == KeyEvent.KEYCODE_SPACE) {
            if (event.getRepeatCount() == 0) activate();
            return true;
        }
        if (code >= KeyEvent.KEYCODE_1 && code <= KeyEvent.KEYCODE_3) {
            if (event.getRepeatCount() == 0) listener.openSource(code - KeyEvent.KEYCODE_1 + 1);
            return true;
        }
        return false;
    }

    private boolean consumes(int code) {
        return code == KeyEvent.KEYCODE_DPAD_UP || code == KeyEvent.KEYCODE_DPAD_DOWN
                || code == KeyEvent.KEYCODE_DPAD_LEFT || code == KeyEvent.KEYCODE_DPAD_RIGHT
                || code == KeyEvent.KEYCODE_DPAD_CENTER || code == KeyEvent.KEYCODE_ENTER
                || code == KeyEvent.KEYCODE_NUMPAD_ENTER || code == KeyEvent.KEYCODE_BUTTON_A
                || code == KeyEvent.KEYCODE_SPACE
                || (code >= KeyEvent.KEYCODE_1 && code <= KeyEvent.KEYCODE_3);
    }

    private void activate() {
        if (focus == SwissFrame.EXIT) listener.exitHome();
        else listener.openSource(focus + 1);
    }

    @Override public boolean onTouchEvent(MotionEvent event) {
        if (event.getAction() != MotionEvent.ACTION_UP) return true;
        float s = scale();
        if (s <= 0f) return true;
        float ox = (getWidth() - SwissFrame.WIDTH * s) * .5f;
        float oy = (getHeight() - SwissFrame.HEIGHT * s) * .5f;
        int hit = SwissFrame.hit((event.getX() - ox) / s, (event.getY() - oy) / s);
        if (hit < 0) return true;
        focusAt(hit, true);
        if (hit == SwissFrame.EXIT) listener.exitHome();
        else listener.openSource(hit + 1);
        return true;
    }

    // ---------------------------------------------------------------- drawing

    private float scale() {
        if (getWidth() <= 0 || getHeight() <= 0) return 0f;
        return Math.min(getWidth() / SwissFrame.WIDTH, getHeight() / SwissFrame.HEIGHT);
    }

    private float x(float design) {
        return (getWidth() - SwissFrame.WIDTH * scale()) * .5f + design * scale();
    }

    private float y(float design) {
        return (getHeight() - SwissFrame.HEIGHT * scale()) * .5f + design * scale();
    }

    private float z(float design) {
        return design * scale();
    }

    @Override protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        wash = null;
    }

    @Override protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        float s = scale();
        if (s <= 0f) return;
        if (wash == null) {
            wash = new LinearGradient(0, 0, 0, getHeight(), palette.top, palette.bottom, Shader.TileMode.CLAMP);
        }
        plate.reset();
        plate.setAntiAlias(true);
        plate.setShader(wash);
        canvas.drawRect(0, 0, getWidth(), getHeight(), plate);
        plate.setShader(null);

        float head = reveal(0f);
        float rules = reveal(.07f);
        float clock = reveal(.11f);
        float dateIn = reveal(.20f);
        float exit = reveal(.48f);
        float caption = reveal(.54f);

        drawHeader(canvas, head);
        drawRules(canvas, rules);
        drawClock(canvas, clock);
        drawDate(canvas, dateIn);
        drawSources(canvas);
        drawExit(canvas, exit);
        drawCaption(canvas, caption);
    }

    private float reveal(float start) {
        float t = (intro - start) / .42f;
        if (t <= 0f) return 0f;
        if (t >= 1f) return 1f;
        float rest = 1f - t;
        return 1f - rest * rest * rest;
    }

    private void drawHeader(Canvas canvas, float alpha) {
        if (alpha <= 0f) return;
        String name = "小米电视";
        float nameSize = z(32);
        float nameBaseline = y(122);
        write(canvas, name, x(130), nameBaseline, nameSize, bold, palette.ink, alpha, .04f, Paint.Align.LEFT);

        // The square is centred on the wordmark's ink box, not on a guessed baseline.
        brush.reset();
        brush.setAntiAlias(true);
        brush.setTypeface(bold);
        brush.setTextSize(nameSize);
        brush.getTextBounds(name, 0, name.length(), ink);
        float center = nameBaseline + (ink.top + ink.bottom) * .5f;
        float mark = z(16);
        plate.setColor(palette.accent);
        plate.setAlpha(alpha(alpha));
        canvas.drawRect(x(96), center - mark * .5f, x(96) + mark, center + mark * .5f, plate);

        float nameWidth = measure(name, nameSize, bold, .04f);
        write(canvas, "信号源选择", x(130) + nameWidth + z(26), nameBaseline, z(22),
                regular, palette.muted, alpha, .14f, Paint.Align.LEFT);
    }

    private void drawRules(Canvas canvas, float alpha) {
        if (alpha <= 0f) return;
        plate.setColor(palette.ink);
        plate.setAlpha(alpha(alpha));
        canvas.drawRect(x(96), y(149), x(96) + z(1728) * alpha, y(151), plate);

        plate.setColor(palette.hair);
        plate.setAlpha(alpha(alpha));
        canvas.drawRect(x(992), y(151), x(994), y(151) + z(833) * alpha, plate);
        for (int i = 0; i < SwissFrame.SOURCE_COUNT; i++) {
            float top = y(SwissFrame.rowTop(i));
            canvas.drawRect(x(1040), top, x(1824), top + z(2), plate);
        }
        canvas.drawRect(x(1040), y(SwissFrame.rowBottom(2)), x(1824), y(SwissFrame.rowBottom(2)) + z(2), plate);
    }

    private void drawClock(Canvas canvas, float alpha) {
        if (alpha <= 0f) return;
        float size = z(300);
        float baseline = y(500);
        float w = measure(hour, size, light, 0f);
        float colon = measure(":", size, light, 0f);
        write(canvas, hour, x(96), baseline, size, light, palette.ink, alpha, 0f, Paint.Align.LEFT);
        write(canvas, ":", x(96) + w + z(2), baseline, size, light, palette.accent, alpha, 0f, Paint.Align.LEFT);
        write(canvas, minute, x(96) + w + colon + z(10), baseline, size, light, palette.ink, alpha, 0f, Paint.Align.LEFT);
    }

    private void drawDate(Canvas canvas, float alpha) {
        if (alpha <= 0f) return;
        write(canvas, date, x(96), y(604), z(34), regular, palette.ink, alpha, .02f, Paint.Align.LEFT);
        float width = measure(date, z(34), regular, .02f);
        write(canvas, weekday, x(96) + width + z(30), y(604), z(26), regular, palette.muted, alpha, .08f, Paint.Align.LEFT);
    }

    private void drawSources(Canvas canvas) {
        for (int i = 0; i < SwissFrame.SOURCE_COUNT; i++) {
            float alpha = reveal(.26f + i * .06f);
            if (alpha <= 0f) continue;
            float top = y(SwissFrame.rowTop(i));
            float bottom = y(SwissFrame.rowBottom(i));
            float left = x(1040);
            float right = x(1824);
            float mix = focusMix[i];
            if (mix > .002f) {
                plate.setColor(palette.accent);
                plate.setAlpha(alpha(alpha));
                canvas.drawRect(left, top, left + (right - left) * mix, bottom, plate);
            }
            drawRowText(canvas, i, palette.ink, alpha, left, top, right, bottom);
            if (mix > .002f) {
                canvas.save();
                canvas.clipRect(left, top, left + (right - left) * mix, bottom);
                drawRowText(canvas, i, palette.accentInk, alpha, left, top, right, bottom);
                canvas.restore();
            }
        }
    }

    private void drawRowText(Canvas canvas, int index, int color, float alpha,
                             float left, float top, float right, float bottom) {
        canvas.save();
        scrub.set(left, top, right, bottom);
        canvas.clipRect(scrub);
        float baseline = bottom - z(54);
        write(canvas, NUMBER[index], left + z(8), baseline, z(24), regular, color, alpha * .8f, .14f, Paint.Align.LEFT);
        write(canvas, TITLE[index], left + z(112), baseline, z(56), medium, color, alpha, .02f, Paint.Align.LEFT);
        write(canvas, PORT[index], right - z(8), baseline - z(2), z(20), regular, color, alpha * .72f, .20f, Paint.Align.RIGHT);
        canvas.restore();
    }

    private void drawExit(Canvas canvas, float alpha) {
        if (alpha <= 0f) return;
        float left = x(SwissFrame.EXIT_LEFT);
        float top = y(SwissFrame.EXIT_TOP);
        float right = x(SwissFrame.EXIT_RIGHT);
        float bottom = y(SwissFrame.EXIT_BOTTOM);
        float mix = focusMix[SwissFrame.EXIT];
        if (mix > .002f) {
            plate.setColor(palette.accent);
            plate.setAlpha(alpha(alpha));
            canvas.drawRect(left, top, left + (right - left) * mix, bottom, plate);
        }
        float edge = z(2);
        plate.setColor(palette.ink);
        plate.setAlpha(alpha(alpha));
        canvas.drawRect(left, top, right, top + edge, plate);
        canvas.drawRect(left, bottom - edge, right, bottom, plate);
        canvas.drawRect(left, top, left + edge, bottom, plate);
        canvas.drawRect(right - edge, top, right, bottom, plate);

        String label = "退出";
        float size = z(25);
        float width = measure(label, size, medium, .12f);
        float centerY = (top + bottom) * .5f;
        float baseline = centerY - (fontAscent(size, medium) + fontDescent(size, medium)) * .5f;
        writeSplit(canvas, label, (left + right - width) * .5f, baseline, size, medium,
                palette.ink, palette.accentInk, alpha, .12f, left, top, right, bottom, mix);
    }

    private void drawCaption(Canvas canvas, float alpha) {
        if (alpha <= 0f) return;
        write(canvas, "↑ ↓ 选择      OK 进入      1 2 3 直达", x(96), y(948), z(18),
                regular, palette.muted, alpha, .06f, Paint.Align.LEFT);
    }

    /** Draws text in the base ink, then again in the inverted ink clipped to the focus fill. */
    private void writeSplit(Canvas canvas, String text, float xStart, float baseline, float size,
                            Typeface face, int baseColor, int focusColor, float alpha, float tracking,
                            float left, float top, float right, float bottom, float mix) {
        write(canvas, text, xStart, baseline, size, face, baseColor, alpha, tracking, Paint.Align.LEFT);
        if (mix <= .002f) return;
        canvas.save();
        canvas.clipRect(left, top, left + (right - left) * mix, bottom);
        write(canvas, text, xStart, baseline, size, face, focusColor, alpha, tracking, Paint.Align.LEFT);
        canvas.restore();
    }

    private void write(Canvas canvas, String text, float xStart, float baseline, float size,
                       Typeface face, int color, float alpha, float tracking, Paint.Align align) {
        if (text == null || text.isEmpty() || alpha <= .004f) return;
        brush.reset();
        brush.setAntiAlias(true);
        brush.setSubpixelText(true);
        brush.setTypeface(face);
        brush.setTextSize(size);
        brush.setColor(color);
        brush.setAlpha(Math.round(255f * Math.min(1f, alpha)));
        brush.setLetterSpacing(tracking);
        brush.setTextAlign(align);
        canvas.drawText(text, xStart, baseline, brush);
    }

    private float measure(String text, float size, Typeface face, float tracking) {
        brush.reset();
        brush.setAntiAlias(true);
        brush.setTypeface(face);
        brush.setTextSize(size);
        brush.setLetterSpacing(tracking);
        brush.setTextAlign(Paint.Align.LEFT);
        return brush.measureText(text);
    }

    private float fontAscent(float size, Typeface face) {
        brush.reset();
        brush.setTypeface(face);
        brush.setTextSize(size);
        return brush.getFontMetrics().ascent;
    }

    private float fontDescent(float size, Typeface face) {
        brush.reset();
        brush.setTypeface(face);
        brush.setTextSize(size);
        return brush.getFontMetrics().descent;
    }

    private static int alpha(float value) {
        return Math.round(255f * Math.max(0f, Math.min(1f, value)));
    }

    @Override protected void onDetachedFromWindow() {
        if (focusAnim != null) focusAnim.cancel();
        if (introAnim != null) introAnim.cancel();
        super.onDetachedFromWindow();
    }
}
