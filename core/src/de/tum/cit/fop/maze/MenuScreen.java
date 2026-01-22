package de.tum.cit.fop.maze;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.utils.viewport.ScreenViewport;
import com.badlogic.gdx.utils.viewport.Viewport;

/**
 * The MenuScreen class is responsible for displaying the main menu of the game.
 * It extends the LibGDX Screen class and sets up the UI components for the menu.
 */
public class MenuScreen implements Screen {

    private final Stage stage;
    private final MazeRunnerGame game;
    private Texture background;
    private com.badlogic.gdx.audio.Sound clickSound;

    /**
     * Constructor for MenuScreen. Sets up the camera, viewport, stage, and UI elements.
     *
     * @param game The main game class, used to access global resources and methods.
     */
    public MenuScreen(MazeRunnerGame game) {
        this.game = game;

        var camera = new OrthographicCamera();
        camera.zoom = 1.0f; // Set camera zoom for a closer view

        Viewport viewport = new ScreenViewport(camera); // Create a viewport with the camera
        stage = new Stage(viewport, game.getSpriteBatch()); // Create a stage for UI elements

        background = new Texture(Gdx.files.internal("menu-background.png"));

        Table table = new Table(); // Create a table for layout
        table.setFillParent(true); // Make the table fill the stage
        stage.addActor(table); // Add the table to the stage

        // Add a label as a title
        Label title = new Label("THE MAZE RUNNER", game.getSkin(), "title");
        title.setFontScale(1.2f);
        table.add(title).padBottom(60).row();

        // --- 1. NEW GAME BUTTON ---
        TextButton goToGameButton = new TextButton("New Game", game.getSkin());
        table.add(goToGameButton).width(300).padBottom(10).row();
        goToGameButton.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                playClickSound();
                game.setScreen(new LevelMenuScreen(game)); // Open the level selection menu
            }
        });

        // --- 2. LOAD GAME BUTTON (THIS IS NEW) ---
        TextButton loadButton = new TextButton("Load Game", game.getSkin());
        table.add(loadButton).width(300).padBottom(10).row();
        loadButton.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                playClickSound();
                // Calls the load method in MazeRunnerGame
                if (game.loadGame()) {
                    System.out.println("Save loaded successfully!");
                } else {
                    System.out.println("No save file found.");
                }
            }
        });

        // --- 3. ACHIEVEMENTS BUTTON ---
        TextButton achievementBtn = new TextButton("Achievements", game.getSkin());
        achievementBtn.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                game.setScreen(new de.tum.cit.fop.maze.ui.AchievementScreen(game));
            }
        });
        table.add(achievementBtn).width(300).padBottom(10).row();

        // --- 4. SETTINGS BUTTON ---
        TextButton settingsButton = new TextButton("Settings", game.getSkin());
        table.add(settingsButton).width(300).padBottom(10).row();
        settingsButton.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                playClickSound();
                game.setScreen(new SettingsScreen(game));
            }
        });

        // --- 5. EXIT BUTTON ---
        TextButton exitButton = new TextButton("Exit", game.getSkin());
        table.add(exitButton).width(300).row();
        exitButton.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                playClickSound();
                Gdx.app.exit();
            }
        });
    }

    private void playClickSound() {
        if (clickSound != null) {
            long id = clickSound.play();
            clickSound.setVolume(id, game.getPreferences().getSoundVolume());
        }
    }

    @Override
    public void render(float delta) {
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT); // Clear the screen

        if (background != null) {
            game.getSpriteBatch().setProjectionMatrix(stage.getCamera().combined);

            game.getSpriteBatch().begin();

            game.getSpriteBatch().draw(background, 0, 0, stage.getWidth(), stage.getHeight());
            game.getSpriteBatch().end();
        }

        stage.act(Math.min(Gdx.graphics.getDeltaTime(), 1 / 30f));
        stage.draw();
    }

    @Override
    public void resize(int width, int height) {
        stage.getViewport().update(width, height, true); // Update the stage viewport on resize
    }

    @Override
    public void dispose() {
        // Dispose of the stage when screen is disposed
        stage.dispose();
    }

    @Override
    public void show() {
        // Set the input processor so the stage can receive input events
        Gdx.input.setInputProcessor(stage);
        if (Gdx.files.internal("audio/click.mp3").exists()) {
            clickSound = Gdx.audio.newSound(Gdx.files.internal("audio/click.mp3"));
        } else if (Gdx.files.internal("audio/pickup.mp3").exists()) {
            clickSound = Gdx.audio.newSound(Gdx.files.internal("audio/pickup.mp3"));
        }
    }

    @Override public void pause() {}
    @Override public void resume() {}
    @Override public void hide() {}
}