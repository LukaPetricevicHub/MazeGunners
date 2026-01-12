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

public class GameScreen implements Screen {

    private final MazeRunnerGame game;
    private final int level;

    private Viewport viewport;
    private GameCamera gameCamera;

    private Maze maze;
    private MazeRenderer mazeRenderer;

    private Player player;
    private List<Enemy> enemies = new ArrayList<>();
    private List<Obstacle> obstacles = new ArrayList<>();

    private RoomLock ambushRoom;
    private float ambushTimer = 0f;

    private Hud hud;

    private static final int TILE_SIZE = 16;
    private static final float PLAYER_WIDTH = 32f;
    private static final float PLAYER_HEIGHT = 64f;

    private GameState gameState = GameState.RUNNING;

    public GameScreen(MazeRunnerGame game, int level) {
        this.game = game;
        this.level = level;
        this.viewport = new ScreenViewport();
    }

    @Override
    public void show() {
        // Decide dimensions for this level (LOGICAL grid = before CELL scaling)
        int logicalW = logicalWForLevel(level);
        int logicalH = logicalHForLevel(level);

        int cell = 4;
        long seed = System.currentTimeMillis();

        // Generate in-memory (Option A)
        MazeGenerator generator = new MazeGenerator(cell);
        maze = generator.generate(logicalW, logicalH, TILE_SIZE, seed);

        // OPTIONAL (later): write properties for assignment/debug
        // Gdx.files.local("maps").mkdirs();
        // MazeExporter.saveToProperties(maze, Gdx.files.local("maps/level-" + level + ".properties"));

        // Camera should use WORLD size (pixels)
        gameCamera = new GameCamera(
                viewport.getWorldWidth(),
                viewport.getWorldHeight(),
                maze.worldWidth(),
                maze.worldHeight()
        );
        gameCamera.setZoom(0.75f);

        // Renderer tiles (adapt indices to your tileset)
        mazeRenderer = new MazeRenderer(
                game.getTile(9, 2), // floor
                game.getTile(1, 7), // wall
                game.getTile(0, 2), // entry
                game.getTile(0, 3)  // exit
        );

        // Spawn player at safe spot near entry
        Point entry = maze.getEntryCell();
        if (entry == null) entry = new Point(1, 1);

        Point safe = findSafeSpawnPosition(entry, maze);

        player = new Player(
                safe.x * TILE_SIZE,
                safe.y * TILE_SIZE,
                game.getCharacterDownAnimation()
        );

        // Spawn obstacles and enemies
        obstacles = new ObstaclePlacer().placeObstacles(maze, 40, seed);
        enemies = new EnemySpawner().spawnEnemies(maze, player, 20, seed);

        // Room lock: choose a region that is actually on floor
        setupAmbushRoomNearCenter(seed);

        // HUD
        Viewport hudViewport = new ScreenViewport();
        hud = new Hud(hudViewport, game.getSpriteBatch(), game.getSkin());
        Gdx.input.setInputProcessor(hud.getStage());
    }

    private void setupAmbushRoomNearCenter(long seed) {
        if (maze == null) return;

        int cx = maze.getWidth() / 2;
        int cy = maze.getHeight() / 2;

        // Move to nearest walkable tile near center
        Point center = findNearestWalkable(new Point(cx, cy), 25);
        if (center == null) center = new Point(cx, cy);

        Rectangle trigger = new Rectangle(
                center.x * TILE_SIZE,
                center.y * TILE_SIZE,
                6 * TILE_SIZE,
                6 * TILE_SIZE
        );

        List<Point> doors = new ArrayList<>();
        doors.add(new Point(center.x, center.y - 1));
        doors.add(new Point(center.x + 6, center.y + 6));

        ambushRoom = new RoomLock(trigger, doors);
    }

    private Point findNearestWalkable(Point start, int maxR) {
        for (int r = 0; r <= maxR; r++) {
            for (int dy = -r; dy <= r; dy++) {
                for (int dx = -r; dx <= r; dx++) {
                    int x = start.x + dx;
                    int y = start.y + dy;
                    if (!maze.inBounds(x, y)) continue;
                    if (maze.getTile(x, y).isWalkable()) return new Point(x, y);
                }
            }
        }
        return null;
    }

    @Override
    public void render(float delta) {
        if (Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE)) {
            game.goToMenu();
            return;
        }
        if (maze == null || player == null) return;

        if (gameState == GameState.RUNNING) {
            player.update(delta, maze, game);

            if (isPlayerOnExit(player, maze) && player.hasKey()) {
                gameState = GameState.WON;
            }
            if (player.getLives() <= 0) {
                gameState = GameState.LOST;
            }
        }

