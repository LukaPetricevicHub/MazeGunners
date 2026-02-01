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
import de.tum.cit.fop.maze.ui.UiButtons;

/**
 * Main menu screen with navigation to game modes and settings.
 */
public class MenuScreen implements Screen {

    private final Stage stage;
    private final MazeRunnerGame game;
    private Texture background;
    private com.badlogic.gdx.audio.Sound clickSound;
    private final Viewport viewport;

    public MenuScreen(MazeRunnerGame game) {
        this.game = game;

        this.viewport = new FitViewport(1280, 720);
        this.stage = new Stage(viewport, game.getSpriteBatch());

        background = new Texture(Gdx.files.internal("menu-background.png"));

        Table table = new Table();
        table.setFillParent(true);
        stage.addActor(table);

        Label title = new Label("ESCAPE THE FACILITY", game.getSkin(), "title");
        title.setFontScale(1.2f);
        table.add(title).padBottom(80).padTop(-40).row();

        TextButton goToGameButton = UiButtons.button("New Game", game.getSkin());
        goToGameButton.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                if (game.getAchievementManager() != null) {
                    game.getAchievementManager().resetAll();
                }
                game.setScreen(new ModeSelectScreen(game));
            }
        });

        TextButton loadButton = UiButtons.button("Load Game", game.getSkin());
        loadButton.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                if (game.loadGame()) {
                    System.out.println("Save loaded successfully!");
                } else {
                    System.out.println("No save file found.");
                }
            }
        });

        
        TextButton gamblingBtn = UiButtons.button("The Casino", game.getSkin());
        gamblingBtn.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                game.goToGambling(); 
            }
        });


        TextButton leaderboardBtn = UiButtons.button("Leaderboard", game.getSkin());
        leaderboardBtn.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                game.setScreen(new LeaderboardScreen(game));
            }
        });

        TextButton settingsButton = UiButtons.button("Settings", game.getSkin());
        settingsButton.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                game.setScreen(new SettingsScreen(game));
            }
        });

        TextButton exitButton = UiButtons.button("Exit", game.getSkin());
        exitButton.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                Gdx.app.exit();
            }
        });

        Table grid = new Table();
        grid.defaults().width(360).height(80).pad(24);

        grid.add(goToGameButton);
        grid.add(loadButton);
        grid.add(gamblingBtn);
        grid.row();

        grid.add(leaderboardBtn);
        grid.add(settingsButton);
        grid.add(exitButton);
        grid.row();

        grid.add();
        grid.add();
        grid.add();

        table.add(grid);
    }


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

    @Override
    public void dispose() {
        stage.dispose();
        if (background != null) background.dispose();
    }

    @Override
    public void show() {
        Gdx.input.setInputProcessor(stage);
    }

    @Override public void pause() {}
    @Override public void resume() {}
    @Override public void hide() {}
}

