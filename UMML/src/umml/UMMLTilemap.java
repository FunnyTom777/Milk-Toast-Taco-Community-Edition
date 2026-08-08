package umml;

import java.util.Arrays;

/**
 * A tile map - a grid of little pictures (tiles) that together make up a
 * level, drawn straight from one sprite sheet. This is how 2D platformers
 * and top-down games build their ground, walls and floors (UMML 2.5).
 *
 * <p>A tile map has three things:
 * <ol>
 *   <li>a <b>tile sheet</b> - one picture with all the tiles laid out in a
 *       grid, every tile the same size, read left-to-right then
 *       top-to-bottom (tile 0 is the top-left one),</li>
 *   <li>a <b>grid</b> - a 2D array of tile numbers; the number says which
 *       tile from the sheet sits in that cell, and a <b>negative</b> number
 *       means "no tile here" (an empty cell),</li>
 *   <li>a <b>position</b> - where the top-left of the whole map sits in the
 *       world.</li>
 * </ol>
 *
 * <pre>
 * UMMLImage sheet = UMMLImage.load("assets/tiles.png");   // e.g. 4x4 tiles
 * UMMLTilemap map = new UMMLTilemap(sheet, 32, 32);       // 32x32 pixel tiles
 *
 * map.setTiles(new int[][] {
 *     {  0,  0,  1,  1,  1,  0 },
 *     {  2,  2,  2,  2,  2,  2 },   // the bottom row is ground
 * });
 *
 * map.setPosition(0, 400);          // push the map down into the world
 *
 * renderer.drawTilemap(map);        // draws only the tiles on screen
 * </pre>
 *
 * <p>Because the map draws straight from the sheet, it is very fast: the
 * renderer skips every tile outside the window, so a huge level costs about
 * the same to draw as a tiny one.
 *
 * <p>For collision, ask the map about individual cells:
 *
 * <pre>
 * boolean solid = map.isEmpty(map.toWorldCol(player.x()), map.toWorldRow(player.y()));
 * </pre>
 *
 * <p>Nothing throws here - every tile number is clamped to the sheet and any
 * out-of-range cell reads as empty.
 */
public final class UMMLTilemap {

    private UMMLImage sheet;
    private int tileWidth;
    private int tileHeight;
    private int[][] tiles = new int[0][0];
    private double x;
    private double y;

    /**
     * Creates a tile map with the given tile sheet and tile size, starting
     * empty (every cell is empty) at position (0,0).
     */
    public UMMLTilemap(UMMLImage sheet, int tileWidth, int tileHeight) {
        this.sheet = sheet;
        this.tileWidth = Math.max(1, tileWidth);
        this.tileHeight = Math.max(1, tileHeight);
    }

    /** Creates a tile map and fills it with the given grid in one go. */
    public UMMLTilemap(UMMLImage sheet, int tileWidth, int tileHeight, int[][] tiles) {
        this(sheet, tileWidth, tileHeight);
        setTiles(tiles);
    }

    // ========================================================================
    // The grid
    // ========================================================================

    /**
     * Replaces the whole grid. Each inner array is one row (top to bottom),
     * each number one cell (left to right). A negative number means empty.
     * Rows do not have to be the same length - shorter rows are treated as
     * empty at the right edge.
     */
    public UMMLTilemap setTiles(int[][] tiles) {
        if (tiles == null || tiles.length == 0) {
            this.tiles = new int[0][0];
            return this;
        }
        int rows = tiles.length;
        int cols = 0;
        for (int[] row : tiles) {
            if (row != null) {
                cols = Math.max(cols, row.length);
            }
        }
        int[][] copy = new int[rows][cols];
        for (int r = 0; r < rows; r++) {
            if (tiles[r] == null) {
                Arrays.fill(copy[r], -1);
            } else {
                System.arraycopy(tiles[r], 0, copy[r], 0, tiles[r].length);
                for (int c = tiles[r].length; c < cols; c++) {
                    copy[r][c] = -1;
                }
            }
        }
        this.tiles = copy;
        return this;
    }

    /** The grid itself (copied so the map can't be broken behind your back). */
    public int[][] tiles() {
        int rows = rows();
        int cols = cols();
        int[][] copy = new int[rows][cols];
        for (int r = 0; r < rows; r++) {
            System.arraycopy(tiles[r], 0, copy[r], 0, cols);
        }
        return copy;
    }

