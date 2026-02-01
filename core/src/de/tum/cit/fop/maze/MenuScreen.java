package de.tum.cit.fop.maze;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.utils.viewport.FitViewport;
import com.badlogic.gdx.utils.viewport.Viewport;
import de.tum.cit.fop.maze.ui.LeaderboardScreen;

/**
 * Main menu screen of the game.
 *
 * This screen is shown when the game starts and allows the player
 * to navigate to different parts of the game such as starting a new
 * game, loading a save file, opening settings, viewing achievements,
 * leaderboards, or exiting the game.
 *
 * All navigation to other screens is handled from here.
 */
public class MenuScreen implements Screen {

    // Core game reference
    private final Stage stage;
    private final MazeRunnerGame game;
    private final Viewport viewport;

    // Visuals & audio
    private Texture background;
    private com.badlogic.gdx.audio.Sound clickSound;

    /**
     * Creates the main menu screen and initializes all UI elements.
     *
     * @param game Reference to the main game instance
     */
    public MenuScreen(MazeRunnerGame game) {
        this.game = game;

        this.viewport = new FitViewport(1280, 720);
        this.stage = new Stage(viewport, game.getSpriteBatch());

        background = new Texture(Gdx.files.internal("menu-background.png"));

        Table table = new Table();
        table.setFillParent(true);
        stage.addActor(table);

        Label title = new Label("THE MAZE RUNNER", game.getSkin(), "title");
        title.setFontScale(1.2f);
        table.add(title).padBottom(80).padTop(-40).row();

        TextButton goToGameButton = new TextButton("New Game", game.getSkin());
        goToGameButton.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                playClickSound();
                if (game.getAchievementManager() != null) {
                    game.getAchievementManager().resetAll();
                }
                game.setScreen(new ModeSelectScreen(game));
            }
        });

        TextButton loadButton = new TextButton("Load Game", game.getSkin());
        loadButton.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                playClickSound();
                if (game.loadGame()) {
                    System.out.println("Save loaded successfully!");
                } else {
                    System.out.println("No save file found.");
                }
            }
        });

        TextButton gamblingBtn = new TextButton("The Casino", game.getSkin());
        gamblingBtn.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                playClickSound();
                game.goToGambling();
            }
        });


        TextButton leaderboardBtn = new TextButton("Leaderboard", game.getSkin());
        leaderboardBtn.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                playClickSound();
                game.setScreen(new LeaderboardScreen(game));
            }
        });

        TextButton settingsButton = new TextButton("Settings", game.getSkin());
        settingsButton.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                playClickSound();
                game.setScreen(new SettingsScreen(game));
            }
        });

        TextButton exitButton = new TextButton("Exit", game.getSkin());
        exitButton.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                playClickSound();
                Gdx.app.exit();
            }
        });

        Table grid = new Table();
        grid.defaults().width(300).height(60).pad(8);

        grid.add(goToGameButton);
        grid.add(loadButton);
        grid.add(gamblingBtn);
        grid.row();

        grid.add(leaderboardBtn);
        grid.add(settingsButton);
        grid.row();

        grid.add();
        grid.add(exitButton);
        grid.add();

        table.add(grid);
    }

    private void playClickSound() {
        if (clickSound != null) {
            long id = clickSound.play();
            clickSound.setVolume(id, game.getPreferences().getSoundVolume());
        }
    }

    /**
     * Renders the menu screen.
     *
     * Draws the background image and all UI elements and updates
     * the stage each frame.
     *
     * @param delta Time elapsed since the last frame
     */
    @Override
    public void render(float delta) {
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        if (background != null) {
            game.getSpriteBatch().setProjectionMatrix(stage.getCamera().combined);
            game.getSpriteBatch().begin();
            game.getSpriteBatch().draw(background, 0, 0, stage.getWidth(), stage.getHeight());
            game.getSpriteBatch().end();
        }

        stage.act(Math.min(Gdx.graphics.getDeltaTime(), 1 / 30f));
        stage.draw();
    }

    @Override public void resize(int width, int height) { stage.getViewport().update(width, height, true); }

    /**
     * Disposes all resources used by the menu screen.
     */
    @Override
    public void dispose() {
        stage.dispose();
        if (background != null) background.dispose();
        if (clickSound != null) clickSound.dispose();
    }

    /**
     * Called when this screen becomes active.
     * Sets the input processor and loads the click sound.
     */
    @Override
    public void show() {
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
