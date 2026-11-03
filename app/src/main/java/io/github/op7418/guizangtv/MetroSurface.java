package io.github.op7418.guizangtv;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.view.Gravity;
import android.view.View;
import android.view.animation.LinearInterpolator;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import java.util.ArrayList;
import java.util.List;

/**
 * Windows-8 style start screen: three big HDMI tiles and two small system tiles
 * on a panned wallpaper, with per-tile photo sheen and focus motion.
 */
final class MetroSurface extends HomeLayer {
    private final ImageView backdrop;
    private final List<MetroTile> tiles = new ArrayList<>();
    private ValueAnimator pan;
    private int materialRequest;
    private int selected;

    MetroSurface(Context c, HomeAppearance style, Drawable wallpaper, int selected,
                 boolean preview, Actions actions) {
        super(c, style, preview, actions);
        this.selected = Math.max(0, Math.min(MetroTiles.ALL.length - 1, selected));

        if (style.get("background") == 0) {
            backdrop = new ImageView(c);
            backdrop.setScaleType(ImageView.ScaleType.CENTER_CROP);
            backdrop.setImageDrawable(wallpaper);
            addView(backdrop, new LayoutParams(-1, -1));
        } else {
            backdrop = null;
            setBackgroundColor(style.light() ? 0xFFF0EFE9 : 0xFF11151B);
            if (preview) previewPhoto.setImageDrawable(wallpaper);
        }
        View shade = new View(c);
        shade.setBackground(new GradientDrawable(GradientDrawable.Orientation.LEFT_RIGHT,
                style.light()
                        ? new int[]{0x44FFFFFF, 0x22FFFFFF, 0x55FFFFFF}
                        : new int[]{0xCC0B1016, 0x5C0B1016, 0xA60B1016}));
        addView(shade, new LayoutParams(-1, -1));

        LinearLayout timeRow = new LinearLayout(c);
        timeRow.setGravity(Gravity.CENTER_VERTICAL);
        clock.setTypeface(style.typeface());
        timeRow.addView(clock, new LinearLayout.LayoutParams(-2, -1));
        weather.setTypeface(style.typeface());
        LinearLayout.LayoutParams wp = new LinearLayout.LayoutParams(-2, -1);
        wp.leftMargin = dp(20);
        timeRow.addView(weather, wp);
        LayoutParams cp = new LayoutParams(-2, dp(45), Gravity.TOP | Gravity.START);
        cp.topMargin = dp(42);
        cp.leftMargin = dp(56);
        addView(timeRow, cp);

        boolean[] sizes = new boolean[MetroTiles.ALL.length];
        for (int i = 0; i < sizes.length; i++) sizes[i] = MetroTiles.ALL[i].big;
        List<MetroLayout.Cell> cells = MetroLayout.place(sizes);
        FrameLayout grid = new FrameLayout(c);
        grid.setClipChildren(false);
        grid.setClipToPadding(false);
        addView(grid, new LayoutParams(dp(MetroLayout.width(cells)), dp(MetroLayout.height(cells)), Gravity.CENTER));
        for (int i = 0; i < cells.size(); i++) {
            MetroLayout.Cell cell = cells.get(i);
            MetroTiles.Tile spec = MetroTiles.ALL[i];
            final MetroTile tile = new MetroTile(c, spec, style.typeface(), preview);
            LayoutParams p = new LayoutParams(dp(cell.width), dp(cell.height), Gravity.TOP | Gravity.START);
            p.leftMargin = dp(cell.left);
            p.topMargin = dp(cell.top);
            grid.addView(tile, p);
            tile.setFieldOffset(.5f + (i - 2) * .16f);
            tiles.add(tile);
            apps.add(tile);
            wire(tile, () -> actions.open(spec.id));
            final int index = i;
            tile.setOnFocusChangeListener((v, focus) -> {
                tile.setFocused(focus);
                if (!focus) return;
                this.selected = index;
                actions.selected(index);
                for (View control : controls)
                    control.setNextFocusUpId(tile.getId());
            });
        }

        LinearLayout side = buildControls();
        LayoutParams sp = new LayoutParams(-2, -2, Gravity.BOTTOM | Gravity.END);
        sp.rightMargin = dp(64);
        sp.bottomMargin = dp(40);
        addView(side, sp);
        wireFocus();
        if (preview) {
            setDescendantFocusability(FOCUS_BLOCK_DESCENDANTS);
            tiles.get(this.selected).showFocus();
        }
        wallpaper(wallpaper);
    }

