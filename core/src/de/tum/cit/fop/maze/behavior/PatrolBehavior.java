package de.tum.cit.fop.maze.behavior;

import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.Array;
import de.tum.cit.fop.maze.enemies.Enemy;
import de.tum.cit.fop.maze.map.Maze;

/**
 * Patrol behavior that moves an enemy back and forth
 * between two fixed patrol points.
 *
 * The enemy walks in a straight line (horizontal or vertical)
 * and reverses direction when reaching the ends.
 */
public class PatrolBehavior implements BehaviorState {

    private static final float ARRIVAL_EPSILON = 1.0f;

    private final Array<Vector2> patrolPoints;
    private int index = 0;
    private int direction = 1;

    /**
     * Creates a patrol behavior with the given patrol points.
     *
     * @param patrolPoints exactly two world positions to patrol between
     */
    public PatrolBehavior(Array<Vector2> patrolPoints) {
        this.patrolPoints = patrolPoints;
    }

    /**
     * Returns the behavior type.
     *
     * @return PATROL behavior type
     */
    @Override
    public BehaviorStateType getType() {
        return BehaviorStateType.PATROL;
    }

    /**
     * Moves the enemy between patrol points.
     *
     * The enemy travels in a straight line and switches direction
     * when reaching a patrol point.
     *
     * @param enemy the enemy using this behavior
     * @param deltaTime time since last frame
     * @param maze the maze used for collision checks
     */
    @Override
    public void execute(Enemy enemy, float deltaTime, Maze maze) {
        if (patrolPoints.size != 2)
            return;

        Vector2 pos = enemy.getPosition();
        Vector2 target = patrolPoints.get(index);

        float dx = target.x - pos.x;
        float dy = target.y - pos.y;


        if (Math.abs(dx) <= ARRIVAL_EPSILON && Math.abs(dy) <= ARRIVAL_EPSILON) {

            // Snap exactly to patrol point
            pos.set(target);

            // Reverse at ends (A <-> B)
            if (index == patrolPoints.size - 1) direction = -1;
            else if (index == 0) direction = 1;

            index += direction;
            return;
        }


        Vector2 velocity = new Vector2();

        if (Math.abs(dx) > Math.abs(dy)) {
            // Horizontal patrol
            velocity.x = Math.signum(dx) * enemy.getSpeed();
            velocity.y = 0f;
        } else {
            // Vertical patrol
            velocity.x = 0f;
            velocity.y = Math.signum(dy) * enemy.getSpeed();
        }

        enemy.getCollisionHandler().move(
                pos,
                velocity,
                deltaTime,
                maze
        );
    }

    /**
     * Resets the patrol to the closest patrol point.
     *
     * Used when an enemy returns to patrol after chasing the player.
     *
     * @param pos the current enemy position
     */
    public void resetToClosest(Vector2 pos) {
        float best = Float.MAX_VALUE;

        for (int i = 0; i < patrolPoints.size; i++) {
            float d = patrolPoints.get(i).dst2(pos);
            if (d < best) {
                best = d;
                index = i;
            }
        }

        direction = (index == 0) ? 1 : -1;

        pos.set(patrolPoints.get(index));
    }

}


