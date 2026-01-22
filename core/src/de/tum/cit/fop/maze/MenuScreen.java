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
import com.badlogic.gdx.utils.viewport.FitViewport; // Use FitViewport
import com.badlogic.gdx.utils.viewport.Viewport;

import de.tum.cit.fop.maze.ui.LeaderboardScreen;
import de.tum.cit.fop.maze.ui.AchievementScreen; // Assuming this is in UI package

public class MenuScreen implements Screen {

    private final Stage stage;
    private final MazeRunnerGame game;
    private Texture background;
    private com.badlogic.gdx.audio.Sound clickSound;
    private Viewport viewport;

    public MenuScreen(MazeRunnerGame game) {
        this.game = game;

        // --- FIX: Initialize the Class Viewport directly ---
        // Use FitViewport (1280x720) so buttons stay the correct size on all screens
        this.viewport = new FitViewport(1280, 720);

        // --- FIX: Pass 'this.viewport' to the Stage ---
        this.stage = new Stage(viewport, game.getSpriteBatch());

        // Background
        background = new Texture(Gdx.files.internal("menu-background.png"));

        // Table Layout
        Table table = new Table();
        table.setFillParent(true);
        stage.addActor(table);

        // Title
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
                game.setScreen(new LevelMenuScreen(game));
            }
        });

        // --- 2. LOAD GAME BUTTON ---
        TextButton loadButton = new TextButton("Load Game", game.getSkin());
        table.add(loadButton).width(300).padBottom(10).row();
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

        // --- 3. ACHIEVEMENTS BUTTON ---
        TextButton achievementBtn = new TextButton("Achievements", game.getSkin());
        achievementBtn.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                playClickSound();
                // Ensure AchievementScreen exists in your project
                game.setScreen(new AchievementScreen(game));
            }
        });
        table.add(achievementBtn).width(300).padBottom(10).row();

        // --- 4. LEADERBOARD BUTTON ---
        TextButton leaderboardBtn = new TextButton("Leaderboard", game.getSkin());
        leaderboardBtn.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                playClickSound();
                game.setScreen(new LeaderboardScreen(game));
            }
        });
        table.add(leaderboardBtn).width(300).padBottom(20).row();

        // --- 5. SETTINGS BUTTON ---
        TextButton settingsButton = new TextButton("Settings", game.getSkin());
        table.add(settingsButton).width(300).padBottom(10).row();
        settingsButton.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                playClickSound();
                game.setScreen(new SettingsScreen(game));
            }
        });

        // --- 6. EXIT BUTTON ---
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
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        if (background != null) {
            game.getSpriteBatch().setProjectionMatrix(stage.getCamera().combined);
            game.getSpriteBatch().begin();
            // Draw background to fill the virtual width/height
            game.getSpriteBatch().draw(background, 0, 0, stage.getWidth(), stage.getHeight());
            game.getSpriteBatch().end();
        }

        stage.act(Math.min(Gdx.graphics.getDeltaTime(), 1 / 30f));
        stage.draw();
    }

    @Override
    public void resize(int width, int height) {
        // This ensures the HUD scales correctly and stays centered
        stage.getViewport().update(width, height, true);
    }

    @Override
    public void dispose() {
        stage.dispose();
    }

    @Override
    public void show() {
        Gdx.input.setInputProcessor(stage);

        // Load sounds safely
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