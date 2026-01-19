package de.tum.cit.fop.maze;


import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.InputListener;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Slider;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.utils.viewport.ScreenViewport;
import com.badlogic.gdx.Graphics;

public class SettingsScreen implements Screen {

    private final MazeRunnerGame game;
    private final Stage stage;
    private com.badlogic.gdx.audio.Sound clickSound;

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
                game.getAudioManager().updateMusicVolume(volumeSlider.getValue());
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

        table.add(new Label("Display", game.getSkin())).padBottom(10).row();

        String currentMode = Gdx.graphics.isFullscreen() ? "Fullscreen: ON" : "Fullscreen: OFF";
        final TextButton fullscreenBtn = new TextButton(currentMode, game.getSkin());
        
        fullscreenBtn.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                playClickSound();
                toggleFullscreen(fullscreenBtn);
            }
        });
        table.add(fullscreenBtn).width(300).padBottom(30).row();


        table.add(new Label("Controls", game.getSkin())).padBottom(10).padTop(20).row();

        table.add(createKeyBindButton("Up", game.getPreferences().getKeyUp(),
                key -> game.getPreferences().setKeyUp(key))).width(300).padBottom(5).row();

        table.add(createKeyBindButton("Left", game.getPreferences().getKeyLeft(),
                key -> game.getPreferences().setKeyLeft(key))).width(300).padBottom(5).row();

        table.add(createKeyBindButton("Down", game.getPreferences().getKeyDown(),
                key -> game.getPreferences().setKeyDown(key))).width(300).padBottom(5).row();

        table.add(createKeyBindButton("Right", game.getPreferences().getKeyRight(),
                key -> game.getPreferences().setKeyRight(key))).width(300).padBottom(20).row();



        //BackToMenu Button
        TextButton backButton = new TextButton("Back to Menu", game.getSkin());
        backButton.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                playClickSound();
                game.goToMenu();
            }
        });
        table.add(backButton).width(300).padTop(20);
    }
    private TextButton createKeyBindButton(String actionName, int currentKey, java.util.function.Consumer<Integer> onSave) {
        String keyName = Input.Keys.toString(currentKey);
        final TextButton button = new TextButton(actionName + ": " + keyName, game.getSkin());

        button.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                button.setText(actionName + ": Press any key...");

                stage.addListener(new InputListener() {
                    @Override
                    public boolean keyDown(InputEvent event, int keycode) {
                        onSave.accept(keycode);

                        button.setText(actionName + ": " + Input.Keys.toString(keycode));

                        stage.removeListener(this);
                        return true;
                    }
                });
            }
        });
        return button;
    }

    private void toggleFullscreen(TextButton button) {
        if (Gdx.graphics.isFullscreen()) {
            Gdx.graphics.setWindowedMode(1280, 720);
            button.setText("Fullscreen: OFF");
        } else {
            Graphics.DisplayMode currentMode = Gdx.graphics.getDisplayMode();
            Gdx.graphics.setFullscreenMode(currentMode);
            button.setText("Fullscreen: ON");
        }
    }

    private void playClickSound() {
        if (clickSound != null) {
            long id = clickSound.play();
            clickSound.setVolume(id, game.getPreferences().getSoundVolume());
        }
    }

    @Override
    public void render(float delta) {
        if (Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE)) {
            playClickSound();
            game.goToMenu();
            return;
        }
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

        if (Gdx.files.internal("audio/click.mp3").exists()) {
            clickSound = Gdx.audio.newSound(Gdx.files.internal("audio/click.mp3"));
        } else if (Gdx.files.internal("audio/pickup.mp3").exists()) {
            clickSound = Gdx.audio.newSound(Gdx.files.internal("audio/pickup.mp3"));
        }
    }

    @Override
    public void dispose() {
        stage.dispose();
        if (clickSound != null) clickSound.dispose();
    }

    @Override public void pause() {}
    @Override public void resume() {}
    @Override public void hide() {}
}