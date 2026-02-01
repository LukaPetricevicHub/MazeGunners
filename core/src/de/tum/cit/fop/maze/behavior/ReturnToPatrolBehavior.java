package de.tum.cit.fop.maze.behavior;

import com.badlogic.gdx.math.Vector2;
import de.tum.cit.fop.maze.ai.GridPathfinder;
import de.tum.cit.fop.maze.ai.SteeringBehavior;
import de.tum.cit.fop.maze.enemies.Enemy;
import de.tum.cit.fop.maze.enemies.FlyingDrone;
import de.tum.cit.fop.maze.map.Maze;

import java.awt.Point;
import java.util.List;

/**
 * Behavior that moves a flying enemy back to its patrol route.
 *
 * The enemy pathfinds to the closest patrol point and then switches
 * back to its normal patrol behavior once it arrives.
 *
 * This behavior is intended for FlyingDrone enemies only.
 */
public class ReturnToPatrolBehavior implements BehaviorState {

    private List<Point> path = List.of();
    private int index = 0;

    /**
     * Returns the behavior type.
     *
     * @return RETURN behavior type
     */
    @Override
    public BehaviorStateType getType() {
        return BehaviorStateType.RETURN;
    }

    /**
     * Moves the enemy back to the nearest patrol point.
     *
     * The enemy follows a path to the patrol location and then snaps
     * exactly onto the patrol point before switching back to patrol mode.
     *
     * @param enemy the enemy using this behavior
     * @param deltaTime time since last frame
     * @param maze the maze used for pathfinding and collision
     */
    @Override
    public void execute(Enemy enemy, float deltaTime, Maze maze) {

        // This behavior should only be used for FlyingDrone
        if (!(enemy instanceof FlyingDrone drone))
            return;

        // Current enemy tile
        Point enemyTile = toTile(enemy.getPosition(), maze);

        // Closest patrol point (WORLD SPACE!)
        Vector2 patrolPoint = drone.getClosestPatrolPoint();

        // Tile under the patrol point (ONLY for pathfinding)
        Point targetTile = new Point(
                (int) (patrolPoint.x / maze.getTileSize()),
                (int) (patrolPoint.y / maze.getTileSize())
        );

        // Recalculate path if needed
        if (path.isEmpty() || index >= path.size()) {
            path = GridPathfinder.findPath(
                    maze,
                    enemyTile,
                    targetTile,
                    enemy.getPathfindingFootprint()
            );
            index = 0;
        }

        // No path => cannot move
        if (path.isEmpty())
            return;

        // Follow path
        index = SteeringBehavior.followPath(enemy, maze, path, index, deltaTime);

        if (index >= path.size()) {

            enemy.getPosition().set(patrolPoint);

            enemy.setCurrentBehavior(drone.createPatrolBehavior());
        }
    }


    private Point toTile(Vector2 pos, Maze maze) {
        return new Point(
                (int) (pos.x / maze.getTileSize()),
                (int) (pos.y / maze.getTileSize())
        );
    }
}
