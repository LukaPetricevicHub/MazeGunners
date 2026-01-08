package de.tum.cit.fop.maze.behavior;

import de.tum.cit.fop.maze.enemies.Enemy;

public interface BehaviorState {
    BehaviorStateType getType();    // Testing

    void execute(Enemy enemy, float delta);
}
