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
    private float pathRecalculationTimer = 0f;
    private static final float pathRecalculationInterval = 0.5f;


    // Identify behavior type
    @Override
    public BehaviorStateType getType() {
        return BehaviorStateType.CHASE;
    }


    // Move enemy to player
    @Override
    public void execute(Enemy enemy, float deltaTime, Maze maze) {
        pathRecalculationTimer += deltaTime;

        if (currentTilePath == null || pathRecalculationTimer >= pathRecalculationInterval) {

            Point enemyTilePosition = new Point(
                    GridUtils.worldToTile(enemy.getPosition().x, maze.getTileSize()),
                    GridUtils.worldToTile(enemy.getPosition().y, maze.getTileSize())
            );

            Point playerTilePosition = new Point(
                    GridUtils.worldToTile(enemy.getPlayerPosition().x, maze.getTileSize()),
                    GridUtils.worldToTile(enemy.getPlayerPosition().y, maze.getTileSize())
            );

            currentTilePath = GridPathfinder.findTilePath(
                    maze,
                    enemyTilePosition,
                    playerTilePosition
            );

            pathRecalculationTimer = 0f;
        }

        if(currentTilePath != null && !currentTilePath.isEmpty()) {
            Point nextTile = currentTilePath.get(0);

            Vector2 targetWorldPosition = new Vector2(
                    nextTile.x * maze.getTileSize(),
                    nextTile.y * maze.getTileSize()
            );

            Vector2 movementDirection =
                    targetWorldPosition.cpy().sub(enemy.getPosition());

            if (movementDirection.len() < 2f)
                currentTilePath.remove(0);
            else
                enemy.moveEnemy(movementDirection, deltaTime);

        }
    }
}

