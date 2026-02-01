package de.tum.cit.fop.maze;

import java.util.ArrayList;

public class GameState {

    public boolean isEndless;
    public int currentFloor;

    // Global Progress
    public int level;
    public int score;
    public long mazeSeed;

    // Player Stats
    public int lives;
    public int experience;

    // Skills
    public int skillSpeedLevel;
    public int skillHealthLevel;

    // Inventory
    public boolean hasGun;
    public boolean hasKey;

    // Position
    public float playerX;
    public float playerY;

    public int skillPoints;
    public boolean skillRapidFire;
    public boolean skillShotgun;
    public boolean skillPiercing;

    public boolean skillDash;
    public boolean skillGhost;

    public boolean skillThorns;
    public boolean skillRepulse;

    public ArrayList<EnemyState> enemies = new ArrayList<>();
    public ArrayList<ItemState> items = new ArrayList<>();

    /**
     * Represents the saved state of an enemy.
     *
     * Used to restore enemies when loading a saved game.
     */
    public static class EnemyState {
        public float x, y;
        public String type;
        public int health;
    }

    /**
     * Represents the saved state of an item.
     *
     * Used to restore collectable items when loading a saved game.
     */
    public static class ItemState {
        public float x, y;
        public String type;
    }

    public boolean shouldRestorePosition = true;

    public int playerLevel;

    public boolean godMode;
    public boolean noClip;

    // Achievements
    public ArrayList<String> unlockedAchievements = new ArrayList<>();
    public GameState() {
    }
}
