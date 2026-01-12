package de.tum.cit.fop.maze.behavior;

import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.Array;
import de.tum.cit.fop.maze.enemies.Enemy;
import de.tum.cit.fop.maze.level.Maze;

public class PatrolBehavior implements BehaviorState {

    // Attributes
    private final Array<Vector2> patrolWorldPositions;
    private int currentPatrolIndex = 0;


    private static final float ENEMY_W = 32f;
    private static final float ENEMY_H = 64f;

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

        if(patrolWorldPositions == null || patrolWorldPositions.size == 0) {
            return;
        }

        float ts = maze.getTileSize();
        float arriveEps = ts * 0.20f;

        Vector2 enemyCenter = new Vector2(enemy.getPosition().x + ENEMY_W / 2f, enemy.getPosition().y + ENEMY_H /2f);

        Vector2 currentTargetPosition = patrolWorldPositions.get(currentPatrolIndex);
        Vector2 movementDirection = currentTargetPosition.cpy().sub(enemy.getPosition());

        enemy.moveEnemy(movementDirection, deltaTime, maze);

        if (movementDirection.len() <= arriveEps) {
            currentPatrolIndex = (currentPatrolIndex + 1) % patrolWorldPositions.size;
            return;
        }
        enemy.moveEnemy(movementDirection, deltaTime, maze);
    }
}