    /** Sets one cell. Column and row start at 0. Out-of-range cells grow the grid. */
    public UMMLTilemap setTile(int col, int row, int index) {
        if (col < 0 || row < 0) {
            return this;
        }
        int rows = Math.max(rows(), row + 1);
        int cols = Math.max(cols(), col + 1);
        if (rows != this.tiles.length || cols != this.tiles[0].length) {
            int[][] grown = new int[rows][cols];
            for (int r = 0; r < rows; r++) {
                Arrays.fill(grown[r], -1);
                if (r < this.tiles.length) {
                    System.arraycopy(this.tiles[r], 0, grown[r], 0, this.tiles[r].length);
                }
            }
            this.tiles = grown;
        }
        this.tiles[row][col] = index;
        return this;
    }

    /** The tile number at (col,row), or -1 if the cell is empty or out of range. */
    public int tileAt(int col, int row) {
        if (row < 0 || row >= rows() || col < 0 || col >= cols()) {
            return -1;
        }
        return tiles[row][col];
    }

    /** True if the cell is empty (a negative tile number, or out of range). */
    public boolean isEmpty(int col, int row) {
        return tileAt(col, row) < 0;
    }

    /** The number of columns (cells wide). */
    public int cols() {
        return tiles.length == 0 ? 0 : tiles[0].length;
    }

    /** The number of rows (cells tall). */
    public int rows() {
        return tiles.length;
    }

    /** The map's width in pixels (columns x tile width). */
    public int widthInPixels() {
        return cols() * tileWidth;
    }

    /** The map's height in pixels (rows x tile height). */
    public int heightInPixels() {
        return rows() * tileHeight;
    }

    // ========================================================================
    // Position
    // ========================================================================

    /** The world x of the map's top-left corner. */
    public double x() {
        return x;
    }

    /** The world y of the map's top-left corner. */
    public double y() {
        return y;
    }

    /** Moves the whole map so its top-left corner is at (x,y). */
    public UMMLTilemap setPosition(double x, double y) {
        this.x = x;
        this.y = y;
        return this;
    }

    /** Slides the whole map by (dx,dy) world pixels. */
    public UMMLTilemap move(double dx, double dy) {
        this.x += dx;
        this.y += dy;
        return this;
    }

    // ========================================================================
    // Coordinate helpers
    // ========================================================================

    /** The world x of the left edge of the given column. */
    public double tileWorldX(int col) {
        return x + col * tileWidth;
    }

    /** The world y of the top edge of the given row. */
    public double tileWorldY(int row) {
        return y + row * tileHeight;
    }

    /** Which column a world x falls in. Out-of-map values go negative or past cols(). */
    public int toWorldCol(double worldX) {
        return (int) Math.floor((worldX - x) / tileWidth);
    }

    /** Which row a world y falls in. Out-of-map values go negative or past rows(). */
    public int toWorldRow(double worldY) {
        return (int) Math.floor((worldY - y) / tileHeight);
    }

    // ========================================================================
    // The sheet
    // ========================================================================

    /** Swaps the tile sheet. Tile numbers now mean tiles in the new sheet. */
    public UMMLTilemap setSheet(UMMLImage sheet) {
        this.sheet = sheet;
        return this;
    }

    /** The picture the tiles are cut from. */
    public UMMLImage sheet() {
        return sheet;
    }

    /** Width of one tile, in pixels. */
    public int tileWidth() {
        return tileWidth;
    }

    /** Height of one tile, in pixels. */
    public int tileHeight() {
        return tileHeight;
    }

    /** How many tile columns fit across the sheet (for computing tile numbers). */
    public int sheetCols() {
        if (sheet == null) {
            return 0;
        }
        return Math.max(1, sheet.width() / tileWidth);
    }

    /** The column within the sheet where a tile number sits. */
    public int sheetCol(int tileIndex) {
        int cols = Math.max(1, sheetCols());
        int index = Math.max(0, tileIndex) % Math.max(1, cols * Math.max(1, sheetRows()));
        return index % cols;
    }

    /** How many tile rows fit down the sheet (for computing tile numbers). */
    public int sheetRows() {
        if (sheet == null) {
            return 0;
        }
        return Math.max(1, sheet.height() / tileHeight);
    }

    /** The row within the sheet where a tile number sits. */
    public int sheetRow(int tileIndex) {
        int cols = Math.max(1, sheetCols());
        int index = Math.max(0, tileIndex) % Math.max(1, cols * Math.max(1, sheetRows()));
        return index / cols;
    }

    @Override
    public String toString() {
        return "UMMLTilemap{" + cols() + "x" + rows() + " of " + tileWidth + "x" + tileHeight + " tiles}";
    }
}
