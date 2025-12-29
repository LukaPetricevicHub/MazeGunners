package de.tum.cit.fop.maze.behavior;

import de.tum.cit.fop.maze.enemies.Enemy;

public class PatrolBehavior implements BehaviorState {


    @Override
    public BehaviorStateType getType() {
        return BehaviorStateType.PATROL;
    }

    @Override
    public void execute(Enemy enemy, float delta) {

    }
}
