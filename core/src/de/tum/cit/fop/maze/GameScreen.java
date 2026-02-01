package de.tum.cit.fop.maze;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.audio.Sound;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.scenes.scene2d.ui.*;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.ScreenUtils;
import com.badlogic.gdx.utils.viewport.ScreenViewport;
import com.badlogic.gdx.utils.viewport.Viewport;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.InputMultiplexer;
import com.badlogic.gdx.utils.Timer;
import de.tum.cit.fop.maze.ui.UiButtons;


import de.tum.cit.fop.maze.camera.GameCamera;
import de.tum.cit.fop.maze.camera.CameraInputController;
import de.tum.cit.fop.maze.enemies.*;
import de.tum.cit.fop.maze.hud.Hud;
import de.tum.cit.fop.maze.items.*;
import de.tum.cit.fop.maze.map.*;
import de.tum.cit.fop.maze.map.RoomDetector;
import de.tum.cit.fop.maze.obstacles.*;
import de.tum.cit.fop.maze.player.Player;
import de.tum.cit.fop.maze.ui.SkillWindow;
import de.tum.cit.fop.maze.util.GridUtils;
import de.tum.cit.fop.maze.util.StoryManager;
import de.tum.cit.fop.maze.world.*;
import de.tum.cit.fop.maze.ui.PauseMenuWindow;
import de.tum.cit.fop.maze.ui.GameOverWindow;
import de.tum.cit.fop.maze.ui.DialogueWindow;
import de.tum.cit.fop.maze.ui.UiButtons;

import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import de.tum.cit.fop.maze.ui.UiButtons;

import java.awt.Point;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import de.tum.cit.fop.maze.ui.UiButtons;

/**
 * Main gameplay screen of the Maze Runner game.
 *
 * This screen controls the entire game loop including:
 * <ul>
 *   <li>Maze generation and loading</li>
 *   <li>Player movement and combat</li>
 *   <li>Enemy and obstacle spawning</li>
 *   <li>HUD and minimap rendering</li>
 *   <li>Pause menu, game over handling, and state transitions</li>
 * </ul>
 *
 * The screen supports both story mode and endless mode gameplay.
 */
public class GameScreen implements Screen {
    // Attributes
    private ShapeRenderer shapeRenderer; // Tool to draw simple shapes
    private float minimapZoom = 6.0f;    // How "zoomed out" the map is
    private float minimapSize = 200f;    // Size of the box in pixels (overridden per frame)

    private final MazeRunnerGame game;
    private final FileHandle levelFile;

    private boolean isGameOverShown = false;
    private boolean isPaused = false;
    private boolean storyShown = false;
    private boolean storyBlocking = false;
    private Stage pauseStage;
    private Window pauseWindow;
    private Window settingsWindow;
    private SkillWindow skillWindow;
    private de.tum.cit.fop.maze.enemies.Boss boss;
    private de.tum.cit.fop.maze.enemies.BossAnimations bossAnims;
    private de.tum.cit.fop.maze.enemies.PlayerAdapter bossTarget;
    private boolean bossDropSpawned = false;

    private Viewport viewport;
    private GameCamera gameCamera;

    private InputMultiplexer multiplexer;
    private CameraInputController cameraInput;

    private Maze maze;
    private MazeRenderer mazeRenderer;

    private Player player;
    private List<Enemy> enemies;
    private List<Obstacle> obstacles;
    private FileHandle activeLevelFile;

    private Hud hud;

    private GameStateController gameStateController;
    private InteractionLogic worldLogic;
    private CombatSystem combatSystem;

    private static final int TILE_SIZE = 32;
    private static final float PLAYER_WIDTH = 32f;
    private static final float PLAYER_HEIGHT = 32f;

    private final int level;
    private int trackedLevel = 1;

    private Sound pickupSound;
    private Sound hitSound;
    private Sound clickSound;
    private List<Collectable> items;

    private FogOfWarRenderer fog;
    private List<Room> rooms;
    private final boolean endlessMode;
    private int floor;
    private long runSeed;
    private Viewport uiViewport;
    private int enemyCount;
    private int trapCount;
    private int itemCount;

    // Minimap exploration (static map, reveal as you move)
    private boolean[][] explored;
    private int exploredW = -1;
    private int exploredH = -1;
    private static final int EXPLORE_RADIUS_TILES = 6;

    // Lightning trap deferred spawning
    private List<LightningTrap> pendingLightningTraps = new ArrayList<>();
    private boolean lightningTrapsActivated = false;

    private static final int[] ENEMIES_PER_LEVEL = {0, 18, 22, 26, 30, 34};
    private static final int[] TRAPS_PER_LEVEL   = {0, 20, 26, 32, 38, 45};
    private static final int[] ITEMS_PER_LEVEL   = {0, 10, 10, 11, 11, 12};

    private int clampLevel(int lv) { return Math.max(1, Math.min(5, lv)); }



    public GameScreen(MazeRunnerGame game, int level, FileHandle levelFile) {
        this(game, level, levelFile, false, level);
    }

    /**
     * Creates a new game screen for a specific level.
     *
     * @param game Reference to the main game instance
     * @param level Level number to load
     * @param levelFile Optional level file, can be null
     * @param endlessMode Whether endless mode is enabled
     * @param startFloor Starting floor for endless mode
     */
    public GameScreen(MazeRunnerGame game, int level, FileHandle levelFile, boolean endlessMode, int startFloor) {
        this.game = game;
        this.levelFile = levelFile;
        this.level = level;
        this.endlessMode = endlessMode;
        this.floor = startFloor;
        this.viewport = new ScreenViewport();
        this.uiViewport = new com.badlogic.gdx.utils.viewport.FitViewport(1280, 720);
        this.viewport.update(Gdx.graphics.getWidth(), Gdx.graphics.getHeight(), true);
    }

    public GameScreen(MazeRunnerGame game, String mapPath) {
        this.game = game;

        this.viewport = new ScreenViewport();
        this.uiViewport = new com.badlogic.gdx.utils.viewport.FitViewport(1280, 720);

        FileHandle localHandle = Gdx.files.local(mapPath);
        if (localHandle.exists()) {
            this.levelFile = localHandle;
        } else {
            this.levelFile = Gdx.files.internal(mapPath);
        }

        this.endlessMode = false;
        this.floor = 1;

        String filename = levelFile.nameWithoutExtension();
        int parsedLevel = 1;
        try {
            String numberPart = filename.replace("level-", "");
            parsedLevel = Integer.parseInt(numberPart);
        } catch (NumberFormatException ignored) {

        }
        this.level = parsedLevel;
    }

