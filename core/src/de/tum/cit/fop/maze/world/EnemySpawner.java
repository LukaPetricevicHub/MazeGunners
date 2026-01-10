package de.tum.cit.fop.maze.world;

import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.Array;
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
    public List<Enemy> spawnEnemies(Maze maze, Player targetPlayer, int enemyCount, long seed) {
        List<Enemy> spawnedEnemies = new ArrayList<>();
        List<Point> availableFloorCells = new ArrayList<>();
        Random random = new Random(seed);

        for (int y = 1; y < maze.getHeight() - 1; y++) {
            for (int x = 1; x < maze.getWidth() - 1; x++) {
                if (maze.getTile(x, y) == Tiles.Floor) {
                    availableFloorCells.add(new Point(x, y));
                }
            }
        }

        Collections.shuffle(availableFloorCells, random);

        int spawnCount = Math.min(enemyCount, availableFloorCells.size());
        for (int i = 0; i < spawnCount; i++) {
            Point cell = availableFloorCells.get(i);
            Vector2 worldPosition =
                    new Vector2(cell.x * maze.getTileSize(), cell.y * maze.getTileSize());

            int enemyType = random.nextInt(3);

            switch (enemyType) {
                case 0 -> spawnedEnemies.add(new MutantWorker(worldPosition, 85f, targetPlayer));
                case 1 -> {Array<Vector2> patrolPath = generatePatrolPath(maze, cell);
                    spawnedEnemies.add(new RobotGuard(worldPosition, 60f, patrolPath, targetPlayer));
                }
                case 2 -> spawnedEnemies.add(new FlyingDrone(worldPosition, 110f, new Array<>(), targetPlayer));
            }
        }

        return spawnedEnemies;
    }


    // Generate simple patrol path
    private Array<Vector2> generatePatrolPath(Maze maze, Point startCell) {
        Array<Vector2> patrolPoints = new Array<>();
        float tileSize = maze.getTileSize();

        patrolPoints.add(new Vector2(startCell.x * tileSize, startCell.y * tileSize));

        int[][] directions = {{3, 0}, {-3, 0}, {0, 3}, {0, -3}};

        for (int[] direction : directions) {
            int targetX = startCell.x + direction[0];
            int targetY = startCell.y + direction[1];

            if(targetX > 0 && targetX < maze.getWidth() && targetY > 0 && targetY < maze.getHeight() && maze.getTile(targetX, targetY) == Tiles.Floor) {
                patrolPoints.add(new Vector2(targetX * tileSize, targetY * tileSize));
                return patrolPoints;
            }
        }

        patrolPoints.add(new Vector2(startCell.x * tileSize, (startCell.y + 1) * tileSize));
        return patrolPoints;
    }
}