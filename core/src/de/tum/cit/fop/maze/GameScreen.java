package de.tum.cit.fop.maze;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.audio.Sound;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.scenes.scene2d.ui.*;
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
import de.tum.cit.fop.maze.enemies.Enemy;
import de.tum.cit.fop.maze.enemies.FlyingDrone;
import de.tum.cit.fop.maze.enemies.MutantWorker;
import de.tum.cit.fop.maze.enemies.RobotGuard;
import de.tum.cit.fop.maze.hud.Hud;
import de.tum.cit.fop.maze.items.ItemSpawner;
import de.tum.cit.fop.maze.items.AbstractCollectable; // Added import
import de.tum.cit.fop.maze.map.*;
import de.tum.cit.fop.maze.obstacles.*;
import de.tum.cit.fop.maze.player.Player;
import de.tum.cit.fop.maze.ui.SkillWindow;
import de.tum.cit.fop.maze.util.GridUtils;
import de.tum.cit.fop.maze.world.*;
import de.tum.cit.fop.maze.obstacles.PropType;
import de.tum.cit.fop.maze.obstacles.Prop;
import de.tum.cit.fop.maze.ui.PauseMenuWindow;

import java.awt.Point;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class GameScreen implements Screen {

    // Attributes
    private final MazeRunnerGame game;
    private final FileHandle levelFile;

    private boolean isGameOverShown = false;
    private boolean isPaused = false;
    private Stage pauseStage;
    private Window pauseWindow;
    private Window settingsWindow;
    private SkillWindow skillWindow;

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

    // --- CONSTRUCTORS ---

    public GameScreen(MazeRunnerGame game, int level, FileHandle levelFile) {
        this(game, level, levelFile, false, level);
    }

    public GameScreen(MazeRunnerGame game, int level) {
        this(game, level, null, false, level);
    }

    public GameScreen(MazeRunnerGame game, int level, FileHandle levelFile, boolean endlessMode, int startFloor) {
        this.game = game;
        this.levelFile = levelFile;
        this.level = level;
        this.viewport = new ScreenViewport();
        this.endlessMode = endlessMode;
        this.floor = startFloor;
    }

    public GameScreen(MazeRunnerGame game, String mapPath) {
        this.game = game;
        this.viewport = new ScreenViewport();
        this.levelFile = Gdx.files.internal(mapPath);
        this.endlessMode = false;
        this.floor = 1;

        // Extract level number from filename
        String filename = levelFile.nameWithoutExtension();
        int parsedLevel = 1;
        try {
            String numberPart = filename.replace("level-", "");
            parsedLevel = Integer.parseInt(numberPart);
        } catch (NumberFormatException e) {
            System.out.println("Could not parse level number from " + filename);
        }
        this.level = parsedLevel;
    }

    @Override
    public void render(float delta) {
        if (gameStateController.isRunning() && Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE)) {
            togglePause();
        }

        if (Gdx.input.isKeyJustPressed(Input.Keys.GRAVE)) {
            hud.getDevConsole().toggle();
        }

        // 2) Camera input
        cameraInput.update(delta);

        // 3) Game logic
        if (!isPaused && gameStateController.isRunning()) {
            player.update(delta, maze, game, this::collidesWithBlockingObstacle);

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

            // Endless: if exit + key, go next floor instead of winning
            if (!isPaused && gameStateController.isRunning() && endlessMode && onExit && player.hasKey()) {
                advanceToNextFloor();
                return;
            }

            gameStateController.update(delta, onExit, player.hasKey(), player.getLives());

            // Trigger Game Over Menu (Once)
            if (!isGameOverShown) {
                if (gameStateController.getState() == GameStateController.State.WON) {
                    player.addScore(1000); // Level Finish Bonus
                    showGameOverMenu(true);
                    isGameOverShown = true;
                } else if (gameStateController.getState() == GameStateController.State.LOST) {
                    showGameOverMenu(false);
                    isGameOverShown = true;
                }
            }

            if (gameStateController.shouldReturnToMenu())
                game.goToMenu();

            if (gameStateController.isRunning()) {
                if (worldLogic.handleKeyPickup(player, maze)) {
                    if (pickupSound != null) {
                        long id = pickupSound.play();
                        pickupSound.setVolume(id, game.getPreferences().getSoundVolume());
                    }
                }
            }

            // Item Pickups
            java.util.Iterator<AbstractCollectable> iter = items.iterator();
            while (iter.hasNext()) {
                AbstractCollectable item = iter.next();
                Rectangle playerBounds = new Rectangle(player.getX(), player.getY(), PLAYER_WIDTH, PLAYER_HEIGHT);

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

            if (gameStateController.isRunning()) {
                int previousLives = player.getLives();
                combatSystem.update(delta, player, maze, enemies, obstacles, gameCamera);

                if (player.getLives() < previousLives) {
                    if (hitSound != null) {
                        long id = hitSound.play();
                        hitSound.setVolume(id, game.getPreferences().getSoundVolume());
                    }
                }
            }

            hud.update(player);
            hud.act(delta);

            // Follow player
            gameCamera.follow(
                    player.getX() + PLAYER_WIDTH / 2f,
                    player.getY() + PLAYER_HEIGHT / 2f,
                    delta
            );
        }

        // Apply smooth zoom AFTER follow
        gameCamera.update(delta);

        // 4) Render world
        ScreenUtils.clear(0, 0, 0, 1);
        game.getSpriteBatch().setProjectionMatrix(gameCamera.getCamera().combined);
        game.getSpriteBatch().begin();

        mazeRenderer.render(maze, game.getSpriteBatch());
        combatSystem.renderProjectiles(game.getSpriteBatch());

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
            }
            else if (obstacle instanceof Prop prop) {
                game.getSpriteBatch().draw(prop.getSprite(),
                        prop.getCollisionBox().x,
                        prop.getCollisionBox().y,
                        prop.getCollisionBox().width,
                        prop.getCollisionBox().height);
            }
        }

        if (player.isDamageFlashing()) {
            game.getSpriteBatch().setColor(Color.RED);
        }

        player.draw(game.getSpriteBatch());
        game.getSpriteBatch().setColor(Color.WHITE);

        // Draw Items
        for (AbstractCollectable item : items) {
            item.render(game.getSpriteBatch());
        }

        // Draw enemies
        for (Enemy enemy : enemies) {
            TextureRegion frame = null;
            if (enemy instanceof MutantWorker mw) frame = mw.getCurrentFrame();
            else if (enemy instanceof RobotGuard rg) frame = rg.getCurrentFrame();
            else if (enemy instanceof FlyingDrone fd) frame = fd.getCurrentFrame();

            if (frame != null) {
                game.getSpriteBatch().draw(frame, enemy.getPosition().x, enemy.getPosition().y, 32, 32);
            }
        }

        float px = player.getX() + PLAYER_WIDTH / 2f;
        float py = player.getY() + PLAYER_HEIGHT / 2f;

        game.getSpriteBatch().end();
        fog.render(game.getSpriteBatch(), gameCamera.getCamera(), px, py);

        // 5) UI
        if (isPaused) {
            pauseStage.getViewport().apply();
            pauseStage.act(delta);
            pauseStage.draw();
        } else {
            hud.getStage().getViewport().apply();
            hud.act(delta);
            hud.draw();
        }
    }

    @Override
    public void show() {
        Gdx.app.log("MAZE", "GameScreen level=" + level);
        runSeed = System.currentTimeMillis();

        viewport.update(Gdx.graphics.getWidth(), Gdx.graphics.getHeight(), true);

        gameStateController = new GameStateController();
        worldLogic = new InteractionLogic();
        combatSystem = new CombatSystem();

        playLevelMusic();

        // 1. GENERATE MAZE & GRAPHICS FIRST (Need map to spawn player)
        initializeMaze(runSeed);
        initializeGraphics();

        // 2. CREATE PLAYER (Must be done before UI)
        initializePlayer();

        // 3. CREATE UI (Now safe because player exists)
        createPauseMenu(); // This creates SkillWindow which calls player.getSkillPoints()
        createHud();

        if (endlessMode) hud.setFloorText("Floor: " + floor);
        else hud.setFloorText("Level: " + level);

        game.getAchievementManager().setHud(hud);

        // 4. ENEMIES & EVENTS
        initializeEnemiesAndObstacles(runSeed);
        initializeEvents();

        trackedLevel = player.getPlayerLevel();
        hud.getDevConsole().setPlayer(player);

        multiplexer = new InputMultiplexer();
        multiplexer.addProcessor(cameraInput);
        multiplexer.addProcessor(hud.getStage());
        Gdx.input.setInputProcessor(multiplexer);

        fog = new FogOfWarRenderer();
        fog.setFogAlpha(0.95f);
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
            game.clearPendingSaveState();
        }
    }

    private void initializeEnemiesAndObstacles(long seed) {
        obstacles = new ObstaclePlacer().placeObstacles(maze, 40, seed);

        int effectiveLevel = endlessMode ? floor : level;

        int propCount = (effectiveLevel <= 5)
                ? switch (effectiveLevel) {
            case 1 -> 10;
            case 2 -> 18;
            case 3 -> 28;
            case 4 -> 40;
            case 5 -> 55;
            default -> 20;
        }
                : 55 + (effectiveLevel - 5) * 8;

        PropPlacer propPlacer = new PropPlacer();
        var placedProps = propPlacer.placeProps(maze, propCount, seed, Math.min(effectiveLevel, 5), propSprites);
        obstacles.addAll(placedProps);

        int baseEnemies = 28;
        int enemyCount = endlessMode ? (baseEnemies + (floor - 1) * 6) : baseEnemies;

        enemies = new EnemySpawner().spawnEnemies(maze, rooms, player, enemyCount, seed);

        int itemCount = endlessMode ? (10 + floor / 2) : 10;
        items = new ItemSpawner().spawnItems(maze, itemCount); // Fixed item count usage
    }

    private void initializeEvents() {
        // Placeholder for event logic
    }

    private void createHud() {
        Viewport hudViewport = new ScreenViewport();
        hud = new Hud(hudViewport, game.getSpriteBatch(), game.getSkin());
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
        // FIXED: Define the loader
        MazeLoader loader = new MazeLoader();
        int effectiveLevel = endlessMode ? floor : level;

        boolean forceFreshMaze = (this.level == 1 && game.getPendingSaveState() == null);

        if (!forceFreshMaze && levelFile != null && levelFile.exists()) {
            activeLevelFile = levelFile;
            maze = loader.loadMazeFromProperties(levelFile, TILE_SIZE);
            Gdx.app.log("MAZE", "Loaded existing map -> " + levelFile.path());
            return;
        }

        FileHandle target = getLevelTargetFile(level);
        Gdx.app.log("MAZE", "Generating NEW RANDOM maze -> " + target.path());

        MazeGenerator generator = new MazeGenerator();
        MazeLoader.LevelSpec spec = new MazeLoader.LevelSpec(80, 80, new HashMap<>());

        // FIXED: Use generateWithRooms since we need rooms
        GeneratedMaze generated = generator.generateWithRooms(spec, TILE_SIZE, seed, effectiveLevel);

        this.maze = generated.getMaze();
        this.rooms = generated.getRooms();

        // Save generated maze
        MazeExporter.saveToProperties(this.maze, target);

        activeLevelFile = target;
        // Reload it back to ensure consistency
        this.maze = loader.loadMazeFromProperties(target, TILE_SIZE);
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

        Texture tileset = new Texture("TILESET2.png");
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
        pauseStage = new Stage(viewport, game.getSpriteBatch());

        skillWindow = new SkillWindow(game.getSkin(), player, () -> {
            playClickSound();
            skillWindow.setVisible(false);
            pauseWindow.setVisible(true);
        });
        skillWindow.setVisible(false);

        pauseWindow = new PauseMenuWindow(
                game.getSkin(),
                () -> { playClickSound(); togglePause(); },
                () -> { playClickSound(); pauseWindow.setVisible(false); settingsWindow.setVisible(true); },
                () -> {
                    playClickSound();
                    skillWindow.refresh();
                    pauseWindow.setVisible(false);
                    skillWindow.setVisible(true);

                    skillWindow.setPosition(
                            (viewport.getWorldWidth() - skillWindow.getWidth()) / 2f,
                            (viewport.getWorldHeight() - skillWindow.getHeight()) / 2f
                    );
                },
                () -> { playClickSound(); game.goToMenu(); }
        );

        TextButton saveButton = new TextButton("Save Game", game.getSkin());
        saveButton.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                playClickSound();
                game.saveGame(player, level);
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

        pauseWindow.setPosition(
                (viewport.getWorldWidth() - pauseWindow.getWidth()) / 2f,
                (viewport.getWorldHeight() - pauseWindow.getHeight()) / 2f
        );

        createSettingsWindow();

        pauseStage.addActor(pauseWindow);
        pauseStage.addActor(settingsWindow);
        pauseStage.addActor(skillWindow);

        settingsWindow.setVisible(false);
        skillWindow.setVisible(false);
        pauseWindow.setVisible(true);
    }

    private void showGameOverMenu(boolean won) {
        if (won) {
            int nextLevel = level + 1;
            game.getPreferences().setMaxLevel(nextLevel);
        }

        if (pauseWindow != null) pauseWindow.setVisible(false);
        if (settingsWindow != null) settingsWindow.setVisible(false);

        de.tum.cit.fop.maze.GameOverWindow window = new de.tum.cit.fop.maze.GameOverWindow(
                game.getSkin(),
                game,
                level,
                won,
                activeLevelFile != null ? activeLevelFile : levelFile,
                player.getScore(),
                player
        );

        window.setPosition(
                (viewport.getWorldWidth() - window.getWidth()) / 2f,
                (viewport.getWorldHeight() - window.getHeight()) / 2f
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
        hud.resize(width, height, true);
        pauseStage.getViewport().update(width, height, true);
        gameCamera.resize(viewport.getWorldWidth(), viewport.getWorldHeight());
        fog.resize(width, height);
    }

    private void advanceToNextFloor() {
        floor++;
        hud.showNotification("FLOOR " + floor);
        hud.setFloorText("Floor: " + floor);

        long seed = System.nanoTime();
        runSeed = seed;

        initializeMaze(seed);

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
    }

    private FileHandle getLevelTargetFile(int level) {
        FileHandle dir = Gdx.files.local("maps");
        dir.mkdirs();
        return dir.child("level-" + level + ".properties");
    }
}