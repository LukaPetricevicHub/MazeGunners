package de.tum.cit.fop.maze.util;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Preferences;
import de.tum.cit.fop.maze.hud.Hud;

import java.util.HashMap;
import java.util.Map;

/**
 * Manages game achievements and their persistent state.
 *
 * Achievements are registered on startup, stored using libGDX
 * preferences, and unlocked via in-game events. When an achievement
 * is unlocked, the player is notified through the HUD.
 */
public class AchievementManager {
    private final Preferences prefs;
    private final Map<String, Achievement> achievements;
    private Hud hud;

    /**
     * Registers all achievements and loads their saved unlock states.
     */
    public AchievementManager() {
        // Use a specific preference file for achievements
        this.prefs = Gdx.app.getPreferences("MazeRunner_Achievements");
        this.achievements = new HashMap<>();

        // Define your achievements here!
        register("win_it_big", "High Roller", "Win a Blackjack hand with a bet over 1000 coins.");
        register("first_blood", "First Blood", "Kill your first enemy.");
        register("level_2", "Novice Runner", "Reach Level 2.");
        register("collector", "Keymaster", "Find the key.");
        register("god_mode", "Cheater", "Use a cheat code.");
    }

    /**
     * Registers a single achievement definition.
     *
     * @param id unique achievement identifier
     * @param title display title
     * @param desc description text
     */
    private void register(String id, String title, String desc) {
        Achievement a = new Achievement(id, title, desc);
        // Load saved status immediately
        if (prefs.contains(id)) {
            a.setUnlocked(true);
        }
        achievements.put(id, a);
    }

    /**
     * Sets the HUD used for achievement pop-up notifications.
     *
     * @param hud HUD instance to notify, or null to disable pop-ups
     */
    public void setHud(Hud hud) {
        this.hud = hud;
    }

    /**
     * Unlocks an achievement by its identifier.
     *
     * The achievement is persisted and a notification is shown
     * if it was not previously unlocked.
     *
     * @param id achievement identifier
     * @return true if newly unlocked, false if already unlocked or unknown
     */
    public boolean unlockAchievement(String id) {
        Achievement a = achievements.get(id);
        if (a != null && !a.isUnlocked()) {
            // Mark as unlocked
            a.setUnlocked(true);

            // Notify Player
            System.out.println("ACHIEVEMENT UNLOCKED: " + a.getTitle());
            if (hud != null) {
                hud.showAchievementPopup(a.getTitle(), a.getDescription());
            }
            return true;
        }
        return false;
    }

    /**
     * Resets all achievements to the locked state.
     */
    public void resetAll() {
        for (Achievement a : achievements.values()) {
            a.setUnlocked(false);
        }
    }

    /**
     * Backwards-compatible unlock wrapper.
     *
     * @param id achievement identifier
     */
    public void unlock(String id) {
        unlockAchievement(id);
    }

    /**
     * Retrieves the IDs of all unlocked achievements.
     *
     * @return list of unlocked achievement IDs
     */
    public java.util.ArrayList<String> getUnlockedIds() {
        java.util.ArrayList<String> ids = new java.util.ArrayList<>();
        for (Achievement a : achievements.values()) {
            if (a.isUnlocked()) ids.add(a.getId());
        }
        return ids;
    }

    /**
     * Applies a list of unlocked achievement IDs.
     *
     * @param ids collection of achievement IDs to mark as unlocked
     */
    public void applyUnlockedIds(java.util.Collection<String> ids) {
        resetAll();
        if (ids == null) return;
        for (String id : ids) {
            Achievement a = achievements.get(id);
            if (a != null) a.setUnlocked(true);
        }
    }

    /**
     * Returns all noted achievements.
     *
     * @return map of achievement IDs to achievement objects
     */
    public Map<String, Achievement> getAchievements() {
        return achievements;
    }
}
