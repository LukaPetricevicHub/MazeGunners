package de.tum.cit.fop.maze.behavior;

import com.badlogic.gdx.math.Vector2;
import de.tum.cit.fop.maze.ai.GridPathfinder;
import de.tum.cit.fop.maze.enemies.Enemy;
import de.tum.cit.fop.maze.map.Maze;
import de.tum.cit.fop.maze.util.GridUtils;
import java.awt.Point;
import java.util.List;

public class RetreatBehavior implements BehaviorState {
    // Attributes
    private static final float RECALC_COOLDOWN = 0.7f;
    private static final float ARRIVAL_THRESHOLD = 4f;

    private List<Point> currentPath = List.of();
    private int currentWaypointIndex = 0;
    private float timeSinceLastRepath = 0f;
    private Point previousEnemyTile = null;


    // Return the retreat state type
    @Override
    public BehaviorStateType getType() {
        return BehaviorStateType.RETREAT;
    }


    // Execute movement back to home
    @Override
    public void execute(Enemy enemy, float deltaTime, Maze maze) {
        timeSinceLastRepath += deltaTime;

        Point enemyTile = convertToTileCoords(enemy.getPosition(), maze);
        Vector2 homePos = enemy.getHomePosition();
        Point homeTile = convertToTileCoords(homePos, maze);

        if (enemyTile.equals(homeTile)) {
            currentPath = List.of();
            currentWaypointIndex = 0;
            return;
        }

        boolean shouldRecalculate = currentPath.isEmpty() || currentWaypointIndex >= currentPath.size() || previousEnemyTile == null || !enemyTile.equals(previousEnemyTile) || timeSinceLastRepath >= RECALC_COOLDOWN;

        if (shouldRecalculate) {
            currentPath = GridPathfinder.findPath(maze, enemyTile, homeTile);
            currentWaypointIndex = 0;
            previousEnemyTile = enemyTile;
            timeSinceLastRepath = 0f;
        }

        if(currentPath.isEmpty() || currentWaypointIndex >= currentPath.size()) {
            return;
        }

        Point targetTile = currentPath.get(currentWaypointIndex);
        Vector2 targetWorldPos = getTileCenter(targetTile, maze);

        Vector2 enemyPos = enemy.getPosition();
        Vector2 travelDir = targetWorldPos.cpy().sub(enemyPos);

        if (travelDir.len2() < ARRIVAL_THRESHOLD * ARRIVAL_THRESHOLD) {
            currentWaypointIndex++;
            return;
        }

        Vector2 retreatVelocity = travelDir.nor().scl(enemy.getSpeed());

        enemy.getCollisionHandler().move(enemyPos, retreatVelocity, deltaTime, maze);
    }


    // Convert world coordinates to tile
    private static Point convertToTileCoords(Vector2 worldPos, Maze maze) {
        return new Point(GridUtils.worldToTile(worldPos.x, maze.getTileSize()), GridUtils.worldToTile(worldPos.y, maze.getTileSize()));
    }


    // Get center position of tile
    private static Vector2 getTileCenter(Point tileCoords, Maze maze) {
        float size = maze.getTileSize();

        return new Vector2(tileCoords.x * size + size * 0.5f, tileCoords.y * size + size * 0.5f);
    }
}