    /**
     * Main game loop.
     *
     * Handles input processing, game state updates, combat,
     * rendering of the maze, entities, HUD, minimap, and pause menu.
     *
     * @param delta Time elapsed since the last frame
     */
    @Override
    public void render(float delta) {

        // Input
        if (gameStateController.isRunning() && Gdx.input.isKeyJustPressed(game.getPreferences().getKeyPause())) {
            togglePause();
        }

        if (Gdx.input.isKeyJustPressed(Input.Keys.K)) {
            int needed = player.getXpNeededForNextLevel() - player.getExperience();
            player.addExperience(Math.max(needed, 0));
            hud.showNotification("CHEAT: FORCED LEVEL UP!");
            if (pickupSound != null) {
                long id = pickupSound.play();
                pickupSound.setVolume(id, game.getPreferences().getSoundVolume());
            }
        }

        if (Gdx.input.isKeyJustPressed(Input.Keys.GRAVE)) {
            hud.getDevConsole().toggle();
        }

        // Story intro (once per level)
        if (!endlessMode && !storyShown && !isPaused && hud != null) {
            storyShown = true;
            storyBlocking = true;
            int introLevel = endlessMode ? floor : level; // TODO verify correct source for story mode level

            String title = StoryManager.getIntroTitle(introLevel);
            String intro = StoryManager.getIntroText(introLevel);
            if (!title.isEmpty() && !intro.isEmpty()) {
                final boolean removedCameraInput = multiplexer != null
                        && multiplexer.getProcessors().contains(cameraInput, true);

                DialogueWindow dialogue = new DialogueWindow(game.getSkin(), title, intro, () -> {
                    if (multiplexer != null && removedCameraInput) {
                        multiplexer.addProcessor(0, cameraInput);
                    }
                    storyBlocking = false;
                });

                hud.getStage().addActor(dialogue);
                dialogue.centerOnStage();

                if (multiplexer != null && removedCameraInput) {
                    multiplexer.removeProcessor(cameraInput);
                }
            } else {
                storyBlocking = false;
            }
        }

        if (!storyBlocking) {
            cameraInput.update(delta);
        }

        // Game logic
        if (!isPaused && !storyBlocking && gameStateController.isRunning()) {

            player.update(delta, maze, game, this::collidesWithBlockingObstacle, gameCamera.getCamera());

            if (!lightningTrapsActivated && player.hasGun()) {
                activateLightningTraps();
            }


            if (player.getPlayerLevel() > trackedLevel) {
                trackedLevel = player.getPlayerLevel();
                hud.showNotification("LEVEL UP! (" + trackedLevel + ")\n+1 Skill Point\nHealth Restored");

                if (pickupSound != null) {
                    long id = pickupSound.play();
                    pickupSound.setPitch(id, 1.5f);
                    pickupSound.setVolume(id, game.getPreferences().getSoundVolume());
                }
            }

            // Boss HUD sync
            if (boss != null && !boss.isDead()) {
                hud.setBossVisible(true);
                hud.setBossHp(boss.getHp(), boss.getMaxHp());
            } else {
                hud.setBossVisible(false);
            }


            boolean onExit = worldLogic.isPlayerOnExit(player, maze);

            if (endlessMode && onExit && player.hasKey()) {
                advanceToNextFloor();
                return;
            }

            gameStateController.update(delta, onExit, player.hasKey(), player.getLives());

            if (!isGameOverShown) {
                if (gameStateController.getState() == GameStateController.State.WON) {
                    player.addScore(1000);
                    showGameOverMenu(true);
                    isGameOverShown = true;
                } else if (gameStateController.getState() == GameStateController.State.LOST) {
                    showGameOverMenu(false);
                    isGameOverShown = true;
                }
            }

            if (gameStateController.shouldReturnToMenu()) {
                game.goToMenu();
            }

            // Item pickup
            if (items != null) {
                Rectangle playerBounds =
                        new Rectangle(player.getX(), player.getY(), PLAYER_WIDTH, PLAYER_HEIGHT);

                var iter = items.iterator();
                while (iter.hasNext()) {
                    Collectable item = iter.next();

                    if (item.isActive() && playerBounds.overlaps(item.getBounds())) {
                        boolean hadKey = player.hasKey();
                        item.applyEffect(player);

                        if (!hadKey && item instanceof KeyItem) {
                            game.getAchievementManager().unlockAchievement("collector");
                            if (hud != null) {
                                hud.showNotification("I found the key.");
                            }
                        }

                        if (pickupSound != null) {
                            long id = pickupSound.play();
                            pickupSound.setVolume(id, game.getPreferences().getSoundVolume());
                        }

                        item.collect();
                        iter.remove();
                    }
                }
            }

            // Combat
            if (gameStateController.isRunning()) {

                int previousLives = player.getLives();

                combatSystem.update(delta, player, bossTarget, boss, maze, enemies, obstacles, gameCamera);


                // Boss death => relic drop
                if (boss != null && boss.isDead() && !bossDropSpawned) {
                    bossDropSpawned = true;

                    float dropX = boss.getHitbox().x + boss.getHitbox().width / 2f;
                    float dropY = boss.getHitbox().y + boss.getHitbox().height / 2f;

                    if (items != null) {
                        items.add(new de.tum.cit.fop.maze.items.BossRelicItem(dropX, dropY));
                    }

                    hud.showNotification("SWORD DEFEATED!\nRelic dropped.");
                }
            }

            hud.update(player);
            hud.act(delta);

            gameCamera.follow(
                    player.getX() + PLAYER_WIDTH / 2f,
                    player.getY() + PLAYER_HEIGHT / 2f,
                    delta
            );
        }

        gameCamera.update(delta);

        // Render world
        ScreenUtils.clear(0, 0, 0, 1);

        game.getSpriteBatch().setProjectionMatrix(gameCamera.getCamera().combined);
        game.getSpriteBatch().begin();

        mazeRenderer.render(maze, game.getSpriteBatch());
        combatSystem.renderProjectiles(game.getSpriteBatch());

        if (obstacles != null) {
            for (Obstacle obstacle : obstacles) {
                if (obstacle instanceof SpikeTrap spike) {
                    game.getSpriteBatch().draw(spike.getCurrentFrame(),
                            spike.getCollisionBox().x, spike.getCollisionBox().y, 32, 32);
                } else if (obstacle instanceof LightningTrap lightning) {
                    Rectangle box = lightning.getCollisionBox();

                    float originX = box.width / 2f;
                    float originY = box.height / 2f;

                    game.getSpriteBatch().draw(lightning.getSprite(), box.x, box.y, originX, originY, box.width, box.height, 1f, 1f, lightning.getRotationDegrees());
                }

                else if (obstacle instanceof FloorHazard hazard) {
                    game.getSpriteBatch().draw(hazard.getSprite(), hazard.getCollisionBox().x, hazard.getCollisionBox().y, 48, 48);
                }
            }
        }

        if (player.isDamageFlashing()) {
            game.getSpriteBatch().setColor(Color.RED);
        }

        player.draw(game.getSpriteBatch());
        game.getSpriteBatch().setColor(Color.WHITE);

        if (items != null) {
            for (Collectable item : items) {
                item.render(game.getSpriteBatch());
            }
        }

        if (enemies != null) {
            for (Enemy enemy : enemies) {
                TextureRegion frame = null;
                if (enemy instanceof MutantWorker mw) frame = mw.getCurrentFrame();
                else if (enemy instanceof RobotGuard rg) frame = rg.getCurrentFrame();
                else if (enemy instanceof FlyingDrone fd) frame = fd.getCurrentFrame();

                if (frame != null) {
                    if (enemy.isHitFlashing()) {
                        game.getSpriteBatch().setColor(Color.RED);
                    }
                    game.getSpriteBatch().draw(frame, enemy.getPosition().x, enemy.getPosition().y, enemy.getRenderWidth(), enemy.getRenderHeight());
                    game.getSpriteBatch().setColor(Color.WHITE);
                }
            }
        }

        if (boss != null && !boss.isDead()) {
            boss.render(game.getSpriteBatch());
        }

        float px = player.getX() + PLAYER_WIDTH / 2f;
        float py = player.getY() + PLAYER_HEIGHT / 2f;

        game.getSpriteBatch().end();
        fog.render(game.getSpriteBatch(), gameCamera.getCamera(), px, py, player.getBullets(), 1.5f * TILE_SIZE);

        if (isPaused) {
            pauseStage.getViewport().apply();
            pauseStage.act(delta);
            pauseStage.draw();
        } else {
            hud.getStage().getViewport().apply();
            hud.act(delta);
            hud.draw();
        }

        updateMinimapExploration();
        if (Gdx.input.isKeyPressed(game.getPreferences().getKeyMap())) {
            renderMinimap();
        }
    }

