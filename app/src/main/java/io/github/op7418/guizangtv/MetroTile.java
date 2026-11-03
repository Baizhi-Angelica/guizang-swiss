package io.github.op7418.guizangtv;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Shader;
import android.graphics.drawable.Drawable;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.View;
import android.view.animation.DecelerateInterpolator;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

/**
 * One flat Windows-8 tile: accent block, a photographic sheen that pans with
 * focus, a light sweep when focused, and a small entrance lift.
 */
final class MetroTile extends FrameLayout {
    private final Face face;
    private final View content;
    private final boolean preview;
    private ValueAnimator focusAnim, sweepAnim;
    private float focus;

    MetroTile(Context c, MetroTiles.Tile spec, android.graphics.Typeface typeface, boolean preview) {
        super(c);
        this.preview = preview;
        face = new Face(spec.accent, dp(2));
        setBackground(face);
        setClipChildren(false);
        setContentDescription(spec.caption.isEmpty() ? spec.label : spec.caption + " " + spec.label);

        ImageView glyph = new ImageView(c);
        Drawable mark = c.getDrawable(spec.icon).mutate();
        mark.setTint(0xFFFFFFFF);
        glyph.setImageDrawable(mark);
        int glyphSize = dp(spec.big ? 54 : 26);
        LayoutParams gp = new LayoutParams(glyphSize, glyphSize, Gravity.TOP | Gravity.START);
        gp.leftMargin = dp(spec.big ? 18 : 12);
        gp.topMargin = dp(spec.big ? 18 : 12);
        addView(glyph, gp);

        LinearLayout block = new LinearLayout(c);
        block.setOrientation(LinearLayout.VERTICAL);
        block.setGravity(Gravity.BOTTOM);
        TextView caption = new TextView(c);
        caption.setText(spec.caption);
        caption.setTextSize(13);
        caption.setTextColor(0xE6FFFFFF);
        caption.setTypeface(typeface);
        caption.setIncludeFontPadding(false);
        caption.setSingleLine(true);
        caption.setVisibility(spec.caption.isEmpty() ? GONE : VISIBLE);
        TextView label = new TextView(c);
        label.setText(spec.label);
        label.setTextSize(spec.big ? 29 : 15);
        label.setTextColor(0xFFFFFFFF);
        label.setTypeface(typeface);
        label.setIncludeFontPadding(false);
        label.setSingleLine(true);
        label.setMaxLines(1);
        label.setEllipsize(TextUtils.TruncateAt.END);
        block.addView(caption, new LinearLayout.LayoutParams(-1, -2));
        block.addView(label, new LinearLayout.LayoutParams(-1, -2));
        LayoutParams bp = new LayoutParams(-1, -2, Gravity.BOTTOM | Gravity.START);
        bp.leftMargin = dp(spec.big ? 18 : 12);
        bp.rightMargin = dp(10);
        bp.bottomMargin = dp(spec.big ? 15 : 10);
        addView(block, bp);
        content = block;
    }

    void setField(Bitmap bitmap) {
        face.field = bitmap;
        face.invalidateSelf();
    }

    /** Sample a different slice of the wallpaper for each tile. */
    void setFieldOffset(float value) {
        face.offset = Math.max(0f, Math.min(1f, value));
        face.invalidateSelf();
    }

    void setFocused(boolean value) {
        if (focusAnim != null) focusAnim.cancel();
        if (value) {
            bringToFront();
            if (!preview) startSweep();
        }
        if (preview) { applyFocus(value ? 1f : 0f); return; }
        focusAnim = ValueAnimator.ofFloat(focus, value ? 1f : 0f);
        focusAnim.setDuration(value ? 200 : 160);
        focusAnim.setInterpolator(new DecelerateInterpolator());
        focusAnim.addUpdateListener(a -> applyFocus((Float) a.getAnimatedValue()));
        focusAnim.start();
    }

    /** Settings preview blocks focus, so the selected tile is painted directly. */
    void showFocus() {
        applyFocus(1f);
    }

    void enter(int order) {
        if (preview) return;
        setAlpha(0f);
        setTranslationY(dp(16));
        animate().alpha(1f).translationY(0f).setStartDelay(order * 55L)
                .setDuration(420).setInterpolator(new DecelerateInterpolator()).start();
    }

