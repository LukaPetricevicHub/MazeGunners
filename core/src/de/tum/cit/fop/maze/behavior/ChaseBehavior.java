package de.tum.cit.fop.maze.behavior;

import com.badlogic.gdx.math.Vector2;
import de.tum.cit.fop.maze.enemies.Enemy;

public class ChaseBehavior implements BehaviorState {

    @Override
    public BehaviorStateType getType() {
        return BehaviorStateType.CHASE;
    }

    @Override
    public void execute(Enemy enemy, float delta) {
        Vector2 target = enemy.getPlayerPosition();
        Vector2 direction = target.cpy().sub(enemy.getPosition());
        enemy.moveEnemy(direction, delta);

        // Terminal test
        System.out.println("Chasing enemy at "
                + enemy.getPosition().x + ", "
                + enemy.getPosition().y);
    }
}