    /**
     * Called when this screen becomes active.
     *
     * Initializes the maze, player, enemies, obstacles, HUD,
     * camera, input handling, and restores saved game state if present.
     */
    @Override
    public void show() {
        Gdx.app.log("MAZE", "GameScreen level=" + level);
        enemies = new ArrayList<>();
        obstacles = new ArrayList<>();
        items = new ArrayList<>();

        runSeed = System.currentTimeMillis();
        shapeRenderer = new ShapeRenderer();

        ObstacleAudioManager.init();

        int w = Gdx.graphics.getWidth();
        int h = Gdx.graphics.getHeight();

        viewport.update(w, h, true);
        uiViewport.update(w, h, true);
        uiViewport.apply(true);

        viewport.update(Gdx.graphics.getWidth(), Gdx.graphics.getHeight(), true);

        gameStateController = new GameStateController();
        worldLogic = new InteractionLogic();
        combatSystem = new CombatSystem();

        playLevelMusic();

        if (game.getPendingSaveState() != null) {
            runSeed = game.getPendingSaveState().mazeSeed;
        } else {
            runSeed = System.currentTimeMillis();
        }


        initializeMaze(runSeed);
        initializeGraphics();
        initializePlayer();
        createPauseMenu();
        createHud();

        ensureExploredGrid();
        markExploredAroundPlayer();

        if (hud != null && maze != null) {
            float diagTiles = (float) Math.hypot(maze.getWidth(), maze.getHeight());
            hud.setHintMaxTiles(diagTiles * 0.7f);
        }

        if (endlessMode) {
            hud.setFloorText("Round: " + floor);
            hud.setEndlessInfo("");
        } else {
            hud.setFloorText("Floor: " + level);
            hud.setEndlessInfo("");
        }

        // Exit target for arrow & hints
        updateHudExitTarget();

        int lv = clampLevel(endlessMode ? floor : level);

        enemyCount = ENEMIES_PER_LEVEL[lv];
        trapCount  = TRAPS_PER_LEVEL[lv];
        itemCount  = ITEMS_PER_LEVEL[lv];
        for (Room r : rooms) {
            if (r.getType() == RoomType.BOSS) {
                Gdx.app.log("BOSS", "Boss room: x=" + r.x() + " y=" + r.y() + " w=" + r.w() + " h=" + r.h());
            }
        }

        Gdx.app.log("SPAWN", "counts: enemies=" + enemyCount + " traps=" + trapCount + " items=" + itemCount);

        // Boss only on level 5
        boss = null;
        bossTarget = null;
        bossDropSpawned = false;

        if (clampLevel(endlessMode ? floor : level) == 5) {
            bossAnims = new BossAnimations("Sword.png");
            boss = BossSpawner.spawnBossIfFinalLevel(maze, 5, bossAnims);
            if (boss != null && endlessMode) {
                float bossHpMult = 1f + Math.max(0, floor - 1) * 0.25f;
                boss.scaleHp(bossHpMult);
            }
            bossTarget = new PlayerAdapter(player);
        }


        Gdx.app.log("SPAWN", "rooms=" + (rooms == null ? 0 : rooms.size()) + " enemies=" + (enemies == null ? 0 : enemies.size()));


        trackedLevel = player.getPlayerLevel();
        hud.getDevConsole().setPlayer(player);

        multiplexer = new InputMultiplexer();
        multiplexer.addProcessor(cameraInput);
        multiplexer.addProcessor(hud.getStage());
        Gdx.input.setInputProcessor(multiplexer);

        fog = new FogOfWarRenderer();
        fog.setFogAlpha(0.97f);

        Gdx.app.log("SPAWN", "counts: enemies=" + enemyCount + " traps=" + trapCount + " items=" + itemCount);


        initializeEnemiesAndObstacles(runSeed);
        applyEndlessScaling();


        Gdx.app.log("SPAWN", "rooms=" + (rooms == null ? 0 : rooms.size()) + " enemies=" + (enemies == null ? 0 : enemies.size()) + " items=" + (items == null ? 0 : items.size()) + " obstacles=" + (obstacles == null ? 0 : obstacles.size()));
        game.clearPendingSaveState();
    }

    /**
     * Initializes the player and restores saved state if a save game exists.
     */
    private void initializePlayer() {
        Point entry = maze.getEntryCell();
        if (entry == null) entry = new Point(1, 1);
        Point safeSpawn = findSafeSpawnPosition(entry, maze);

        player = new Player(safeSpawn.x * TILE_SIZE, safeSpawn.y * TILE_SIZE, game.getProgression(), game.getAnimDown(), game.getAnimUp(), game.getAnimLeft(), game.getAnimRight(), game);

        // Check for saved data
        GameState savedState = game.getPendingSaveState();
        if (savedState != null) {
            System.out.println("Loading Saved State...");

            player.setScore(savedState.score);
            player.setLives(savedState.lives);
            player.setPlayerLevel(savedState.playerLevel);
            player.setExperience(savedState.experience);
            player.setSkillSpeedLevel(savedState.skillSpeedLevel);
            player.setSkillHealthLevel(savedState.skillHealthLevel);
            player.setSkillPoints(savedState.skillPoints);

            if (savedState.skillRapidFire) player.unlockRapidFire();
            if (savedState.skillShotgun) player.unlockShotgun();
            if (savedState.skillPiercing) player.unlockPiercing();

            if (savedState.skillDash) player.unlockDash();
            if (savedState.skillGhost) player.unlockGhost();

            if (savedState.skillThorns) player.unlockThorns();
            if (savedState.skillRepulse) player.unlockRepulse();

            if (savedState.hasGun) {
                player.equipGun();
            }
            if (savedState.ammoInClip > 0 || savedState.ammoReserve > 0) {
                player.setAmmo(savedState.ammoInClip, savedState.ammoReserve);
            }
            if (savedState.hasKey) {
                player.giveKey();
            }
            if (savedState.godMode) {
                player.setGodMode(true);
            }
            if (savedState.noClip) {
                player.setNoClip(true);
            }

            if (savedState.shouldRestorePosition) {
                if (canPlayerOccupy(savedState.playerX, savedState.playerY, maze)) {
                    player.setPosition(savedState.playerX, savedState.playerY);
                } else {
                    System.out.println("Saved position invalid (wall). Using default spawn.");
                }
            }
        }

        player.applyRunGamble(game.getRunGamble());
    }

