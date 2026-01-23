package de.tum.cit.fop.maze.world;

import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;
import de.tum.cit.fop.maze.map.Maze;
import de.tum.cit.fop.maze.map.Room;
import de.tum.cit.fop.maze.map.Tiles;
import de.tum.cit.fop.maze.obstacles.*;

import java.awt.Point;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

public class ObstaclePlacer {

    /**
     * Places exactly trapCount traps (if possible).
     * Traps will NOT spawn inside excludedRoom (your SPAWN room).
     */
    public List<Obstacle> placeObstacles(Maze maze, int trapCount, long seed, Room excludedRoom) {
        List<Obstacle> placementResult = new ArrayList<>();
        Random randomGen = new Random(seed);

        List<Point> validFloorCells = getFloorPoints(maze);
        Collections.shuffle(validFloorCells, randomGen);

        for (Point cell : validFloorCells) {
            if (placementResult.size() >= trapCount) break;

            // EXCLUDE SPAWN ROOM (tile coords)
            if (excludedRoom != null && isInsideRoom(excludedRoom, cell.x, cell.y)) {
                continue;
            }

            Obstacle trap = spawnRandomTrap(cell, maze, randomGen);
            if (trap == null) continue;

            if (isValidArea(trap, maze)) {
                placementResult.add(trap);
            }
        }

        return placementResult;
    }

    private boolean isInsideRoom(Room r, int tx, int ty) {
        return tx >= r.x() && tx < r.x() + r.w()
                && ty >= r.y() && ty < r.y() + r.h();
    }

    private List<Point> getFloorPoints(Maze maze) {
        List<Point> cellList = new ArrayList<>();
        for (int y = 1; y < maze.getHeight() - 1; y++) {
            for (int x = 1; x < maze.getWidth() - 1; x++) {
                if (maze.getTile(x, y) == Tiles.FLOOR) {
                    cellList.add(new Point(x, y));
                }
            }
        }
        return cellList;
    }

    private Obstacle spawnRandomTrap(Point cell, Maze maze, Random rng) {
        float size = maze.getTileSize();
        Vector2 spawnPos = new Vector2(
                cell.x * size + (size / 4f),
                cell.y * size + (size / 4f)
        );

        int roll = rng.nextInt(3);
        return switch (roll) {
            case 0 -> new SpikeTrap(spawnPos);
            case 1 -> new SlimeTrap(spawnPos);
            case 2 -> new FloorHazard(spawnPos);
            default -> null;
        };
    }

    private boolean isValidArea(Obstacle obstacle, Maze maze) {
        Rectangle hitBox = obstacle.getCollisionBox();
        float ts = maze.getTileSize();

        int gridX = (int) ((hitBox.x + hitBox.width / 2f) / ts);
        int gridY = (int) ((hitBox.y + hitBox.height / 2f) / ts);

        for (int dy = gridY - 1; dy <= gridY + 1; dy++) {
            for (int dx = gridX - 1; dx <= gridX + 1; dx++) {
                if (!maze.inBounds(dx, dy)) return false;
                if (maze.getTile(dx, dy) != Tiles.FLOOR) return false;
            }
        }
        return true;
    }
}
