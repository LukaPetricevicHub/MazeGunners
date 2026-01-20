package de.tum.cit.fop.maze.obstacles;

public enum PropType {

    BARREL("BARREL.png", 1, 1, true, 35),
    CRATE ("CRATE.png",  1, 1, true, 35),
    SKELETON ("SKELETON.png",  1, 1, true, 35),
    ROBOT ("ROBOT.png",  1, 1, true, 35);

    public final String texturePath;
    public final int tilesW;
    public final int tilesH;
    public final boolean blocksMovement;

    // selection weight (higher = more common)
    public final int weight;

    PropType(String texturePath, int tilesW, int tilesH, boolean blocksMovement, int weight) {
        this.texturePath = texturePath;
        this.tilesW = tilesW;
        this.tilesH = tilesH;
        this.blocksMovement = blocksMovement;
        this.weight = weight;
    }
}