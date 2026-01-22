package de.tum.cit.fop.maze;

import com.badlogic.gdx.Game;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.audio.Sound;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Animation;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.scenes.scene2d.ui.Skin; // Keep Skin
// REMOVED: import com.badlogic.gdx.scenes.scene2d.ui.List; <--- THIS WAS THE ERROR
import java.util.List; // <--- ADDED CORRECT IMPORT

import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.Json;
import de.tum.cit.fop.maze.enemies.Enemy;
import de.tum.cit.fop.maze.enemies.FlyingDrone;
import de.tum.cit.fop.maze.enemies.MutantWorker;
import de.tum.cit.fop.maze.enemies.RobotGuard;
import de.tum.cit.fop.maze.items.*;
import de.tum.cit.fop.maze.util.AchievementManager;
import games.spooky.gdx.nativefilechooser.NativeFileChooser;
import de.tum.cit.fop.maze.util.AudioManager;
import de.tum.cit.fop.maze.util.GamePreferences;
import de.tum.cit.fop.maze.player.Player;
import de.tum.cit.fop.maze.score.LeaderboardManager;

/**
 * The MazeRunnerGame class represents the core of the Maze Runner game.
 * It manages the screens and global resources like SpriteBatch and Skin.
 */
public class MazeRunnerGame extends Game {
    // Screens
    private MenuScreen menuScreen;
    private GameScreen gameScreen;

    // Sprite Batch for rendering
    private SpriteBatch spriteBatch;

    // UI Skin
    private Skin skin;

    private AudioManager audioManager;
    private GamePreferences preferences;

    // Standard Walking Animations
    private Animation<TextureRegion> animDown, animUp, animLeft, animRight;

    // Shooting Animations
    private Animation<TextureRegion> shootDownAnim, shootUpAnim, shootLeftAnim, shootRightAnim;

    // Bullet Texture
    private TextureRegion bulletRegion;

    private Texture tileset;
    private TextureRegion[][] tiles;

    // Sound
    private Sound wallHitSound;

    private AchievementManager achievementManager;

    // Variable to temporarily hold loaded data until GameScreen is ready
    private GameState pendingSaveState = null;
    private LeaderboardManager leaderboardManager;


    /**
     * Constructor for MazeRunnerGame.
     * @param fileChooser The file chooser for the game.
     */
    public MazeRunnerGame(NativeFileChooser fileChooser) {
        super();
    }

    @Override
    public void create() {
        spriteBatch = new SpriteBatch();
        skin = new Skin(Gdx.files.internal("craft/craftacular-ui.json"));

        preferences = new GamePreferences();
        audioManager = new AudioManager();

        audioManager.playBackgroundMusic("music/background.mp3");

        // Load animations and create bullet texture
        this.loadCharacterAnimation();
        this.createBulletTexture();

        achievementManager = new AchievementManager();
        leaderboardManager = new LeaderboardManager();

        goToMenu(); // Start at Main Menu

        tileset = new Texture(Gdx.files.internal("basictiles.png"));
        tiles = TextureRegion.split(tileset, 16, 16);
        if(tiles.length < 2 || tiles[0].length < 4){
            throw new IllegalStateException("basictiles.png doesn't have enough tiles!");
        }
    }

    private void createBulletTexture() {
        // Generate a 4x4 yellow pixel texture
        Pixmap pixmap = new Pixmap(4, 4, Pixmap.Format.RGBA8888);
        pixmap.setColor(Color.YELLOW);
        pixmap.fill();
        bulletRegion = new TextureRegion(new Texture(pixmap));
        pixmap.dispose();
    }

    public void playWallHitSound(){
        if(wallHitSound != null){
            wallHitSound.play(0.6f);
        }
    }

    public void goToMenu() {
        if (audioManager != null) {
            audioManager.playBackgroundMusic("music/background.mp3");
        }
        this.setScreen(new MenuScreen(this));
        if (gameScreen != null) {
            gameScreen.dispose();
            gameScreen = null;
        }
        this.setScreen(new MenuScreen(this));
    }

