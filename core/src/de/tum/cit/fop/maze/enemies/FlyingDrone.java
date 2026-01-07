package de.tum.cit.fop.maze.enemies;

import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.Array;
import de.tum.cit.fop.maze.behavior.BehaviorState;
import de.tum.cit.fop.maze.behavior.ChaseBehavior;
import de.tum.cit.fop.maze.behavior.PatrolBehavior;
import de.tum.cit.fop.maze.player.Player;

public class FlyingDrone extends Enemy {

    private BehaviorState patrolBehavior;
    private BehaviorState chaseBehavior;
    private float chaseDistance = 3f; // distance to player to switch to chase

    public FlyingDrone(Vector2 position, float speed, Array<Vector2> patrolPoints, Player player) {
        super(position, speed, null, player);
        patrolBehavior = new PatrolBehavior(patrolPoints);
        chaseBehavior = new ChaseBehavior();
        this.currentBehavior = patrolBehavior;
    }

    // Example for behavior switch
    @Override
    public void updateEnemyPosition(float deltaTime) {
        // Switch behavior based on distance to player
        if (position.dst(player.getX(), player.getY()) < chaseDistance)
            currentBehavior = chaseBehavior;
        else
            currentBehavior = patrolBehavior;

        // Execute current behavior
        if (currentBehavior != null)
            currentBehavior.execute(this, deltaTime);

        collisionBox.setPosition(position.x, position.y);
    }
}

