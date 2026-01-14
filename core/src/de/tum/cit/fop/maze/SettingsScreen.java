package de.tum.cit.fop.maze;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Slider;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.utils.viewport.ScreenViewport;

public class SettingsScreen implements Screen {

    private final MazeRunnerGame game;
    private final Stage stage;

    public SettingsScreen(MazeRunnerGame game) {
        this.game = game;
        this.stage = new Stage(new ScreenViewport(), game.getSpriteBatch());

        Table table = new Table();
        table.setFillParent(true);
        stage.addActor(table);

        //title
        table.add(new Label("Settings", game.getSkin(), "title")).padBottom(40).row();

        //Volume control
        table.add(new Label("Music Volume", game.getSkin())).padBottom(10).row();

        // VolumeSlider
        final Slider volumeSlider = new Slider(0f, 1f, 0.1f, false, game.getSkin());
        volumeSlider.setValue(game.getPreferences().getMusicVolume()); // Load saved value
        table.add(volumeSlider).width(300).padBottom(20).row();

        //Volume Slider Listener
        volumeSlider.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                game.getPreferences().setMusicVolume(volumeSlider.getValue());
                game.getAudioManager().updateVolume(volumeSlider.getValue());
            }
        });

        //Sound Effects Volume Slider
        table.add(new Label("Sound Effects Volume", game.getSkin())).padBottom(10).padTop(20).row();

        final Slider sfxSlider = new Slider(0f, 1f, 0.05f, false, game.getSkin());
        table.add(sfxSlider).width(300).padBottom(20).row();

        sfxSlider.setValue(game.getPreferences().getSoundVolume());

        sfxSlider.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                game.getPreferences().setSoundVolume(sfxSlider.getValue());
            }
        });

        //BackToMenu Button
        TextButton backButton = new TextButton("Back to Menu", game.getSkin());
        backButton.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                game.goToMenu();
            }
        });
        table.add(backButton).width(300).padTop(20);

    }

    @Override
    public void render(float delta) {
        Gdx.gl.glClearColor(0, 0, 0, 1);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        stage.act(Math.min(Gdx.graphics.getDeltaTime(), 1 / 30f));
        stage.draw();
    }

    @Override
    public void resize(int width, int height) {
        stage.getViewport().update(width, height, true);
    }

    @Override
    public void show() {
        Gdx.input.setInputProcessor(stage);
    }

    @Override
    public void dispose() {
        stage.dispose();
    }

    @Override public void pause() {}
    @Override public void resume() {}
    @Override public void hide() {}
}