    public void goToGame() {
        this.setScreen(new LevelMenuScreen(this));
        if (menuScreen != null) {
            menuScreen.dispose();
            menuScreen = null;
        }
    }

    public void goToLevel(int levelNumber) {
        String mapPath = "maps/level-" + levelNumber + ".properties";
        if (menuScreen != null) {
            menuScreen.dispose();
            menuScreen = null;
        }
        this.setScreen(new GameScreen(this, mapPath));
    }

    /**
     * SAVES the game, including Player stats, Enemies, and Items.
     */
    public void saveGame(Player player, int currentLevel, List<Enemy> enemies, List<AbstractCollectable> items, boolean endlessMode, int floor, long currentSeed) {
        Json json = new Json();
        GameState state = new GameState();
        state.skillPoints = player.getSkillPoints();

        state.skillRapidFire = player.hasRapidFire();
        state.skillShotgun = player.hasShotgun();
        state.skillPiercing = player.hasPiercing();

        state.skillDash = player.hasDash();
        state.skillGhost = player.hasGhost();

        state.skillThorns = player.hasThorns();
        state.skillRepulse = player.hasRepulse();

        state.isEndless = endlessMode; // Save this!
        state.currentFloor = floor;
        state.mazeSeed = currentSeed;

        // 1. Save Player Data
        state.level = currentLevel;
        state.playerLevel = player.getPlayerLevel();
        state.score = player.getScore();
        state.lives = player.getLives();
        state.experience = player.getExperience();
        state.skillSpeedLevel = player.getSkillSpeedLevel();
        state.skillHealthLevel = player.getSkillHealthLevel();
        state.hasGun = player.hasGun();
        state.godMode = player.isGodMode();
        state.noClip = player.isNoClip();
        state.playerX = player.getX();
        state.playerY = player.getY();
        state.shouldRestorePosition = true;

        // 2. Save Enemies
        if (enemies != null) {
            for (Enemy e : enemies) {
                GameState.EnemyState es = new GameState.EnemyState();
                es.x = e.getPosition().x; // Fixed getX() issue
                es.y = e.getPosition().y;

                if (e instanceof MutantWorker) es.type = "slime";
                else if (e instanceof RobotGuard) es.type = "robot";
                else if (e instanceof FlyingDrone) es.type = "drone";
                else es.type = "unknown";

                state.enemies.add(es);
            }
        }

        // 3. Save Items
        if (items != null) {
            for (AbstractCollectable i : items) {
                if (!i.isActive()) continue;

                GameState.ItemState is = new GameState.ItemState();
                is.x = i.getBounds().x;
                is.y = i.getBounds().y;

                if (i instanceof GunCollectable) is.type = "gun";
                else if (i instanceof HeartItem) is.type = "heart";
                else if (i instanceof SpeedItem) is.type = "speed";
                else if (i instanceof ShieldItem) is.type = "shield";

                state.items.add(is);
            }
        }

        // 4. Write to File
        String text = json.toJson(state);
        FileHandle file = Gdx.files.local("saves/save.json");
        file.parent().mkdirs();
        file.writeString(text, false);

        System.out.println("GAME SAVED! (Enemies: " + state.enemies.size() + ", Items: " + state.items.size() + ")");
    }

