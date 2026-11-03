package io.github.op7418.guizangtv;

/**
 * Geometry for the Swiss home in a 1920 x 1080 design space. Pure math so the
 * focus model and row stacking can be unit tested without Android.
 */
final class SwissFrame {
    static final float WIDTH = 1920f, HEIGHT = 1080f;

    /** Page margins and the rules that carry the grid. */
    static final float MARGIN = 96f;
    static final float HEADER_RULE = 150f;
    static final float SPINE_X = 992f;
    static final float FOOTER_RULE = 984f;

    /** Right-hand signal-source column and its three rows. */
    static final float COLUMN_LEFT = 1040f;
    static final float COLUMN_RIGHT = 1824f;
    static final float ROWS_TOP = 300f;
    static final float ROW_HEIGHT = 170f;
    static final int SOURCE_COUNT = 3;

    /** Exit target, bottom right. */
    static final int EXIT = SOURCE_COUNT;
    static final float EXIT_LEFT = 1644f, EXIT_TOP = 904f;
    static final float EXIT_RIGHT = 1824f, EXIT_BOTTOM = 984f;

    /** Direction codes shared with the board and the tests. */
    static final int UP = 1, DOWN = 2, LEFT = 3, RIGHT = 4;

    static float rowTop(int source) {
        return ROWS_TOP + ROW_HEIGHT * source;
    }

    static float rowBottom(int source) {
        return rowTop(source) + ROW_HEIGHT;
    }

    /**
     * Remote-control focus model: Up/Down walk the three sources and then the
     * exit box; Left from the exit returns to the last source. Edges hold.
     */
    static int move(int focus, int direction) {
        int value = Math.max(0, Math.min(EXIT, focus));
        switch (direction) {
            case UP:
                if (value == EXIT) return SOURCE_COUNT - 1;
                return Math.max(0, value - 1);
            case DOWN:
                if (value == EXIT) return EXIT;
                return Math.min(EXIT, value + 1);
            case LEFT:
                if (value == EXIT) return SOURCE_COUNT - 1;
                return value;
            case RIGHT:
                return value;
            default:
                return value;
        }
    }

    /** True when the point lands on a focusable target; used for touch pads. */
    static int hit(double x, double y) {
        for (int i = 0; i < SOURCE_COUNT; i++) {
            if (x >= COLUMN_LEFT && x <= COLUMN_RIGHT && y >= rowTop(i) && y <= rowBottom(i)) return i;
        }
        if (x >= EXIT_LEFT && x <= EXIT_RIGHT && y >= EXIT_TOP && y <= EXIT_BOTTOM) return EXIT;
        return -1;
    }

    private SwissFrame() { }
}
