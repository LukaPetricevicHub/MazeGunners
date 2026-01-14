package de.tum.cit.fop.maze.map;

public enum Tiles {
    WALL,
    FLOOR,
    SPAWN,
    EXIT,
    KEY;

    public boolean isWall() {
        return this == WALL;
    }

    public boolean isWalkable() {
        return this == FLOOR
                || this == SPAWN
                || this == EXIT
                || this == KEY;
    }

}

