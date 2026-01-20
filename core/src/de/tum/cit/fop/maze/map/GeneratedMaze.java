package de.tum.cit.fop.maze.map;

import java.util.List;

/**
 * Result object for maze generation that keeps
 * both the tile grid and the semantic room layout.
 */
public final class GeneratedMaze {

    private final Maze maze;
    private final List<Room> rooms;

    public GeneratedMaze(Maze maze, List<Room> rooms) {
        this.maze = maze;
        this.rooms = rooms;
    }

    public Maze getMaze() {
        return maze;
    }

    public List<Room> getRooms() {
        return rooms;
    }
}

