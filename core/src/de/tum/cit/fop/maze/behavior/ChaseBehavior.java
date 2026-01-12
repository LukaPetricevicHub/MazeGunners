package de.tum.cit.fop.maze.behavior;

import com.badlogic.gdx.math.Vector2;
import de.tum.cit.fop.maze.ai.GridPathfinder;
import de.tum.cit.fop.maze.enemies.Enemy;
import de.tum.cit.fop.maze.level.Maze;
import de.tum.cit.fop.maze.util.GridUtils;

import java.awt.Point;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;

public class ChaseBehavior implements BehaviorState {

    private Deque<Point> currentPath;
    private Point lastPlayerTile;

    private static final float ARRIVAL_DIST = 3f;
    private static final float REPATH_COOLDOWN = 0.6f;
    private float repathTimer = 0f;

    @Override
    public BehaviorStateType getType() {
        return BehaviorStateType.CHASE;
    }

    @Override
    public void execute(Enemy enemy, float deltaTime, Maze maze) {

        repathTimer -= deltaTime;

        Point enemyTile = new Point(
                GridUtils.worldToTile(enemy.getPosition().x, maze.getTileSize()),
                GridUtils.worldToTile(enemy.getPosition().y, maze.getTileSize())
        );

        Point playerTile = new Point(
                GridUtils.worldToTile(enemy.getPlayerPosition().x, maze.getTileSize()),
                GridUtils.worldToTile(enemy.getPlayerPosition().y, maze.getTileSize())
        );

        // Already at player tile → no thinking needed
        if (enemyTile.equals(playerTile)) {
            currentPath = null;
            return;
        }

        boolean playerChangedTile =
                lastPlayerTile == null || !lastPlayerTile.equals(playerTile);

        if (currentPath == null
                || currentPath.isEmpty()
                || (playerChangedTile && repathTimer <= 0f)) {

            List<Point> path =
                    GridPathfinder.findTilePath(maze, enemyTile, playerTile);

            currentPath = new ArrayDeque<>(path);
            lastPlayerTile = playerTile;
            repathTimer = REPATH_COOLDOWN;
        }

        if (currentPath == null || currentPath.isEmpty())
            return;

        Point next = currentPath.peekFirst();

        Vector2 target = new Vector2(
                (next.x + 0.5f) * maze.getTileSize(),
                (next.y + 0.5f) * maze.getTileSize()
        );

        Vector2 toTarget = target.cpy().sub(enemy.getPosition());

        if (toTarget.len() <= ARRIVAL_DIST) {
            currentPath.pollFirst();
            return;
        }

        Vector2 velocity =
                toTarget.nor().scl(enemy.getSpeed());

        enemy.getCollisionHandler().move(
                enemy.getPosition(),
                velocity,
                deltaTime,
                maze
        );
    }
}
