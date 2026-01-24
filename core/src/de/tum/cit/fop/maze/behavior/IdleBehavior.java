package de.tum.cit.fop.maze.behavior;

import de.tum.cit.fop.maze.enemies.Enemy;
import de.tum.cit.fop.maze.map.Maze;

public class IdleBehavior implements BehaviorState {

    @Override
    public BehaviorStateType getType() {
        return BehaviorStateType.IDLE;
    }

    @Override
    public void execute(Enemy enemy, float deltaTime, Maze maze) {
        // intentionally empty: enemy should do nothing while idle
        // (you can optionally reset animation timers here if needed)
    }
}


