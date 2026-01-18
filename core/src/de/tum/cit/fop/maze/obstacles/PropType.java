package de.tum.cit.fop.maze.obstacles;

public enum PropType {
    BOX(true, 32, 32, "props/box.png"),
    BARREL(true, 32, 32, "props/barrel.png"),
    CHAIR(true, 32, 32, "props/chair.png"),
    SHELF(true, 64, 32, "props/shelf.png");

    public final boolean blocks;
    public final int wPx;
    public final int hPx;
    public final String texturePath;

    PropType(boolean blocks, int wPx, int hPx, String texturePath) {
        this.blocks = blocks;
        this.wPx = wPx;
        this.hPx = hPx;
        this.texturePath = texturePath;
    }
}
