package de.tum.cit.fop.maze.level;

public enum Tiles {
    WALL,
    FLOOR,
    ENTRY,
    EXIT,
    TRAP,
    ENEMY,
    KEY;

    public boolean isWall() {
        return this == WALL;
    }

    public boolean isWalkable() {
        return this == FLOOR
                || this == ENTRY
                || this == EXIT
                || this == KEY
                || this == ENEMY;
    }

}

