package io.github.op7418.guizangtv;

import java.util.ArrayList;
import java.util.List;

/**
 * Unit grid for the metro strip. A big tile covers 2x2 units, a small tile 1x1;
 * two small tiles stack in the same unit column. Pure layout math so it can be
 * unit tested without Android.
 */
final class MetroLayout {
    static final int UNIT = 111, GAP = 12, STRIDE = UNIT + GAP;

    static final class Cell {
        final int left, top, width, height;
        Cell(int left, int top, int width, int height) {
            this.left = left; this.top = top; this.width = width; this.height = height;
        }
    }

    static List<Cell> place(boolean[] big) {
        List<Cell> cells = new ArrayList<>();
        int column = 0, row = 0;
        for (boolean wide : big) {
            if (wide) {
                if (row == 1) { column++; row = 0; }
                cells.add(new Cell(column * STRIDE, 0, UNIT * 2 + GAP, UNIT * 2 + GAP));
                column += 2;
            } else {
                cells.add(new Cell(column * STRIDE, row * STRIDE, UNIT, UNIT));
                if (row == 0) row = 1;
                else { row = 0; column++; }
            }
        }
        return cells;
    }

    static int width(List<Cell> cells) {
        int max = 0;
        for (Cell cell : cells) max = Math.max(max, cell.left + cell.width);
        return max;
    }

    static int height(List<Cell> cells) {
        int max = 0;
        for (Cell cell : cells) max = Math.max(max, cell.top + cell.height);
        return max;
    }

    private MetroLayout() { }
}
