package de.tum.cit.fop.maze.behavior;

import de.tum.cit.fop.maze.enemies.Enemy;
import de.tum.cit.fop.maze.level.Maze;

public class RetreatBehavior implements BehaviorState {

    @Override
    public BehaviorStateType getType() {
        return BehaviorStateType.RETREAT;
    }

    @Override
    public void execute(Enemy enemy, float delta, Maze maze) {
        // Logic for returning to a home base or spawn point
    }
}
