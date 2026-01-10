package de.tum.cit.fop.maze.behavior;

import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.Array;
import de.tum.cit.fop.maze.enemies.Enemy;
import de.tum.cit.fop.maze.level.Maze;

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


    // Move enemy
    @Override
    public void execute(Enemy enemy, float deltaTime, Maze maze) {

        if(patrolWorldPositions.size == 0)
            return;

        Vector2 currentTargetPosition = patrolWorldPositions.get(currentPatrolIndex);
        Vector2 movementDirection = currentTargetPosition.cpy().sub(enemy.getPosition());

        enemy.moveEnemy(movementDirection, deltaTime);

        if(enemy.getPosition().dst(currentTargetPosition) < 2f) {
            currentPatrolIndex++;

            if (currentPatrolIndex >= patrolWorldPositions.size) {
                currentPatrolIndex = 0;
            }
        }
    }
}
