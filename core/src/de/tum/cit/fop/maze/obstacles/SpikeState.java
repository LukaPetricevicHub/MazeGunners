package de.tum.cit.fop.maze.obstacles;

/**
 * States for the spike trap animation cycle.
 */
public enum SpikeState {
    /** Trap is idle and retracted. */
    IDLE,
    /** Trap is extending upward. */
    EXTENDING,
    /** Trap is fully active and dangerous. */
    ACTIVE,
    /** Trap is retracting back down. */
    RETRACTING
}
