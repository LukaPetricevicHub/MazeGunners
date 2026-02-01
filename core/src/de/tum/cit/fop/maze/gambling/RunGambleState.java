package de.tum.cit.fop.maze.gambling;

public class RunGambleState {
    public int speed = 5;
    public int strength = 5;
    public int bonusHearts = 0;
    public float damageMult = 1f;
    public float speedMult = 1f;
    public boolean freezeWeapon = false;
    public boolean poisonKnuckles = false;

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
