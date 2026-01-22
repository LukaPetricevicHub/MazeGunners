package de.tum.cit.fop.maze;

import com.badlogic.gdx.Game;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.audio.Sound;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.Color; // Added
import com.badlogic.gdx.graphics.Pixmap; // Added
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Animation;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.Json;
import de.tum.cit.fop.maze.util.AchievementManager;
import games.spooky.gdx.nativefilechooser.NativeFileChooser;
import de.tum.cit.fop.maze.util.AudioManager;
import de.tum.cit.fop.maze.util.GamePreferences;
import de.tum.cit.fop.maze.player.Player;

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

    // --- NEW: Shooting Animations ---
    private Animation<TextureRegion> shootDownAnim, shootUpAnim, shootLeftAnim, shootRightAnim;

    // --- NEW: Bullet Texture ---
    private TextureRegion bulletRegion;

    private Texture tileset;
    private TextureRegion[][] tiles;

    // Sound
    private Sound wallHitSound;

    private AchievementManager achievementManager;

    // Variable to temporarily hold loaded data until GameScreen is ready
    private GameState pendingSaveState = null;

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

        goToMenu(); // Start at Main Menu

        tileset = new Texture(Gdx.files.internal("basictiles.png"));
        tiles = TextureRegion.split(tileset, 16, 16);
        if(tiles.length < 2 || tiles[0].length < 4){
            throw new IllegalStateException("basictiles.png doesn't have enough tiles!");
        }
    }

    private void createBulletTexture() {
        // Generate a 4x4 yellow pixel texture for the bullet so we don't need an image file
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

    /**
     * Switches to the Level Selection Menu.
     */
    public void goToGame() {
        this.setScreen(new LevelMenuScreen(this));
        if (menuScreen != null) {
            menuScreen.dispose();
            menuScreen = null;
        }
    }

    /**
     * Directly launches a specific level file (Used by Load Game).
     */
    public void goToLevel(int levelNumber) {
        String mapPath = "maps/level-" + levelNumber + ".properties";

        if (menuScreen != null) {
            menuScreen.dispose();
            menuScreen = null;
        }

        // Launch GameScreen directly
        this.setScreen(new GameScreen(this, mapPath));
    }


    /**
     * SAVES the current game state to "save.json".
     */
    public void saveGame(Player player, int currentLevel) {
        Json json = new Json();
        GameState state = new GameState();

        // 1. Copy data
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

        // 2. Write to file
        String text = json.toJson(state);

        // --- FIX: Create the directory if it doesn't exist ---
        FileHandle file = Gdx.files.local("saves/save.json");
        file.parent().mkdirs(); // <--- THIS PREVENTS THE CRASH
        // ----------------------------------------------------

        file.writeString(text, false);

        System.out.println("GAME SAVED! (Level " + currentLevel + ")");
    }

    /**
     * LOADS the game state from "save.json".
     */
    public boolean loadGame() {
        if (!Gdx.files.local("saves/save.json").exists()) {
            System.out.println("No save file found!");
            return false;
        }

        try {
            Json json = new Json();
            String text = Gdx.files.local("saves/save.json").readString();

            // 1. Deserialize JSON
            this.pendingSaveState = json.fromJson(GameState.class, text);

            // 2. Unlock the saved level so we are allowed to play it
            getPreferences().setMaxLevel(Math.max(getPreferences().getMaxLevel(), pendingSaveState.level));

            // 3. LAUNCH THE SAVED LEVEL DIRECTLY
            goToLevel(pendingSaveState.level);

            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    public void handleLevelTransition(Player player) {
        GameState state = new GameState();

        state.level = 0; // Irrelevant for transition
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

    public GameState getPendingSaveState() {
        return pendingSaveState;
    }

    public void clearPendingSaveState() {
        pendingSaveState = null;
    }


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
        // (Currently mapping to walking animations to prevent errors if you don't have separate sprites)
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

    // --- NEW GETTERS FOR PLAYER CLASS ---
    public Animation<TextureRegion> getShootDownAnim() { return shootDownAnim; }
    public Animation<TextureRegion> getShootUpAnim() { return shootUpAnim; }
    public Animation<TextureRegion> getShootLeftAnim() { return shootLeftAnim; }
    public Animation<TextureRegion> getShootRightAnim() { return shootRightAnim; }
    public TextureRegion getBulletRegion() { return bulletRegion; }
}