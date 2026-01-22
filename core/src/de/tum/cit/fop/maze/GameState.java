package de.tum.cit.fop.maze;
public class GameState {

    // --- Global Progress ---
    public int level;
    public int score;

    // --- Player Stats ---
    public int lives;
    public int experience;

    // --- Skills ---
    // (We need these to restore your upgrades)
    public int skillSpeedLevel;
    public int skillHealthLevel;

    // --- Inventory ---
    public boolean hasGun;

    // --- Position ---
    // (Optional: We save this, but we might just spawn you at the start of the level
    // if the maze regenerates randomly)
    public float playerX;
    public float playerY;

    public boolean shouldRestorePosition = true;

    public int playerLevel;

    public boolean godMode;
    public boolean noClip;

    // IMPORTANT: An empty constructor is required for the JSON loader to work!
    public GameState() {
    }
}