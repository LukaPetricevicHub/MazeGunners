package de.tum.cit.fop.maze.enemies;

import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.Array;
import de.tum.cit.fop.maze.behavior.PatrolBehavior;
import de.tum.cit.fop.maze.player.Player;

public class RobotGuard extends Enemy {

    public RobotGuard(Vector2 position, float speed, Player player) {
        super(position, speed, new PatrolBehavior(testPatrolPoints()), player); // Replace with actual patrol points
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

    // Update the enemy's position
    @Override
    public void updateEnemyPosition(float deltaTime) {
        if (currentBehavior != null)
            currentBehavior.execute(this, deltaTime);
        collisionBox.setPosition(position.x, position.y);
    }


}
