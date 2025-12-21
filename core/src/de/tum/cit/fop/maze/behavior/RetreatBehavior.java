package de.tum.cit.fop.maze.behavior;

public class RetreatBehavior implements BehaviorState {


    @Override
    public BehaviorStateType getType() {
        return BehaviorStateType.RETREAT;
    }
}
