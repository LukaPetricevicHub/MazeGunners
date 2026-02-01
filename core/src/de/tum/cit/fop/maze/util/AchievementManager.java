package de.tum.cit.fop.maze.util;

import de.tum.cit.fop.maze.hud.Hud;

import java.util.HashMap;
import java.util.Map;

public class AchievementManager {
    private final Map<String, Achievement> achievements;
    private Hud hud; // Reference to HUD to show popup

    public AchievementManager() {
        this.achievements = new HashMap<>();

        // Define your achievements here!
        register("win_it_big", "High Roller", "Win a Blackjack hand with a bet over 1000 coins.");
        register("first_blood", "First Blood", "Kill your first enemy.");
        register("level_2", "Novice Runner", "Reach Level 2.");
        register("collector", "Keymaster", "Find the key.");
        register("god_mode", "Cheater", "Use a cheat code.");
    }

    private void register(String id, String title, String desc) {
        Achievement a = new Achievement(id, title, desc);
        achievements.put(id, a);
    }

    public void setHud(Hud hud) {
        this.hud = hud;
    }

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

    public void resetAll() {
        for (Achievement a : achievements.values()) {
            a.setUnlocked(false);
        }
    }

    // Backwards-compatible wrapper
    public void unlock(String id) {
        unlockAchievement(id);
    }

    public java.util.ArrayList<String> getUnlockedIds() {
        java.util.ArrayList<String> ids = new java.util.ArrayList<>();
        for (Achievement a : achievements.values()) {
            if (a.isUnlocked()) ids.add(a.getId());
        }
        return ids;
    }

    public void applyUnlockedIds(java.util.Collection<String> ids) {
        resetAll();
        if (ids == null) return;
        for (String id : ids) {
            Achievement a = achievements.get(id);
            if (a != null) a.setUnlocked(true);
        }
    }

    public Map<String, Achievement> getAchievements() {
        return achievements;
    }
}
