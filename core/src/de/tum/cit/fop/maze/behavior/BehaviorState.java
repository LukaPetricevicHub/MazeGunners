package de.tum.cit.fop.maze.behavior;

import de.tum.cit.fop.maze.enemies.Enemy;
import de.tum.cit.fop.maze.map.Maze;

/**
 * Represents a behavior state for an enemy.
 *
 * Each behavior controls how an enemy acts and moves during a game update.
 */
public interface BehaviorState {

    /**
     * Returns the type of this behavior.
     *
     * @return the behavior state type
     */
    BehaviorStateType getType();


    /**
     * Executes the behavior logic for one update step.
     *
     * @param enemy the enemy using this behavior
     * @param deltaTime time since last frame (seconds)
     * @param maze the maze used for collision and movement checks
     */
    void execute(Enemy enemy, float deltaTime, Maze maze);
}
