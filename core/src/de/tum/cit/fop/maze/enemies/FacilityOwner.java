package de.tum.cit.fop.maze.enemies;

import com.badlogic.gdx.math.Vector2;
import de.tum.cit.fop.maze.behavior.BehaviorState;

public class FacilityOwner extends Enemy{


    public FacilityOwner(Vector2 position, float speed, BehaviorState currentBehavior) {
        super(position, speed, currentBehavior);
    }

    @Override
    public Vector2 getPlayerPosition() {
        return null;
    }
}
