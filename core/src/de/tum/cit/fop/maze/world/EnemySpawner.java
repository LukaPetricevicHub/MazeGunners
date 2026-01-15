package de.tum.cit.fop.maze.world;

import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.Array;
import de.tum.cit.fop.maze.enemies.*;
import de.tum.cit.fop.maze.map.Maze;
import de.tum.cit.fop.maze.map.Tiles;
import de.tum.cit.fop.maze.player.Player;
import java.awt.Point;
import java.util.*;

public class EnemySpawner {
    // Attributes
    private static final float AVG_ENEMY_W = 32f;
    private static final float AVG_ENEMY_H = 64f;


    // Create and place maze enemies
    public List<Enemy> spawnEnemies(Maze maze, Player targetPlayer, int enemyCount, long seed) {
        List<Enemy> spawnedEnemies = new ArrayList<>();
        List<Point> spawnCandidates = new ArrayList<>();
        Random rngGen = new Random(seed);

        for (int y = 1; y < maze.getHeight() - 1; y++) {
            for (int x = 1; x < maze.getWidth() - 1; x++) {

                if(maze.getTile(x, y) == Tiles.FLOOR) {
                    spawnCandidates.add(new Point(x, y));
                }
            }
        }

        Collections.shuffle(spawnCandidates, rngGen);

        for (Point cell : spawnCandidates) {
            if (spawnedEnemies.size() >= enemyCount)
                break;


            float worldX = cell.x * maze.getTileSize();
            float worldY = cell.y * maze.getTileSize();

            if(!isSafeToSpawn(worldX, worldY, maze)) {
                continue;
            }

            Vector2 spawnLocation = new Vector2(worldX, worldY);
            int enemySelection = rngGen.nextInt(3);

            switch (enemySelection) {
                case 0 -> spawnedEnemies.add(new MutantWorker(spawnLocation, 175f, targetPlayer));
                case 1 -> spawnedEnemies.add(new RobotGuard(spawnLocation, 60f, createPatrolRoute(maze, cell), targetPlayer));
                case 2 -> spawnedEnemies.add(new FlyingDrone(spawnLocation, 110f, createPatrolRoute(maze, cell), targetPlayer));
            }
        }

        return spawnedEnemies;
    }


    // Validate specific enemy spawn location
    private boolean isSafeToSpawn(float x, float y, Maze maze) {
        float padding = 2f;

        float left   = x + padding;
        float right  = x + AVG_ENEMY_W - padding;
        float bottom = y + padding;
        float top    = y + AVG_ENEMY_H - padding;

        return !maze.isWallWorld(left, bottom) && !maze.isWallWorld(right, bottom)
                && !maze.isWallWorld(left, top) && !maze.isWallWorld(right, top);
    }


    // Create movement path for AI
    private Array<Vector2> createPatrolRoute(Maze maze, Point startCell) {
        Array<Vector2> patrolRoute = new Array<>();
        float tileSize = maze.getTileSize();

        Vector2 startPoint = new Vector2((startCell.x + 0.5f) * tileSize, (startCell.y + 0.5f) * tileSize);
        patrolRoute.add(startPoint);

        int[][] offsets = {{3, 0}, {-3, 0}, {0, 3}, {0, -3}};

        for (int[] move : offsets) {
            int targetX = startCell.x + move[0];
            int targetY = startCell.y + move[1];

            if(!maze.inBounds(targetX, targetY))
                continue;

            if (maze.getTile(targetX, targetY) != Tiles.FLOOR) {
                continue;
            }

            patrolRoute.add(new Vector2((targetX + 0.5f) * tileSize, (targetY + 0.5f) * tileSize));
            return patrolRoute;
        }

        return patrolRoute;
    }
}