    /**
     * Spawns enemies, obstacles, and items for the current maze.
     */
    private void initializeEnemiesAndObstacles(long seed) {
        Room spawnRoom = findSpawnRoom();

        // 1) Traps
        ObstaclePlacer placer = new ObstaclePlacer();

        obstacles = placer.placeObstacles(maze, trapCount, seed, spawnRoom, false);

        pendingLightningTraps = placer.findLightningTrapCandidates(maze, seed, spawnRoom);

        lightningTrapsActivated = false;


        GameState savedState = game.getPendingSaveState();

        // 3) Enemies (NOT in spawn room)
        if (savedState != null && savedState.enemies != null && !savedState.enemies.isEmpty()) {
            // restore enemies
            enemies = new ArrayList<>();
            for (GameState.EnemyState es : savedState.enemies) {
                Vector2 pos = new Vector2(es.x, es.y);
                if (es.type.equals("slime")) enemies.add(new MutantWorker(pos, 50f, player));
                else if (es.type.equals("robot")) {
                    Array<Vector2> dummyPath = new Array<>();
                    dummyPath.add(pos);
                    enemies.add(new RobotGuard(pos, 70f, dummyPath, player));
                } else if (es.type.equals("drone")) {
                    Array<Vector2> dummyPath = new Array<>();
                    dummyPath.add(pos);
                    enemies.add(new FlyingDrone(pos, 90f, dummyPath, player));
                }
            }
        } else {
            // fresh spawn using per-level enemyCount (NOT baseEnemies)
            enemies = new EnemySpawner().spawnEnemies(maze, rooms, spawnRoom, player, enemyCount, seed);
        }

        // 4) Items & powerups
        if (savedState != null && savedState.items != null && !savedState.items.isEmpty()) {
            items = new ArrayList<>();
            for (GameState.ItemState is : savedState.items) {
                if (is == null || is.type == null) continue;
                int tx = (int) (is.x / TILE_SIZE);
                int ty = (int) (is.y / TILE_SIZE);
                if (maze.getTile(tx, ty) == Tiles.EXIT || maze.getTile(tx, ty) == Tiles.SPAWN) {
                    continue;
                }
                if (is.type.equals("gun")) items.add(new GunCollectable(is.x, is.y));
                else if (is.type.equals("heart")) items.add(new HeartItem(is.x, is.y));
                else if (is.type.equals("speed")) items.add(new SpeedItem(is.x, is.y));
                else if (is.type.equals("shield")) items.add(new ShieldItem(is.x, is.y));
                else if (is.type.equals("key") && !player.hasKey()) items.add(new KeyItem(is.x, is.y));
                else if (is.type.equals("ammo")) items.add(new AmmoItem(is.x, is.y));
            }
        } else {
            items = new ItemSpawner().spawnItems(maze, itemCount); // use per-level
        }

        ensureKeyItemPresent(seed);
    }

    /**
     * Renders the minimap overlay.
     *
     * The minimap shows only explored tiles and updates dynamically
     * as the player moves. Enemies and the boss are displayed only
     * on already explored tiles.
     *
     * The minimap is shown while the map key is pressed.
     */

    private void renderMinimap() {
        if (shapeRenderer == null || uiViewport == null || maze == null || player == null) return;

        uiViewport.apply();

        float worldW = uiViewport.getWorldWidth();
        float worldH = uiViewport.getWorldHeight();

        minimapSize = Math.min(worldW, worldH) * 0.5f;

        float mapX = worldW - minimapSize - 20;
        float mapY = 20;

        float tileScale = minimapSize / Math.max(maze.getWidth(), maze.getHeight());
        float mapW = maze.getWidth() * tileScale;
        float mapH = maze.getHeight() * tileScale;
        float mapOriginX = mapX + (minimapSize - mapW) * 0.5f;
        float mapOriginY = mapY + (minimapSize - mapH) * 0.5f;

        Vector2 scissorBL = new Vector2(mapX, mapY);
        Vector2 scissorTR = new Vector2(mapX + minimapSize, mapY + minimapSize);
        uiViewport.project(scissorBL);
        uiViewport.project(scissorTR);
        int scissorX = Math.round(scissorBL.x);
        int scissorY = Math.round(scissorBL.y);
        int scissorW = Math.round(scissorTR.x - scissorBL.x);
        int scissorH = Math.round(scissorTR.y - scissorBL.y);

        // blending for transparent background
        Gdx.gl.glEnable(GL20.GL_BLEND);
        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);
        Gdx.gl.glEnable(GL20.GL_SCISSOR_TEST);
        Gdx.gl.glScissor(scissorX, scissorY, scissorW, scissorH);

        shapeRenderer.setProjectionMatrix(uiViewport.getCamera().combined);

