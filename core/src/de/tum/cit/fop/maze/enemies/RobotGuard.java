package de.tum.cit.fop.maze.enemies;

import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.Array;
import de.tum.cit.fop.maze.behavior.PatrolBehavior;

public class RobotGuard extends Enemy {

    public RobotGuard(Vector2 position, float speed) {
        super(position, speed, new PatrolBehavior(testPatrolPoints())); // Replace with actual patrol points
    }

    // Example patrol points
    private static Array<Vector2> testPatrolPoints() {
        Array<Vector2> patrolPoints = new Array<>();
        patrolPoints.add(new Vector2(2, 2));
        patrolPoints.add(new Vector2(2, 5));
        patrolPoints.add(new Vector2(5, 2));
        patrolPoints.add(new Vector2(5, 5));
        return patrolPoints;
    }

    @Override
    public Vector2 getPlayerPosition() {
        return new Vector2(5, 5); // Replace with player position
    }


}
