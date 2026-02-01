package de.tum.cit.fop.maze.progress;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Preferences;

/**
 * Handles permanent player progression using libGDX Preferences.
 *
 * Stores data such as coins, permanent upgrades, unlocked weapons,
 * and story completion status across game sessions.
 */
public class Progression {

    private static final String PREF_NAME = "maze_progress";
    private final Preferences prefs;

    /**
     * Creates a new Progression instance backed by persistent preferences.
     */
    public Progression() {
        prefs = Gdx.app.getPreferences(PREF_NAME);
    }

    /**
     * @return true if story mode was completed
     */
    public boolean isStoryCompleted() {
        return prefs.getBoolean("story_completed", false);
    }

    /**
     * Sets the story completion flag.
     *
     * @param value completion state
     */
    public void setStoryCompleted(boolean value) {
        prefs.putBoolean("story_completed", value);
        prefs.flush();
    }


    /**
     * Handles coin management.
     *
     * Provides methods to get, add, set, and spend coins.
     * Coin values are always kept non-negative.
     */
    public int getCoins() {
        return prefs.getInteger("coins", 0);
    }

    public void setCoins(int coins) {
        prefs.putInteger("coins", Math.max(0, coins));
        prefs.flush();
    }

    public void addCoins(int amount) {
        if (amount <= 0) return;
        setCoins(getCoins() + amount);
    }

    public boolean spendCoins(int amount) {
        if (amount <= 0) return true;
        int current = getCoins();
        if (current < amount) return false;
        setCoins(current - amount);
        return true;
    }

    /**
     * Handles permanent player upgrades.
     *
     * Stores and updates persistent stats such as hearts, speed, strength,
     * damage and speed multipliers, skill points, and unlocked weapons.
     */
    public int getPermanentHearts() {
        return prefs.getInteger("perm_hearts", 0);
    }

    public void addPermanentHeart() {
        int hearts = getPermanentHearts();
        prefs.putInteger("perm_hearts", hearts + 1);
        prefs.flush();
    }


    public int getPermanentSpeed() {
        return prefs.getInteger("perm_speed", 5);
    }

    public int getPermanentStrength() {
        return prefs.getInteger("perm_strength", 5);
    }

    public void setPermanentSpeed(int v) {
        v = Math.max(1, Math.min(10, v));
        prefs.putInteger("perm_speed", v);
        prefs.flush();
    }

    public void setPermanentStrength(int v) {
        v = Math.max(1, Math.min(10, v));
        prefs.putInteger("perm_strength", v);
        prefs.flush();
    }

    public void addPermanentSpeed(int delta) {
        setPermanentSpeed(getPermanentSpeed() + delta);
    }

    public void addPermanentStrength(int delta) {
        setPermanentStrength(getPermanentStrength() + delta);
    }

    public float getPermanentDamageMult() {
        return prefs.getFloat("perm_dmg_mult", 1.0f);
    }

    public void setPermanentDamageMult(float v) {
        prefs.putFloat("perm_dmg_mult", Math.max(0.5f, Math.min(3f, v)));
        prefs.flush();
    }

    public float getPermanentSpeedMult() {
        return prefs.getFloat("perm_speed_mult", 1.0f);
    }

    public void setPermanentSpeedMult(float v) {
        prefs.putFloat("perm_speed_mult", Math.max(0.5f, Math.min(3f, v)));
        prefs.flush();
    }


    public boolean hasFreezeWeapon() {
        return prefs.getBoolean("freeze_weapon", false);
    }

    public void unlockFreezeWeapon() {
        prefs.putBoolean("freeze_weapon", true);
        prefs.flush();
    }

    public boolean hasPoisonKnuckles() {
        return prefs.getBoolean("poison_knuckles", false);
    }

    public void unlockPoisonKnuckles() {
        prefs.putBoolean("poison_knuckles", true);
        prefs.flush();
    }


    public int getPermanentSkillPoints() {
        return prefs.getInteger("perm_skill_points", 0);
    }

    public void setPermanentSkillPoints(int v) {
        prefs.putInteger("perm_skill_points", Math.max(0, v));
        prefs.flush();
    }

    public void addPermanentSkillPoints(int delta) {
        setPermanentSkillPoints(getPermanentSkillPoints() + delta);
    }


    public int getPermanentSpeedStat() {
        return getPermanentSpeed();
    }

    public void setPermanentSpeedStat(int v) {
        setPermanentSpeed(v);
    }

    public void setHasFreezeWeapon(boolean v) {
        prefs.putBoolean("freeze_weapon", v);
        prefs.flush();
    }

    public void setHasPoisonKnuckles(boolean v) {
        prefs.putBoolean("poison_knuckles", v);
        prefs.flush();
    }


    /**
     * Handles casino-related values and full progression reset.
     */
    public int getCasinoHearts() {
        return prefs.getInteger("casino_hearts", 5);
    }

    public void setCasinoHearts(int hearts) {
        prefs.putInteger("casino_hearts", Math.max(0, hearts));
        prefs.flush();
    }


    public void wipeAllProgress() {
        prefs.clear();
        prefs.flush();
    }
}
