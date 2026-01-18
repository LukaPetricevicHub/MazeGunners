package de.tum.cit.fop.maze.map;

public enum Tiles {
    WALL(0),
    FLOOR(1),
    SPAWN(2),
    EXIT(3),
    KEY(4),
    DOOR(5);

    private final int id;

    Tiles(int id) { this.id = id; }

    public int id() { return id; }

    public static Tiles fromId(int id) {
        for (Tiles t : values()) {
            if (t.id == id) return t;
        }
        // IMPORTANT: unknown ids should NOT become floor
        return WALL;
    }

    public boolean isWall() {
        return this == WALL;
    }

    public boolean isWalkable() {
        return this == FLOOR || this == SPAWN || this == EXIT || this == KEY || this == DOOR;
    }
}
