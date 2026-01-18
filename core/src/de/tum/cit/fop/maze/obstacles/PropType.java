package de.tum.cit.fop.maze.obstacles;

public enum PropType {

    CRATE (0, 1, 1, 1, true),
    BARREL(2, 1, 1, 1, true),
    CHAIR (2, 2, 1, 1, true),
    SHELF (0
            , 2, 2, 1, true),
    TABLE (2, 2, 2, 1, true);

    public final int col;
    public final int row;
    public final int tilesW;
    public final int tilesH;
    public final boolean blocks;

    PropType(int col, int row, int tilesW, int tilesH, boolean blocks) {
        this.col = col;
        this.row = row;
        this.tilesW = tilesW;
        this.tilesH = tilesH;
        this.blocks = blocks;
    }
}
