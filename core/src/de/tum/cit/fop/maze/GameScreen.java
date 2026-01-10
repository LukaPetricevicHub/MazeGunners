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
import de.tum.cit.fop.maze.level.Maze;
import de.tum.cit.fop.maze.level.MazeGenerator;
import de.tum.cit.fop.maze.level.MazeLoader;
import de.tum.cit.fop.maze.level.MazeRenderer;
import de.tum.cit.fop.maze.obstacles.Obstacle;
import de.tum.cit.fop.maze.obstacles.SpikeTrap;
import de.tum.cit.fop.maze.player.Player;
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
    private static final float PLAYER_WIDTH = 16f;
    private static final float PLAYER_HEIGHT = 32f;


    public GameScreen(MazeRunnerGame game, String mapPath) {
        this.game = game;
        this.viewport = new ScreenViewport();
    }


    @Override
    public void render(float delta) {
        if (Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE)) {
            game.goToMenu();
        }

        // Update player
        player.update(delta, maze, game);
        Rectangle playerBounds =
                new Rectangle(player.getX(), player.getY(), PLAYER_WIDTH, PLAYER_HEIGHT);

        // Update room lock
        ambushRoom.update(playerBounds, maze);
        if (ambushRoom.isLocked()) {
            ambushTimer += delta;
            if (ambushTimer > 5f) {
                ambushRoom.unlockRoom(maze);
            }
        }

        // Update enemies
        for (Enemy enemy : enemies) {
            enemy.update(delta, maze);

            if (playerBounds.overlaps(enemy.getCollisionBox())) {
                player.takeDamage(1);
            }
        }

        // Update obstacles
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
        hud.draw();
    }

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
        if (entry == null) entry = new Point(1, 1);

        float spawnX = (entry.x + 0.5f) * TILE_SIZE - PLAYER_WIDTH / 2f;
        float spawnY = (entry.y + 0.5f) * TILE_SIZE - PLAYER_HEIGHT / 2f;

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
        hud = new Hud(viewport, game.getSpriteBatch(), game.getSkin());
        Gdx.input.setInputProcessor(hud.getStage());
    }


    @Override
    public void resize(int width, int height) {
        viewport.update(width, height, true);
        hud.resize(width, height, true);
        gameCamera.resize(viewport.getWorldWidth(), viewport.getWorldHeight());
    }

    @Override public void pause() {}
    @Override public void resume() {}
    @Override public void hide() {}

    @Override
    public void dispose() {
        hud.dispose();
    }
}
