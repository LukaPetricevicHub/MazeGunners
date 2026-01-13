package de.tum.cit.fop.maze.behavior;

import com.badlogic.gdx.math.Vector2;
import de.tum.cit.fop.maze.ai.GridPathfinder;
import de.tum.cit.fop.maze.enemies.Enemy;
import de.tum.cit.fop.maze.map.Maze;
import de.tum.cit.fop.maze.util.GridUtils;

import java.awt.Point;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class EvadeBehavior implements BehaviorState {

    // Attributes
    private List<Point> currentPath;
    private float pathTimer = 0f;
    private static final float RECALC_INTERVAL = 0.6f;

    private static final int SEARCH_RADIUS = 10;


    @Override
    public BehaviorStateType getType() {
        return BehaviorStateType.EVADE;
    }


    @Override
    public void execute(Enemy enemy, float deltaTime, Maze maze) {
        pathTimer += deltaTime;

        if (currentPath == null || currentPath.isEmpty() || pathTimer >= RECALC_INTERVAL) {

            Point enemyTile = new Point(
                    GridUtils.worldToTile(enemy.getPosition().x, maze.getTileSize()),
                    GridUtils.worldToTile(enemy.getPosition().y, maze.getTileSize())
            );

            Point playerTile = new Point(
                    GridUtils.worldToTile(enemy.getPlayerPosition().x, maze.getTileSize()),
                    GridUtils.worldToTile(enemy.getPlayerPosition().y, maze.getTileSize())
            );

            Point escapeTarget = findEscapeTile(enemyTile, playerTile, maze);

            if (escapeTarget != null) {
                currentPath = GridPathfinder.findTilePath(maze, enemyTile, escapeTarget);
            }

            pathTimer = 0f;
        }

        if (currentPath == null || currentPath.isEmpty())
            return;


        Point next = currentPath.get(0);

        Vector2 targetWorld = new Vector2(
                (next.x + 0.5f) * maze.getTileSize(),
                (next.y + 0.5f) * maze.getTileSize()
        );

        Vector2 direction = targetWorld.sub(enemy.getPosition());

        if (direction.len() < 2f) {
            currentPath.remove(0);
            return;
        }

        Vector2 velocity =
                direction.nor().scl(enemy.getSpeed());

        enemy.getCollisionHandler().move(
                enemy.getPosition(),
                velocity,
                deltaTime,
                maze
        );
    }


    // Pick an escape tile
    private Point findEscapeTile(Point enemy, Point player, Maze maze) {
        List<Point> candidates = new ArrayList<>();

        for (int dy = -SEARCH_RADIUS; dy <= SEARCH_RADIUS; dy++) {
            for (int dx = -SEARCH_RADIUS; dx <= SEARCH_RADIUS; dx++) {

                int cx = enemy.x + dx;
                int cy = enemy.y + dy;

                if (cx < 0 || cy < 0 || cx >= maze.getWidth() || cy >= maze.getHeight())
                    continue;

                if (!maze.getTile(cx, cy).isWalkable())
                    continue;

                int distToPlayer = Math.abs(cx - player.x) + Math.abs(cy - player.y);
                if (distToPlayer > SEARCH_RADIUS / 2) {
                    candidates.add(new Point(cx, cy));
                }
            }
        }

        if (candidates.isEmpty())
            return null;

        return candidates.get(new Random().nextInt(candidates.size()));
    }
}

