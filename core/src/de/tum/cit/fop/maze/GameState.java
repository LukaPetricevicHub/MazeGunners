package de.tum.cit.fop.maze;

import java.util.ArrayList;

/**
 * Serializable container for saving and loading game state.
 */
public class GameState {

    /** Whether the run is in endless mode. */
    public boolean isEndless;
    /** Current floor in endless mode. */
    public int currentFloor;



    public int level;
    /** Current score value. */
    public int score;
    /** Seed used to generate the maze. */
    public long mazeSeed;


    public int lives;
    /** Current experience points. */
    public int experience;



    public int skillSpeedLevel;
    /** Health skill level. */
    public int skillHealthLevel;


    public boolean hasGun;
    /** Whether the player has the key. */
    public boolean hasKey;
    /** Ammo in the current clip. */
    public int ammoInClip;
    /** Ammo in reserve. */
    public int ammoReserve;




    public float playerX;
    /** Player Y position in world coordinates. */
    public float playerY;

    /** Unspent skill points. */
    public int skillPoints;
    /** Whether rapid fire is unlocked. */
    public boolean skillRapidFire;
    /** Whether shotgun is unlocked. */
    public boolean skillShotgun;
    /** Whether piercing is unlocked. */
    public boolean skillPiercing;

    /** Whether dash is unlocked. */
    public boolean skillDash;
    /** Whether ghost is unlocked. */
    public boolean skillGhost;

    /** Whether thorns is unlocked. */
    public boolean skillThorns;
    /** Whether repulse is unlocked. */
    public boolean skillRepulse;

    /** Saved enemies for restoration. */
    public ArrayList<EnemyState> enemies = new ArrayList<>();
    /** Saved items for restoration. */
    public ArrayList<ItemState> items = new ArrayList<>();
    /** Saved unlocked achievements. */
    public ArrayList<String> unlockedAchievements = new ArrayList<>();

    /**
     * Serializable enemy snapshot.
     */
    public static class EnemyState {
        /** Enemy X position. */
        public float x;
        /** Enemy Y position. */
        public float y;
        /** Enemy type identifier. */
        public String type;
        /** Optional enemy health value. */
        public int health;
    }

    /**
     * Serializable item snapshot.
     */
    public static class ItemState {
        public float x, y;
        public String type;
        public String name;
    }

    /** Whether the player position should be restored from the save. */
    public boolean shouldRestorePosition = true;

    /** Player level value. */
    public int playerLevel;

    /** Whether god mode is enabled. */
    public boolean godMode;
    /** Whether noclip is enabled. */
    public boolean noClip;
    /** Whether the story item has been collected. */
    public boolean hasStoryItem;
    /** Current story item name. */
    public String storyItemName;


    public int runGambleSpeed = 5;
    /** Run-only strength buff value. */
    public int runGambleStrength = 5;
    /** Run-only bonus hearts value. */
    public int runGambleHearts = 0;
    /** Run-only damage multiplier value. */
    public float runGambleDamageMult = 1f;
    /** Run-only speed multiplier value. */
    public float runGambleSpeedMult = 1f;
    /** Run-only freeze weapon flag. */
    public boolean runGambleFreeze = false;
    /** Run-only poison knuckles flag. */
    public boolean runGamblePoison = false;


    /**
     * No-argument constructor required for JSON deserialization.
     */
    public GameState() {
    }
}
