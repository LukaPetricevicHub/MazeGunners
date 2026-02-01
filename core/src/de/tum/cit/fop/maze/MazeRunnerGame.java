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
    private Animation<TextureRegion> idleDown, idleUp, idleLeft, idleRight;

    // Shooting Animations
    private Animation<TextureRegion> shootDownAnim, shootUpAnim, shootLeftAnim, shootRightAnim;
    private Animation<TextureRegion> gunDownIdleRun;
    private Animation<TextureRegion> gunUpIdleRun;
    private Animation<TextureRegion> gunSideIdleRun;
    private Animation<TextureRegion> gunSideLeftIdleRun;
    private Animation<TextureRegion> gunDownShoot;
    private Animation<TextureRegion> gunUpShoot;
    private Animation<TextureRegion> gunSideShoot;
    private Animation<TextureRegion> gunSideLeftShoot;

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

            if (achievementManager != null) {
                achievementManager.applyUnlockedIds(state.unlockedAchievements);
            }

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

        if (achievementManager != null) {
            state.unlockedAchievements = achievementManager.getUnlockedIds();
        }
    }

    /**
     * Returns a pending game state that should be restored on the next screen.
     *
     * @return The pending game state or null if none exists
     */
    public GameState getPendingSaveState() { return pendingSaveState; }
    public void clearPendingSaveState() { pendingSaveState = null; }

    private void loadCharacterAnimation() {
        // Character 2 assets (6 frames per strip, 64x64 each)
        animDown = loadStripAnimation("character2/The Male adventurer - Free/Walk/walk_down.png", 0.08f);
        animUp = loadStripAnimation("character2/The Male adventurer - Free/Walk/walk_up.png", 0.08f);
        animLeft = loadStripAnimation("character2/The Male adventurer - Free/Walk/walk_left_down.png", 0.08f);
        animRight = loadStripAnimation("character2/The Male adventurer - Free/Walk/walk_right_down.png", 0.08f);

        idleDown = loadStripAnimation("character2/The Male adventurer - Free/Idle/idle_down.png", 0.12f);
        idleUp = loadStripAnimation("character2/The Male adventurer - Free/Idle/idle_up.png", 0.12f);
        idleLeft = loadStripAnimation("character2/The Male adventurer - Free/Idle/idle_left_down.png", 0.12f);
        idleRight = loadStripAnimation("character2/The Male adventurer - Free/Idle/idle_right_down.png", 0.12f);

        shootDownAnim = animDown;
        shootUpAnim = animUp;
        shootLeftAnim = animLeft;
        shootRightAnim = animRight;

        // Paper-doll gun overlays (directional sheets)
        gunDownIdleRun = loadGunAnimation("Gun/Gun_down_idle-and-run-Sheet6.png", 6, 0.08f);
        gunUpIdleRun = loadGunAnimation("Gun/Gun_up_idle-and-run-Sheet6.png", 6, 0.08f);
        gunSideIdleRun = loadGunAnimation("Gun/Gun_side_idle-and-run-Sheet6.png", 6, 0.08f);
        gunSideLeftIdleRun = loadGunAnimation("Gun/Gun_side-left_idle-and-run-Sheet6.png", 6, 0.08f);

        gunDownShoot = loadGunAnimation("Gun/Gun_down_shoot-Sheet3.png", 3, 0.06f);
        gunUpShoot = loadGunAnimation("Gun/Gun_up_shoot-Sheet3.png", 3, 0.06f);
        gunSideShoot = loadGunAnimation("Gun/Gun_side_shoot-Sheet3.png", 3, 0.06f);
        gunSideLeftShoot = loadGunAnimation("Gun/Gun_side-left_shoot-Sheet3.png", 3, 0.06f);
    }

    private Animation<TextureRegion> loadStripAnimation(String path, float frameDuration) {
        Texture texture = new Texture(Gdx.files.internal(path));
        int frameHeight = texture.getHeight();
        int frameCount = 8;
        int frameWidth = texture.getWidth() / frameCount;
        if (frameWidth <= 0 || texture.getWidth() % frameCount != 0) {
            // Fallback to square frames
            frameWidth = frameHeight;
            frameCount = Math.max(1, texture.getWidth() / frameWidth);
        }

        TextureRegion[][] split = TextureRegion.split(texture, frameWidth, frameHeight);
        Array<TextureRegion> frames = new Array<>();
        for (int i = 0; i < frameCount; i++) {
            frames.add(split[0][i]);
        }
        return new Animation<>(frameDuration, frames, Animation.PlayMode.LOOP);
    }

    private Animation<TextureRegion> loadGunAnimation(String path, int frames, float frameDuration) {
        Texture texture = new Texture(Gdx.files.internal(path));
        int frameWidth = Math.max(1, texture.getWidth() / frames);
        int frameHeight = texture.getHeight();

        TextureRegion[][] split = TextureRegion.split(texture, frameWidth, frameHeight);
        Array<TextureRegion> regions = new Array<>();
        for (int i = 0; i < frames; i++) {
            regions.add(split[0][i]);
        }
        return new Animation<>(frameDuration, regions, Animation.PlayMode.LOOP);
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
    public Animation<TextureRegion> getIdleDown() { return idleDown; }
    public Animation<TextureRegion> getIdleUp() { return idleUp; }
    public Animation<TextureRegion> getIdleLeft() { return idleLeft; }
    public Animation<TextureRegion> getIdleRight() { return idleRight; }
    public LeaderboardManager getLeaderboardManager() { return leaderboardManager; }

    public Animation<TextureRegion> getShootDownAnim() { return shootDownAnim; }
    public Animation<TextureRegion> getShootUpAnim() { return shootUpAnim; }
    public Animation<TextureRegion> getShootLeftAnim() { return shootLeftAnim; }
    public Animation<TextureRegion> getShootRightAnim() { return shootRightAnim; }
    public Animation<TextureRegion> getGunDownIdleRun() { return gunDownIdleRun; }
    public Animation<TextureRegion> getGunUpIdleRun() { return gunUpIdleRun; }
    public Animation<TextureRegion> getGunSideIdleRun() { return gunSideIdleRun; }
    public Animation<TextureRegion> getGunSideLeftIdleRun() { return gunSideLeftIdleRun; }
    public Animation<TextureRegion> getGunDownShoot() { return gunDownShoot; }
    public Animation<TextureRegion> getGunUpShoot() { return gunUpShoot; }
    public Animation<TextureRegion> getGunSideShoot() { return gunSideShoot; }
    public Animation<TextureRegion> getGunSideLeftShoot() { return gunSideLeftShoot; }
    public TextureRegion getBulletRegion() { return bulletRegion; }

    public Progression getProgression() { return progression; }
}