    /**
     * LOADS the game state from "save.json".
     */
    public boolean loadGame() {
        FileHandle file = Gdx.files.local("saves/save.json");
        if (!file.exists()) {
            System.out.println("No save file found!");
            return false;
        }

        try {
            Json json = new Json();
            // Read the file exactly once
            GameState state = json.fromJson(GameState.class, file.readString());
            this.pendingSaveState = state;

            // Update preferences, but DO NOT start the level yet
            getPreferences().setMaxLevel(Math.max(getPreferences().getMaxLevel(), state.level));

            // --- REMOVED THIS LINE: goToLevel(pendingSaveState.level); ---
            // This line was "stealing" the save data before the real screen could use it.

            if (state.isEndless) {
                // Now we create the CORRECT screen with the saved Floor
                setScreen(new GameScreen(this, 1, null, true, state.currentFloor));
            } else {
                // Story Mode
                String mapFile = "maps/level-" + state.level + ".properties";
                setScreen(new GameScreen(this, mapFile));
            }

            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    public void handleLevelTransition(Player player) {
        GameState state = new GameState();
        state.level = 0;
        state.score = player.getScore();
        state.lives = player.getLives();
        state.playerLevel = player.getPlayerLevel();
        state.experience = player.getExperience();
        state.skillSpeedLevel = player.getSkillSpeedLevel();
        state.skillHealthLevel = player.getSkillHealthLevel();
        state.hasGun = player.hasGun();
        state.godMode = player.isGodMode();
        state.noClip = player.isNoClip();
        state.shouldRestorePosition = true;
        this.pendingSaveState = state;
    }

    public GameState getPendingSaveState() { return pendingSaveState; }
    public void clearPendingSaveState() { pendingSaveState = null; }

    private void loadCharacterAnimation() {
        Texture walkSheet = new Texture(Gdx.files.internal("Main_character.png"));
        int frameWidth = 16;
        int frameHeight = 32;

        TextureRegion[][] tmp = TextureRegion.split(walkSheet, frameWidth, frameHeight);

        // Row 0: Down
        Array<TextureRegion> framesDown = new Array<>();
        for (int i = 0; i < 4; i++) framesDown.add(tmp[0][i]);
        animDown = new Animation<>(0.15f, framesDown, Animation.PlayMode.LOOP);

        // Row 1: Right
        Array<TextureRegion> framesRight = new Array<>();
        for (int i = 0; i < 4; i++) framesRight.add(tmp[1][i]);
        animRight = new Animation<>(0.15f, framesRight, Animation.PlayMode.LOOP);

        // Row 2: Up
        Array<TextureRegion> framesUp = new Array<>();
        for (int i = 0; i < 4; i++) framesUp.add(tmp[2][i]);
        animUp = new Animation<>(0.15f, framesUp, Animation.PlayMode.LOOP);

        // Row 3: Left
        Array<TextureRegion> framesLeft = new Array<>();
        for (int i = 0; i < 4; i++) framesLeft.add(tmp[3][i]);
        animLeft = new Animation<>(0.15f, framesLeft, Animation.PlayMode.LOOP);

        // --- SETUP SHOOTING ANIMATIONS ---
        shootDownAnim = animDown;
        shootUpAnim = animUp;
        shootLeftAnim = animLeft;
        shootRightAnim = animRight;
    }

    @Override
    public void dispose() {
        getScreen().hide();
        getScreen().dispose();
        spriteBatch.dispose();
        skin.dispose();
        tileset.dispose();
        if(wallHitSound != null) wallHitSound.dispose();
        if (audioManager != null) audioManager.dispose();
    }

    // Getters
    public Skin getSkin() { return skin; }
    public SpriteBatch getSpriteBatch() { return spriteBatch; }
    public TextureRegion getTile(int row, int col){ return tiles[row][col]; }
    public AudioManager getAudioManager() { return audioManager; }
    public GamePreferences getPreferences() { return preferences; }
    public AchievementManager getAchievementManager() { return achievementManager; }
    public Animation<TextureRegion> getAnimDown() { return animDown; }
    public Animation<TextureRegion> getAnimUp() { return animUp; }
    public Animation<TextureRegion> getAnimLeft() { return animLeft; }
    public Animation<TextureRegion> getAnimRight() { return animRight; }
    public LeaderboardManager getLeaderboardManager() { return leaderboardManager; }

    // --- NEW GETTERS FOR PLAYER CLASS ---
    public Animation<TextureRegion> getShootDownAnim() { return shootDownAnim; }
    public Animation<TextureRegion> getShootUpAnim() { return shootUpAnim; }
    public Animation<TextureRegion> getShootLeftAnim() { return shootLeftAnim; }
    public Animation<TextureRegion> getShootRightAnim() { return shootRightAnim; }
    public TextureRegion getBulletRegion() { return bulletRegion; }
}