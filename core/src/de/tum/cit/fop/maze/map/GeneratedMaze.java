package de.tum.cit.fop.maze.map;

import java.util.List;

/**
 * Result object for maze generation that keeps
 * both the tile grid and the semantic room layout.
 */
public final class GeneratedMaze {

    /** Generated maze instance. */
    private final Maze maze;
    /** Rooms produced during generation. */
    private final List<Room> rooms;

    /**
     * Creates a generated maze result.
     *
     * @param maze Generated maze
     * @param rooms Generated rooms
     */
    public GeneratedMaze(Maze maze, List<Room> rooms) {
        this.maze = maze;
        this.rooms = rooms;
    }

    /**
     * Retrieves the generated maze.
     *
     * @return Maze instance
     */
    public Maze getMaze() {
        return maze;
    }

    /**
     * Retrieves the generated rooms.
     *
     * @return Room list
     */
    public List<Room> getRooms() {
        return rooms;
    }
}

