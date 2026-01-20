package de.tum.cit.fop.maze.behavior;

import com.badlogic.gdx.math.Vector2;
import de.tum.cit.fop.maze.ai.GridPathfinder;
import de.tum.cit.fop.maze.ai.SteeringBehavior;
import de.tum.cit.fop.maze.enemies.Enemy;
import de.tum.cit.fop.maze.enemies.FlyingDrone;
import de.tum.cit.fop.maze.map.Maze;

import java.awt.Point;
import java.util.List;

public class ReturnToPatrolBehavior implements BehaviorState {

    private List<Point> path = List.of();
    private int index = 0;

    @Override
    public BehaviorStateType getType() {
        return BehaviorStateType.RETURN;
    }

    @Override
    public void execute(Enemy enemy, float deltaTime, Maze maze) {

        // This behavior should only be used for FlyingDrone
        if (!(enemy instanceof FlyingDrone drone))
            return;

        // Convert current world position to tile coordinates
        Point enemyTile = toTile(enemy.getPosition(), maze);

        // Convert target patrol position to tile
        Point targetTile = toTile(drone.getClosestPatrolPoint(), maze);

        // If no path or finished path → recalc
        if (path.isEmpty() || index >= path.size()) {
            path = GridPathfinder.findPath(
                    maze,
                    enemyTile,
                    targetTile,
                    enemy.getPathfindingFootprint()
            );
            index = 0;
        }

        // If still no valid path, cannot move
        if (path.isEmpty())
            return;

        // Follow path
        index = SteeringBehavior.followPath(
                enemy, maze, path, index, deltaTime
        );

        // Once we’ve reached the final tile → resume patrol
        if (index >= path.size()) {
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