    private void applyFocus(float value) {
        focus = value;
        float scale = 1f + .035f * value;
        setScaleX(scale);
        setScaleY(scale);
        content.setTranslationY(-dp(2) * value);
        face.focus = value;
        face.invalidateSelf();
    }

    private void startSweep() {
        if (sweepAnim != null) sweepAnim.cancel();
        face.sweep = 0f;
        sweepAnim = ValueAnimator.ofFloat(0f, 1f);
        sweepAnim.setDuration(720);
        sweepAnim.setStartDelay(60);
        sweepAnim.setInterpolator(new DecelerateInterpolator());
        sweepAnim.addUpdateListener(a -> {
            face.sweep = (Float) a.getAnimatedValue();
            face.invalidateSelf();
        });
        sweepAnim.start();
    }

    @Override protected void onDetachedFromWindow() {
        if (focusAnim != null) focusAnim.cancel();
        if (sweepAnim != null) sweepAnim.cancel();
        animate().cancel();
        super.onDetachedFromWindow();
    }

    private int dp(float value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }

    /** Flat accent face with a cropped photo layer, bottom shade and a diagonal sweep. */
    private static final class Face extends Drawable {
        private final int accent;
        private final float radius;
        private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.FILTER_BITMAP_FLAG);
        private final Paint fill = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Path clip = new Path();
        private final RectF bounds = new RectF(), target = new RectF();
        private final Rect source = new Rect();
        private Bitmap field;
        private float focus, sweep = 1f, offset = .5f;
        private Shader shader, sheen;

        Face(int accent, float radius) {
            this.accent = accent; this.radius = radius;
        }

        @Override protected void onBoundsChange(Rect rect) {
            shader = new LinearGradient(0, rect.top + rect.height() * .34f, 0, rect.bottom,
                    0x00000000, 0x99000000, Shader.TileMode.CLAMP);
            float band = Math.max(1f, rect.width() * .34f);
            sheen = new LinearGradient(0, 0, band, 0,
                    new int[]{0x00FFFFFF, 0x30FFFFFF, 0x00FFFFFF},
                    new float[]{0f, .5f, 1f}, Shader.TileMode.CLAMP);
        }

        @Override public void draw(Canvas canvas) {
            Rect rect = getBounds();
            if (rect.isEmpty()) return;
            bounds.set(rect);
            clip.reset();
            clip.addRoundRect(bounds, radius, radius, Path.Direction.CW);
            int save = canvas.save();
            canvas.clipPath(clip);
            canvas.drawColor(accent);
            if (field != null && !field.isRecycled() && field.getWidth() > 0 && field.getHeight() > 0) {
                float ratio = bounds.width() / bounds.height();
                float cropW = field.getWidth() * .66f, cropH = field.getHeight() * .66f;
                if (cropW / cropH > ratio) cropW = cropH * ratio;
                else cropH = cropW / ratio;
                float roomX = Math.max(0f, field.getWidth() - cropW);
                float roomY = Math.max(0f, field.getHeight() - cropH);
                float x = clamp(offset) * roomX + focus * roomX * .14f;
                float y = roomY * .5f;
                source.set((int) x, (int) y, (int) (x + cropW), (int) (y + cropH));
                target.set(bounds);
                paint.setAlpha(94);
                canvas.drawBitmap(field, source, target, paint);
            }
            if (shader != null) {
                fill.setShader(shader);
                canvas.drawRect(bounds, fill);
            }
            if (sweep < 1f && sheen != null) {
                canvas.save();
                canvas.rotate(-16, bounds.centerX(), bounds.centerY());
                float band = Math.max(1f, bounds.width() * .34f);
                float x = bounds.left - band + sweep * (bounds.width() + band);
                canvas.translate(x, 0);
                fill.setShader(sheen);
                canvas.drawRect(0, bounds.top - bounds.height(), band, bounds.bottom + bounds.height(), fill);
                canvas.restore();
            }
            canvas.restoreToCount(save);
        }

        private static float clamp(float value) {
            return value < 0f ? 0f : value > 1f ? 1f : value;
        }

        @Override public void setAlpha(int alpha) {
            // View-level alpha already fades the whole tile; keep the face fully opaque.
        }

        @Override public void setColorFilter(ColorFilter filter) {
            fill.setColorFilter(filter);
            invalidateSelf();
        }

        @Override public int getOpacity() {
            return PixelFormat.TRANSLUCENT;
        }
    }
}
