package de.tum.cit.fop.maze.ai;

import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.Array;

/**
 * Represents a patrol path built around a room entrance.
 *
 * The patrol consists of world-space points that an enemy
 * can move between while guarding an entrance.
 */
public class EntrancePatrol {

    private final Array<Vector2> patrolPoints;

    /**
     * Creates a new entrance patrol.
     *
     * @param patrolPoints ordered list of patrol points in world coordinates
     */
    public EntrancePatrol(Array<Vector2> patrolPoints) {
        this.patrolPoints = patrolPoints;
    }

    /**
     * Returns the patrol points of this entrance patrol.
     *
     * @return patrol points in world coordinates
     */
    public Array<Vector2> getPatrolPoints() {
        return patrolPoints;
    }
}
