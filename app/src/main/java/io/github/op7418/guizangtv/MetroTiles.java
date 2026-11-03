package io.github.op7418.guizangtv;

/**
 * The fixed Windows-8 style tile set. Edit labels, captions, colors and icons here;
 * the metro layout always shows exactly these tiles, independent of 首页内容.
 */
final class MetroTiles {
    static final class Tile {
        final String id, caption, label;
        final int accent, icon;
        final boolean big;
        Tile(String id, String caption, String label, int accent, int icon, boolean big) {
            this.id = id; this.caption = caption; this.label = label;
            this.accent = accent; this.icon = icon; this.big = big;
        }
    }

    private static final int RED = 0xFFE51400;
    private static final int BLUE = 0xFF0078D7;
    private static final int GRAPHITE = 0xFF34343C;
    private static final int TEAL = 0xFF00ABA9;
    private static final int ORANGE = 0xFFF09609;

    /** Three big HDMI sources, then two small system tiles. */
    static final Tile[] ALL = {
            new Tile("hdmi:1", "HDMI 1", "Switch", RED, R.drawable.mark_gamepad, true),
            new Tile("hdmi:2", "HDMI 2", "江苏有线电视", BLUE, R.drawable.mark_tv, true),
            new Tile("hdmi:3", "HDMI 3", "Apple TV", GRAPHITE, R.drawable.mark_appletv, true),
            new Tile("metro:tv-settings", "", "电视设置", TEAL, R.drawable.mark_gear_six, false),
            new Tile("com.mitv.tvhome", "", "小米原桌面", ORANGE, R.drawable.mark_house, false),
    };

    private MetroTiles() { }
}
