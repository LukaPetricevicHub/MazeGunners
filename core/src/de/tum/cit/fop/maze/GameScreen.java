package de.tum.cit.fop.maze;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.utils.ScreenUtils;
import com.badlogic.gdx.utils.viewport.ScreenViewport;
import com.badlogic.gdx.utils.viewport.Viewport;

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
import de.tum.cit.fop.maze.world.EnemySpawner;
import de.tum.cit.fop.maze.world.ObstaclePlacer;
import de.tum.cit.fop.maze.world.RoomLock;

import java.awt.Point;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class GameScreen implements Screen {

    // Attributes
    private final MazeRunnerGame game;

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

    private static final int TILE_SIZE = 16;
    private static final float PLAYER_WIDTH = 32f;
    private static final float PLAYER_HEIGHT = 64f;

    private GameState gameState = GameState.RUNNING;


    // Constructor
    public GameScreen(MazeRunnerGame game, String mapPath) {
        this.game = game;
        this.viewport = new ScreenViewport();
    }


    // Render
    @Override
    public void render(float delta) {
        if (Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE)) {
            game.goToMenu();
        }

        // Update player
        if (gameState == GameState.RUNNING)
            player.update(delta, maze, game);



        if(gameState == GameState.RUNNING) {

            if (isPlayerOnExit(player, maze) && player.hasKey()) {
                gameState = GameState.WON;
                Gdx.app.log("GAME", "Victory!");
            }
            if(player.getLives() <= 0) {
                gameState = GameState.LOST;
                Gdx.app.log("GAME", "Game Over!");
            }
        }

        Point nearestExit = findNearestExit(player, maze);
        if (nearestExit != null) {
            float exitWorldX = nearestExit.x * maze.getTileSize() + maze.getTileSize() / 2f;
            float exitWorldY = nearestExit.y * maze.getTileSize() + maze.getTileSize() / 2f;

            hud.setExitTarget(exitWorldX, exitWorldY);
        }


        Rectangle playerBounds = new Rectangle(player.getX(), player.getY(), PLAYER_WIDTH, PLAYER_HEIGHT);

        // Update room lock
        if (gameState == GameState.RUNNING) {
            ambushRoom.update(playerBounds, maze);

            if (ambushRoom.isLocked()) {
                ambushTimer += delta;
                if (ambushTimer > 5f) {
                    ambushRoom.unlockRoom(maze);
                }
            }
        }

        // Update enemies
        if (gameState == GameState.RUNNING) {
            for (Enemy enemy : enemies) {
                enemy.update(delta, maze);

                if (playerBounds.overlaps(enemy.getCollisionBox()))
                    player.takeDamage(1);

            }
        }


        // Update obstacles
        if (gameState == GameState.RUNNING) {
            for (Obstacle obstacle : obstacles) {
                obstacle.updateCooldown(delta);

                if (obstacle.tryActivate(playerBounds)) {
                    player.takeDamage(obstacle.getDamageToPlayer());
                    obstacle.startCooldown();
                }

                if (obstacle instanceof SpikeTrap spikeTrap) {
                    spikeTrap.update(delta);
                }
            }
        }


        // Update HUD and camera
        hud.update(player);
        hud.act(delta);

        gameCamera.follow(
                player.getX() + PLAYER_WIDTH / 2f,
                player.getY() + PLAYER_HEIGHT / 2f,
                0.1f
        );

        // Render world
        ScreenUtils.clear(0, 0, 0, 1);
        game.getSpriteBatch().setProjectionMatrix(gameCamera.getCamera().combined);
        game.getSpriteBatch().begin();

        mazeRenderer.render(maze, game.getSpriteBatch());

        // Draw obstacles
        for (Obstacle obstacle : obstacles) {
            if (obstacle instanceof SpikeTrap spikeTrap) {
                game.getSpriteBatch().draw(
                        spikeTrap.getCurrentFrame(),
                        spikeTrap.getCollisionBox().x,
                        spikeTrap.getCollisionBox().y,
                        16,
                        16
                );
            }
        }

        // Draw player
        if(player.isDamageFlashing()) {
            game.getSpriteBatch().setColor(Color.RED);
        }

        player.draw(game.getSpriteBatch());
        game.getSpriteBatch().setColor(Color.WHITE);

        // Draw enemies
        for (Enemy enemy : enemies) {
            TextureRegion frame = null;

            if (enemy instanceof MutantWorker mw) frame = mw.getCurrentFrame();
            else if (enemy instanceof RobotGuard rg) frame = rg.getCurrentFrame();
            else if (enemy instanceof FlyingDrone fd) frame = fd.getCurrentFrame();

            if (frame != null) {
                game.getSpriteBatch().draw(
                        frame,
                        enemy.getPosition().x,
                        enemy.getPosition().y,
                        32,
                        64
                );
            }
        }

        game.getSpriteBatch().end();

        hud.getStage().getViewport().apply();

        hud.draw();

        if (gameState == GameState.WON || gameState == GameState.LOST) {
            game.goToMenu();
        }
    }

    // Show
    @Override
    public void show() {
        MazeLoader.LevelSpec spec = new MazeLoader.LevelSpec(101, 101, Map.of());
        long seed = System.currentTimeMillis();
        maze = new MazeGenerator().generate(spec, TILE_SIZE, seed);

        // Setup camera
        gameCamera = new GameCamera(
                viewport.getWorldWidth(),
                viewport.getWorldHeight(),
                maze.getWidth() * TILE_SIZE,
                maze.getHeight() * TILE_SIZE
        );
        gameCamera.setZoom(0.75f);

        // Setup renderer
        mazeRenderer = new MazeRenderer(
                game.getTile(1, 0),
                game.getTile(0, 0),
                game.getTile(3, 2),
                game.getTile(2, 3)
        );

        // Spawn player
        Point entry = maze.getEntryCell();
        if(entry == null) entry = new Point(1, 1);

        Point safeSpawnCell = findSafeSpawnPosition(entry, maze);

        float spawnX = safeSpawnCell.x * TILE_SIZE;
        float spawnY = safeSpawnCell.y * TILE_SIZE;

        player = new Player(spawnX, spawnY, game.getCharacterDownAnimation());


        // Spawn obstacles and enemies
        obstacles = new ObstaclePlacer().placeObstacles(maze, 40, seed);
        enemies = new EnemySpawner().spawnEnemies(maze, player, 20, seed);

        // Setup room lock
        Rectangle trigger = new Rectangle(50 * TILE_SIZE, 50 * TILE_SIZE, 5 * TILE_SIZE, 5 * TILE_SIZE);

        List<Point> doors = new ArrayList<>();
        doors.add(new Point(50, 49));
        doors.add(new Point(55, 55));

        ambushRoom = new RoomLock(trigger, doors);

        // Setup HUD
        Viewport hudViewport = new ScreenViewport();
        hud = new Hud(hudViewport, game.getSpriteBatch(), game.getSkin());

        Gdx.input.setInputProcessor(hud.getStage());
    }


    // Resize
    @Override
    public void resize(int width, int height) {
        viewport.update(width, height, true);
        hud.resize(width, height, true);
        gameCamera.resize(viewport.getWorldWidth(), viewport.getWorldHeight());
    }


    // Find a spawn point, so fat player is not stuck
    private Point findSafeSpawnPosition(Point entryCell, Maze maze) {
        int maxRadius = 10; // search radius in tiles

        for (int r = 0; r <= maxRadius; r++) {
            for (int dy = -r; dy <= r; dy++) {
                for (int dx = -r; dx <= r; dx++) {

                    int cx = entryCell.x + dx;
                    int cy = entryCell.y + dy;

                    if (!GridUtils.inBounds(cx, cy, maze.getWidth(), maze.getHeight())) {
                        continue;
                    }

                    if (!Tiles.isWalkable(maze.getTile(cx, cy))) {
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


    private boolean isPlayerOnExit(Player player, Maze maze) {
        int tileX = (int) ((player.getX() + 16) / maze.getTileSize());
        int tileY = (int) ((player.getY() + 32) / maze.getTileSize());

        return maze.getTile(tileX, tileY) == Tiles.Exit;
    }


    private enum GameState {
        RUNNING,
        WON,
        LOST
    }


    // Find the nearest exit
    private Point findNearestExit(Player player, Maze maze) {
        Point nearest = null;
        float bestDistSq = Float.MAX_VALUE;

        float px = player.getX() + PLAYER_WIDTH / 2f;
        float py = player.getY() + PLAYER_HEIGHT / 2f;

        for (Point exit : maze.getExitCells()) {
            float ex = exit.x * maze.getTileSize() + maze.getTileSize() / 2f;
            float ey = exit.y * maze.getTileSize() + maze.getTileSize() / 2f;

            float dx = ex - px;
            float dy = ey - py;
            float distSq = dx * dx + dy * dy;

            if (distSq < bestDistSq) {
                bestDistSq = distSq;
                nearest = exit;
            }
        }

        return nearest;
    }

    @Override public void pause() {}
    @Override public void resume() {}
    @Override public void hide() {}

    @Override
    public void dispose() {
        hud.dispose();
    }
}
