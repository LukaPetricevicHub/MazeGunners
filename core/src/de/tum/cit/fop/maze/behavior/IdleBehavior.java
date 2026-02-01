package de.tum.cit.fop.maze.behavior;

import de.tum.cit.fop.maze.enemies.Enemy;
import de.tum.cit.fop.maze.map.Maze;

/**
 * Idle behavior where the enemy does nothing.
 *
 * This behavior is used when the enemy is not patrolling,
 * chasing the player, or performing any other action.
 */
public class IdleBehavior implements BehaviorState {

    /**
     * Returns the behavior type.k
     *
     * @return IDLE behavior type
     */
    @Override
    public BehaviorStateType getType() {
        return BehaviorStateType.IDLE;
    }

    /**
     * Executes idle behavior.
     *
     * The enemy stays in place and performs no actions.
     *
     * @param enemy the enemy using this behavior
     * @param deltaTime time since last frame
     * @param maze the maze (not used here)
     */
    @Override
    public void execute(Enemy enemy, float deltaTime, Maze maze) {}
}


