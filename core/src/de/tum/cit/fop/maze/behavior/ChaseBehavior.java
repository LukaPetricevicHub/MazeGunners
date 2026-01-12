package de.tum.cit.fop.maze.behavior;

import com.badlogic.gdx.math.Vector2;
import de.tum.cit.fop.maze.ai.GridPathfinder;
import de.tum.cit.fop.maze.enemies.Enemy;
import de.tum.cit.fop.maze.level.Maze;
import de.tum.cit.fop.maze.util.GridUtils;

import java.awt.Point;
import java.util.List;

public class ChaseBehavior implements BehaviorState {

    // Attributes
    private List<Point> currentTilePath;
    private Point lastPlayerTile;


    // Identify behavior type
    @Override
    public BehaviorStateType getType() {
        return BehaviorStateType.CHASE;
    }


    // Move enemy to player using grid pathfinding
    @Override
    public void execute(Enemy enemy, float deltaTime, Maze maze) {

        Point enemyTile = new Point(
                GridUtils.worldToTile(enemy.getPosition().x, maze.getTileSize()),
                GridUtils.worldToTile(enemy.getPosition().y, maze.getTileSize())
        );

        Point playerTile = new Point(
                GridUtils.worldToTile(enemy.getPlayerPosition().x, maze.getTileSize()),
                GridUtils.worldToTile(enemy.getPlayerPosition().y, maze.getTileSize())
        );

        boolean playerMovedTile =
                lastPlayerTile == null || !lastPlayerTile.equals(playerTile);

        if (currentTilePath == null || currentTilePath.isEmpty() || playerMovedTile) {
            currentTilePath = GridPathfinder.findTilePath(
                    maze,
                    enemyTile,
                    playerTile
            );
            lastPlayerTile = playerTile;
        }

        if (currentTilePath == null || currentTilePath.isEmpty())
            return;


        Point nextTile = currentTilePath.get(0);

        Vector2 targetWorldPosition = new Vector2(
                (nextTile.x + 0.5f) * maze.getTileSize(),
                (nextTile.y + 0.5f) * maze.getTileSize()
        );

        Vector2 movementDirection =
                targetWorldPosition.cpy().sub(enemy.getPosition());

        if (movementDirection.len() < 2f) {
            currentTilePath.remove(0);
            return;
        }

        Vector2 velocity =
                movementDirection.nor().scl(enemy.getSpeed());

        enemy.getCollisionHandler().move(
                enemy.getPosition(),
                velocity,
                deltaTime,
                maze
        );
    }
}


