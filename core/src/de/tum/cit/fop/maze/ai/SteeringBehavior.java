package de.tum.cit.fop.maze.ai;

import com.badlogic.gdx.math.Vector2;
import de.tum.cit.fop.maze.map.Maze;
import de.tum.cit.fop.maze.enemies.Enemy;
import java.awt.Point;
import java.util.List;

/**
 * Utility class that handles smooth enemy movement along a tile-based path.
 *
 * The movement uses simple steering toward waypoints and advances the path
 * index once a waypoint is reached.
 */
public final class SteeringBehavior {

    /** Distance threshold for waypoint arrival. */
    private static final float ARRIVAL_THRESHOLD = 4f;

    private SteeringBehavior() {}

    /**
     * Moves the given enemy along the provided tile path.
     *
     * The enemy steers toward the current waypoint and advances to the next
     * waypoint once it is close enough. If the enemy is attack-locked,
     * movement is paused and the waypoint index is not advanced.
     *
     * @param enemy the enemy to move
     * @param maze the maze used for collision checks
     * @param path the tile path to follow
     * @param currentWaypointIndex index of the current waypoint in the path
     * @param deltaTime time elapsed since last frame
     * @return the updated waypoint index
     */
    public static int followPath(Enemy enemy, Maze maze, List<Point> path, int currentWaypointIndex, float deltaTime) {
        if (path == null || currentWaypointIndex >= path.size())
            return currentWaypointIndex;








        
        if (enemy.isAttackingLocked()) {
            return currentWaypointIndex;
        }


        Vector2 currentEnemyPosition = enemy.getPosition();
        Point targetTile = path.get(currentWaypointIndex);
        float tileSize = maze.getTileSize();
        Vector2 targetWorldPos;

        if (currentWaypointIndex > 0) {
            Point previousTile = path.get(currentWaypointIndex - 1);
            targetWorldPos = getTransitionTarget(previousTile, targetTile, currentEnemyPosition, tileSize);
        } else {
            targetWorldPos = new Vector2(
                    targetTile.x * tileSize + tileSize * 0.5f,
                    targetTile.y * tileSize + tileSize * 0.5f
            );
        }

        Vector2 distToTarget = targetWorldPos.cpy().sub(currentEnemyPosition);

        if (distToTarget.len2() <= ARRIVAL_THRESHOLD * ARRIVAL_THRESHOLD)
            return currentWaypointIndex + 1;

        Vector2 desiredVelocity = distToTarget.nor().scl(enemy.getSpeed());
        boolean hasMoved = enemy.getCollisionHandler().move(currentEnemyPosition, desiredVelocity, deltaTime, maze);
        boolean axisSuccess = false;

        if (!hasMoved && Math.abs(desiredVelocity.x) > 0.001f) {
            axisSuccess = enemy.getCollisionHandler().move(
                    currentEnemyPosition,
                    new Vector2(desiredVelocity.x, 0),
                    deltaTime,
                    maze
            );
        }

        if (!hasMoved && !axisSuccess && Math.abs(desiredVelocity.y) > 0.001f) {
            axisSuccess = enemy.getCollisionHandler().move(
                    currentEnemyPosition,
                    new Vector2(0, desiredVelocity.y),
                    deltaTime,
                    maze
            );
        }

        if (!hasMoved && !axisSuccess)
            return currentWaypointIndex + 1;

        return currentWaypointIndex;
    }

    /**
     * Clamps a target to a reachable edge aligned with the current position.
     *
     * @param from Current position
     * @param target Target position
     * @param tileSize Tile size in world units
     * @return Adjusted target on a shared axis
     */
    private static Vector2 clampToReachableEdge(Vector2 from, Vector2 target, float tileSize) {
        Vector2 moveDir = target.cpy().sub(from);

        if (Math.abs(moveDir.x) > Math.abs(moveDir.y))
            return new Vector2(target.x, from.y);
        else
            return new Vector2(from.x, target.y);
    }

    /**
     * Computes a world-space target position for transitioning between two
     * adjacent tiles.
     *
     * Instead of moving directly to the center of the next tile, it
     * guids through the shared edge.
     *
     * @param fromTile Current tile
     * @param toTile Next tile
     * @param currentPos Current world position
     * @param tileSize Tile size in world units
     * @return Transition target in world coordinates
     */
    private static Vector2 getTransitionTarget(Point fromTile, Point toTile, Vector2 currentPos, float tileSize) {
        float centerX = fromTile.x * tileSize + tileSize * 0.5f;
        float centerY = fromTile.y * tileSize + tileSize * 0.5f;

        if (toTile.x > fromTile.x)
            return new Vector2(fromTile.x * tileSize + tileSize, centerY);

        if (toTile.x < fromTile.x)
            return new Vector2(fromTile.x * tileSize, centerY);

        if (toTile.y > fromTile.y)
            return new Vector2(centerX, fromTile.y * tileSize + tileSize);

        if (toTile.y < fromTile.y)
            return new Vector2(centerX, fromTile.y * tileSize);

        return currentPos;
    }
}
