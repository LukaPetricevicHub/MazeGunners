package de.tum.cit.fop.maze.ai;

import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.Array;

public class EntrancePatrol {

    private final Array<Vector2> patrolPoints;

    public EntrancePatrol(Array<Vector2> patrolPoints) {
        this.patrolPoints = patrolPoints;
    }

    public Array<Vector2> getPatrolPoints() {
        return patrolPoints;
    }
}
