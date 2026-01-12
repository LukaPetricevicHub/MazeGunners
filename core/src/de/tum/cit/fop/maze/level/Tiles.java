package de.tum.cit.fop.maze.level;

public enum Tiles {
    WALL(false),
    FLOOR(true),
    ENTRY(true),
    EXIT(true),
    KEY(true),
    TRAP(true),
    ENEMY(true);

    private final boolean walkable;

    Tiles(boolean walkable){
        this.walkable = walkable;
    }

    public boolean isWalkable(){
        return walkable;
    }

    public boolean isWall(){
        return this == WALL;
    }

}
