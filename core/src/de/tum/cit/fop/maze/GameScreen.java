package de.tum.cit.fop.maze;


import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
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
     * @param mapPath
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

        gameCamera = new GameCamera( viewport.getWorldWidth(), viewport.getWorldHeight(), worldWidth, worldHeight);
        gameCamera.setZoom(0.75f);

        gameCamera  = new GameCamera(viewport.getWorldWidth(), viewport.getWorldHeight(), worldWidth, worldHeight);
        gameCamera.setZoom(0.75f);

        TextureRegion floor = game.getTile(0, 0);
        TextureRegion wall = game.getTile(1, 0);
        TextureRegion entry = game.getTile(3,2);
        TextureRegion exit = game.getTile(2, 3);

        if(floor == null || wall == null || entry == null || exit == null){
            throw new IllegalStateException("One or more maze tiles are missing in basictiles.png!");
        }

        mazeRenderer = new MazeRenderer(floor, wall, entry, exit);

        Point enter = maze.getEntryCell();
        if(enter == null){
            enter = new Point(1, 1);
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
        Gdx.input.setInputProcessor(hud.getStage());
        font = game.getSkin().getFont("font");
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

