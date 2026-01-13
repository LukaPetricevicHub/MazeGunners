package de.tum.cit.fop.maze;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.utils.ScreenUtils;
import com.badlogic.gdx.utils.viewport.ScreenViewport;
import com.badlogic.gdx.utils.viewport.Viewport;

import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.ui.Window;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.InputMultiplexer;

import de.tum.cit.fop.maze.camera.GameCamera;
import de.tum.cit.fop.maze.enemies.Enemy;
import de.tum.cit.fop.maze.enemies.FlyingDrone;
import de.tum.cit.fop.maze.enemies.MutantWorker;
import de.tum.cit.fop.maze.enemies.RobotGuard;
import de.tum.cit.fop.maze.hud.Hud;
import de.tum.cit.fop.maze.level.*;
import de.tum.cit.fop.maze.obstacles.Obstacle;
import de.tum.cit.fop.maze.obstacles.SpikeTrap;
import de.tum.cit.fop.maze.player.Player;
import de.tum.cit.fop.maze.util.GridUtils;
import de.tum.cit.fop.maze.world.*;

import java.awt.Point;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;



public class GameScreen implements Screen {

    // Attributes
    private final MazeRunnerGame game;

    private boolean isPaused = false;
    private Stage pauseStage;

    private Viewport viewport;
    private GameCamera gameCamera;

    private Maze maze;
    private MazeRenderer mazeRenderer;

    private Player player;
    private List<Enemy> enemies;
    private List<Obstacle> obstacles;

    private RoomLock ambushRoom;
    private float ambushTimer = 0f;

    private Hud hud;

    private GameStateController gameStateController;
    private InteractionLogic worldLogic;
    private CombatSystem combatSystem;

    //Constants
    private static final int TILE_SIZE = 16;
    private static final float PLAYER_WIDTH = 32f;
    private static final float PLAYER_HEIGHT = 64f;

    private Texture keyTexture;
    private final int level;

    // Constructor
    public GameScreen(MazeRunnerGame game, int level) {
        this.game = game;
        this.level = level;
        this.viewport = new ScreenViewport();
    }

    // Render
    @Override
    public void render(float delta) {
        // 1. INPUT
        if (gameStateController.isRunning() && Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE)) {
            togglePause();
        }

        // 2. LOGIC (Only runs if NOT paused)
        if (!isPaused && gameStateController.isRunning()) {
            player.update(delta, maze, game);

            // Update GameState
            boolean onExit = worldLogic.isPlayerOnExit(player, maze);
            gameStateController.update(delta, onExit, player.hasKey(), player.getLives());

            // Check Win/Loss
            if (gameStateController.getState() == GameStateController.State.WON) hud.showCenterMessage("VICTORY");
            if (gameStateController.getState() == GameStateController.State.LOST) hud.showCenterMessage("GAME OVER");

            if (gameStateController.shouldReturnToMenu()) game.goToMenu();
            if (gameStateController.isRunning()) worldLogic.handleKeyPickup(player, maze);

            // Navigation Arrow
            Point nearestExit = worldLogic.findNearestExit(player, maze);
            if (nearestExit != null) {
                float exitX = nearestExit.x * TILE_SIZE + TILE_SIZE / 2f;
                float exitY = nearestExit.y * TILE_SIZE + TILE_SIZE / 2f;
                hud.setExitTarget(exitX, exitY);
            }

            // Ambush Room Logic
            Rectangle playerBounds = new Rectangle(player.getX(), player.getY(), PLAYER_WIDTH, PLAYER_HEIGHT);
            ambushRoom.update(playerBounds, maze);
            if (ambushRoom.isLocked()) {
                ambushTimer += delta;
                if (ambushTimer > 5f) ambushRoom.unlockRoom(maze);
            }

            // Combat
            combatSystem.update(delta, player, maze, enemies, obstacles);

            // HUD Data & Camera
            hud.update(player);
            gameCamera.follow(player.getX() + PLAYER_WIDTH / 2f, player.getY() + PLAYER_HEIGHT / 2f, 0.1f);
        }

        // 3. RENDER WORLD
        ScreenUtils.clear(0, 0, 0, 1);
        game.getSpriteBatch().setProjectionMatrix(gameCamera.getCamera().combined);
        game.getSpriteBatch().begin();

        mazeRenderer.render(maze, game.getSpriteBatch());

        // Draw Obstacles
        for (Obstacle obstacle : obstacles) {
            if (obstacle instanceof SpikeTrap spikeTrap) {
                game.getSpriteBatch().draw(spikeTrap.getCurrentFrame(), spikeTrap.getCollisionBox().x, spikeTrap.getCollisionBox().y, 16, 16);
            }
        }

        // Draw Player
        if (player.isDamageFlashing()) game.getSpriteBatch().setColor(Color.RED);
        player.draw(game.getSpriteBatch());
        game.getSpriteBatch().setColor(Color.WHITE);

        // Draw Enemies
        for (Enemy enemy : enemies) {
            TextureRegion frame = null;
            if (enemy instanceof MutantWorker mw) frame = mw.getCurrentFrame();
            else if (enemy instanceof RobotGuard rg) frame = rg.getCurrentFrame();
            else if (enemy instanceof FlyingDrone fd) frame = fd.getCurrentFrame();

            if (frame != null) {
                game.getSpriteBatch().draw(frame, enemy.getPosition().x, enemy.getPosition().y, 32, 64);
            }
        }
        game.getSpriteBatch().end();

