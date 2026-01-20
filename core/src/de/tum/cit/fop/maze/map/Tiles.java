package de.tum.cit.fop.maze.map;

public enum Tiles {

    WALL(0),
    FLOOR(1),
    SPAWN(2),
    EXIT(3),
    KEY(4),
    DOOR(5);

    public final int id;

    Tiles(int id) {
        this.id = id;
    }

    public boolean isWall() {
        return this == WALL;
    }

    public boolean isWalkable() {
        return this != WALL;
    }

    public static Tiles fromId(int id) {
        for (Tiles t : values()) {
            if (t.id == id) return t;
        }
        throw new IllegalArgumentException("Unknown tile id: " + id);
    }
}
