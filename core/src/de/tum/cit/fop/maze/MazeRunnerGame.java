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
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.Json;

import java.util.List;

import de.tum.cit.fop.maze.enemies.Enemy;
import de.tum.cit.fop.maze.enemies.FlyingDrone;
import de.tum.cit.fop.maze.enemies.MutantWorker;
import de.tum.cit.fop.maze.enemies.RobotGuard;
import de.tum.cit.fop.maze.items.*;
import de.tum.cit.fop.maze.player.Player;
import de.tum.cit.fop.maze.progress.Progression;
import de.tum.cit.fop.maze.score.LeaderboardManager;
import de.tum.cit.fop.maze.ui.GamblingScreen;
import de.tum.cit.fop.maze.util.AchievementManager;
import de.tum.cit.fop.maze.util.AudioManager;
import de.tum.cit.fop.maze.util.GamePreferences;
import games.spooky.gdx.nativefilechooser.NativeFileChooser;

/**
 * Main entry point of the game.
 *
 * Initializes global resources like render, audio, preferences,
 * animations, and controls transitions between different game screens.
 * Also handles saving and loading the game state.
 */
public class MazeRunnerGame extends Game {

    private SpriteBatch spriteBatch;
    private Skin skin;

    private AudioManager audioManager;
    private GamePreferences preferences;

    // Permanent progression (endless unlock + gambling rewards)
    private Progression progression;

    // Standard Walking Animations
    private Animation<TextureRegion> animDown, animUp, animLeft, animRight;

    // Shooting Animations
    private Animation<TextureRegion> shootDownAnim, shootUpAnim, shootLeftAnim, shootRightAnim;

    // Bullet Texture
    private TextureRegion bulletRegion;

    private Texture tileset;
    private TextureRegion[][] tiles;

    private Sound wallHitSound;

    private AchievementManager achievementManager;

    private GameState pendingSaveState = null;
    private LeaderboardManager leaderboardManager;

    /**
     * Create a new MazeRunnerGame instance.
     *
     * @param fileChooser Native file chooser used for loading external files
     */
    public MazeRunnerGame(NativeFileChooser fileChooser) {
        super();
    }

    /**
     * Initialize the game when it starts.
     * Loads assets, preferences, audio, animations, and opens the main menu.
     */
    @Override
    public void create() {
        spriteBatch = new SpriteBatch();
        skin = new Skin(Gdx.files.internal("craft/craftacular-ui.json"));

        preferences = new GamePreferences();
        audioManager = new AudioManager();

        progression = new Progression();

        audioManager.playBackgroundMusic("music/background.mp3");

        loadCharacterAnimation();
        createBulletTexture();

        achievementManager = new AchievementManager();
        leaderboardManager = new LeaderboardManager();

        goToMenu();

        tileset = new Texture(Gdx.files.internal("basictiles.png"));
        tiles = TextureRegion.split(tileset, 16, 16);
        if (tiles.length < 2 || tiles[0].length < 4) {
            throw new IllegalStateException("basictiles.png doesn't have enough tiles!");
        }
    }

    private void createBulletTexture() {
        Pixmap pixmap = new Pixmap(4, 4, Pixmap.Format.RGBA8888);
        pixmap.setColor(Color.YELLOW);
        pixmap.fill();
        bulletRegion = new TextureRegion(new Texture(pixmap));
        pixmap.dispose();
    }

    public void playWallHitSound() {
        if (wallHitSound != null) {
            wallHitSound.play(0.6f * getPreferences().getSoundVolume());
        }
    }

    public void goToMenu() {
        if (audioManager != null) {
            audioManager.playBackgroundMusic("music/background.mp3");
        }
        setScreen(new MenuScreen(this));
    }


    public void goToGambling() {
        setScreen(new GamblingScreen(this));
    }

