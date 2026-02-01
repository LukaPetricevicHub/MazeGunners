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
    /**
     * Retrieves the current coin balance.
     *
     * @return current coin count
     */
    public int getCoins() {
        return prefs.getInteger("coins", 0);
    }

    /**
     * Sets the coin balance, clamped to non-negative values.
     *
     * @param coins new coin balance
     */
    public void setCoins(int coins) {
        prefs.putInteger("coins", Math.max(0, coins));
        prefs.flush();
    }

    /**
     * Adds coins to the balance.
     *
     * @param amount amount to add
     */
    public void addCoins(int amount) {
        if (amount <= 0) return;
        setCoins(getCoins() + amount);
    }

    /**
     * Spends coins if sufficient balance exists.
     *
     * @param amount amount to spend
     * @return true if spend succeeded, false if insufficient balance
     */
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
    /**
     * Retrieves the number of permanent bonus hearts.
     *
     * @return permanent heart bonus
     */
    public int getPermanentHearts() {
        return prefs.getInteger("perm_hearts", 0);
    }

    /**
     * Increments permanent heart bonus by one.
     */
    public void addPermanentHeart() {
        int hearts = getPermanentHearts();
        prefs.putInteger("perm_hearts", hearts + 1);
        prefs.flush();
    }


    /**
     * Retrieves permanent speed stat.
     *
     * @return permanent speed value
     */
    public int getPermanentSpeed() {
        return prefs.getInteger("perm_speed", 5);
    }

    /**
     * Retrieves permanent strength stat.
     *
     * @return permanent strength value
     */
    public int getPermanentStrength() {
        return prefs.getInteger("perm_strength", 5);
    }

    /**
     * Sets permanent speed stat.
     *
     * @param v new speed value
     */
    public void setPermanentSpeed(int v) {
        v = Math.max(1, Math.min(99, v));
        prefs.putInteger("perm_speed", v);
        prefs.flush();
    }

    /**
     * Sets permanent strength stat.
     *
     * @param v new strength value
     */
    public void setPermanentStrength(int v) {
        v = Math.max(1, Math.min(99, v));
        prefs.putInteger("perm_strength", v);
        prefs.flush();
    }

    /**
     * Adds a delta to permanent speed stat.
     *
     * @param delta amount to add
     */
    public void addPermanentSpeed(int delta) {
        setPermanentSpeed(getPermanentSpeed() + delta);
    }

    /**
     * Adds a delta to permanent strength stat.
     *
     * @param delta amount to add
     */
    public void addPermanentStrength(int delta) {
        setPermanentStrength(getPermanentStrength() + delta);
    }

    /**
     * Retrieves permanent damage multiplier.
     *
     * @return damage multiplier
     */
    public float getPermanentDamageMult() {
        return prefs.getFloat("perm_dmg_mult", 1.0f);
    }

    /**
     * Sets permanent damage multiplier.
     *
     * @param v new multiplier value
     */
    public void setPermanentDamageMult(float v) {
        prefs.putFloat("perm_dmg_mult", Math.max(0.5f, Math.min(3f, v)));
        prefs.flush();
    }

    /**
     * Retrieves permanent speed multiplier.
     *
     * @return speed multiplier
     */
    public float getPermanentSpeedMult() {
        return prefs.getFloat("perm_speed_mult", 1.0f);
    }

    /**
     * Sets permanent speed multiplier.
     *
     * @param v new multiplier value
     */
    public void setPermanentSpeedMult(float v) {
        prefs.putFloat("perm_speed_mult", Math.max(0.5f, Math.min(3f, v)));
        prefs.flush();
    }


    /**
     * Indicates whether freeze weapon is unlocked.
     *
     * @return true if unlocked
     */
    public boolean hasFreezeWeapon() {
        return prefs.getBoolean("freeze_weapon", false);
    }

    /**
     * Unlocks the freeze weapon perk.
     */
    public void unlockFreezeWeapon() {
        prefs.putBoolean("freeze_weapon", true);
        prefs.flush();
    }

    /**
     * Indicates whether poison knuckles are unlocked.
     *
     * @return true if unlocked
     */
    public boolean hasPoisonKnuckles() {
        return prefs.getBoolean("poison_knuckles", false);
    }

    /**
     * Unlocks the poison knuckles perk.
     */
    public void unlockPoisonKnuckles() {
        prefs.putBoolean("poison_knuckles", true);
        prefs.flush();
    }


    /**
     * Retrieves permanent skill points.
     *
     * @return skill points count
     */
    public int getPermanentSkillPoints() {
        return prefs.getInteger("perm_skill_points", 0);
    }

    /**
     * Sets permanent skill points.
     *
     * @param v new skill points value
     */
    public void setPermanentSkillPoints(int v) {
        prefs.putInteger("perm_skill_points", Math.max(0, v));
        prefs.flush();
    }

    /**
     * Adds a delta to permanent skill points.
     *
     * @param delta amount to add
     */
    public void addPermanentSkillPoints(int delta) {
        setPermanentSkillPoints(getPermanentSkillPoints() + delta);
    }


    /**
     * Retrieves permanent speed stat.
     *
     * @return speed stat value
     */
    public int getPermanentSpeedStat() {
        return getPermanentSpeed();
    }

    /**
     * Sets the permanent speed stat.
     *
     * @param v new speed value
     */
    public void setPermanentSpeedStat(int v) {
        setPermanentSpeed(v);
    }

    /**
     * Sets whether the freeze weapon perk is active.
     *
     * @param v true to enable, false to disable
     */
    public void setHasFreezeWeapon(boolean v) {
        prefs.putBoolean("freeze_weapon", v);
        prefs.flush();
    }

    /**
     * Sets whether the poison knuckles perk is active.
     *
     * @param v true to enable, false to disable
     */
    public void setHasPoisonKnuckles(boolean v) {
        prefs.putBoolean("poison_knuckles", v);
        prefs.flush();
    }


    /**
     * Handles casino-related values and full progression reset.
     */
    /**
     * Retrieves the casino heart count.
     *
     * @return casino heart count
     */
    public int getCasinoHearts() {
        return prefs.getInteger("casino_hearts", 5);
    }

    /**
     * Sets the casino heart count.
     *
     * @param hearts new heart count
     */
    public void setCasinoHearts(int hearts) {
        prefs.putInteger("casino_hearts", Math.max(0, hearts));
        prefs.flush();
    }


    /**
     * Clears all persisted progression data.
     */
    public void wipeAllProgress() {
        prefs.clear();
        prefs.flush();
    }
}
