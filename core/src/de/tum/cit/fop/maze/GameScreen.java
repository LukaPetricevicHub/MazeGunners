package de.tum.cit.fop.maze;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.ScreenUtils;
import com.badlogic.gdx.utils.viewport.ScreenViewport;
import com.badlogic.gdx.utils.viewport.Viewport;

import de.tum.cit.fop.maze.enemies.Enemy;
import de.tum.cit.fop.maze.enemies.FlyingDrone;
import de.tum.cit.fop.maze.enemies.MutantWorker;
import de.tum.cit.fop.maze.enemies.RobotGuard;
import de.tum.cit.fop.maze.hud.Hud;

import de.tum.cit.fop.maze.camera.GameCamera;
import de.tum.cit.fop.maze.level.Maze;
import de.tum.cit.fop.maze.level.MazeRenderer;
import de.tum.cit.fop.maze.player.Player;
import de.tum.cit.fop.maze.level.MazeLoader;
import de.tum.cit.fop.maze.level.MazeGenerator;

import java.awt.Point;
import java.util.ArrayList;
import java.util.List;

/**
 * The GameScreen class is responsible for rendering the gameplay screen.
 * It handles the game logic and rendering of the game elements.
 */
public class GameScreen implements Screen {

    private final MazeRunnerGame game;
    private GameCamera gameCamera;
    private BitmapFont font;
    private Viewport viewport;
    private Maze maze;
    private MazeRenderer mazeRenderer;

    private Player player;
    private List<Enemy> enemies;
    private Hud hud;

    /**
     * Constructor for GameScreen.
     * Maze loading / generation belongs here.
     */
    public GameScreen(MazeRunnerGame game, String mapPath) {
        this.game = game;

        FileHandle file = Gdx.files.internal(mapPath);

        MazeLoader loader = new MazeLoader();
        MazeLoader.LevelSpec spec = loader.loadFromProperties(file);

        MazeGenerator generator = new MazeGenerator();
        int tileSize = 16;
        long seed = System.currentTimeMillis();
        maze = generator.generate(spec, tileSize, seed);

        float worldWidth = maze.getWidth() * tileSize;
        float worldHeight = maze.getHeight() * tileSize;

        viewport = new ScreenViewport();

        gameCamera = new GameCamera(
                viewport.getWorldWidth(),
                viewport.getWorldHeight(),
                worldWidth,
                worldHeight
        );
        gameCamera.setZoom(0.75f);

        TextureRegion floor = game.getTile(0, 0);
        TextureRegion wall = game.getTile(1, 0);
        TextureRegion entry = game.getTile(3, 2);
        TextureRegion exit = game.getTile(2, 3);

        if (floor == null || wall == null || entry == null || exit == null) {
            throw new IllegalStateException("One or more maze tiles are missing in basictiles.png!");
        }

        mazeRenderer = new MazeRenderer(floor, wall, entry, exit);

        Point enter = maze.getEntryCell();
        if (enter == null) {
            enter = new Point(1, 1);
        }
    }

    @Override
    public void render(float delta) {
        // Escape → Menu
        if (Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE)) {
            game.goToMenu();
        }

        player.update(delta, maze);

        for (Enemy enemy : enemies) {
            enemy.update(delta);
        }

        hud.update(player);
        hud.act(delta);

        // Collision: Player vs Enemies
        Rectangle playerBox = new Rectangle(player.getX(), player.getY(), 32, 64);

        for (Enemy enemy : enemies) {
            if (playerBox.overlaps(enemy.getCollisionBox())) {
                player.takeDamage(1);
            }
        }

        // Camera follow
        float targetX = player.getX() + 16;
        float targetY = player.getY() + 32;
        gameCamera.follow(targetX, targetY, 0.1f);

        ScreenUtils.clear(0, 0, 0, 1);
        game.getSpriteBatch().setProjectionMatrix(gameCamera.getCamera().combined);
        game.getSpriteBatch().begin();

        mazeRenderer.render(maze, game.getSpriteBatch());

        // Draw player
        player.draw(game.getSpriteBatch());

        // Draw enemies
        for (Enemy enemy : enemies) {
            TextureRegion frame = null;
            if (enemy instanceof MutantWorker mw) frame = mw.getCurrentFrame();
            else if (enemy instanceof FlyingDrone fd) frame = fd.getCurrentFrame();
            else if (enemy instanceof RobotGuard rg) frame = rg.getCurrentFrame();

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
        font = game.getSkin().getFont("font");

        // Player
        player = new Player(
                200,
                200,
                game.getCharacterDownAnimation()
        );

        // One instance of each enemy
        enemies = new ArrayList<>();

        enemies.add(new MutantWorker(
                new Vector2(400, 300),
                100f,
                player));
        enemies.add(new FlyingDrone(
                new Vector2(500, 300), 80f,
                new Array<Vector2>(),
                player)); // Add patrol points if needed
        enemies.add(new RobotGuard(
                new Vector2(600, 300),
                70f,
                player));

        // HUD
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
