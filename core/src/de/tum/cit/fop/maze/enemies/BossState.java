package de.tum.cit.fop.maze.enemies;

/**
 * Animation and behavior states for the boss.
 */
public enum BossState {
    /** Boss is idle. */
    IDLE,
    /** Boss is walking. */
    WALK,
    /** Boss is running. */
    RUN,
    /** Alternate run animation. */
    RUN_2,
    /** Boss is attacking. */
    ATTACK,
    /** Boss is performing a combo attack. */
    COMBO,
    /** Boss is taking a hit. */
    HIT,
    /** Boss is dying. */
    DEATH
}
