package de.tum.cit.fop.maze;

import java.util.ArrayList;

public class GameState {

    public boolean isEndless;
    public int currentFloor;


    
    public int level;
    public int score;
    public long mazeSeed;

    
    public int lives;
    public int experience;

    
    
    public int skillSpeedLevel;
    public int skillHealthLevel;

    
    public boolean hasGun;
    public boolean hasKey;
    public int ammoInClip;
    public int ammoReserve;

    
    
    
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
    public ArrayList<String> unlockedAchievements = new ArrayList<>();

    public static class EnemyState {
        public float x, y;
        public String type; 
        public int health; 
    }

    public static class ItemState {
        public float x, y;
        public String type; 
    }

    public boolean shouldRestorePosition = true;

    public int playerLevel;

    public boolean godMode;
    public boolean noClip;

    
    public int runGambleSpeed = 5;
    public int runGambleStrength = 5;
    public int runGambleHearts = 0;
    public float runGambleDamageMult = 1f;
    public float runGambleSpeedMult = 1f;
    public boolean runGambleFreeze = false;
    public boolean runGamblePoison = false;

    
    public GameState() {
    }
}
