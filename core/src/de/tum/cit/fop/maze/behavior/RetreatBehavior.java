package de.tum.cit.fop.maze.behavior;

import de.tum.cit.fop.maze.enemies.Enemy;

public class RetreatBehavior implements BehaviorState {


    @Override
    public BehaviorStateType getType() {
        return BehaviorStateType.RETREAT;
    }

    @Override
    public void execute(Enemy enemy, float delta) {

    }
}
