package de.tum.cit.fop.maze.behavior;

import com.badlogic.gdx.math.Vector2;
import de.tum.cit.fop.maze.ai.GridPathfinder;
import de.tum.cit.fop.maze.enemies.Enemy;
import de.tum.cit.fop.maze.map.Maze;
import de.tum.cit.fop.maze.util.GridUtils;
import java.awt.Point;
import java.util.List;

/**
 * Retreat behavior that moves an enemy back to its home position.
 *
 * The enemy uses pathfinding to return to where it originally spawned.
 * Paths are recalculated periodically to avoid getting stuck.
 */
public class RetreatBehavior implements BehaviorState {
    // Attributes
    private static final float RECALC_COOLDOWN = 0.7f;
    private static final float ARRIVAL_THRESHOLD = 4f;

    private List<Point> currentPath = List.of();
    private int currentWaypointIndex = 0;
    private float timeSinceLastRepath = 0f;
    private Point previousEnemyTile = null;

    /**
     * Returns the behavior type.
     *
     * @return RETREAT behavior type
     */
    @Override
    public BehaviorStateType getType() {
        return BehaviorStateType.RETREAT;
    }


    /**
     * Moves the enemy back to its home position using pathfinding.
     *
     * The enemy follows a path tile by tile and stops once it reaches home.
     *
     * @param enemy the enemy using this behavior
     * @param deltaTime time since last frame
     * @param maze the maze used for pathfinding and collision
     */
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
    /**
     * Converts world coordinates to tile coordinates.
     *
     * @param worldPos World position
     * @param maze Maze reference
     * @return Tile coordinates
     */
    private static Point convertToTileCoords(Vector2 worldPos, Maze maze) {
        return new Point(GridUtils.worldToTile(worldPos.x, maze.getTileSize()), GridUtils.worldToTile(worldPos.y, maze.getTileSize()));
    }


    // Get center position of tile
    /**
     * Computes the center of a tile in world coordinates.
     *
     * @param tileCoords Tile coordinates
     * @param maze Maze reference
     * @return Center position in world units
     */
    private static Vector2 getTileCenter(Point tileCoords, Maze maze) {
        float size = maze.getTileSize();

        return new Vector2(tileCoords.x * size + size * 0.5f, tileCoords.y * size + size * 0.5f);
    }
}