    public boolean loadGame() {
        FileHandle file = Gdx.files.local("saves/save.json");
        if (!file.exists()) {
            System.out.println("No save file found!");
            return false;
        }

        try {
            Json json = new Json();
            GameState state = json.fromJson(GameState.class, file.readString());
            this.pendingSaveState = state;

            getPreferences().setMaxLevel(Math.max(getPreferences().getMaxLevel(), state.level));

            if (state.isEndless) {
                setScreen(new GameScreen(this, 1, null, true, state.currentFloor));
            } else {
                String mapFile = "maps/level-" + state.level + ".properties";
                setScreen(new GameScreen(this, mapFile));
            }

            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Saves the current game state, including player data, enemies,
     * items, and progression information.
     *
     * @param player        The current player
     * @param currentLevel  The current level number
     * @param enemies       List of active enemies
     * @param items         List of collectable items
     * @param endlessMode   Whether the game is in endless mode
     * @param floor         Current floor in endless mode
     * @param currentSeed   Seed used for maze generation
     */
    public void saveGame(Player player, int currentLevel, List<Enemy> enemies, List<Collectable> items, boolean endlessMode, int floor, long currentSeed) {
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

        state.isEndless = endlessMode;
        state.currentFloor = floor;
        state.mazeSeed = currentSeed;

        state.level = currentLevel;
        state.playerLevel = player.getPlayerLevel();
        state.score = player.getScore();
        state.lives = player.getLives();
        state.experience = player.getExperience();
        state.skillSpeedLevel = player.getSkillSpeedLevel();
        state.skillHealthLevel = player.getSkillHealthLevel();
        state.hasGun = player.hasGun();
        state.hasKey = player.hasKey();
        state.godMode = player.isGodMode();
        state.noClip = player.isNoClip();
        state.playerX = player.getX();
        state.playerY = player.getY();
        state.shouldRestorePosition = true;

        if (enemies != null) {
            for (Enemy e : enemies) {
                GameState.EnemyState es = new GameState.EnemyState();
                es.x = e.getPosition().x;
                es.y = e.getPosition().y;

                if (e instanceof MutantWorker) es.type = "slime";
                else if (e instanceof RobotGuard) es.type = "robot";
                else if (e instanceof FlyingDrone) es.type = "drone";
                else es.type = "unknown";

                state.enemies.add(es);
            }
        }

        if (items != null) {
            for (Collectable i : items) {
                if (!i.isActive()) continue;

                GameState.ItemState is = new GameState.ItemState();
                is.x = i.getBounds().x;
                is.y = i.getBounds().y;

                if (i instanceof GunCollectable) is.type = "gun";
                else if (i instanceof HeartItem) is.type = "heart";
                else if (i instanceof SpeedItem) is.type = "speed";
                else if (i instanceof ShieldItem) is.type = "shield";
                else if (i instanceof KeyItem) is.type = "key";

                state.items.add(is);
            }
        }

        String text = json.toJson(state);
        FileHandle file = Gdx.files.local("saves/save.json");
        file.parent().mkdirs();
        file.writeString(text, false);

        System.out.println("GAME SAVED! (Enemies: " + state.enemies.size() + ", Items: " + state.items.size() + ")");
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

    /**
     * Returns a pending game state that should be restored on the next screen.
     *
     * @return The pending game state or null if none exists
     */
    public GameState getPendingSaveState() { return pendingSaveState; }
    public void clearPendingSaveState() { pendingSaveState = null; }

    private void loadCharacterAnimation() {
        Texture walkSheet = new Texture(Gdx.files.internal("Main_character.png"));
        int frameWidth = 16;
        int frameHeight = 32;

        TextureRegion[][] tmp = TextureRegion.split(walkSheet, frameWidth, frameHeight);

        Array<TextureRegion> framesDown = new Array<>();
        for (int i = 0; i < 4; i++) framesDown.add(tmp[0][i]);
        animDown = new Animation<>(0.15f, framesDown, Animation.PlayMode.LOOP);

        Array<TextureRegion> framesRight = new Array<>();
        for (int i = 0; i < 4; i++) framesRight.add(tmp[1][i]);
        animRight = new Animation<>(0.15f, framesRight, Animation.PlayMode.LOOP);

        Array<TextureRegion> framesUp = new Array<>();
        for (int i = 0; i < 4; i++) framesUp.add(tmp[2][i]);
        animUp = new Animation<>(0.15f, framesUp, Animation.PlayMode.LOOP);

        Array<TextureRegion> framesLeft = new Array<>();
        for (int i = 0; i < 4; i++) framesLeft.add(tmp[3][i]);
        animLeft = new Animation<>(0.15f, framesLeft, Animation.PlayMode.LOOP);

        shootDownAnim = animDown;
        shootUpAnim = animUp;
        shootLeftAnim = animLeft;
        shootRightAnim = animRight;
    }

    public void markStoryCompleted() {
        progression.setStoryCompleted(true);
    }

    /**
     * Disposes all loaded resources when the game is closed.
     */
    @Override
    public void dispose() {
        if (getScreen() != null) {
            getScreen().hide();
            getScreen().dispose();
        }

        if (spriteBatch != null) spriteBatch.dispose();
        if (skin != null) skin.dispose();
        if (tileset != null) tileset.dispose();
        if (wallHitSound != null) wallHitSound.dispose();
        if (audioManager != null) audioManager.dispose();
    }

    /**
     * Getters & Setters
     */
    public Skin getSkin() { return skin; }
    public SpriteBatch getSpriteBatch() { return spriteBatch; }
    public TextureRegion getTile(int row, int col) { return tiles[row][col]; }
    public AudioManager getAudioManager() { return audioManager; }
    public GamePreferences getPreferences() { return preferences; }
    public AchievementManager getAchievementManager() { return achievementManager; }
    public Animation<TextureRegion> getAnimDown() { return animDown; }
    public Animation<TextureRegion> getAnimUp() { return animUp; }
    public Animation<TextureRegion> getAnimLeft() { return animLeft; }
    public Animation<TextureRegion> getAnimRight() { return animRight; }
    public LeaderboardManager getLeaderboardManager() { return leaderboardManager; }

    public Animation<TextureRegion> getShootDownAnim() { return shootDownAnim; }
    public Animation<TextureRegion> getShootUpAnim() { return shootUpAnim; }
    public Animation<TextureRegion> getShootLeftAnim() { return shootLeftAnim; }
    public Animation<TextureRegion> getShootRightAnim() { return shootRightAnim; }
    public TextureRegion getBulletRegion() { return bulletRegion; }

    public Progression getProgression() { return progression; }
}
