package de.tum.cit.fop.maze.gambling;

/**
 * Stores temporary run-specific gamble modifiers.
 */
public class RunGambleState {
    /** Speed stat modifier for the current run. */
    public int speed = 5;
    /** Strength stat modifier for the current run. */
    public int strength = 5;
    /** Bonus hearts granted for the current run. */
    public int bonusHearts = 0;
    /** Damage multiplier applied to the player. */
    public float damageMult = 1f;
    /** Speed multiplier applied to the player. */
    public float speedMult = 1f;
    /** Whether freeze weapon effect is active. */
    public boolean freezeWeapon = false;
    /** Whether poison knuckles effect is active. */
    public boolean poisonKnuckles = false;

    /**
     * Resets all modifiers to their default values.
     */
    public void resetToDefaults() {
        speed = 5;
        strength = 5;
        bonusHearts = 0;
        damageMult = 1f;
        speedMult = 1f;
        freezeWeapon = false;
        poisonKnuckles = false;
    }
}
