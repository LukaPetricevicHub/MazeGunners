package de.tum.cit.fop.maze;

import java.util.ArrayList;

public class GameState {

    public boolean isEndless;
    public int currentFloor;


    // --- Global Progress ---
    public int level;
    public int score;
    public long mazeSeed;

    // --- Player Stats ---
    public int lives;
    public int experience;

    // --- Skills ---
    // (We need these to restore your upgrades)
    public int skillSpeedLevel;
    public int skillHealthLevel;

    // --- Inventory ---
    public boolean hasGun;
    public boolean hasKey;

    // --- Position ---
    // (Optional: We save this, but we might just spawn you at the start of the level
    // if the maze regenerates randomly)
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

    public static class EnemyState {
        public float x, y;
        public String type; // "robot", "slime", "drone"
        public int health; // Optional, if you want to save enemy HP
    }

    public static class ItemState {
        public float x, y;
        public String type; // "gun", "heart", "speed", "shield", "key"
    }

    public boolean shouldRestorePosition = true;

    public int playerLevel;

    public boolean godMode;
    public boolean noClip;

    // --- Achievements (save-specific) ---
    public ArrayList<String> unlockedAchievements = new ArrayList<>();

    // IMPORTANT: An empty constructor is required for the JSON loader to work!
    public GameState() {
    }
}