        // 4. RENDER UI
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
        //Core Logic Systems
        gameStateController = new GameStateController();
        worldLogic = new InteractionLogic();
        combatSystem = new CombatSystem();

        //Start Level Audio
        playLevelMusic();

        //Create UI Systems
        createPauseMenu();
        createHud();

        //Generate the Maze
        long seed = System.currentTimeMillis();
        initializeMaze(seed);

        //Setup Graphics
        initializeGraphics();

        //Spawn Entities
        initializePlayer();
        initializeEnemiesAndObstacles(seed);
        initializeEvents();

        Gdx.input.setInputProcessor(hud.getStage());
    }
    private void initializePlayer() {
        Point entry = maze.getEntryCell();
        if (entry == null) entry = new Point(1, 1);

        Point safeSpawn = findSafeSpawnPosition(entry, maze);

        player = new Player(
                safeSpawn.x * TILE_SIZE,
                safeSpawn.y * TILE_SIZE,
                game.getCharacterDownAnimation()
        );
    }
    private void initializeEnemiesAndObstacles(long seed) {
        obstacles = new ObstaclePlacer().placeObstacles(maze, 40, seed);

        enemies = new EnemySpawner().spawnEnemies(maze, player, 20, seed);
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
        // Using the constant TILE_SIZE we added earlier
        maze = new MazeGenerator(3).generate(50, 50, TILE_SIZE, seed);
    }
    private void initializeGraphics() {
        gameCamera = new GameCamera(
                viewport.getWorldWidth(),
                viewport.getWorldHeight(),
                maze.getWidth() * TILE_SIZE,
                maze.getHeight() * TILE_SIZE
        );
        gameCamera.setZoom(0.75f);

        keyTexture = new Texture("key.png");

        mazeRenderer = new MazeRenderer(
                game.getTile(1, 0),
                game.getTile(0, 0),
                game.getTile(3, 2),
                game.getTile(2, 3),
                new TextureRegion(keyTexture)
        );
    }

    private void togglePause() {
        isPaused = !isPaused;

        if (isPaused) {
            game.getAudioManager().pauseMusic();
            Gdx.input.setInputProcessor(pauseStage);
        } else {
            game.getAudioManager().resumeMusic();
            Gdx.input.setInputProcessor(hud.getStage());
        }
    }

    private void createPauseMenu() {
        pauseStage = new Stage(viewport, game.getSpriteBatch());

        Window pauseWindow = new Window("Paused", game.getSkin());
        pauseWindow.getTitleLabel().setAlignment(1);
        pauseWindow.setMovable(false);

        TextButton resumeBtn = new TextButton("Resume", game.getSkin());
        resumeBtn.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                togglePause();
            }
        });

        TextButton menuBtn = new TextButton("Main Menu", game.getSkin());
        menuBtn.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                game.goToMenu();
            }
        });

        pauseWindow.add(resumeBtn).width(200).pad(10).row();
        pauseWindow.add(menuBtn).width(200).pad(10).row();

        pauseWindow.pack();

        pauseWindow.setPosition(
                (viewport.getWorldWidth() - pauseWindow.getWidth()) / 2f,
                (viewport.getWorldHeight() - pauseWindow.getHeight()) / 2f
        );

        pauseStage.addActor(pauseWindow);
    }

    // Resize
    @Override
    public void resize(int width, int height) {
        viewport.update(width, height, true);
        hud.resize(width, height, true);

        pauseStage.getViewport().update(width, height, true);

        gameCamera.resize(viewport.getWorldWidth(), viewport.getWorldHeight());
    }

    // Find a spawn point, so fat player is not stuck
    private Point findSafeSpawnPosition(Point entryCell, Maze maze) {
        int maxRadius = 10;

        for (int r = 0; r <= maxRadius; r++) {
            for (int dy = -r; dy <= r; dy++) {
                for (int dx = -r; dx <= r; dx++) {

                    int cx = entryCell.x + dx;
                    int cy = entryCell.y + dy;

                    if (!GridUtils.inBounds(cx, cy, maze.getWidth(), maze.getHeight())) {
                        continue;
                    }

                    if (!maze.getTile(cx, cy).isWalkable()) {
                        continue;
                    }

                    float worldX = cx * maze.getTileSize();
                    float worldY = cy * maze.getTileSize();

                    if (canPlayerOccupy(worldX, worldY, maze)) {
                        return new Point(cx, cy);
                    }
                }
            }
        }

        return entryCell;
    }

    // Player collision check
    private boolean canPlayerOccupy(float worldX, float worldY, Maze maze) {
        float inset = 2f;

        float left   = worldX + inset;
        float right  = worldX + PLAYER_WIDTH - inset;
        float bottom = worldY + inset;
        float top    = worldY + PLAYER_HEIGHT - inset;

        return !maze.isWallWorld(left, bottom)
                && !maze.isWallWorld(right, bottom)
                && !maze.isWallWorld(left, top)
                && !maze.isWallWorld(right, top);
    }

    @Override public void pause() {}
    @Override public void resume() {}
    @Override public void hide() {}

    @Override
    public void dispose() {
        hud.dispose();
        keyTexture.dispose();
        pauseStage.dispose();
    }
}
