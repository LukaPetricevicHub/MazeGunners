package de.tum.cit.fop.maze;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Graphics;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.InputListener;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.*;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.utils.viewport.ExtendViewport;

/**
 * Screen for configuring game settings such as audio, display,
 * and key bindings.
 *
 * Provides UI controls for volume adjustment, fullscreen mode,
 * and custom key mappings.
 */

public class SettingsScreen implements Screen {

    private final MazeRunnerGame game;
    private final Stage stage;
    private com.badlogic.gdx.audio.Sound clickSound;

    private static final float VIRTUAL_WIDTH = 1280;
    private static final float VIRTUAL_HEIGHT = 720;

    /**
     * Creates the settings screen and initializes all UI elements.
     *
     * @param game Reference to the main game instance
     */
    public SettingsScreen(MazeRunnerGame game) {
        this.game = game;
        this.stage = new Stage(new ExtendViewport(VIRTUAL_WIDTH, VIRTUAL_HEIGHT), game.getSpriteBatch());

        Table mainTable = new Table();
        mainTable.setFillParent(true);
        stage.addActor(mainTable);

        Table contentTable = new Table();
        contentTable.top();
        contentTable.pad(50);

        Value elementWidth = Value.percentWidth(0.5f, mainTable);

        Label title = new Label("Settings", game.getSkin());
        title.setFontScale(1.5f);
        contentTable.add(title).padBottom(40).row();

        // Audio section
        contentTable.add(new Label("Audio", game.getSkin())).padBottom(15).row();

        // FIXED: Removed "small" - uses default font now
        contentTable.add(new Label("Music Volume", game.getSkin())).padBottom(5).row();

        final Slider volumeSlider = new Slider(0f, 1f, 0.1f, false, game.getSkin());
        volumeSlider.setValue(game.getPreferences().getMusicVolume());
        contentTable.add(volumeSlider).width(elementWidth).padBottom(20).row();

        volumeSlider.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                game.getPreferences().setMusicVolume(volumeSlider.getValue());
                game.getAudioManager().updateMusicVolume(volumeSlider.getValue());
            }
        });

        // FIXED: Removed "small"
        contentTable.add(new Label("Sound Effects", game.getSkin())).padBottom(5).row();

        final Slider sfxSlider = new Slider(0f, 1f, 0.05f, false, game.getSkin());
        sfxSlider.setValue(game.getPreferences().getSoundVolume());
        contentTable.add(sfxSlider).width(elementWidth).padBottom(40).row();

        sfxSlider.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                game.getPreferences().setSoundVolume(sfxSlider.getValue());
            }
        });

        // --- DISPLAY SECTION ---
        contentTable.add(new Label("Display", game.getSkin())).padBottom(15).row();

        String currentMode = Gdx.graphics.isFullscreen() ? "Fullscreen: ON" : "Fullscreen: OFF";
        final TextButton fullscreenBtn = new TextButton(currentMode, game.getSkin());

        fullscreenBtn.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                playClickSound();
                toggleFullscreen(fullscreenBtn);
            }
        });
        contentTable.add(fullscreenBtn).width(elementWidth).height(50).padBottom(40).row();

        // Controls section
        contentTable.add(new Label("Controls", game.getSkin())).padBottom(20).row();

        addKeyBindRow(contentTable, "Up", game.getPreferences().getKeyUp(),
                key -> game.getPreferences().setKeyUp(key), elementWidth);

        addKeyBindRow(contentTable, "Left", game.getPreferences().getKeyLeft(),
                key -> game.getPreferences().setKeyLeft(key), elementWidth);

        addKeyBindRow(contentTable, "Down", game.getPreferences().getKeyDown(),
                key -> game.getPreferences().setKeyDown(key), elementWidth);

        addKeyBindRow(contentTable, "Right", game.getPreferences().getKeyRight(),
                key -> game.getPreferences().setKeyRight(key), elementWidth);

        addKeyBindRow(contentTable, "Pause", game.getPreferences().getKeyPause(),
                key -> game.getPreferences().setKeyPause(key), elementWidth);

        addKeyBindRow(contentTable, "Map (Hold)", game.getPreferences().getKeyMap(),
                key -> game.getPreferences().setKeyMap(key), elementWidth);

        addKeyBindRow(contentTable, "Melee", game.getPreferences().getKeyMelee(),
                key -> game.getPreferences().setKeyMelee(key), elementWidth);

        addKeyBindRow(contentTable, "Shoot", game.getPreferences().getKeyShoot(),
                key -> game.getPreferences().setKeyShoot(key), elementWidth);

        addKeyBindRow(contentTable, "Zoom In", game.getPreferences().getKeyZoomIn(),
                key -> game.getPreferences().setKeyZoomIn(key), elementWidth);

        addKeyBindRow(contentTable, "Zoom Out", game.getPreferences().getKeyZoomOut(),
                key -> game.getPreferences().setKeyZoomOut(key), elementWidth);

        addKeyBindRow(contentTable, "Dash", game.getPreferences().getKeyDash(),
                key -> game.getPreferences().setKeyDash(key), elementWidth);

        addKeyBindRow(contentTable, "Repulse", game.getPreferences().getKeyRepulse(),
                key -> game.getPreferences().setKeyRepulse(key), elementWidth);


        // --- BACK BUTTON ---
        TextButton backButton = new TextButton("Back to Menu", game.getSkin());
        backButton.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                playClickSound();
                game.goToMenu();
            }
        });
        contentTable.add(backButton).width(elementWidth).height(60).padTop(30).padBottom(100);

        ScrollPane scrollPane = new ScrollPane(contentTable, game.getSkin());
        scrollPane.setFadeScrollBars(false);
        scrollPane.setScrollingDisabled(true, false);

        mainTable.add(scrollPane).grow().center();
    }


    /**
     * Adds a row to the settings menu for rebinding a key.
     *
     * @param table   Parent table
     * @param name    Action name
     * @param key     Currently assigned key
     * @param onSave  Callback to store the new key
     * @param width   Width of the UI element
     */
    private void addKeyBindRow(Table table, String name, int key, java.util.function.Consumer<Integer> onSave, Value width) {
        table.add(createKeyBindButton(name, key, onSave)).width(width).height(50).padBottom(10).row();
    }

    private TextButton createKeyBindButton(String actionName, int currentKey, java.util.function.Consumer<Integer> onSave) {
        String keyName = Input.Keys.toString(currentKey);
        final TextButton button = new TextButton(actionName + ": " + keyName, game.getSkin());
        button.getLabel().setWrap(true);

        button.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                button.setText(actionName + ": Press key...");
                playClickSound();

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

    /**
     * Toggles between fullscreen and windowed mode.
     *
     * @param button Button whose label reflects the current mode
     */
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
