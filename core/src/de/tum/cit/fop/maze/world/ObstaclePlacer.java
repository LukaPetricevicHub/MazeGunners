package de.tum.cit.fop.maze.world;

import com.badlogic.gdx.math.Vector2;
import de.tum.cit.fop.maze.level.Maze;
import de.tum.cit.fop.maze.level.Tiles;
import de.tum.cit.fop.maze.obstacles.Obstacle;
import de.tum.cit.fop.maze.obstacles.SpikeTrap;

import java.awt.Point;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

public class ObstaclePlacer {

    // Place obstacles randomly on floor tiles
    public List<Obstacle> placeObstacles(Maze maze, int obstacleCount, long seed) {
        List<Obstacle> placedObstacles = new ArrayList<>();
        List<Point> availableFloorCells = new ArrayList<>();
        Random random = new Random(seed);

        for (int y = 1; y < maze.getHeight() - 1; y++) {
            for (int x = 1; x < maze.getWidth() - 1; x++) {
                if (maze.getTile(x, y) == Tiles.FLOOR) {
                    availableFloorCells.add(new Point(x, y));
                }
            }
        }

        Collections.shuffle(availableFloorCells, random);

        int spawnCount = Math.min(obstacleCount, availableFloorCells.size());
        for (int i = 0; i < spawnCount; i++) {
            Point cell = availableFloorCells.get(i);
            Vector2 worldPosition =
                    new Vector2(cell.x * maze.getTileSize(), cell.y * maze.getTileSize());

            placedObstacles.add(new SpikeTrap(worldPosition));
        }

        return placedObstacles;
    }
}
