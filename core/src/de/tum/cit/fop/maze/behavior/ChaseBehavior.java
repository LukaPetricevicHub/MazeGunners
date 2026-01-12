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

    // match drawn size
    private static final float ENEMY_W = 32f;
    private static final float ENEMY_H = 64f;

    // how close is "arrived at this node"

    private static final float ARRIVE_EPS_TILES = 0.20f;


    // Identify behavior type
    @Override
    public BehaviorStateType getType() {
        return BehaviorStateType.CHASE;
    }


    // Move enemy to player
    @Override
    public void execute(Enemy enemy, float deltaTime, Maze maze) {
        pathRecalculationTimer += deltaTime;

        float ts = maze.getTileSize();
        float arriveEps = ts * 0.20f;

        Vector2 enemyCenter = new Vector2(
                enemy.getPosition().x + ENEMY_W / 2f,
                enemy.getPosition().y + ENEMY_H / 2f
        );

        Vector2 playerPos = enemy.getPlayerPosition();
        Vector2 playerCenter = new Vector2(
                playerPos.x + ENEMY_W / 2f,
                playerPos.y + ENEMY_H / 2f
        );

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

            // Drop the first node if it's the tile we're already on
            if (currentTilePath != null && !currentTilePath.isEmpty() && currentTilePath.get(0).equals(enemyTilePosition)) {
                currentTilePath.remove(0);
            }
        }

        if (currentTilePath == null || currentTilePath.isEmpty()) return;

        Point next = currentTilePath.get(0);

        Vector2 targetWorld = new Vector2(
                (next.x + 0.5f) * ts,
                (next.y + 0.5f) * ts
        );

        Vector2 dir = targetWorld.cpy().sub(enemyCenter);

        if (dir.len() <= arriveEps) {
            currentTilePath.remove(0);
        } else {
            enemy.moveEnemy(dir, deltaTime, maze);
        }
    }
}