        Point nearestExit = findNearestExit(player, maze);
        if (nearestExit != null && hud != null) {
            float exitWorldX = nearestExit.x * maze.getTileSize() + maze.getTileSize() / 2f;
            float exitWorldY = nearestExit.y * maze.getTileSize() + maze.getTileSize() / 2f;
            hud.setExitTarget(exitWorldX, exitWorldY);
        }

        Rectangle playerBounds = new Rectangle(player.getX(), player.getY(), PLAYER_WIDTH, PLAYER_HEIGHT);

        if (gameState == GameState.RUNNING && ambushRoom != null) {
            ambushRoom.update(playerBounds, maze);
            if (ambushRoom.isLocked()) {
                ambushTimer += delta;
                if (ambushTimer > 5f) ambushRoom.unlockRoom(maze);
            }
        }

        if (gameState == GameState.RUNNING) {
            for (Enemy enemy : enemies) {
                enemy.update(delta, maze);
                if (playerBounds.overlaps(enemy.getCollisionBox())) player.takeDamage(1);
            }
            for (Obstacle obstacle : obstacles) {
                obstacle.updateCooldown(delta);
                if (obstacle.tryActivate(playerBounds)) {
                    player.takeDamage(obstacle.getDamageToPlayer());
                    obstacle.startCooldown();
                }
                if (obstacle instanceof SpikeTrap spikeTrap) spikeTrap.update(delta);
            }
        }

        if (hud != null) {
            hud.update(player);
            hud.act(delta);
        }

        if (gameCamera != null) {
            gameCamera.follow(
                    player.getX() + PLAYER_WIDTH / 2f,
                    player.getY() + PLAYER_HEIGHT / 2f,
                    0.1f
            );
        }

        ScreenUtils.clear(0, 0, 0, 1);

        game.getSpriteBatch().setProjectionMatrix(gameCamera.getCamera().combined);
        game.getSpriteBatch().begin();

        mazeRenderer.render(maze, game.getSpriteBatch());

        for (Obstacle obstacle : obstacles) {
            if (obstacle instanceof SpikeTrap spikeTrap) {
                game.getSpriteBatch().draw(
                        spikeTrap.getCurrentFrame(),
                        spikeTrap.getCollisionBox().x,
                        spikeTrap.getCollisionBox().y,
                        16, 16
                );
            }
        }

        if (player.isDamageFlashing()) game.getSpriteBatch().setColor(Color.RED);
        player.draw(game.getSpriteBatch());
        game.getSpriteBatch().setColor(Color.WHITE);

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

        if (hud != null) {
            hud.getStage().getViewport().apply();
            hud.draw();
        }

        if (gameState == GameState.WON || gameState == GameState.LOST) {
            game.goToMenu();
        }
    }

    @Override
    public void resize(int width, int height) {
        viewport.update(width, height, true);
        if (hud != null) hud.resize(width, height, true);
        if (gameCamera != null) gameCamera.resize(viewport.getWorldWidth(), viewport.getWorldHeight());
    }

    private Point findSafeSpawnPosition(Point entryCell, Maze maze) {
        int maxRadius = 20;

        for (int r = 0; r <= maxRadius; r++) {
            for (int dy = -r; dy <= r; dy++) {
                for (int dx = -r; dx <= r; dx++) {
                    int cx = entryCell.x + dx;
                    int cy = entryCell.y + dy;

                    if (!GridUtils.inBounds(cx, cy, maze.getWidth(), maze.getHeight())) continue;
                    if (!maze.getTile(cx, cy).isWalkable()) continue;

                    float worldX = cx * maze.getTileSize();
                    float worldY = cy * maze.getTileSize();

                    if (canPlayerOccupy(worldX, worldY, maze)) return new Point(cx, cy);
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
        return maze.getTile(tileX, tileY) == Tiles.EXIT;
    }

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

    private int logicalWForLevel(int level) {
        return switch (level) {
            case 1 -> 70;
            case 2 -> 85;
            case 3 -> 100;
            case 4 -> 110;
            case 5 -> 120;
            default -> 100;
        };
    }

    private int logicalHForLevel(int level) {
        return switch (level) {
            case 1 -> 52;
            case 2 -> 64;
            case 3 -> 75;
            case 4 -> 82;
            case 5 -> 90;
            default -> 75;
        };
    }

    private enum GameState { RUNNING, WON, LOST }

    @Override public void pause() {}
    @Override public void resume() {}
    @Override public void hide() {}

    @Override
    public void dispose() {
        if (hud != null) hud.dispose();
    }
}

