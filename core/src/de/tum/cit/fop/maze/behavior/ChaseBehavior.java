package de.tum.cit.fop.maze.behavior;

public class ChaseBehavior implements BehaviorState {


    @Override
    public BehaviorStateType getType() {
        return BehaviorStateType.CHASE;
    }
}
