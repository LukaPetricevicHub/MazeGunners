package de.tum.cit.fop.maze.behavior;

import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.Array;
import de.tum.cit.fop.maze.enemies.Enemy;

public class PatrolBehavior implements BehaviorState {

    private Array<Vector2> patrolPoints;
    private int currentTargetIndex = 0;

    public PatrolBehavior(Array<Vector2> patrolPoints) {
        this.patrolPoints = patrolPoints;
    }

    @Override
    public BehaviorStateType getType() {
        return BehaviorStateType.PATROL;
    }

    @Override
    public void execute(Enemy enemy, float delta) {
        if (patrolPoints.size == 0)
            return;

        Vector2 target = patrolPoints.get(currentTargetIndex);
        Vector2 direction = target.cpy().sub(enemy.getPosition());
        enemy.moveEnemy(direction, delta);

        if (enemy.getPosition().dst(target) < 0.1f) {
            currentTargetIndex++;
            if (currentTargetIndex >= patrolPoints.size) {
                currentTargetIndex = 0;
            }
        }

        // Terminal test
        System.out.println("Patrolling enemy at "
                + enemy.getPosition().x + ", "
                + enemy.getPosition().y);
    }
}
