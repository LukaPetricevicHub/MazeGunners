package de.tum.cit.fop.maze.map;

/**
 * Represents the different types of tiles that can appear in the maze.
 *
 * Each tile has a numeric id which is used when loading or saving mazes
 * from files. Tiles also define basic properties such as whether they
 * block movement or can be walked on.
 */
public enum Tiles {

    WALL(0),
    FLOOR(1),
    SPAWN(2),
    EXIT(3),
    KEY(4),
    DOOR(5);

    /** Numeric tile id used in save files. */
    public final int id;

    Tiles(int id) {
        this.id = id;
    }

    /**
     * Checks whether this tile is a wall.
     *
     * @return true if the tile is a wall, false otherwise
     */
    public boolean isWall() {
        return this == WALL;
    }

    /**
     * Checks whether this tile can be walked on.
     *
     * @return true if the tile is not a wall
     */
    public boolean isWalkable() {
        return this != WALL;
    }

    /**
     * Converts a numeric tile id into a {@link Tiles} value.
     *
     * @param id numeric tile id
     * @return matching tile type
     * @throws IllegalArgumentException if the id is unknown
     */
    public static Tiles fromId(int id) {
        for (Tiles t : values()) {
            if (t.id == id) return t;
        }
        throw new IllegalArgumentException("Unknown tile id: " + id);
    }
}
