package de.tum.cit.fop.maze.behavior;

import com.badlogic.gdx.math.Vector2;
import de.tum.cit.fop.maze.enemies.Enemy;
import de.tum.cit.fop.maze.level.Maze;

public class ChaseBehavior implements BehaviorState {

    @Override
    public BehaviorStateType getType() {
        return BehaviorStateType.CHASE;
    }

    @Override
    public void execute(Enemy enemy, float deltaTime, Maze maze) {

        Vector2 enemyPos = enemy.getPosition();
        Vector2 playerPos = enemy.getPlayerPosition();

        Vector2 direction = playerPos.cpy().sub(enemyPos);

        if (direction.isZero(0.01f))
            return;

        Vector2 velocity =
                direction.nor().scl(enemy.getSpeed());

        enemy.getCollisionHandler().move(
                enemyPos,
                velocity,
                deltaTime,
                maze
        );
    }
}

