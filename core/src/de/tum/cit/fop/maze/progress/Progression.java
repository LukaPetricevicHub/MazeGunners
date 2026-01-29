package de.tum.cit.fop.maze.progress;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Preferences;

/**
 * Stores permanent progression across sessions:
 * - Story completion (unlocks Endless)
 * - Permanent rewards from Gambling (skill points, hearts, etc.)
 */
public class Progression {

    private static final String PREFS_NAME = "maze_runner_progress";

    private static final String KEY_STORY_COMPLETED = "story_completed";
    private static final String KEY_PERM_SKILL_POINTS = "perm_skill_points";
    private static final String KEY_PERM_HEARTS = "perm_hearts";
    private static final String KEY_ATTACK_GLOVE_LEVEL = "attack_glove_level";
    private static final String KEY_OLD_GAMBLER = "old_gambler_unlocked";

    private final Preferences prefs;

    public Progression() {
        prefs = Gdx.app.getPreferences(PREFS_NAME);
    }

    public boolean isStoryCompleted() {
        return prefs.getBoolean(KEY_STORY_COMPLETED, false);
    }

    public void setStoryCompleted(boolean completed) {
        prefs.putBoolean(KEY_STORY_COMPLETED, completed);
        prefs.flush();
    }

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
