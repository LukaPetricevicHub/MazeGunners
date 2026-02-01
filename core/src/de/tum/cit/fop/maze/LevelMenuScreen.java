package de.tum.cit.fop.maze;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.ScrollPane;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.ScreenUtils;
import com.badlogic.gdx.utils.viewport.ScreenViewport;
import de.tum.cit.fop.maze.ui.UiButtons;

/**
 * Screen for selecting story mode levels.
 *
 * This screen lists all available story levels found in the {@code maps/}
 * directory and allows the player to select and start a level.
 * Levels that are not yet unlocked are shown as locked and cannot be selected.
 */
public class LevelMenuScreen implements Screen {

    private final MazeRunnerGame game;
    private final Stage stage;

    /**
     * Creates a new level selection screen for story mode.
     *
     * @param game Reference to the main game instance
     */
    public LevelMenuScreen(MazeRunnerGame game) {
        this.game = game;
        this.stage = new Stage(new ScreenViewport(new OrthographicCamera()), game.getSpriteBatch());
    }

    /**
     * Called when this screen becomes active.
     * Sets the input processor and builds the user interface.
     */
    @Override
    public void show() {
        Gdx.input.setInputProcessor(stage);
        buildUI();
    }

    /**
     * Builds the UI for the level selection menu.
     *
     * Displays a scrollable list of available levels and disables
     * levels that are locked based on player progression.
     */
    private void buildUI() {
        stage.clear();

        Table root = new Table();
        root.setFillParent(true);
        stage.addActor(root);

        Label title = new Label("Story Mode - Select Level", game.getSkin(), "title");
        title.setAlignment(Align.center);
        root.add(title).padBottom(25).row();

        Table listTable = new Table();
        ScrollPane scroll = new ScrollPane(listTable, game.getSkin());
        scroll.setFadeScrollBars(false);

        var levels = LevelDiscovery.listAllLevels();
        int maxUnlocked = game.getPreferences().getMaxLevel();

        if (levels.size == 0) {
            listTable.add(new Label("No .properties levels found in maps/.", game.getSkin()))
                    .pad(10).row();
        } else {
            for (FileHandle levelFile : levels) {
                int lvl = guessDifficulty(levelFile.name());
                boolean locked = lvl > maxUnlocked;
                String label = locked ? ("Floor " + lvl + " (Locked)") : ("Floor " + lvl);

                TextButton btn = UiButtons.button(label, game.getSkin());
                btn.setDisabled(locked);
                btn.addListener(new ChangeListener() {
                    @Override
                    public void changed(ChangeEvent event, Actor actor) {
                        if (btn.isDisabled()) return;
                        // Your MazeRunnerGame uses: new GameScreen(game, mapPathString)
                        game.setScreen(new GameScreen(game, levelFile.path()));
                    }
                });

                listTable.add(btn).width(360).pad(6).row();
            }
        }

        root.add(scroll).width(440).height(460).row();

        TextButton back = UiButtons.button("Back", game.getSkin());
        back.addListener(new ChangeListener() {
            @Override public void changed(ChangeEvent event, Actor actor) {
                game.setScreen(new ModeSelectScreen(game));
            }
        });
        root.add(back).width(200).padTop(18).row();
    }

    /**
     * Attempts to extract the level number from a level filename.
     *
     * @param filename Name of the level file
     * @return Parsed level number, or {@code 1} if parsing fails
     */
    private int guessDifficulty(String filename) {
        String digits = filename.replaceAll("\\D+", "");
        if (digits.isEmpty()) return 1;
        try { return Integer.parseInt(digits); }
        catch (NumberFormatException e) { return 1; }
    }

    @Override
    public void render(float delta) {
        ScreenUtils.clear(0, 0, 0, 1);
        stage.act(delta);
        stage.draw();
    }

    /**
     * Updates the viewport when the window is resized.
     *
     * @param width new width in pixels
     * @param height new height in pixels
     */
    @Override
    public void resize(int width, int height) { stage.getViewport().update(width, height, true); }

    /**
     * Called when the application is paused.
     */
    @Override
    public void pause() {}

    /**
     * Called when the application resumes.
     */
    @Override
    public void resume() {}

    /**
     * Called when the screen is no longer the current screen.
     */
    @Override
    public void hide() {}

    /**
     * Disposes screen resources.
     */
    @Override
    public void dispose() { stage.dispose(); }
}

