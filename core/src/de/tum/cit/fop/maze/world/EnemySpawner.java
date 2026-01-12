package de.tum.cit.fop.maze.world;

import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.Array;
import de.tum.cit.fop.maze.collision.CollisionHandler;
import de.tum.cit.fop.maze.enemies.Enemy;
import de.tum.cit.fop.maze.enemies.FlyingDrone;
import de.tum.cit.fop.maze.enemies.MutantWorker;
import de.tum.cit.fop.maze.enemies.RobotGuard;
import de.tum.cit.fop.maze.level.Maze;
import de.tum.cit.fop.maze.level.Tiles;
import de.tum.cit.fop.maze.player.Player;

import java.awt.Point;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

public class EnemySpawner {

    // Spawns enemies randomly
    public List<Enemy> spawnEnemies(
            Maze maze,
            Player targetPlayer,
            int enemyCount,
            long seed
    ) {
        List<Enemy> spawnedEnemies = new ArrayList<>();
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

        int spawnCount = Math.min(enemyCount, availableFloorCells.size());
        for (int i = 0; i < spawnCount; i++) {
            Point cell = availableFloorCells.get(i);
            Vector2 worldPosition =
                    new Vector2(
                            (cell.x + 0.5f) * maze.getTileSize(),
                            (cell.y + 0.5f) * maze.getTileSize()
                    );

            int enemyType = random.nextInt(3);


            switch (enemyType) {
                case 0 -> spawnedEnemies.add(
                        new MutantWorker(
                                worldPosition,
                                85f,
                                targetPlayer
                        )
                );
                case 1 -> {
                    Array<Vector2> patrolPath = generatePatrolPath(maze, cell);
                    spawnedEnemies.add(
                            new RobotGuard(
                                    worldPosition,
                                    60f,
                                    patrolPath,
                                    targetPlayer
                            )
                    );
                }
                case 2 -> {
                    Array<Vector2> patrolPath = generatePatrolPath(maze, cell);
                    spawnedEnemies.add(
                            new FlyingDrone(
                                    worldPosition,
                                    110f,
                                    patrolPath,
                                    targetPlayer
                            )
                    );
                }
            }

        }

        return spawnedEnemies;
    }


    // Generate simple patrol path
    private Array<Vector2> generatePatrolPath(Maze maze, Point startCell) {
        Array<Vector2> patrolPoints = new Array<>();
        float ts = maze.getTileSize();

        Vector2 start =
                new Vector2((startCell.x + 0.5f) * ts, (startCell.y + 0.5f) * ts);
        patrolPoints.add(start);

        int[][] directions = {{3, 0}, {-3, 0}, {0, 3}, {0, -3}};

        for (int[] direction : directions) {
            int tx = startCell.x + direction[0];
            int ty = startCell.y + direction[1];

            if (!maze.inBounds(tx, ty)) continue;
            if (maze.getTile(tx, ty) != Tiles.FLOOR) continue;

            Vector2 target =
                    new Vector2((tx + 0.5f) * ts, (ty + 0.5f) * ts);

            if (target.dst2(start) > 1f) {
                patrolPoints.add(target);
                return patrolPoints;
            }
        }

        for (int r = 1; r <= 6; r++) {
            for (int dy = -r; dy <= r; dy++) {
                for (int dx = -r; dx <= r; dx++) {
                    int tx = startCell.x + dx;
                    int ty = startCell.y + dy;

                    if (!maze.inBounds(tx, ty)) continue;
                    if (maze.getTile(tx, ty) != Tiles.FLOOR) continue;

                    Vector2 target =
                            new Vector2((tx + 0.5f) * ts, (ty + 0.5f) * ts);

                    if (target.dst2(start) > 1f) {
                        patrolPoints.add(target);
                        return patrolPoints;
                    }
                }
            }
        }

        return patrolPoints;
    }
}
