package io.github.op7418.guizangtv;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

/** The Swiss home's focus model and row stacking, independent of Android. */
public final class SwissFrameTest {
    @Test public void rowsStackBelowTheHeader() {
        assertEquals(300f, SwissFrame.rowTop(0), 0f);
        assertEquals(470f, SwissFrame.rowTop(1), 0f);
        assertEquals(640f, SwissFrame.rowTop(2), 0f);
        assertEquals(810f, SwissFrame.rowBottom(2), 0f);
        assertEquals(SwissFrame.EXIT_TOP, SwissFrame.FOOTER_RULE - 80f, 0f);
    }

    @Test public void columnsSitInsideTheMargins() {
        assertEquals(SwissFrame.MARGIN, 96f, 0f);
        assertEquals(SwissFrame.WIDTH - SwissFrame.MARGIN, SwissFrame.COLUMN_RIGHT, 0f);
        assertEquals(SwissFrame.EXIT_RIGHT, SwissFrame.COLUMN_RIGHT, 0f);
        assertEquals(0f, SwissFrame.SPINE_X - SwissFrame.COLUMN_LEFT + 48f, 0f);
    }

    @Test public void downWalksTheSourcesThenTheExit() {
        assertEquals(1, SwissFrame.move(0, SwissFrame.DOWN));
        assertEquals(2, SwissFrame.move(1, SwissFrame.DOWN));
        assertEquals(SwissFrame.EXIT, SwissFrame.move(2, SwissFrame.DOWN));
        assertEquals(SwissFrame.EXIT, SwissFrame.move(SwissFrame.EXIT, SwissFrame.DOWN));
    }

    @Test public void upReturnsFromTheExitToTheLastSource() {
        assertEquals(2, SwissFrame.move(SwissFrame.EXIT, SwissFrame.UP));
        assertEquals(1, SwissFrame.move(2, SwissFrame.UP));
        assertEquals(0, SwissFrame.move(0, SwissFrame.UP));
        assertEquals(2, SwissFrame.move(SwissFrame.EXIT, SwissFrame.LEFT));
    }

    @Test public void focusNeverLeavesTheBoard() {
        int focus = 0;
        for (int i = 0; i < 40; i++) {
            focus = SwissFrame.move(focus, i % 2 == 0 ? SwissFrame.DOWN : SwissFrame.UP);
            if (focus < 0 || focus > SwissFrame.EXIT) throw new AssertionError("focus " + focus);
        }
        assertEquals(0, SwissFrame.move(0, SwissFrame.LEFT));
        assertEquals(SwissFrame.EXIT, SwissFrame.move(SwissFrame.EXIT, SwissFrame.RIGHT));
    }

    @Test public void hitTestFindsRowsAndExit() {
        assertEquals(0, SwissFrame.hit(1100, 320));
        assertEquals(1, SwissFrame.hit(1500, 600));
        assertEquals(2, SwissFrame.hit(1800, 800));
        assertEquals(SwissFrame.EXIT, SwissFrame.hit(1700, 940));
        assertEquals(-1, SwissFrame.hit(500, 900));
    }
}
