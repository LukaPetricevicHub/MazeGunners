package de.tum.cit.fop.maze.behavior;

import com.badlogic.gdx.math.Vector2;
import de.tum.cit.fop.maze.ai.GridPathfinder;
import de.tum.cit.fop.maze.ai.SteeringBehavior;
import de.tum.cit.fop.maze.enemies.Enemy;
import de.tum.cit.fop.maze.map.Maze;
import java.awt.Point;
import java.util.List;

public class ChaseBehavior implements BehaviorState {

    // Attributes
    private static final float RECALCULATION_COOLDOWN = 0.3f;
    private static final float MIN_STUCK_DIST = 1.0f;
    private static final float MAX_STUCK_DURATION = 0.25f;

    private List<Point> currentPath = List.of();
    private int currentWaypointIndex = 0;
    private Point trackedPlayerTile = null;
    private float timeUntilRepath = 0f;
    private Vector2 previousFramePos = null;
    private float timeSinceLastMovement = 0f;


    // Return the chase state type
    @Override
    public BehaviorStateType getType() {
        return BehaviorStateType.CHASE;
    }


    // Logic for pursuing the player
    @Override
    public void execute(Enemy enemy, float deltaTime, Maze maze) {
        Vector2 currentPosition = enemy.getPosition();

        if (previousFramePos != null) {
            if (currentPosition.dst2(previousFramePos) < MIN_STUCK_DIST * MIN_STUCK_DIST)
                timeSinceLastMovement += deltaTime;
            else
                timeSinceLastMovement = 0f;

        } else {
            previousFramePos = new Vector2(currentPosition);
        }

        previousFramePos.set(currentPosition);

        Point enemyTile = convertToTileCoords(currentPosition, maze);
        Point playerTile = convertToTileCoords(enemy.getPlayerPosition(), maze);

        timeUntilRepath -= deltaTime;

        boolean isStuck = timeSinceLastMovement >= MAX_STUCK_DURATION;
        boolean needsNewPath = isStuck || currentPath.isEmpty() || currentWaypointIndex >= currentPath.size() || trackedPlayerTile == null || !playerTile.equals(trackedPlayerTile) || timeUntilRepath <= 0f;

        if (needsNewPath) {
            Point bestGoal = GridPathfinder.findReachableGoalNearPlayer(
                    maze,
                    enemyTile,
                    playerTile,
                    2,
                    enemy.getPathfindingFootprint()
            );


            if (bestGoal != null)
                currentPath = currentPath = GridPathfinder.findPath(
                        maze,
                        enemyTile,
                        bestGoal,
                        enemy.getPathfindingFootprint()
                );

            else
                currentPath = List.of();


            if (!currentPath.isEmpty() && currentPath.get(0).equals(enemyTile)) {
                currentWaypointIndex = 1;
            } else {
                currentWaypointIndex = 0;
            }

            trackedPlayerTile = playerTile;
            timeUntilRepath = RECALCULATION_COOLDOWN;
            timeSinceLastMovement = 0f;
            previousFramePos.set(currentPosition);
        }

        if(currentPath.isEmpty() || currentWaypointIndex >= currentPath.size())
            return;

        currentWaypointIndex = SteeringBehavior.followPath(enemy, maze, currentPath, currentWaypointIndex, deltaTime);
    }


    // Convert world coordinates to tile
    private static Point convertToTileCoords(Vector2 worldPos, Maze maze) {
        int tileX = (int) (worldPos.x / maze.getTileSize());
        int tileY = (int) (worldPos.y / maze.getTileSize());

        return new Point(tileX, tileY);
    }


    // Get center position of tile
    private static Vector2 getTileCenter(Point tileCoords, Maze maze) {
        float size = maze.getTileSize();

        return new Vector2(tileCoords.x * size + size * 0.5f, tileCoords.y * size + size * 0.5f);
    }


    // Check direct path to player
    private boolean canDirectChase(Enemy enemy, Maze maze) {
        Vector2 pos = enemy.getPosition();
        Vector2 playerPos = enemy.getPlayerPosition();

        Vector2 direction = playerPos.cpy().sub(pos);

        if(direction.len2() < 1f)
            return false;


        Vector2 movementStep = direction.nor().scl(4f);
        Vector2 testPosition = pos.cpy().add(movementStep);

        return !enemy.getCollisionHandler().move(testPosition, Vector2.Zero, 0f, maze);
    }
}