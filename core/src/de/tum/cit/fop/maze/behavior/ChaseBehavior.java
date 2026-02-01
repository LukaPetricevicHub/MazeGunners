package de.tum.cit.fop.maze.behavior;

import com.badlogic.gdx.math.Vector2;
import de.tum.cit.fop.maze.ai.GridPathfinder;
import de.tum.cit.fop.maze.ai.SteeringBehavior;
import de.tum.cit.fop.maze.enemies.Enemy;
import de.tum.cit.fop.maze.map.Maze;
import java.awt.Point;
import java.util.List;

/**
 * Behavior where an enemy actively chases the player.
 *
 * The enemy uses grid-based pathfinding to move toward the player's position.
 * Paths are recalculated periodically or when the player moves.
 * If no valid path exists, the enemy temporarily stops and retries later.
 */
public class ChaseBehavior implements BehaviorState {

    private static final float RECALCULATION_COOLDOWN = 0.3f;
    private static final float MIN_STUCK_DIST = 1.0f;
    private static final float MAX_STUCK_DURATION = 0.25f;

    private static final float NO_PATH_RETRY_DELAY = 0.6f;

    private boolean noPathAvailable = false;
    private float noPathRetryTimer = 0f;


    private List<Point> currentPath = List.of();
    private int currentWaypointIndex = 0;
    private Point trackedPlayerTile = null;
    private float timeUntilRepath = 0f;
    private Vector2 previousFramePos = null;
    private float timeSinceLastMovement = 0f;

    /**
     * Returns the behavior type.
     *
     * @return CHASE behavior type
     */
    @Override
    public BehaviorStateType getType() {
        return BehaviorStateType.CHASE;
    }

    /**
     * Executes chase logic for the enemy.
     *
     * The enemy:
     * - Tracks the player's tile position
     * - Recalculates paths when needed
     * - Detects when it is stuck
     * - Moves along the current path using steering behavior
     *
     * @param enemy the enemy using this behavior
     * @param deltaTime time since last frame
     * @param maze the maze for collision and pathfinding
     */
    @Override
    public void execute(Enemy enemy, float deltaTime, Maze maze) {
        Vector2 currentPosition = enemy.getPosition();

        if (noPathAvailable) {
            noPathRetryTimer -= deltaTime;
            if (noPathRetryTimer <= 0f) {
                noPathAvailable = false;
            }
            return;
        }

        if (previousFramePos != null) {
            if (currentPosition.dst2(previousFramePos) < MIN_STUCK_DIST * MIN_STUCK_DIST)
                timeSinceLastMovement += deltaTime;
            else
                timeSinceLastMovement = 0f;
        } else {
            previousFramePos = new Vector2(currentPosition);
        }

        previousFramePos.set(currentPosition);

        if (enemy.isAttackingLocked())
            return;

        Point enemyTile = convertToTileCoords(currentPosition, maze);
        Point playerTile = convertToTileCoords(enemy.getPlayerPosition(), maze);

        timeUntilRepath -= deltaTime;

        boolean isStuck = timeSinceLastMovement >= MAX_STUCK_DURATION;
        boolean needsNewPath =
                !noPathAvailable && (
                        isStuck
                                || currentPath.isEmpty()
                                || currentWaypointIndex >= currentPath.size()
                                || trackedPlayerTile == null
                                || !playerTile.equals(trackedPlayerTile)
                                || timeUntilRepath <= 0f
                );


        if (needsNewPath) {
            Point bestGoal = GridPathfinder.findReachableGoalNearPlayer(
                    maze,
                    enemyTile,
                    playerTile,
                    2,
                    enemy.getPathfindingFootprint()
            );

            if (bestGoal != null) {
                currentPath = GridPathfinder.findPath(
                        maze,
                        enemyTile,
                        bestGoal,
                        enemy.getPathfindingFootprint()
                );
            } else {
                currentPath = List.of();
            }

            if (currentPath.isEmpty()) {
                noPathAvailable = true;
                noPathRetryTimer = NO_PATH_RETRY_DELAY;
                return; // HOLD POSITION
            }

            noPathAvailable = false; //?


            if (currentPath.get(0).equals(enemyTile)) {
                currentWaypointIndex = 1;
            } else {
                currentWaypointIndex = 0;
            }

            trackedPlayerTile = playerTile;
            timeUntilRepath = RECALCULATION_COOLDOWN;
            timeSinceLastMovement = 0f;
            previousFramePos.set(currentPosition);
        }

        if (currentPath.isEmpty() || currentWaypointIndex >= currentPath.size())
            return;

        currentWaypointIndex =
                SteeringBehavior.followPath(enemy, maze, currentPath, currentWaypointIndex, deltaTime);
    }

    private static Point convertToTileCoords(Vector2 worldPos, Maze maze) {
        int tileX = (int) (worldPos.x / maze.getTileSize());
        int tileY = (int) (worldPos.y / maze.getTileSize());
        return new Point(tileX, tileY);
    }

    /**
     * Computes the world-space center of a tile.
     *
     * @param tileCoords Tile coordinates
     * @param maze Maze reference
     * @return Center position in world units
     */
    private static Vector2 getTileCenter(Point tileCoords, Maze maze) {
        float size = maze.getTileSize();
        return new Vector2(
                tileCoords.x * size + size * 0.5f,
                tileCoords.y * size + size * 0.5f
        );
    }

    /**
     * Checks whether a direct chase without pathfinding is possible.
     *
     * @param enemy Enemy attempting to chase
     * @param maze Maze reference
     * @return True if a direct path is possible
     */
    private boolean canDirectChase(Enemy enemy, Maze maze) {
        Vector2 pos = enemy.getPosition();
        Vector2 playerPos = enemy.getPlayerPosition();

        Vector2 direction = playerPos.cpy().sub(pos);

        if (direction.len2() < 1f)
            return false;

        Vector2 movementStep = direction.nor().scl(4f);
        Vector2 testPosition = pos.cpy().add(movementStep);

        return !enemy.getCollisionHandler().move(testPosition, Vector2.Zero, 0f, maze);
    }
}
