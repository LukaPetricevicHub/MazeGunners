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

import de.tum.cit.fop.maze.camera.GameCamera;
import de.tum.cit.fop.maze.camera.CameraInputController;
import de.tum.cit.fop.maze.enemies.Enemy;
import de.tum.cit.fop.maze.enemies.FlyingDrone;
import de.tum.cit.fop.maze.enemies.MutantWorker;
import de.tum.cit.fop.maze.enemies.RobotGuard;
import de.tum.cit.fop.maze.hud.Hud;
import de.tum.cit.fop.maze.map.*;
import de.tum.cit.fop.maze.obstacles.*;
import de.tum.cit.fop.maze.player.Player;
import de.tum.cit.fop.maze.util.GridUtils;
import de.tum.cit.fop.maze.world.*;
import de.tum.cit.fop.maze.FogOfWarRenderer;
import de.tum.cit.fop.maze.obstacles.PropType;
import de.tum.cit.fop.maze.obstacles.Prop;


import java.awt.Point;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class GameScreen implements Screen {

    // Attributes
    private final MazeRunnerGame game;

    private boolean isGameOverShown = false;
    private boolean isPaused = false;
    private Stage pauseStage;
    private Window pauseWindow;
    private Window settingsWindow;

    private Viewport viewport;
    private GameCamera gameCamera;

    private InputMultiplexer multiplexer;
    private CameraInputController cameraInput;

    private Maze maze;
    private MazeRenderer mazeRenderer;

    private Player player;
    private List<Enemy> enemies;
    private List<Obstacle> obstacles;
    private final Map<PropType, TextureRegion> propSprites = new HashMap<>();
    private Texture propTilesetTexture;
    private static final int PROP_TILE_PX = 32;
    private Texture texFloorRoom, texFloorHall, texWallFill, texWallSet, texSpawn, texExit, texDoor, texKey;


    private RoomLock ambushRoom;
    private float ambushTimer = 0f;

    private Hud hud;

    private GameStateController gameStateController;
    private InteractionLogic worldLogic;
    private CombatSystem combatSystem;

    private static final int TILE_SIZE = 32;
    private static final float PLAYER_WIDTH = 32f;
    private static final float PLAYER_HEIGHT = 32f;

    private Texture keyTexture;
    private final int level;

    private Sound pickupSound;
    private Sound hitSound;

    private FogOfWarRenderer fog;

    public GameScreen(MazeRunnerGame game, int level) {
        this.game = game;
        this.level = level;
        this.viewport = new ScreenViewport();
    }


    @Override
    public void render(float delta) {
        // 1) Pause toggle
        if (gameStateController.isRunning() && Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE)) {
            togglePause();
        }

        // 2) Camera input
        cameraInput.update(delta);

        // 3) Game logic
        if (!isPaused && gameStateController.isRunning()) {
            player.update(delta, maze, game, this::collidesWithBlockingObstacle);


            boolean onExit = worldLogic.isPlayerOnExit(player, maze);

            gameStateController.update(delta, onExit, player.hasKey(), player.getLives());
            // Trigger Game Over Menu (Once)
            if (!isGameOverShown) {
                if (gameStateController.getState() == GameStateController.State.WON) {
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

            Rectangle playerBounds = new Rectangle(player.getX(), player.getY(), PLAYER_WIDTH, PLAYER_HEIGHT);

            if (gameStateController.isRunning()) {
                ambushRoom.update(playerBounds, maze);
                if (ambushRoom.isLocked()) {
                    ambushTimer += delta;
                    if (ambushTimer > 5f) {
                        ambushRoom.unlockRoom(maze);
                    }
                }
            }

            if (gameStateController.isRunning()) {
                combatSystem.update(delta, player, maze, enemies, obstacles, gameCamera);
            }

            hud.update(player);
            hud.act(delta);

            // Follow player (position)
            gameCamera.follow(
                    player.getX() + PLAYER_WIDTH / 2f,
                    player.getY() + PLAYER_HEIGHT / 2f,
                    delta
            );
        }

        // Apply smooth zoom AFTER follow (so they don't fight)
        gameCamera.update(delta);

        // 4) Render world
        ScreenUtils.clear(0, 0, 0, 1);
        game.getSpriteBatch().setProjectionMatrix(gameCamera.getCamera().combined);
        game.getSpriteBatch().begin();

        mazeRenderer.render(maze, game.getSpriteBatch());



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

        for (Enemy enemy : enemies) {
            TextureRegion frame = null;
            if (enemy instanceof MutantWorker mw) frame = mw.getCurrentFrame();
            else if (enemy instanceof RobotGuard rg) frame = rg.getCurrentFrame();
            else if (enemy instanceof FlyingDrone fd) frame = fd.getCurrentFrame();

            if (frame != null) {
                game.getSpriteBatch().draw(frame, enemy.getPosition().x, enemy.getPosition().y, 32, 32);
            }
        }

        float worldW = maze.getWidth() * TILE_SIZE;
        float worldH = maze.getHeight() * TILE_SIZE;

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
        long seed = System.currentTimeMillis();

        // IMPORTANT: gives ScreenViewport a real size BEFORE creating GameCamera
        viewport.update(Gdx.graphics.getWidth(), Gdx.graphics.getHeight(), true);

        gameStateController = new GameStateController();
        worldLogic = new InteractionLogic();
        combatSystem = new CombatSystem();

        playLevelMusic();

        createPauseMenu();
        createHud();

        initializeMaze(seed);
        initializeGraphics();

        initializePlayer();
        initializeEnemiesAndObstacles(seed);
        initializeEvents();

        multiplexer = new InputMultiplexer();
        multiplexer.addProcessor(cameraInput);     // camera FIRST
        multiplexer.addProcessor(hud.getStage());  // UI SECOND
        Gdx.input.setInputProcessor(multiplexer);

        fog = new FogOfWarRenderer();
        fog.setFogAlpha(0.95f);
    }

    private void initializePlayer() {
        Point entry = maze.getEntryCell();
        if (entry == null) entry = new Point(1, 1);
        Point safeSpawn = findSafeSpawnPosition(entry, maze);
        player = new Player(safeSpawn.x * TILE_SIZE, safeSpawn.y * TILE_SIZE, game.getCharacterDownAnimation());
    }

    private void initializeEnemiesAndObstacles(long seed) {
        obstacles = new ObstaclePlacer().placeObstacles(maze, 40, seed);

        int propCount = switch (level) {
            case 1 -> 10;
            case 2 -> 18;
            case 3 -> 28;
            case 4 -> 40;
            case 5 -> 55;
            default -> 20;
        };

        obstacles.addAll(new PropPlacer().placeProps(maze, propCount, seed, level, propSprites));

        enemies = new EnemySpawner().spawnEnemies(maze, player, 200, seed);
    }

    private void initializeEvents() {
        Rectangle trigger = new Rectangle(50 * TILE_SIZE, 50 * TILE_SIZE, 5 * TILE_SIZE, 5 * TILE_SIZE);
        List<Point> doors = new ArrayList<>();
        doors.add(new Point(50, 49));
        doors.add(new Point(55, 55));
        ambushRoom = new RoomLock(trigger, doors);
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
        MazeLoader loader = new MazeLoader();

        FileHandle template = Gdx.files.internal("maps/level-" + level + ".properties");
        MazeLoader.LevelSpec spec = loader.loadFromProperties(template);

        MazeGenerator gen = new MazeGenerator();
        Maze generated = gen.generate(spec, TILE_SIZE, seed, level);

        Gdx.files.local("maps").mkdirs();
        FileHandle out = Gdx.files.local("maps/level-" + level + ".properties");
        MazeExporter.saveToProperties(generated, out);

        MazeLoader.LevelSpec spec2 = loader.loadFromProperties(out);
        maze = loader.buildMaze(spec2, TILE_SIZE);
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

        texFloorRoom = new Texture("FLOOR.png");
        texFloorHall = new Texture("DOORSTART.png");
        texWallFill  = new Texture("WALL_FILL.png");
        texSpawn = new Texture("SPAWN.png");
        texExit  = new Texture("EXIT.png");
        texKey   = new Texture("key.png");
        texDoor  = new Texture("DOORSTART.png");

        // 4x2 sheet texture
        texWallSet = new Texture("TILESET.png");

        mazeRenderer = new MazeRenderer(
                new TextureRegion(texFloorRoom),
                new TextureRegion(texFloorHall),
                new TextureRegion(texWallFill),
                new TextureRegion(texWallSet),
                new TextureRegion(texSpawn),
                new TextureRegion(texExit),
                new TextureRegion(texKey),
                new TextureRegion(texDoor)
        );

        if (Gdx.files.internal("audio/pickup.mp3").exists()) {
            pickupSound = Gdx.audio.newSound(Gdx.files.internal("audio/pickup.mp3"));
        }
        if (Gdx.files.internal("audio/hit.mp3").exists()) {
            hitSound = Gdx.audio.newSound(Gdx.files.internal("audio/hit.mp3"));
        }

        loadPropTexturesForLevel();

    }

    private void loadPropTexturesForLevel() {
        propTilesetTexture = new Texture("PROPS.png");
        int textureH = propTilesetTexture.getHeight();

        for (PropType type : PropType.values()) {
            int srcX = type.col * PROP_TILE_PX;
            int srcY = textureH - (type.row + type.tilesH) * PROP_TILE_PX;
            int srcW = type.tilesW * PROP_TILE_PX;
            int srcH = type.tilesH * PROP_TILE_PX;
            propSprites.put(type, new TextureRegion(propTilesetTexture, srcX, srcY, srcW, srcH));
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
        createPauseWindow();
        createSettingsWindow();

        settingsWindow.setVisible(false);
        pauseWindow.setVisible(true);

        pauseStage.addActor(pauseWindow);
        pauseStage.addActor(settingsWindow);
    }
    private void showGameOverMenu(boolean won) {
        //Creates Window
        String title = won ? "VICTORY!" : "GAME OVER";
        Window gameOverWindow = new Window(title, game.getSkin());
        gameOverWindow.getTitleLabel().setAlignment(1);
        gameOverWindow.setMovable(false);

        String message = won ? "Level Complete!" : "Better luck next time...";
        gameOverWindow.add(new Label(message, game.getSkin())).padBottom(20).row();

        // Next Level Button
        if (won && level < 5) {
            TextButton nextBtn = new TextButton("Next Level", game.getSkin());
            nextBtn.addListener(new ChangeListener() {
                @Override
                public void changed(ChangeEvent event, Actor actor) {
                    game.setScreen(new GameScreen(game, level + 1));
                }
            });
            gameOverWindow.add(nextBtn).width(200).padBottom(10).row();
        }

        // Restart Button
        TextButton restartBtn = new TextButton("Restart Level", game.getSkin());
        restartBtn.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                game.setScreen(new GameScreen(game, level));
            }
        });
        gameOverWindow.add(restartBtn).width(200).padBottom(10).row();

        // Main Menu Button
        TextButton menuBtn = new TextButton("Main Menu", game.getSkin());
        menuBtn.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                game.goToMenu();
            }
        });
        gameOverWindow.add(menuBtn).width(200).row();

        //Show
        gameOverWindow.pack();
        gameOverWindow.setPosition(
                (viewport.getWorldWidth() - gameOverWindow.getWidth()) / 2f,
                (viewport.getWorldHeight() - gameOverWindow.getHeight()) / 2f
        );

        pauseStage.addActor(gameOverWindow);

        isPaused = true;
        Gdx.input.setInputProcessor(pauseStage);
    }

    private void createPauseWindow() {
        pauseWindow = new Window("Paused", game.getSkin());
        pauseWindow.getTitleLabel().setAlignment(1);
        pauseWindow.setMovable(false);

        TextButton resumeBtn = new TextButton("Resume", game.getSkin());
        resumeBtn.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                togglePause();
            }
        });

        TextButton settingsBtn = new TextButton("Settings", game.getSkin());
        settingsBtn.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                pauseWindow.setVisible(false);
                settingsWindow.setVisible(true);
            }
        });

        TextButton menuBtn = new TextButton("Exit to Menu", game.getSkin());
        menuBtn.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                game.goToMenu();
            }
        });

        pauseWindow.add(resumeBtn).width(200).pad(10).row();
        pauseWindow.add(settingsBtn).width(200).pad(10).row();
        pauseWindow.add(menuBtn).width(200).pad(10).row();
        pauseWindow.pack();
        pauseWindow.setPosition(
                (viewport.getWorldWidth() - pauseWindow.getWidth()) / 2f,
                (viewport.getWorldHeight() - pauseWindow.getHeight()) / 2f
        );
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
                game.getPreferences().setSoundVolume(sfxSlider.getValue());
            }
        });

        TextButton backBtn = new TextButton("Back", game.getSkin());
        backBtn.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
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

        // 1) wall checks (same as before)
        boolean wallFree = !maze.isWallWorld(worldX + inset, worldY + inset)
                && !maze.isWallWorld(worldX + PLAYER_WIDTH - inset, worldY + inset)
                && !maze.isWallWorld(worldX + inset, worldY + PLAYER_HEIGHT - inset)
                && !maze.isWallWorld(worldX + PLAYER_WIDTH - inset, worldY + PLAYER_HEIGHT - inset);

        if (!wallFree) return false;

        // 2) obstacle check (rectangle overlap)
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
        if (fog != null) fog.dispose();
        if (propTilesetTexture != null) propTilesetTexture.dispose();
        propSprites.clear();
        if (texFloorRoom != null) texFloorRoom.dispose();
        if (texFloorHall != null) texFloorHall.dispose();
        if (texWallFill != null) texWallFill.dispose();
        if (texWallSet != null) texWallSet.dispose();
        if (texSpawn != null) texSpawn.dispose();
        if (texExit != null) texExit.dispose();
        if (texKey != null) texKey.dispose();
        if (texDoor != null) texDoor.dispose();

    }
}
