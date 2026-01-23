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


import de.tum.cit.fop.maze.camera.GameCamera;
import de.tum.cit.fop.maze.camera.CameraInputController;
import de.tum.cit.fop.maze.enemies.*;
import de.tum.cit.fop.maze.hud.Hud;
import de.tum.cit.fop.maze.items.*;
import de.tum.cit.fop.maze.map.*;
import de.tum.cit.fop.maze.obstacles.*;
import de.tum.cit.fop.maze.player.Player;
import de.tum.cit.fop.maze.ui.SkillWindow;
import de.tum.cit.fop.maze.util.GridUtils;
import de.tum.cit.fop.maze.world.*;
import de.tum.cit.fop.maze.obstacles.PropType;
import de.tum.cit.fop.maze.obstacles.Prop;
import de.tum.cit.fop.maze.ui.PauseMenuWindow;
import de.tum.cit.fop.maze.ui.GameOverWindow;

import com.badlogic.gdx.graphics.glutils.ShapeRenderer;

import java.awt.Point;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class GameScreen implements Screen {
    // Attributes
    private ShapeRenderer shapeRenderer; // Tool to draw simple shapes
    private float minimapZoom = 6.0f;    // How "zoomed out" the map is
    private float minimapSize = 200f;    // Size of the box in pixels (200x200)

    private final MazeRunnerGame game;
    private final FileHandle levelFile;

    private boolean isGameOverShown = false;
    private boolean isPaused = false;
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
    private Texture propTilesetTexture;
    private final Map<PropType, TextureRegion> propSprites = new HashMap<>();
    private final Map<PropType, Texture> propTextures = new HashMap<>();
    private FileHandle activeLevelFile;

    private Hud hud;

    private GameStateController gameStateController;
    private InteractionLogic worldLogic;
    private CombatSystem combatSystem;

    private static final int TILE_SIZE = 32;
    private static final float PLAYER_WIDTH = 32f;
    private static final float PLAYER_HEIGHT = 32f;

    private Texture keyTexture;
    private final int level;
    private int trackedLevel = 1;

    private Sound pickupSound;
    private Sound hitSound;
    private Sound clickSound;
    private List<AbstractCollectable> items;

    private FogOfWarRenderer fog;
    private List<Room> rooms;
    private final boolean endlessMode;
    private int floor;
    private long runSeed;
    private Viewport uiViewport;
    private int enemyCount;
    private int trapCount;
    private int itemCount;
    private int propCount;


    private static final int[] ENEMIES_PER_LEVEL = {0, 18, 22, 26, 30, 34};
    private static final int[] TRAPS_PER_LEVEL   = {0, 20, 26, 32, 38, 45};
    private static final int[] ITEMS_PER_LEVEL   = {0, 10, 10, 11, 11, 12};
    private static final int[] PROPS_PER_LEVEL   = {0, 15, 18, 20, 22, 24};

    private int clampLevel(int lv) { return Math.max(1, Math.min(5, lv)); }


    // --- CONSTRUCTORS ---

    public GameScreen(MazeRunnerGame game, int level, FileHandle levelFile) {
        this(game, level, levelFile, false, level);
    }

    public GameScreen(MazeRunnerGame game, int level) {
        this(game, level, null, false, level);
    }

    // This is the one "Floor 1" uses:
    public GameScreen(MazeRunnerGame game, int level, FileHandle levelFile, boolean endlessMode, int startFloor) {
        this.game = game;
        this.levelFile = levelFile;
        this.level = level;
        this.endlessMode = endlessMode;
        this.floor = startFloor;

        // --- FIX: INITIALIZE VIEWPORTS HERE TOO ---
        this.viewport = new ScreenViewport();
        this.uiViewport = new com.badlogic.gdx.utils.viewport.FitViewport(1280, 720);
        // ------------------------------------------

        // Initialize viewport for the HUD specifically if needed early (usually done in show(), but safe here)
        this.viewport.update(Gdx.graphics.getWidth(), Gdx.graphics.getHeight(), true);
    }

    // This is the one "Load Game" uses:
    public GameScreen(MazeRunnerGame game, String mapPath) {
        this.game = game;

        // --- FIX: INITIALIZE BOTH VIEWPORTS HERE ---
        this.viewport = new ScreenViewport();
        this.uiViewport = new com.badlogic.gdx.utils.viewport.FitViewport(1280, 720);
        // ------------------------------------------

        FileHandle localHandle = Gdx.files.local(mapPath);
        if (localHandle.exists()) {
            this.levelFile = localHandle;
        } else {
            this.levelFile = Gdx.files.internal(mapPath);
        }

        this.endlessMode = false;
        this.floor = 1;

        // Extract level number from filename
        String filename = levelFile.nameWithoutExtension();
        int parsedLevel = 1;
        try {
            String numberPart = filename.replace("level-", "");
            parsedLevel = Integer.parseInt(numberPart);
        } catch (NumberFormatException e) {
            // Ignore if parsing fails
        }
        this.level = parsedLevel;
    }

    @Override
    public void render(float delta) {

        // =============================
        // INPUT
        // =============================
        if (gameStateController.isRunning() && Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE)) {
            togglePause();
        }

        if (Gdx.input.isKeyJustPressed(Input.Keys.K)) {
            int needed = player.getXpNeededForNextLevel() - player.getExperience();
            player.addExperience(Math.max(needed, 0));
            hud.showNotification("CHEAT: FORCED LEVEL UP!");
            if (pickupSound != null) pickupSound.play();
        }

        if (Gdx.input.isKeyJustPressed(Input.Keys.GRAVE)) {
            hud.getDevConsole().toggle();
        }

        cameraInput.update(delta);

        // =============================
        // GAME LOGIC
        // =============================
        if (!isPaused && gameStateController.isRunning()) {

            player.update(delta, maze, game, this::collidesWithBlockingObstacle, gameCamera.getCamera());

            if (player.getPlayerLevel() > trackedLevel) {
                trackedLevel = player.getPlayerLevel();
                hud.showNotification("LEVEL UP! (" + trackedLevel + ")\n+1 Skill Point\nHealth Restored");

                if (pickupSound != null) {
                    long id = pickupSound.play();
                    pickupSound.setPitch(id, 1.5f);
                    pickupSound.setVolume(id, game.getPreferences().getSoundVolume());
                }
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

            if (gameStateController.isRunning()) {
                if (worldLogic.handleKeyPickup(player, maze)) {
                    if (pickupSound != null) {
                        long id = pickupSound.play();
                        pickupSound.setVolume(id, game.getPreferences().getSoundVolume());
                    }
                }
            }

            // =============================
            // ITEM PICKUPS
            // =============================
            if (items != null) {
                Rectangle playerBounds =
                        new Rectangle(player.getX(), player.getY(), PLAYER_WIDTH, PLAYER_HEIGHT);

                var iter = items.iterator();
                while (iter.hasNext()) {
                    AbstractCollectable item = iter.next();

                    if (item.isActive() && playerBounds.overlaps(item.getBounds())) {
                        item.applyEffect(player);

                        if (pickupSound != null) {
                            long id = pickupSound.play();
                            pickupSound.setVolume(id, game.getPreferences().getSoundVolume());
                        }

                        item.collect();
                        iter.remove();
                    }
                }
            }

            // =============================
            // COMBAT
            // =============================
            if (gameStateController.isRunning()) {

                int previousLives = player.getLives();

                combatSystem.update(
                        delta,
                        player,
                        bossTarget,
                        boss,
                        maze,
                        enemies,
                        obstacles,
                        gameCamera
                );

                if (player.getLives() < previousLives && hitSound != null) {
                    long id = hitSound.play();
                    hitSound.setVolume(id, game.getPreferences().getSoundVolume());
                }

                // Boss death → relic drop
                if (boss != null && boss.isDead() && !bossDropSpawned) {
                    bossDropSpawned = true;

                    float dropX = boss.getHitbox().x + boss.getHitbox().width / 2f;
                    float dropY = boss.getHitbox().y + boss.getHitbox().height / 2f;

                    if (items != null) {
                        items.add(new de.tum.cit.fop.maze.items.BossRelicItem(dropX, dropY));
                    }

                    hud.showNotification("BOSS DEFEATED!\nRelic dropped.");
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

        // =============================
        // RENDER WORLD
        // =============================
        ScreenUtils.clear(0, 0, 0, 1);

        game.getSpriteBatch().setProjectionMatrix(gameCamera.getCamera().combined);
        game.getSpriteBatch().begin();

        mazeRenderer.render(maze, game.getSpriteBatch());
        combatSystem.renderProjectiles(game.getSpriteBatch());

        if (obstacles != null) {
            for (Obstacle obstacle : obstacles) {
                if (obstacle instanceof SpikeTrap spike) {
                    game.getSpriteBatch().draw(spike.getCurrentFrame(),
                            spike.getCollisionBox().x, spike.getCollisionBox().y, 64, 64);
                } else if (obstacle instanceof SlimeTrap slime) {
                    game.getSpriteBatch().draw(slime.getSprite(),
                            slime.getCollisionBox().x, slime.getCollisionBox().y, 64, 64);
                } else if (obstacle instanceof FloorHazard hazard) {
                    game.getSpriteBatch().draw(hazard.getSprite(),
                            hazard.getCollisionBox().x, hazard.getCollisionBox().y, 64, 64);
                } else if (obstacle instanceof Prop prop) {
                    game.getSpriteBatch().draw(prop.getSprite(),
                            prop.getCollisionBox().x,
                            prop.getCollisionBox().y,
                            prop.getCollisionBox().width,
                            prop.getCollisionBox().height);
                }
            }
        }

        if (player.isDamageFlashing()) {
            game.getSpriteBatch().setColor(Color.RED);
        }

        player.draw(game.getSpriteBatch());
        game.getSpriteBatch().setColor(Color.WHITE);

        if (items != null) {
            for (AbstractCollectable item : items) {
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
                    game.getSpriteBatch().draw(frame,
                            enemy.getPosition().x,
                            enemy.getPosition().y,
                            32, 32);
                }
            }
        }

        if (boss != null && !boss.isDead()) {
            boss.render(game.getSpriteBatch());
        }

        float px = player.getX() + PLAYER_WIDTH / 2f;
        float py = player.getY() + PLAYER_HEIGHT / 2f;

        game.getSpriteBatch().end();

        fog.render(game.getSpriteBatch(), gameCamera.getCamera(), px, py);

        // =============================
        // UI
        // =============================
        if (isPaused) {
            pauseStage.getViewport().apply();
            pauseStage.act(delta);
            pauseStage.draw();
        } else {
            hud.getStage().getViewport().apply();
            hud.act(delta);
            hud.draw();
        }

        if (Gdx.input.isKeyPressed(Input.Keys.M)) {
            renderMinimap();
        }

        // =============================
        // BOSS HP BAR
        // =============================
        if (boss != null && !boss.isDead()) {
            uiViewport.apply();
            shapeRenderer.setProjectionMatrix(uiViewport.getCamera().combined);

            float barW = 420f;
            float barH = 18f;
            float x = (uiViewport.getWorldWidth() - barW) / 2f;
            float y = uiViewport.getWorldHeight() - 35f;

            float pct = boss.getHp() / (float) boss.getMaxHp();
            pct = Math.max(0f, Math.min(1f, pct));

            Gdx.gl.glEnable(GL20.GL_BLEND);
            shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);

            shapeRenderer.setColor(0f, 0f, 0f, 0.6f);
            shapeRenderer.rect(x - 2, y - 2, barW + 4, barH + 4);

            shapeRenderer.setColor(0.8f, 0.1f, 0.1f, 1f);
            shapeRenderer.rect(x, y, barW * pct, barH);

            shapeRenderer.end();
            Gdx.gl.glDisable(GL20.GL_BLEND);
        }
    }


    @Override
    public void show() {
        Gdx.app.log("MAZE", "GameScreen level=" + level);
        enemies = new ArrayList<>();
        obstacles = new ArrayList<>();
        items = new ArrayList<>();

        runSeed = System.currentTimeMillis();
        shapeRenderer = new ShapeRenderer();

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
            // Use the seed that created the saved map
            runSeed = game.getPendingSaveState().mazeSeed;
        } else {
            // New game: Random seed
            runSeed = System.currentTimeMillis();
        }

        // 1) Maze & graphics first (camera needs maze size)
        initializeMaze(runSeed);
        initializeGraphics();
        // 2) Player now (some UI depends on player)
        initializePlayer();
        // 3) UI now (skill window uses player)
        createPauseMenu();
        createHud();

        // Correct label for each mode
        if (endlessMode) hud.setFloorText("Round: " + floor);
        else hud.setFloorText("Floor: " + level);

        // Exit target for arrow/hints
        updateHudExitTarget();

        int lv = clampLevel(endlessMode ? floor : level);

        enemyCount = ENEMIES_PER_LEVEL[lv];
        trapCount  = TRAPS_PER_LEVEL[lv];
        itemCount  = ITEMS_PER_LEVEL[lv];
        propCount  = PROPS_PER_LEVEL[lv];

        Gdx.app.log("SPAWN", "counts: enemies=" + enemyCount
                + " traps=" + trapCount
                + " items=" + itemCount
                + " props=" + propCount);
        // 4) Spawn enemies/items

        // Boss only on level 5
        boss = null;
        bossTarget = null;
        bossDropSpawned = false;

        if (clampLevel(endlessMode ? floor : level) == 5) {
            bossAnims = new BossAnimations("Sword.png");
            boss = BossSpawner.spawnBossIfFinalLevel(maze, 5, bossAnims);
            bossTarget = new PlayerAdapter(player);
        }


        Gdx.app.log("SPAWN", "rooms=" + (rooms == null ? 0 : rooms.size())
                + " enemies=" + (enemies == null ? 0 : enemies.size()));


        trackedLevel = player.getPlayerLevel();
        hud.getDevConsole().setPlayer(player);

        multiplexer = new InputMultiplexer();
        multiplexer.addProcessor(cameraInput);
        multiplexer.addProcessor(hud.getStage());
        Gdx.input.setInputProcessor(multiplexer);

        fog = new FogOfWarRenderer();
        fog.setFogAlpha(0.95f);

        Gdx.app.log("SPAWN", "counts: enemies=" + enemyCount
                + " traps=" + trapCount
                + " items=" + itemCount
                + " props=" + propCount);

// IMPORTANT: actually spawn things
        initializeEnemiesAndObstacles(runSeed);

// log AFTER spawning
        Gdx.app.log("SPAWN", "rooms=" + (rooms == null ? 0 : rooms.size())
                + " enemies=" + (enemies == null ? 0 : enemies.size())
                + " items=" + (items == null ? 0 : items.size())
                + " obstacles=" + (obstacles == null ? 0 : obstacles.size()));


        game.clearPendingSaveState();
    }


    private void initializePlayer() {
        Point entry = maze.getEntryCell();
        if (entry == null) entry = new Point(1, 1);
        Point safeSpawn = findSafeSpawnPosition(entry, maze);

        // FIXED: Added 'game' to the end of constructor for Bullet system
        player = new Player(
                safeSpawn.x * TILE_SIZE,
                safeSpawn.y * TILE_SIZE,
                game.getAnimDown(),
                game.getAnimUp(),
                game.getAnimLeft(),
                game.getAnimRight(),
                game // <--- IMPORTANT: Required for bullets
        );

        // --- CHECK FOR SAVED DATA ---
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
    }

    private void initializeEnemiesAndObstacles(long seed) {
        Room spawnRoom = findSpawnRoom();

        // 1) Traps (NOT in spawn room)
        obstacles = new ObstaclePlacer().placeObstacles(maze, trapCount, seed, spawnRoom);

        // 2) Props (ALLOWED in spawn room) - use propCount instead of 20
        int effectiveLevel = endlessMode ? floor : level;
        PropPlacer propPlacer = new PropPlacer();
        var placedProps = propPlacer.placeProps(maze, propCount, seed, Math.min(effectiveLevel, 5), propSprites);
        obstacles.addAll(placedProps);

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

        // 4) Items / powerups (ALLOWED in spawn room)
        if (savedState != null && savedState.items != null && !savedState.items.isEmpty()) {
            items = new ArrayList<>();
            for (GameState.ItemState is : savedState.items) {
                if (is.type.equals("gun")) items.add(new GunCollectable(is.x, is.y));
                else if (is.type.equals("heart")) items.add(new HeartItem(is.x, is.y));
                else if (is.type.equals("speed")) items.add(new SpeedItem(is.x, is.y));
                else if (is.type.equals("shield")) items.add(new ShieldItem(is.x, is.y));
            }
        } else {
            items = new ItemSpawner().spawnItems(maze, itemCount); // use per-level
        }
    }


    private void initializeEvents() {
        // Placeholder for event logic
    }

    private void renderMinimap() {
        if (shapeRenderer == null || uiViewport == null || maze == null || player == null) return;

        uiViewport.apply();

        float worldW = uiViewport.getWorldWidth();
        float worldH = uiViewport.getWorldHeight();

        float mapX = worldW - minimapSize - 20;
        float mapY = worldH - minimapSize - 20;

        // Show a 30-tile range (15 left, 15 right)
        float viewRange = 15 * TILE_SIZE;
        float scale = minimapSize / (viewRange * 2);

        float playerX = player.getX();
        float playerY = player.getY();
        float centerX = mapX + minimapSize / 2f;
        float centerY = mapY + minimapSize / 2f;

        // blending for transparent background
        Gdx.gl.glEnable(GL20.GL_BLEND);
        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);

        shapeRenderer.setProjectionMatrix(uiViewport.getCamera().combined);

        // =========================
        // FILLED PASS
        // =========================
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);

        // Background
        shapeRenderer.setColor(0, 0, 0, 0.5f);
        shapeRenderer.rect(mapX, mapY, minimapSize, minimapSize);

        // Walls
        shapeRenderer.setColor(Color.LIGHT_GRAY);

        int radius = 20;
        int startCol = (int)(playerX / TILE_SIZE) - radius;
        int endCol   = (int)(playerX / TILE_SIZE) + radius;
        int startRow = (int)(playerY / TILE_SIZE) - radius;
        int endRow   = (int)(playerY / TILE_SIZE) + radius;

        for (int row = startRow; row <= endRow; row++) {
            for (int col = startCol; col <= endCol; col++) {
                if (col < 0 || row < 0 || col >= maze.getWidth() || row >= maze.getHeight()) continue;

                if (maze.getTile(col, row).isWall()) {
                    float relX = (col * TILE_SIZE - playerX) * scale;
                    float relY = (row * TILE_SIZE - playerY) * scale;

                    if (Math.abs(relX) < minimapSize / 2 && Math.abs(relY) < minimapSize / 2) {
                        shapeRenderer.rect(centerX + relX, centerY + relY,
                                TILE_SIZE * scale, TILE_SIZE * scale);
                    }
                }
            }
        }

        // Enemies (red)
        if (enemies != null) {
            shapeRenderer.setColor(Color.RED);
            for (Enemy enemy : enemies) {
                float ex = enemy.getPosition().x;
                float ey = enemy.getPosition().y;

                float relX = (ex - playerX) * scale;
                float relY = (ey - playerY) * scale;

                if (Math.abs(relX) < minimapSize / 2 && Math.abs(relY) < minimapSize / 2) {
                    shapeRenderer.circle(centerX + relX, centerY + relY, 4);
                }
            }
        }

        // Boss (purple)
        if (boss != null && !boss.isDead()) {
            shapeRenderer.setColor(Color.PURPLE);

            Rectangle hb = boss.getHitbox();
            float bx = hb.x + hb.width / 2f;
            float by = hb.y + hb.height / 2f;

            float relX = (bx - playerX) * scale;
            float relY = (by - playerY) * scale;

            if (Math.abs(relX) < minimapSize / 2 && Math.abs(relY) < minimapSize / 2) {
                shapeRenderer.circle(centerX + relX, centerY + relY, 5);
            }
        }

        // Player (green)
        shapeRenderer.setColor(Color.GREEN);
        shapeRenderer.circle(centerX, centerY, 5);

        shapeRenderer.end();

        // =========================
        // BORDER PASS
        // =========================
        shapeRenderer.begin(ShapeRenderer.ShapeType.Line);
        shapeRenderer.setColor(Color.WHITE);
        shapeRenderer.rect(mapX, mapY, minimapSize, minimapSize);
        shapeRenderer.end();

        Gdx.gl.glDisable(GL20.GL_BLEND);
    }


    private void createHud() {
        hud = new Hud(uiViewport, game.getSpriteBatch(), game.getSkin());
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

    private void initializeMaze(long seed) {
        int effectiveLevel = endlessMode ? floor : level;

        GameState saved = game.getPendingSaveState();
        boolean loadingSave = (saved != null);

        // =========================
        // 1) LOAD GAME -> REGEN FROM SAVED SEED
        // =========================
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

        // =========================
        // 2) NEW RUN / LEVEL SELECT -> ALWAYS GENERATE NEW
        // =========================
        FileHandle localTarget = getLevelTargetFile(level);

        Gdx.app.log("MAZE", "NEW RUN -> generating NEW maze -> " + localTarget.path());

        MazeGenerator generator = new MazeGenerator();
        MazeLoader.LevelSpec spec = new MazeLoader.LevelSpec(80, 80, new HashMap<>());
        GeneratedMaze generated = generator.generateWithRooms(
                spec, TILE_SIZE, seed, clampLevel(effectiveLevel)
        );

        this.rooms = generated.getRooms();

        // Optional: keep writing the file (debug/replay)
        MazeExporter.saveToProperties(generated.getMaze(), localTarget);
        activeLevelFile = localTarget;

        // You can use generated.getMaze() directly, but keeping your reload is fine:
        MazeLoader loader = new MazeLoader();
        this.maze = loader.loadMazeFromProperties(localTarget, TILE_SIZE);
        this.maze.setRooms(this.rooms);

        Gdx.app.log("MAZE", "Generated + saved + reloaded -> " + localTarget.path()
                + " | rooms=" + (rooms == null ? 0 : rooms.size()));
    }




    private void initializeGraphics() {
        gameCamera = new GameCamera(
                viewport.getWorldWidth(),
                viewport.getWorldHeight(),
                maze.getWidth() * TILE_SIZE,
                maze.getHeight() * TILE_SIZE
        );

        gameCamera.setZoom(0.25f);
        cameraInput = new CameraInputController(gameCamera);

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

        loadPropTexturesForLevel();
    }

    private void loadPropTexturesForLevel() {
        for (Texture t : propTextures.values()) t.dispose();
        propTextures.clear();
        propSprites.clear();

        for (PropType type : PropType.values()) {
            Texture tex = new Texture(Gdx.files.internal(type.texturePath));
            tex.setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest);

            propTextures.put(type, tex);
            propSprites.put(type, new TextureRegion(tex));
        }
    }

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
                () -> { playClickSound(); pauseWindow.setVisible(false); settingsWindow.setVisible(true); },
                () -> {
                    playClickSound();
                    skillWindow.refresh();
                    pauseWindow.setVisible(false);
                    skillWindow.setVisible(true);

                    // Center skill window
                    skillWindow.setPosition(
                            (uiViewport.getWorldWidth() - skillWindow.getWidth()) / 2f,
                            (uiViewport.getWorldHeight() - skillWindow.getHeight()) / 2f
                    );
                },
                () -> { playClickSound(); game.goToMenu(); }
        );

        TextButton saveButton = new TextButton("Save Game", game.getSkin());
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
        pauseStage.addActor(rootTable);      // The dark background + pause menu
        pauseStage.addActor(settingsWindow); // Popups on top
        pauseStage.addActor(skillWindow);    // Popups on top

        settingsWindow.setVisible(false);
        skillWindow.setVisible(false);
    }

    private void showGameOverMenu(boolean won) {
        if (won) {
            int nextLevel = level + 1;
            game.getPreferences().setMaxLevel(nextLevel);
        }

        if (pauseWindow != null) pauseWindow.setVisible(false);
        if (settingsWindow != null) settingsWindow.setVisible(false);

        // FIX: Remove "de.tum.cit.fop.maze." prefix so it uses the import
        GameOverWindow window = new GameOverWindow(
                game.getSkin(),
                game,
                level,
                won,
                activeLevelFile != null ? activeLevelFile : levelFile,
                player.getScore(),
                player
        );
        window.setPosition(
                (uiViewport.getWorldWidth() - window.getWidth()) / 2f,
                (uiViewport.getWorldHeight() - window.getHeight()) / 2f
        );

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

        TextButton backBtn = new TextButton("Back", game.getSkin());
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
        settingsWindow.setPosition(
                (viewport.getWorldWidth() - settingsWindow.getWidth()) / 2f,
                (viewport.getWorldHeight() - settingsWindow.getHeight()) / 2f
        );
    }

    @Override
    public void resize(int width, int height) {
        viewport.update(width, height, true);
        uiViewport.update(width, height, true);

        if (hud != null) hud.resize(width, height, true);
        if (gameCamera != null) gameCamera.resize(viewport.getWorldWidth(), viewport.getWorldHeight());
        if (fog != null) fog.resize(width, height);
    }


    private void advanceToNextFloor() {
        floor++;
        hud.showNotification("ROUND " + floor);
        hud.setFloorText("Round: " + floor);

        long seed = System.nanoTime();
        runSeed = seed;

        initializeMaze(seed);
        updateHudExitTarget();

        int lv = clampLevel(floor);  // endless floors clamp to 1..5 for scaling
        enemyCount = ENEMIES_PER_LEVEL[lv];
        trapCount  = TRAPS_PER_LEVEL[lv];
        itemCount  = ITEMS_PER_LEVEL[lv];
        propCount  = PROPS_PER_LEVEL[lv];


        gameCamera = new GameCamera(
                viewport.getWorldWidth(),
                viewport.getWorldHeight(),
                maze.getWidth() * TILE_SIZE,
                maze.getHeight() * TILE_SIZE
        );
        gameCamera.setZoom(0.25f);
        cameraInput = new CameraInputController(gameCamera);

        Point entry = maze.getEntryCell();
        if (entry == null) entry = new Point(1, 1);
        Point safeSpawn = findSafeSpawnPosition(entry, maze);
        player.setPosition(safeSpawn.x * TILE_SIZE, safeSpawn.y * TILE_SIZE);
        player.clearKey();

        initializeEnemiesAndObstacles(seed);
        boss = null;
        bossTarget = null;
        bossDropSpawned = false;

        if (clampLevel(floor) == 5) {
            if (bossAnims != null) bossAnims.dispose();
            bossAnims = new BossAnimations("Sword.png");
            boss = BossSpawner.spawnBossIfFinalLevel(maze, 5, bossAnims);
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


    @Override public void pause() {}
    @Override public void resume() {}
    @Override public void hide() {}

    @Override
    public void dispose() {
        if (hud != null) hud.dispose();
        if (keyTexture != null) keyTexture.dispose();
        if (pauseStage != null) pauseStage.dispose();
        if (pickupSound != null) pickupSound.dispose();
        if (hitSound != null) hitSound.dispose();
        if (clickSound != null) clickSound.dispose();
        if (items != null) {
            for (AbstractCollectable item : items) {
                item.dispose();
            }
        }
        if (fog != null) fog.dispose();
        if (propTilesetTexture != null) propTilesetTexture.dispose();
        propSprites.clear();
        if (mazeRenderer != null) mazeRenderer.dispose();
        for (Texture t : propTextures.values()) t.dispose();
        propTextures.clear();
        propSprites.clear();
        if (bossAnims != null) bossAnims.dispose();
    }

    private void updateHudExitTarget() {
        Point exit = maze.getExitCell();
        if (exit == null) {
            hud.clearExitTarget();
            return;
        }

        hud.setExitTarget(
                exit.x * TILE_SIZE + TILE_SIZE / 2f,
                exit.y * TILE_SIZE + TILE_SIZE / 2f
        );
    }


    private FileHandle getLevelTargetFile(int level) {
        FileHandle dir = Gdx.files.local("maps");
        dir.mkdirs();
        return dir.child("level-" + level + ".properties");
    }
}