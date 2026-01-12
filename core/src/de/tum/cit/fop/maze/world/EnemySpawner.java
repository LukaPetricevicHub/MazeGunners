package de.tum.cit.fop.maze.world;

import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.Array;
import de.tum.cit.fop.maze.enemies.*;
import de.tum.cit.fop.maze.level.Maze;
import de.tum.cit.fop.maze.level.Tiles;
import de.tum.cit.fop.maze.player.Player;

import java.awt.Point;
import java.util.*;

public class EnemySpawner {

    private static final float ENEMY_WIDTH = 32f;
    private static final float ENEMY_HEIGHT = 64f;

    public List<Enemy> spawnEnemies(
            Maze maze,
            Player targetPlayer,
            int enemyCount,
            long seed
    ) {
        List<Enemy> enemies = new ArrayList<>();
        List<Point> candidates = new ArrayList<>();
        Random rng = new Random(seed);

        // collect floor tiles
        for (int y = 1; y < maze.getHeight() - 1; y++) {
            for (int x = 1; x < maze.getWidth() - 1; x++) {
                if (maze.getTile(x, y) == Tiles.FLOOR) {
                    candidates.add(new Point(x, y));
                }
            }
        }

        Collections.shuffle(candidates, rng);

        for (Point cell : candidates) {
            if (enemies.size() >= enemyCount)
                break;

            float worldX = cell.x * maze.getTileSize();
            float worldY = cell.y * maze.getTileSize();

            if (!canSpawnEnemyAt(worldX, worldY, maze))
                continue;

            Vector2 spawnPos = new Vector2(worldX, worldY);

            int type = rng.nextInt(3);

            switch (type) {
                case 0 -> enemies.add(
                        new MutantWorker(spawnPos, 200f, targetPlayer)
                );
                case 1 -> enemies.add(
                        new RobotGuard(
                                spawnPos,
                                60f,
                                generatePatrolPath(maze, cell),
                                targetPlayer
                        )
                );
                case 2 -> enemies.add(
                        new FlyingDrone(
                                spawnPos,
                                110f,
                                generatePatrolPath(maze, cell),
                                targetPlayer
                        )
                );
            }
        }

        return enemies;
    }

    // Ensures the full enemy body fits inside walkable tiles
    private boolean canSpawnEnemyAt(float x, float y, Maze maze) {
        float inset = 2f;

        float left   = x + inset;
        float right  = x + ENEMY_WIDTH - inset;
        float bottom = y + inset;
        float top    = y + ENEMY_HEIGHT - inset;

        return !maze.isWallWorld(left, bottom)
                && !maze.isWallWorld(right, bottom)
                && !maze.isWallWorld(left, top)
                && !maze.isWallWorld(right, top);
    }

    // unchanged
    private Array<Vector2> generatePatrolPath(Maze maze, Point startCell) {
        Array<Vector2> patrolPoints = new Array<>();
        float ts = maze.getTileSize();

        Vector2 start =
                new Vector2((startCell.x + 0.5f) * ts, (startCell.y + 0.5f) * ts);
        patrolPoints.add(start);

        int[][] dirs = {{3,0},{-3,0},{0,3},{0,-3}};

        for (int[] d : dirs) {
            int tx = startCell.x + d[0];
            int ty = startCell.y + d[1];

            if (!maze.inBounds(tx, ty)) continue;
            if (maze.getTile(tx, ty) != Tiles.FLOOR) continue;

            patrolPoints.add(
                    new Vector2((tx + 0.5f) * ts, (ty + 0.5f) * ts)
            );
            return patrolPoints;
        }

        return patrolPoints;
    }
}