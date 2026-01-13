package de.tum.cit.fop.maze.world;

import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;
import de.tum.cit.fop.maze.map.Maze;
import de.tum.cit.fop.maze.map.Tiles;
import de.tum.cit.fop.maze.obstacles.*;
import java.awt.Point;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

public class ObstaclePlacer {


    // Generate obstacles within the maze
    public List<Obstacle> placeObstacles(Maze maze, int obstacleCount, long seed) {
        List<Obstacle> result = new ArrayList<>();
        Random rng = new Random(seed);
        List<Point> floorCells = collectFloorCells(maze);
        Collections.shuffle(floorCells, rng);
        for (Point cell : floorCells) {
            if (result.size() >= obstacleCount) break;
            Obstacle obstacle = createRandomObstacle(cell, maze, rng);
            if (obstacle == null) continue;
            if (canPlaceObstacle(obstacle, maze)) {
                result.add(obstacle);
            }
        }
        return result;
    }


    // Find all valid floor cells
    private List<Point> collectFloorCells(Maze maze) {
        List<Point> cells = new ArrayList<>();
        for (int y = 1; y < maze.getHeight() - 1; y++) {
            for (int x = 1; x < maze.getWidth() - 1; x++) {
                if (maze.getTile(x, y) == Tiles.FLOOR) {
                    cells.add(new Point(x, y));
                }
            }
        }
        return cells;
    }


    // Instantiate a random obstacle type
    private Obstacle createRandomObstacle(Point cell, Maze maze, Random rng) {
        Vector2 worldPos = new Vector2(
                cell.x * maze.getTileSize() + (maze.getTileSize() / 4f),
                cell.y * maze.getTileSize() + (maze.getTileSize() / 4f)
        );
        int roll = rng.nextInt(3);
        return switch (roll) {
            case 0 -> new SpikeTrap(worldPos);
            case 1 -> new SlimeTrap(worldPos);
            case 2 -> new FloorHazard(worldPos);
            default -> null;
        };
    }


    // Verify the obstacle placement area
    private boolean canPlaceObstacle(Obstacle obstacle, Maze maze) {
        Rectangle box = obstacle.getCollisionBox();
        float tileSize = maze.getTileSize();
        int centerX = (int) ((box.x + box.width / 2) / tileSize);
        int centerY = (int) ((box.y + box.height / 2) / tileSize);
        for (int y = centerY - 1; y <= centerY + 1; y++) {
            for (int x = centerX - 1; x <= centerX + 1; x++) {
                if (!maze.inBounds(x, y)) {
                    return false;
                }
                if (maze.getTile(x, y) != Tiles.FLOOR) {
                    return false;
                }
            }
        }
        return true;
    }
}