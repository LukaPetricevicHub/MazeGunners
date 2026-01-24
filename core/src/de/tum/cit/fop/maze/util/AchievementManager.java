package de.tum.cit.fop.maze.util;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Preferences;
import de.tum.cit.fop.maze.hud.Hud;

import java.util.HashMap;
import java.util.Map;

public class AchievementManager {
    private final Preferences prefs;
    private final Map<String, Achievement> achievements;
    private Hud hud; // Reference to HUD to show popup

    public AchievementManager() {
        // Use a specific preference file for achievements
        this.prefs = Gdx.app.getPreferences("MazeRunner_Achievements");
        this.achievements = new HashMap<>();

        // Define your achievements here!
        register("first_blood", "First Blood", "Kill your first enemy.");
        register("level_2", "Novice Runner", "Reach Level 2.");
        register("collector", "Keymaster", "Find the key.");
        register("god_mode", "Cheater", "Use a cheat code.");
    }

    private void register(String id, String title, String desc) {
        Achievement a = new Achievement(id, title, desc);
        // Load saved status immediately
        if (prefs.contains(id)) {
            a.setUnlocked(true);
        }
        achievements.put(id, a);
    }

    public void setHud(Hud hud) {
        this.hud = hud;
    }

    public void unlock(String id) {
        Achievement a = achievements.get(id);
        if (a != null && !a.isUnlocked()) {
            // Mark as unlocked
            a.setUnlocked(true);

            // Save to file
            prefs.putBoolean(id, true);
            prefs.flush();

            // Notify Player
            System.out.println("ACHIEVEMENT UNLOCKED: " + a.getTitle());
            if (hud != null) {
                hud.showNotification("ACHIEVEMENT UNLOCKED!\n" + a.getTitle());
                // You can play a specific jingle sound here if you want
            }
        }
    }

    public Map<String, Achievement> getAchievements() {
        return achievements;
    }
}