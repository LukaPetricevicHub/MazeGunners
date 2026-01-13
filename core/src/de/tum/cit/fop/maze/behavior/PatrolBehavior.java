package de.tum.cit.fop.maze.behavior;

import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.Array;
import de.tum.cit.fop.maze.enemies.Enemy;
import de.tum.cit.fop.maze.map.Maze;

public class PatrolBehavior implements BehaviorState {

    // Attributes
    private final Array<Vector2> patrolWorldPositions;
    private int currentPatrolIndex = 0;


    // Creates patrol behavior
    public PatrolBehavior(Array<Vector2> patrolWorldPositions) {
        this.patrolWorldPositions = patrolWorldPositions;
    }


    // Identify behavior
    @Override
    public BehaviorStateType getType() {
        return BehaviorStateType.PATROL;
    }


    // Move enemy along patrol points
    @Override
    public void execute(Enemy enemy, float deltaTime, Maze maze) {

        if (patrolWorldPositions.size == 0)
            return;

        Vector2 currentTarget = patrolWorldPositions.get(currentPatrolIndex);
        Vector2 direction = currentTarget.cpy().sub(enemy.getPosition());

        if (direction.len() < 2f) {
            currentPatrolIndex++;

            if (currentPatrolIndex >= patrolWorldPositions.size)
                currentPatrolIndex = 0;

            return;
        }

        Vector2 velocity =
                direction.nor().scl(enemy.getSpeed());

        enemy.getCollisionHandler().move(
                enemy.getPosition(),
                velocity,
                deltaTime,
                maze
        );
    }
}

