package de.tum.cit.fop.maze.behavior;

public class PatrolBehavior implements BehaviorState {


    @Override
    public BehaviorStateType getType() {
        return BehaviorStateType.PATROL;
    }
}
