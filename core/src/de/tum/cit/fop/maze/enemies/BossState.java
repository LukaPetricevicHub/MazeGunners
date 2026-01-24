package de.tum.cit.fop.maze.enemies;

public enum BossState {
    IDLE,
    CHASE,

    MELEE_WINDUP,
    MELEE_ATTACK,

    LINE_WINDUP,
    LINE_ATTACK,

    AOE_WINDUP,
    AOE_ATTACK,

    DASH_WINDUP,
    DASHING,

    RECOVER,

    DEAD
}
