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

    private Deque<Point> currentTilePath;
    private Point lastPlayerTile;
    private float repathTimer = 0f;
    private static final float REPATH_INTERVAL = 0.5f;

    private Vector2 lastWorldPosition;


    @Override
    public BehaviorStateType getType() {
        return BehaviorStateType.CHASE;
    }


    @Override
    public void execute(Enemy enemy, float deltaTime, Maze maze) {

        // Detect stuck enemy → force re-path
        if (lastWorldPosition != null &&
                enemy.getPosition().epsilonEquals(lastWorldPosition, 0.1f)) {
            currentTilePath = null;
        }
        lastWorldPosition = enemy.getPosition().cpy();

        Point enemyTile = new Point(
                GridUtils.worldToTile(enemy.getPosition().x, maze.getTileSize()),
                GridUtils.worldToTile(enemy.getPosition().y, maze.getTileSize())
        );

        Point playerTile = new Point(
                GridUtils.worldToTile(enemy.getPlayerPosition().x, maze.getTileSize()),
                GridUtils.worldToTile(enemy.getPlayerPosition().y, maze.getTileSize())
        );

        if (enemyTile.equals(playerTile))
            return;

        boolean playerMovedTile =
                lastPlayerTile == null || !lastPlayerTile.equals(playerTile);

        repathTimer -= deltaTime;

        if (currentTilePath == null || currentTilePath.isEmpty()
                || playerMovedTile || repathTimer <= 0f) {

            List<Point> newPath =
                    GridPathfinder.findTilePath(maze, enemyTile, playerTile);

            currentTilePath = new ArrayDeque<>(newPath);
            lastPlayerTile = playerTile;
            repathTimer = REPATH_INTERVAL;
        }

        if (currentTilePath.isEmpty())
            return;

        Point nextTile = currentTilePath.peekFirst();

        Vector2 targetWorldPos = new Vector2(
                (nextTile.x + 0.5f) * maze.getTileSize(),
                (nextTile.y + 0.5f) * maze.getTileSize()
        );

        Point enemyTileNow = new Point(
                GridUtils.worldToTile(enemy.getPosition().x, maze.getTileSize()),
                GridUtils.worldToTile(enemy.getPosition().y, maze.getTileSize())
        );

        if (enemyTileNow.equals(nextTile)) {
            enemy.getPosition().set(targetWorldPos);
            currentTilePath.pollFirst();
            return;
        }

        Vector2 dir = targetWorldPos.cpy().sub(enemy.getPosition());
        if (dir.isZero(0.01f))
            return;

        Vector2 velocity = dir.nor().scl(enemy.getSpeed());

        enemy.getCollisionHandler().move(
                enemy.getPosition(),
                velocity,
                deltaTime,
                maze
        );
    }
}
