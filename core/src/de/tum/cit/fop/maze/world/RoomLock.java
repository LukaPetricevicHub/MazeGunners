package de.tum.cit.fop.maze.world;

import com.badlogic.gdx.math.Rectangle;
import de.tum.cit.fop.maze.level.Maze;
import de.tum.cit.fop.maze.level.Tiles;

import java.awt.Point;
import java.util.List;

public class RoomLock {

    // Attributes
    private Rectangle triggerArea;
    private List<Point> doorCells;
    private boolean isLocked = false;
    private boolean hasTriggered = false;


    // Constructor
    public RoomLock(Rectangle triggerArea, List<Point> doorCells) {
        this.triggerArea = triggerArea;
        this.doorCells = doorCells;
    }


    // Check trigger and locks room
    public void update(Rectangle playerBounds, Maze maze) {
        if (!hasTriggered && triggerArea.overlaps(playerBounds)) {
            lockRoom(maze);
            hasTriggered = true;
        }
    }


    // Close doors by placing walls
    private void lockRoom(Maze maze) {
        isLocked = true;

        for (Point cell : doorCells) {
            maze.setTile(cell.x, cell.y, Tiles.Wall);
        }
    }


    // Open doors after unlocking
    public void unlockRoom(Maze maze) {
        if (!isLocked) return;

        for (Point cell : doorCells) {
            maze.setTile(cell.x, cell.y, Tiles.Floor);
        }

        isLocked = false;
    }


    // Is a room locked?
    public boolean isLocked() {
        return isLocked;
    }
}
