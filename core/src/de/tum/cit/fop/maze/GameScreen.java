package de.tum.cit.fop.maze;


import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.ScreenUtils;
import com.badlogic.gdx.utils.viewport.ScreenViewport;
import com.badlogic.gdx.utils.viewport.Viewport;

import de.tum.cit.fop.maze.enemies.MutantWorker;
import de.tum.cit.fop.maze.hud.Hud;

import de.tum.cit.fop.maze.camera.GameCamera;
import de.tum.cit.fop.maze.level.Maze;
import de.tum.cit.fop.maze.level.MazeRenderer;
import de.tum.cit.fop.maze.player.Player;
import de.tum.cit.fop.maze.level.Tiles;
import de.tum.cit.fop.maze.level.MazeLoader;
import de.tum.cit.fop.maze.level.MazeGenerator;

import java.awt.*;
import java.util.HashMap;
import java.util.Map;


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
    private MutantWorker testEnemy;
    private Hud hud;


    /**
     * Constructor for GameScreen. Sets up the camera and font.
     *
     * @param game The main game class, used to access global resources and methods.
     */
    public GameScreen(MazeRunnerGame game) {
        this.game = game;
    }

    // Screen interface methods with necessary functionality
    @Override
    public void render(float delta) {
        // Check for escape key press to go back to the menu
        if (Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE)) {
            game.goToMenu();
        }

        player.update(delta, maze);
        testEnemy.update(delta);
        hud.update(player);
        hud.act(delta);

        // Check collision (Player vs Enemy)
        Rectangle playerBox = new Rectangle(player.getX(), player.getY(), 32, 64);
        Rectangle enemyBox = testEnemy.getCollisionBox();

        if (playerBox.overlaps(enemyBox)) {
            player.takeDamage(1);
        }

        // Render
        float targetX = player.getX() + 16;
        float targetY = player.getY() + 32;
        gameCamera.follow(targetX, targetY, 0.1f);

        ScreenUtils.clear(0,0,0,1);
        game.getSpriteBatch().setProjectionMatrix(gameCamera.getCamera().combined);
        game.getSpriteBatch().begin();

        mazeRenderer.render(maze, game.getSpriteBatch());


        // Draw the player
        player.draw(game.getSpriteBatch());

        // Test enemy animation using the skeleton mob
        game.getSpriteBatch().draw(
                testEnemy.getCurrentFrame(),
                testEnemy.getPosition().x,
                testEnemy.getPosition().y,
                32,
                64
        );

        game.getSpriteBatch().end();
        hud.draw();
    }

    @Override
    public void show() {
        viewport = new ScreenViewport();
        viewport.apply();

        Map<Point, Integer> overrides = new HashMap<>();
        MazeLoader.LevelSpec spec = new MazeLoader.LevelSpec(20, 15, overrides);


        MazeGenerator generator = new MazeGenerator();
        int tileSize = 32;
        long seed = System.currentTimeMillis();
        maze = generator.generate(spec, tileSize, seed);

        float worldWidth = maze.getWidth() * maze.getTileSize();
        float worldHeight = maze.getHeight() * maze.getTileSize();

        gameCamera  = new GameCamera(viewport.getWorldWidth(), viewport.getWorldHeight(), worldWidth, worldHeight);
        gameCamera.setZoom(0.75f);
        mazeRenderer = new MazeRenderer(
                game.getTile(0, 0),
                game.getTile(0, 1),
                game.getTile(1, 0),
                game.getTile(1, 1)
        );

        font = game.getSkin().getFont("font");

        Point entry = maze.getEntryCell();
        if(entry == null){
            entry = new Point(1, 1);
        }

        // Player
        player = new Player(
                200,
                200,
                game.getCharacterDownAnimation()
        );

        // TestEnemy
        testEnemy = new MutantWorker(
                new Vector2(400, 300),
                100f,
                player
        );

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

    @Override
    public void pause() {
    }

    @Override
    public void resume() {
    }

    @Override
    public void hide() {
    }

    @Override
    public void dispose() {
        hud.dispose();
    }
}

