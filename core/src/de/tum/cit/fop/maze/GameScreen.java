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
import de.tum.cit.fop.maze.player.Player;

/**
 * The GameScreen class is responsible for rendering the gameplay screen.
 * It handles the game logic and rendering of the game elements.
 */
public class GameScreen implements Screen {

    private final MazeRunnerGame game;
    private OrthographicCamera camera;
    private BitmapFont font;
    private Viewport viewport;

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

        player.update(delta);
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
        ScreenUtils.clear(0, 0, 0, 1); // Clear the screen
        camera.update(); // Update the camera

        game.getSpriteBatch().setProjectionMatrix(camera.combined);
        game.getSpriteBatch().begin();


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

        // Text
        font.draw(game.getSpriteBatch(),
                "Press ESC to go to menu",
                camera.position.x - 100,
                camera.position.y + 120);

        game.getSpriteBatch().end();

        hud.draw();
    }

    @Override
    public void show() {
        camera = new OrthographicCamera();
        viewport = new ScreenViewport(camera);
        camera.zoom = 0.75f;
        font = game.getSkin().getFont("font");

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