    private LinearLayout buildControls() {
        LinearLayout side = new LinearLayout(getContext());
        side.setOrientation(LinearLayout.HORIZONTAL);
        side.setGravity(Gravity.END);
        String[] names = {"全部应用", "其他信号源", "设置", "Codex"};
        String[] ids = {"apps", "inputs", "settings", "assistant"};
        int count = HomeAssistant.enabled(getContext()) ? names.length : names.length - 1;
        for (int i = 0; i < count; i++) {
            final int index = i;
            LinearLayout line = new LinearLayout(getContext());
            line.setGravity(Gravity.END | Gravity.CENTER_VERTICAL);
            TextView label = text(names[i], 16);
            label.setTypeface(style.typeface());
            int mode = style.get("controls");
            line.addView(label, new LinearLayout.LayoutParams(-2, -2));
            if (mode != 0) {
                ImageView icon = new ImageView(getContext());
                icon.setImageDrawable(HomeIcons.get(getContext(), "control:" + ids[i], style.ink()));
                LinearLayout.LayoutParams ip = new LinearLayout.LayoutParams(dp(23), dp(23));
                ip.leftMargin = dp(10);
                line.addView(icon, ip);
            }
            line.setPadding(dp(12), 0, dp(12), 0);
            side.addView(line, new LinearLayout.LayoutParams(-2, dp(40)));
            controls.add(line);
            wire(line, () -> actions.control(index));
            line.setContentDescription(names[i]);
            paintControl(line, label, false, mode == 1);
            line.setOnFocusChangeListener((v, focus) -> paintControl(line, label, focus, mode == 1));
        }
        return side;
    }

    private void paintControl(View v, TextView label, boolean focused, boolean iconsOnly) {
        v.setAlpha(focused ? 1f : style.faded());
        label.setVisibility(iconsOnly && !focused ? View.GONE : View.VISIBLE);
        v.setBackground(focused
                ? WallpaperMaterial.rounded(style.light() ? 0x33FFFFFF : 0x2EFFFFFF, dp(10))
                : null);
    }

    private void wireFocus() {
        int n = tiles.size();
        for (int i = 0; i < n; i++) {
            MetroTile tile = tiles.get(i);
            tile.setNextFocusLeftId(tiles.get((i + n - 1) % n).getId());
            tile.setNextFocusRightId(tiles.get((i + 1) % n).getId());
            View up = tile, down = tile;
            if (!MetroTiles.ALL[i].big) {
                if (i > 0 && !MetroTiles.ALL[i - 1].big) up = tiles.get(i - 1);
                if (i + 1 < n && !MetroTiles.ALL[i + 1].big) down = tiles.get(i + 1);
            }
            if (i == n - 1 && !controls.isEmpty()) down = controls.get(0);
            tile.setNextFocusUpId(up.getId());
            tile.setNextFocusDownId(down.getId());
        }
        for (int i = 0; i < controls.size(); i++) {
            View line = controls.get(i);
            line.setNextFocusUpId(tiles.get(Math.min(selected, n - 1)).getId());
            line.setNextFocusDownId(line.getId());
            line.setNextFocusLeftId(i == 0 ? line.getId() : controls.get(i - 1).getId());
            line.setNextFocusRightId(i == controls.size() - 1 ? line.getId() : controls.get(i + 1).getId());
        }
    }

    @Override void focusApp() {
        if (tiles.isEmpty()) return;
        selected = Math.max(0, Math.min(tiles.size() - 1, selected));
        tiles.get(selected).requestFocus();
    }

    @Override void wallpaper(Drawable drawable) {
        if (style.get("background") != 0) return;
        if (preview) previewPhoto.setImageDrawable(drawable);
        if (backdrop != null) backdrop.setImageDrawable(drawable);
        final int request = ++materialRequest;
        WallpaperMaterial.request(drawable, 28, field -> {
            if (request != materialRequest) return;
            for (MetroTile tile : tiles) tile.setField(field);
        });
    }

    @Override protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        if (preview) return;
        if (backdrop != null && pan == null) {
            pan = ValueAnimator.ofFloat(0f, 1f);
            pan.setDuration(96000);
            pan.setRepeatCount(ValueAnimator.INFINITE);
            pan.setRepeatMode(ValueAnimator.REVERSE);
            pan.setInterpolator(new LinearInterpolator());
            pan.addUpdateListener(a -> {
                float progress = (Float) a.getAnimatedValue();
                float scale = 1.05f + .05f * progress;
                backdrop.setScaleX(scale);
                backdrop.setScaleY(scale);
                backdrop.setTranslationX(dp(16) * progress - dp(8));
                backdrop.setTranslationY(dp(6) * progress - dp(3));
            });
            pan.start();
        }
        for (int i = 0; i < tiles.size(); i++) tiles.get(i).enter(i);
    }

    @Override protected void onWindowVisibilityChanged(int visibility) {
        super.onWindowVisibilityChanged(visibility);
        if (pan == null) return;
        if (visibility == VISIBLE) pan.resume();
        else pan.pause();
    }

    @Override protected void onDetachedFromWindow() {
        if (pan != null) {
            pan.cancel();
            pan = null;
        }
        super.onDetachedFromWindow();
    }
}
