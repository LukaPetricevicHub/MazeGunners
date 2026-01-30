package de.tum.cit.fop.maze.progress;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Preferences;

/**
 * Stores permanent progression across sessions:
 * - Story completion (unlocks Endless)
 * - Permanent rewards from Gambling (skill points, hearts, etc.)
 * - Permanent coins bank (optional)
 */
public class Progression {

    private static final String PREFS_NAME = "maze_runner_progress";

    private static final String KEY_STORY_COMPLETED = "story_completed";
    private static final String KEY_PERM_SKILL_POINTS = "perm_skill_points";
    private static final String KEY_PERM_HEARTS = "perm_hearts";
    private static final String KEY_ATTACK_GLOVE_LEVEL = "attack_glove_level";
    private static final String KEY_OLD_GAMBLER = "old_gambler_unlocked";

    // ✅ ADD THIS
    private static final String KEY_COINS = "perm_coins";

    private final Preferences prefs;

    public Progression() {
        prefs = Gdx.app.getPreferences(PREFS_NAME);
    }

    // =========================
    // STORY
    // =========================
    public boolean isStoryCompleted() {
        return prefs.getBoolean(KEY_STORY_COMPLETED, false);
    }

    public void setStoryCompleted(boolean completed) {
        prefs.putBoolean(KEY_STORY_COMPLETED, completed);
        prefs.flush();
    }

    // =========================
    // PERMANENT COINS (BANK)
    // =========================
    public int getCoins() {
        return prefs.getInteger(KEY_COINS, 0);
    }

    public void addCoins(int amount) {
        if (amount <= 0) return;
        prefs.putInteger(KEY_COINS, getCoins() + amount);
        prefs.flush();
    }

    public boolean spendCoins(int amount) {
        if (amount <= 0) return true;
        int c = getCoins();
        if (c < amount) return false;
        prefs.putInteger(KEY_COINS, c - amount);
        prefs.flush();
        return true;
    }

    public void setCoins(int amount){
        prefs.putInteger(KEY_COINS, Math.max(0, amount));
        prefs.flush();
    }

    // =========================
    // PERMANENT REWARDS
    // =========================
    public int getPermanentSkillPoints() {
        return prefs.getInteger(KEY_PERM_SKILL_POINTS, 0);
    }

    public void addPermanentSkillPoints(int amount) {
        if (amount <= 0) return;
        prefs.putInteger(KEY_PERM_SKILL_POINTS, getPermanentSkillPoints() + amount);
        prefs.flush();
    }

    public int getPermanentHearts() {
        return prefs.getInteger(KEY_PERM_HEARTS, 0);
    }

    public void addPermanentHearts(int amount) {
        if (amount <= 0) return;
        prefs.putInteger(KEY_PERM_HEARTS, getPermanentHearts() + amount);
        prefs.flush();
    }

    public int getAttackGloveLevel() {
        return prefs.getInteger(KEY_ATTACK_GLOVE_LEVEL, 0);
    }

    public void addAttackGloveLevel(int amount) {
        if (amount <= 0) return;
        prefs.putInteger(KEY_ATTACK_GLOVE_LEVEL, getAttackGloveLevel() + amount);
        prefs.flush();
    }



    public boolean hasOldGamblerUnlocked() {
        return prefs.getBoolean(KEY_OLD_GAMBLER, false);
    }

    public void unlockOldGambler() {
        prefs.putBoolean(KEY_OLD_GAMBLER, true);
        prefs.flush();
    }
}