        // Filled pass
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);

        // Background
        shapeRenderer.setColor(0, 0, 0, 0.5f);
        shapeRenderer.rect(mapX, mapY, minimapSize, minimapSize);

        // Explored tiles (rooms + halls only)
        for (int row = 0; row < maze.getHeight(); row++) {
            for (int col = 0; col < maze.getWidth(); col++) {
                if (!explored[col][row]) continue;
                if (!maze.getTile(col, row).isWalkable()) continue;

                shapeRenderer.setColor(Color.GRAY);

                float x = mapOriginX + col * tileScale;
                float y = mapOriginY + row * tileScale;
                shapeRenderer.rect(x, y, tileScale, tileScale);
            }
        }

        // Enemies (red) -
        if (enemies != null) {
            shapeRenderer.setColor(Color.RED);
            for (Enemy enemy : enemies) {
                float ex = enemy.getPosition().x;
                float ey = enemy.getPosition().y;

                int tileX = (int) (ex / TILE_SIZE);
                int tileY = (int) (ey / TILE_SIZE);
                if (!inExplored(tileX, tileY)) continue;

                float cx = mapOriginX + (tileX + 0.5f) * tileScale;
                float cy = mapOriginY + (tileY + 0.5f) * tileScale;
                shapeRenderer.circle(cx, cy, Math.max(2f, tileScale * 0.35f));
            }
        }

        // Boss (purple)
        if (boss != null && !boss.isDead()) {
            shapeRenderer.setColor(Color.PURPLE);

            Rectangle hb = boss.getHitbox();
            float bx = hb.x + hb.width / 2f;
            float by = hb.y + hb.height / 2f;

            int tileX = (int) (bx / TILE_SIZE);
            int tileY = (int) (by / TILE_SIZE);
            if (inExplored(tileX, tileY)) {
                float cx = mapOriginX + (tileX + 0.5f) * tileScale;
                float cy = mapOriginY + (tileY + 0.5f) * tileScale;
                shapeRenderer.circle(cx, cy, Math.max(2f, tileScale * 0.45f));
            }
        }

        // Player (green)
        shapeRenderer.setColor(Color.GREEN);
        int pTileX = (int) (player.getX() / TILE_SIZE);
        int pTileY = (int) (player.getY() / TILE_SIZE);
        float pcx = mapOriginX + (pTileX + 0.5f) * tileScale;
        float pcy = mapOriginY + (pTileY + 0.5f) * tileScale;
        shapeRenderer.circle(pcx, pcy, Math.max(2f, tileScale * 0.45f));

        // Entrance (always)
        Point entry = maze.getEntryCell();
        if (entry != null) {
            shapeRenderer.setColor(Color.CYAN);
            float ex = mapOriginX + (entry.x + 0.5f) * tileScale;
            float ey = mapOriginY + (entry.y + 0.5f) * tileScale;
            shapeRenderer.rect(ex - tileScale * 0.3f, ey - tileScale * 0.3f,
                    tileScale * 0.6f, tileScale * 0.6f);
        }

        // Exit (only if player has key)
        if (player.hasKey()) {
            Point exit = maze.getExitCell();
            if (exit != null) {
                shapeRenderer.setColor(Color.ORANGE);
                float ex = mapOriginX + (exit.x + 0.5f) * tileScale;
                float ey = mapOriginY + (exit.y + 0.5f) * tileScale;
                shapeRenderer.rect(ex - tileScale * 0.3f, ey - tileScale * 0.3f,
                        tileScale * 0.6f, tileScale * 0.6f);
            }
        }

        shapeRenderer.end();

        Gdx.gl.glDisable(GL20.GL_SCISSOR_TEST);
        Gdx.gl.glDisable(GL20.GL_BLEND);
    }

    private void updateMinimapExploration() {
        if (maze == null || player == null) return;
        ensureExploredGrid();
        markExploredAroundPlayer();
    }

    private void ensureExploredGrid() {
        if (maze == null) return;
        if (explored == null || exploredW != maze.getWidth() || exploredH != maze.getHeight()) {
            exploredW = maze.getWidth();
            exploredH = maze.getHeight();
            explored = new boolean[exploredW][exploredH];
        }
    }

    private void markExploredAroundPlayer() {
        if (explored == null || maze == null || player == null) return;
        int cx = (int) (player.getX() / TILE_SIZE);
        int cy = (int) (player.getY() / TILE_SIZE);
        for (int y = cy - EXPLORE_RADIUS_TILES; y <= cy + EXPLORE_RADIUS_TILES; y++) {
            for (int x = cx - EXPLORE_RADIUS_TILES; x <= cx + EXPLORE_RADIUS_TILES; x++) {
                if (x < 0 || y < 0 || x >= exploredW || y >= exploredH) continue;
                explored[x][y] = true;
            }
        }
    }

    private boolean inExplored(int x, int y) {
        if (explored == null) return false;
        if (x < 0 || y < 0 || x >= exploredW || y >= exploredH) return false;
        return explored[x][y];
    }


    private void createHud() {
        hud = new Hud(uiViewport, game.getSpriteBatch(), game.getSkin());
        if (game.getAchievementManager() != null) {
            game.getAchievementManager().setHud(hud);
        }
    }

    private void playLevelMusic() {
        String musicFile = switch (this.level) {
            case 1 -> "music/level1.mp3";
            case 2 -> "music/level2.mp3";
            case 3 -> "music/level3.mp3";
            case 4 -> "music/level4.mp3";
            case 5 -> "music/level5.mp3";
            default -> "music/background.mp3";
        };
        game.getAudioManager().playBackgroundMusic(musicFile);
    }

    private void activateLightningTraps() {
        lightningTrapsActivated = true;

        obstacles.addAll(pendingLightningTraps);
        pendingLightningTraps.clear();
    }


    /**
     * Initializes the maze for the current level or endless mode.
     */
    private void initializeMaze(long seed) {
        int effectiveLevel = endlessMode ? floor : level;


        GameState saved = game.getPendingSaveState();
        boolean loadingSave = (saved != null);
        if (loadingSave && saved.shouldRestorePosition) {
            storyShown = true; // don't show story when resuming mid-level
        }

        if (endlessMode) {
            FileHandle endlessFile = getEndlessLevelFile(floor);
            if (endlessFile != null && endlessFile.exists()) {
                MazeLoader loader = new MazeLoader();
                this.maze = loader.loadMazeFromProperties(endlessFile, TILE_SIZE);
                this.rooms = RoomDetector.detectRooms(this.maze);
                this.maze.setRooms(this.rooms);
                applyRoomMask(this.maze, this.rooms);
                assignRoomTypesFromTiles(this.maze, this.rooms);
                activeLevelFile = endlessFile;
                return;
            }
        }


        // 1) LOAD GAME => REGEN FROM SAVED SEED
        if (loadingSave) {
            long savedSeed = saved.mazeSeed;
            int savedEffectiveLevel = saved.isEndless ? saved.currentFloor : saved.level;

            Gdx.app.log("MAZE", "LOAD SAVE -> regen maze seed=" + savedSeed);

            MazeGenerator generator = new MazeGenerator();
            MazeLoader.LevelSpec spec = new MazeLoader.LevelSpec(80, 80, new HashMap<>());
            GeneratedMaze generated = generator.generateWithRooms(
                    spec, TILE_SIZE, savedSeed, clampLevel(savedEffectiveLevel)
            );

            this.rooms = generated.getRooms();
            this.maze = generated.getMaze();
            this.maze.setRooms(this.rooms);

            activeLevelFile = null;
            return;
        }

        // 2) NEW RUN => LEVEL SELECT
        FileHandle localTarget = getLevelTargetFile(level);

        Gdx.app.log("MAZE", "NEW RUN -> generating NEW maze -> " + localTarget.path());

        MazeGenerator generator = new MazeGenerator();
        MazeLoader.LevelSpec spec = new MazeLoader.LevelSpec(80, 80, new HashMap<>());
        GeneratedMaze generated = generator.generateWithRooms(
                spec, TILE_SIZE, seed, clampLevel(effectiveLevel)
        );

        this.rooms = generated.getRooms();

        MazeExporter.saveToProperties(generated.getMaze(), localTarget);
        activeLevelFile = localTarget;

        MazeLoader loader = new MazeLoader();
        this.maze = loader.loadMazeFromProperties(localTarget, TILE_SIZE);
        this.maze.setRooms(this.rooms);

        Gdx.app.log("MAZE", "Generated + saved + reloaded -> " + localTarget.path()
                + " | rooms=" + (rooms == null ? 0 : rooms.size()));
    }




    private void initializeGraphics() {
        gameCamera = new GameCamera(viewport.getWorldWidth(), viewport.getWorldHeight(), maze.getWidth() * TILE_SIZE, maze.getHeight() * TILE_SIZE);

        gameCamera.setZoom(0.25f);
        cameraInput = new CameraInputController(gameCamera, game.getPreferences());

        Texture tileset = new Texture("tileset1.png");
        mazeRenderer = new MazeRenderer(tileset);

        if (Gdx.files.internal("audio/pickup.mp3").exists()) {
            pickupSound = Gdx.audio.newSound(Gdx.files.internal("audio/pickup.mp3"));
        }
        if (Gdx.files.internal("audio/hit.mp3").exists()) {
            hitSound = Gdx.audio.newSound(Gdx.files.internal("audio/hit.mp3"));
        }
        if (Gdx.files.internal("audio/click.mp3").exists()) {
            clickSound = Gdx.audio.newSound(Gdx.files.internal("audio/click.mp3"));
        }

    }

    /**
     * Toggles the pause and updates input handling.
     */
    private void togglePause() {
        isPaused = !isPaused;

        if (isPaused) {
            game.getAudioManager().pauseMusic();
            multiplexer.clear();
            multiplexer.addProcessor(cameraInput);
            multiplexer.addProcessor(pauseStage);
            Gdx.input.setInputProcessor(multiplexer);
        } else {
            game.getAudioManager().resumeMusic();
            multiplexer.clear();
            multiplexer.addProcessor(cameraInput);
            multiplexer.addProcessor(hud.getStage());
            Gdx.input.setInputProcessor(multiplexer);
        }
    }

    private void createPauseMenu() {
        pauseStage = new Stage(uiViewport, game.getSpriteBatch());

        // 1. Create a dark semi-transparent background texture
        Pixmap pixmap = new Pixmap(1, 1, Pixmap.Format.RGBA8888);
        pixmap.setColor(0, 0, 0, 0.6f); // Black with 60% opacity
        pixmap.fill();
        TextureRegionDrawable darkBg = new TextureRegionDrawable(new TextureRegion(new Texture(pixmap)));
        pixmap.dispose();

        // 2. Create a Root Table that fills the screen
        Table rootTable = new Table();
        rootTable.setFillParent(true);
        rootTable.setBackground(darkBg);

        // 3. Create the Skill Window (hidden by default)
        skillWindow = new SkillWindow(game.getSkin(), player, () -> {
            playClickSound();
            skillWindow.setVisible(false);
            pauseWindow.setVisible(true);
        });
        skillWindow.setVisible(false);

        // 4. Create the Pause Window
        pauseWindow = new PauseMenuWindow(
                game.getSkin(),
                () -> { playClickSound(); togglePause(); },
                () -> {
                    playClickSound();
                    pauseWindow.setVisible(false);
                    settingsWindow.setVisible(true);
                    centerSettingsWindow();
                },
                () -> {
                    playClickSound();
                    skillWindow.refresh();
                    pauseWindow.setVisible(false);
                    skillWindow.setVisible(true);

                    // Center skill window
                    skillWindow.setPosition((uiViewport.getWorldWidth() - skillWindow.getWidth()) / 2f, (uiViewport.getWorldHeight() - skillWindow.getHeight()) / 2f);
                },
                () -> {
                    playClickSound();
                    pauseWindow.setVisible(false);
                    de.tum.cit.fop.maze.ui.AchievementsWindow window =
                            new de.tum.cit.fop.maze.ui.AchievementsWindow(
                                    game.getSkin(),
                                    game.getAchievementManager().getAchievements().values(),
                                    () -> {
                                        playClickSound();
                                        pauseWindow.setVisible(true);
                                    }
                            );
                    window.setPosition(
                            (uiViewport.getWorldWidth() - window.getWidth()) / 2f,
                            (uiViewport.getWorldHeight() - window.getHeight()) / 2f
                    );
                    pauseStage.addActor(window);
                },
                () -> { playClickSound(); game.goToMenu(); }
        );

        TextButton saveButton = UiButtons.button("Save Game", game.getSkin());
        saveButton.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                playClickSound();
                game.saveGame(player, level, enemies, items, endlessMode, floor, runSeed);
                saveButton.setText("Saved!");
                Timer.schedule(new Timer.Task() {
                    @Override
                    public void run() {
                        saveButton.setText("Save Game");
                    }
                }, 1f);
            }
        });

        pauseWindow.add(saveButton).fillX().pad(10).row();
        pauseWindow.pack();

        // 5. Add the Pause Window to the Root Table (Centered)
        rootTable.add(pauseWindow);

        // 6. Create Settings Window (hidden)
        createSettingsWindow();

        // 7. Add everything to the Stage
        pauseStage.addActor(rootTable);
        pauseStage.addActor(settingsWindow);
        pauseStage.addActor(skillWindow);

        settingsWindow.setVisible(false);
        skillWindow.setVisible(false);
    }

    private void showGameOverMenu(boolean won) {
        if (won) {
            int nextLevel = level + 1;
            game.getPreferences().setMaxLevel(nextLevel);
            if (!endlessMode && level >= 5) {
                game.markStoryCompleted();
            }
        }

        if (pauseWindow != null) pauseWindow.setVisible(false);
        if (settingsWindow != null) settingsWindow.setVisible(false);


        GameOverWindow window = new GameOverWindow(game.getSkin(), game, level, won, activeLevelFile != null ? activeLevelFile : levelFile, player.getScore(), player, endlessMode, floor);
        window.setPosition((uiViewport.getWorldWidth() - window.getWidth()) / 2f, (uiViewport.getWorldHeight() - window.getHeight()) / 2f);

        pauseStage.addActor(window);

        isPaused = true;
        Gdx.input.setInputProcessor(pauseStage);
    }

    private void playClickSound() {
        if (clickSound != null) {
            long id = clickSound.play();
            clickSound.setVolume(id, game.getPreferences().getSoundVolume());
        }
    }

    private void createSettingsWindow() {
        settingsWindow = new Window("Settings", game.getSkin());
        settingsWindow.getTitleLabel().setAlignment(1);
        settingsWindow.setMovable(false);

        settingsWindow.add(new Label("Music Volume", game.getSkin())).padTop(10).row();
        final Slider musicSlider = new Slider(0f, 1f, 0.05f, false, game.getSkin());
        musicSlider.setValue(game.getPreferences().getMusicVolume());
        settingsWindow.add(musicSlider).width(200).row();

        musicSlider.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                playClickSound();
                game.getPreferences().setMusicVolume(musicSlider.getValue());
                game.getAudioManager().updateMusicVolume(musicSlider.getValue());
            }
        });

        settingsWindow.add(new Label("SFX Volume", game.getSkin())).padTop(10).row();
        final Slider sfxSlider = new Slider(0f, 1f, 0.05f, false, game.getSkin());
        sfxSlider.setValue(game.getPreferences().getSoundVolume());
        settingsWindow.add(sfxSlider).width(200).padBottom(20).row();

        sfxSlider.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                playClickSound();
                game.getPreferences().setSoundVolume(sfxSlider.getValue());
            }
        });

        TextButton backBtn = UiButtons.button("Back", game.getSkin());
        backBtn.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                playClickSound();
                settingsWindow.setVisible(false);
                pauseWindow.setVisible(true);
            }
        });

        settingsWindow.add(backBtn).width(200).pad(10).row();
        settingsWindow.pack();
        centerSettingsWindow();
    }

    private void centerSettingsWindow() {
        if (settingsWindow == null || uiViewport == null) return;
        settingsWindow.setPosition(
                (uiViewport.getWorldWidth() - settingsWindow.getWidth()) / 2f,
                (uiViewport.getWorldHeight() - settingsWindow.getHeight()) / 2f - 40f
        );
    }

    @Override
    public void resize(int width, int height) {
        viewport.update(width, height, true);
        uiViewport.update(width, height, true);

        if (hud != null) hud.resize(width, height, true);
        if (gameCamera != null) gameCamera.resize(viewport.getWorldWidth(), viewport.getWorldHeight());
        if (fog != null) fog.resize(width, height);
        centerSettingsWindow();
    }


    /**
     * Advances the game to the next floor in endless mode.
     */
    private void advanceToNextFloor() {
        floor++;
        hud.setFloorText("Round: " + floor);
        int hpGain = Math.max(0, (floor - 1) * 2);
        String msg = "NEW ROUND STARTED";
        if (hpGain > 0) {
            msg += "\nENEMIES + " + hpGain + " HP";
        }
        hud.showCenterNotification(msg, 7f);

        long seed = System.nanoTime();
        runSeed = seed;

        initializeMaze(seed);
        updateHudExitTarget();

        int lv = clampLevel(floor);
        enemyCount = ENEMIES_PER_LEVEL[lv];
        trapCount  = TRAPS_PER_LEVEL[lv];
        itemCount  = ITEMS_PER_LEVEL[lv];

        gameCamera = new GameCamera(viewport.getWorldWidth(), viewport.getWorldHeight(), maze.getWidth() * TILE_SIZE, maze.getHeight() * TILE_SIZE);
        gameCamera.setZoom(0.25f);
        cameraInput = new CameraInputController(gameCamera, game.getPreferences());

        Point entry = maze.getEntryCell();
        if (entry == null) entry = new Point(1, 1);
        Point safeSpawn = findSafeSpawnPosition(entry, maze);
        player.setPosition(safeSpawn.x * TILE_SIZE, safeSpawn.y * TILE_SIZE);
        player.clearKey();

        initializeEnemiesAndObstacles(seed);
        applyEndlessScaling();
        boss = null;
        bossTarget = null;
        bossDropSpawned = false;

        if (clampLevel(floor) == 5) {
            if (bossAnims != null) bossAnims.dispose();
            bossAnims = new BossAnimations("Sword.png");
            boss = BossSpawner.spawnBossIfFinalLevel(maze, 5, bossAnims);
            if (boss != null && endlessMode) {
                float bossHpMult = 1f + Math.max(0, floor - 1) * 0.25f;
                boss.scaleHp(bossHpMult);
            }
            bossTarget = new PlayerAdapter(player);
        }

        multiplexer.clear();
        multiplexer.addProcessor(cameraInput);
        multiplexer.addProcessor(hud.getStage());
        Gdx.input.setInputProcessor(multiplexer);

        isGameOverShown = false;
    }

    private Point findSafeSpawnPosition(Point entryCell, Maze maze) {
        int maxRadius = 10;
        for (int r = 0; r <= maxRadius; r++) {
            for (int dy = -r; dy <= r; dy++) {
                for (int dx = -r; dx <= r; dx++) {
                    int cx = entryCell.x + dx;
                    int cy = entryCell.y + dy;
                    if (!GridUtils.inBounds(cx, cy, maze.getWidth(), maze.getHeight())) continue;
                    if (!maze.getTile(cx, cy).isWalkable()) continue;

                    float worldX = cx * maze.getTileSize();
                    float worldY = cy * maze.getTileSize();
                    if (canPlayerOccupyWithObstacles(worldX, worldY, maze)) return new Point(cx, cy);
                }
            }
        }
        return entryCell;
    }

    private boolean canPlayerOccupy(float worldX, float worldY, Maze maze) {
        float inset = 2f;
        return !maze.isWallWorld(worldX + inset, worldY + inset)
                && !maze.isWallWorld(worldX + PLAYER_WIDTH - inset, worldY + inset)
                && !maze.isWallWorld(worldX + inset, worldY + PLAYER_HEIGHT - inset)
                && !maze.isWallWorld(worldX + PLAYER_WIDTH - inset, worldY + PLAYER_HEIGHT - inset);
    }

    private boolean canPlayerOccupyWithObstacles(float worldX, float worldY, Maze maze) {
        float inset = 2f;
        boolean wallFree = !maze.isWallWorld(worldX + inset, worldY + inset)
                && !maze.isWallWorld(worldX + PLAYER_WIDTH - inset, worldY + inset)
                && !maze.isWallWorld(worldX + inset, worldY + PLAYER_HEIGHT - inset)
                && !maze.isWallWorld(worldX + PLAYER_WIDTH - inset, worldY + PLAYER_HEIGHT - inset);

        if (!wallFree) return false;

        Rectangle r = new Rectangle(worldX, worldY, PLAYER_WIDTH, PLAYER_HEIGHT);
        return !collidesWithBlockingObstacle(r);
    }

    private boolean collidesWithBlockingObstacle(Rectangle rect) {
        if (obstacles == null) return false;
        for (Obstacle obstacle : obstacles) {
            if (obstacle.blocksMovement() && obstacle.getCollisionBox().overlaps(rect)) {
                return true;
            }
        }
        return false;
    }

    private Room findSpawnRoom() {
        if (rooms == null) return null;
        for (Room r : rooms) {
            if (r.getType() == RoomType.SPAWN) return r;
        }
        return null;
    }

    private Room findExitRoom() {
        if (rooms == null) return null;
        for (Room r : rooms) {
            if (r.getType() == RoomType.EXIT) return r;
        }
        return null;
    }

    private Room findKeyRoom() {
        if (rooms == null) return null;
        for (Room r : rooms) {
            if (r.getType() == RoomType.KEY) return r;
        }
        return null;
    }

    private void ensureKeyItemPresent(long seed) {
        if (player == null || player.hasKey()) return;
        if (items == null) items = new ArrayList<>();

        for (Collectable c : items) {
            if (c instanceof KeyItem) return;
        }

        Point keyTile = findFirstTileOfType(maze, Tiles.KEY);
        if (keyTile != null) {
            maze.setTile(keyTile.x, keyTile.y, Tiles.FLOOR);
            Room r = findRoomContaining(rooms, keyTile);
            if (r != null) r.setType(RoomType.KEY);
            items.add(new KeyItem(keyTile.x * TILE_SIZE, keyTile.y * TILE_SIZE));
            return;
        }

        Room keyRoom = findKeyRoom();
        if (keyRoom == null) {
            Room spawnRoom = findSpawnRoom();
            Room exitRoom = findExitRoom();
            keyRoom = pickFallbackKeyRoom(spawnRoom, exitRoom);
            if (keyRoom != null) keyRoom.setType(RoomType.KEY);
        }

        if (keyRoom != null) {
            java.util.Random rng = new java.util.Random(seed ^ 0x9E3779B97F4A7C15L);
            Point p = keyRoom.randomInterior(rng);
            if (p != null && maze.getTile(p.x, p.y).isWalkable()) {
                items.add(new KeyItem(p.x * TILE_SIZE, p.y * TILE_SIZE));
            }
        }
    }

    private Room pickFallbackKeyRoom(Room spawnRoom, Room exitRoom) {
        if (rooms == null || rooms.isEmpty()) return null;
        Room best = null;
        int bestScore = -1;

        int sx = spawnRoom != null ? spawnRoom.centerX() : -1;
        int sy = spawnRoom != null ? spawnRoom.centerY() : -1;
        int ex = exitRoom != null ? exitRoom.centerX() : -1;
        int ey = exitRoom != null ? exitRoom.centerY() : -1;

        for (Room r : rooms) {
            if (r == spawnRoom || r == exitRoom) continue;
            int ds = (sx < 0) ? Integer.MAX_VALUE : dist2(r.centerX(), r.centerY(), sx, sy);
            int de = (ex < 0) ? Integer.MAX_VALUE : dist2(r.centerX(), r.centerY(), ex, ey);
            int score = Math.min(ds, de);
            if (score > bestScore) {
                bestScore = score;
                best = r;
            }
        }
        return best;
    }

    private int dist2(int ax, int ay, int bx, int by) {
        int dx = ax - bx;
        int dy = ay - by;
        return dx * dx + dy * dy;
    }

    private void applyRoomMask(Maze maze, List<Room> rooms) {
        if (maze == null) return;
        maze.clearRoomMask();
        if (rooms == null) return;
        for (Room r : rooms) {
            for (int y = r.y(); y < r.y() + r.h(); y++) {
                for (int x = r.x(); x < r.x() + r.w(); x++) {
                    maze.setRoomCell(x, y, true);
                }
            }
        }
    }

    private void assignRoomTypesFromTiles(Maze maze, List<Room> rooms) {
        if (maze == null || rooms == null) return;

        Point spawn = maze.getEntryCell();
        if (spawn != null) {
            Room r = findRoomContaining(rooms, spawn);
            if (r != null) r.setType(RoomType.SPAWN);
        }

        Point exit = maze.getExitCell();
        if (exit != null) {
            Room r = findRoomContaining(rooms, exit);
            if (r != null) r.setType(RoomType.EXIT);
        }

        Point key = findFirstTileOfType(maze, Tiles.KEY);
        if (key != null) {
            Room r = findRoomContaining(rooms, key);
            if (r != null) r.setType(RoomType.KEY);
        }
    }

    private Room findRoomContaining(List<Room> rooms, Point p) {
        for (Room r : rooms) {
            if (p.x >= r.x() && p.x < r.x() + r.w()
                    && p.y >= r.y() && p.y < r.y() + r.h()) {
                return r;
            }
        }
        return null;
    }

    private Point findFirstTileOfType(Maze maze, Tiles tile) {
        for (int y = 0; y < maze.getHeight(); y++) {
            for (int x = 0; x < maze.getWidth(); x++) {
                if (maze.getTile(x, y) == tile) {
                    return new Point(x, y);
                }
            }
        }
        return null;
    }

    private void applyEndlessScaling() {
        if (combatSystem == null) return;
        if (!endlessMode) {
            combatSystem.setEnemyDamageMult(1f);
            return;
        }

        int hpAdd = Math.max(0, (floor - 1) * 2);
        if (enemies != null) {
            for (Enemy e : enemies) {
                if (e != null) e.addHp(hpAdd);
            }
        }
        combatSystem.setEnemyDamageMult(1f);
    }


    @Override public void pause() {}
    @Override public void resume() {}
    @Override public void hide() {
        if (combatSystem != null) combatSystem.stopLoopingSfx();
        if (enemies != null) {
            for (Enemy e : enemies) {
                if (e instanceof FlyingDrone fd) {
                    fd.stopAllSounds();
                }
            }
        }
    }

    @Override
    public void dispose() {
        if (combatSystem != null) combatSystem.stopLoopingSfx();
        if (enemies != null) {
            for (Enemy e : enemies) {
                if (e instanceof FlyingDrone fd) {
                    fd.stopAllSounds();
                }
            }
        }
        if (hud != null) hud.dispose();
        if (pauseStage != null) pauseStage.dispose();
        if (pickupSound != null) pickupSound.dispose();
        if (hitSound != null) hitSound.dispose();
        if (clickSound != null) clickSound.dispose();
        if (items != null) {
            for (Collectable item : items) {
                item.dispose();
            }
        }
        if (fog != null) fog.dispose();
        if (mazeRenderer != null) mazeRenderer.dispose();
        if (bossAnims != null) bossAnims.dispose();
        ObstacleAudioManager.dispose();
    }

    private void updateHudExitTarget() {
        Point exit = maze.getExitCell();
        if (exit == null) {
            hud.clearExitTarget();
            hud.setExitRoomWorldRect(null);
            return;
        }

        hud.setExitTarget(exit.x * TILE_SIZE + TILE_SIZE / 2f, exit.y * TILE_SIZE + TILE_SIZE / 2f);

        Room exitRoom = findExitRoom();
        if (exitRoom != null) {
            float rx = exitRoom.x() * TILE_SIZE;
            float ry = exitRoom.y() * TILE_SIZE;
            float rw = exitRoom.w() * TILE_SIZE;
            float rh = exitRoom.h() * TILE_SIZE;
            hud.setExitRoomWorldRect(new Rectangle(rx, ry, rw, rh));
        } else {
            hud.setExitRoomWorldRect(null);
        }
    }

    private FileHandle getLevelTargetFile(int level) {
        FileHandle dir = Gdx.files.local("maps");
        dir.mkdirs();
        return dir.child("level-" + level + ".properties");
    }

    private FileHandle getEndlessLevelFile(int floor) {
        String name = "maps/endless-" + floor + ".properties";

        FileHandle internal = Gdx.files.internal(name);
        if (internal.exists()) return internal;

        FileHandle local = Gdx.files.local(name);
        if (local.exists()) return local;

        return null;
    }
}

