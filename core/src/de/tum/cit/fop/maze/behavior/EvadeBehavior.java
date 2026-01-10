package de.tum.cit.fop.maze.behavior;

import de.tum.cit.fop.maze.enemies.Enemy;
import de.tum.cit.fop.maze.level.Maze;

public class EvadeBehavior implements BehaviorState {

    @Override
    public BehaviorStateType getType() {
        return BehaviorStateType.EVADE;
    }

    @Override
    public void execute(Enemy enemy, float delta, Maze maze) {
        // Logic for running away from player (to be implemented later)
    }
}
