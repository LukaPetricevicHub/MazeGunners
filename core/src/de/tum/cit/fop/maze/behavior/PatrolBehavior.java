package de.tum.cit.fop.maze.behavior;

import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.Array;
import de.tum.cit.fop.maze.enemies.Enemy;
import de.tum.cit.fop.maze.map.Maze;

public class PatrolBehavior implements BehaviorState {

    // Attributes
    private final Array<Vector2> patrolPoints;
    private int activeIndex = 0;


    // Creates patrol behavior
    public PatrolBehavior(Array<Vector2> patrolPoints) {
        this.patrolPoints = patrolPoints;
    }


    // Identify behavior
    @Override
    public BehaviorStateType getType() {
        return BehaviorStateType.PATROL;
    }


    // Move enemy along patrol points
    @Override
    public void execute(Enemy enemy, float deltaTime, Maze maze) {

        if (patrolPoints.size == 0)
            return;

        Vector2 currentTargetPos = patrolPoints.get(activeIndex);
        Vector2 travelDir = currentTargetPos.cpy().sub(enemy.getPosition());

        float targetThreshold = 2f;
        if (travelDir.len() < targetThreshold) {
            activeIndex++;

            if(activeIndex >= patrolPoints.size)
                activeIndex = 0;
            return;
        }

        Vector2 walkVelocity = travelDir.nor().scl(enemy.getSpeed());

        enemy.getCollisionHandler().move(
                enemy.getPosition(),
                walkVelocity,
                deltaTime,
                maze
        );
    }
}
