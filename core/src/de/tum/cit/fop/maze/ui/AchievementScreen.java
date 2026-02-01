package de.tum.cit.fop.maze.ui;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.*;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.utils.viewport.ScreenViewport;
import de.tum.cit.fop.maze.MazeRunnerGame;
import de.tum.cit.fop.maze.util.Achievement;
import de.tum.cit.fop.maze.ui.UiButtons;

/**
 * Screen that displays the player's achievements in a scrollable list.
 */
public class AchievementScreen implements Screen {

    private final MazeRunnerGame game;
    private final Stage stage;

    /**
     * Creates the achievements screen.
     *
     * @param game main game instance
     */
    public AchievementScreen(MazeRunnerGame game) {
        this.game = game;
        this.stage = new Stage(new ScreenViewport(), game.getSpriteBatch());

        Table mainTable = new Table();
        mainTable.setFillParent(true);
        stage.addActor(mainTable);

        // Title
        mainTable.add(new Label("Achievements", game.getSkin(), "title")).padBottom(30).row();

        // --- THE SCROLLABLE LIST ---
        Table listTable = new Table();
        listTable.top();

        // 1. Get the achievements from the manager
        // 2. Loop through them and create a label for each
        for (Achievement a : game.getAchievementManager().getAchievements().values()) {

            Table row = new Table();
            // Optional: Add a dark background to each row so it's easier to read
            // (If this crashes, remove the .setBackground line)
            if (game.getSkin().has("white", com.badlogic.gdx.scenes.scene2d.utils.Drawable.class)) {
                row.setBackground(game.getSkin().newDrawable("white", 0, 0, 0, 0.5f));
            }

            row.pad(10);

            // Determine Text & Color based on unlock status
            String titleText = a.getTitle();
            String descText = a.isUnlocked() ? a.getDescription() : "Locked";
            Color titleColor = a.isUnlocked() ? Color.GREEN : Color.GRAY;

            Label titleLabel = new Label(titleText, game.getSkin());
            titleLabel.setColor(titleColor);
            titleLabel.setFontScale(1.2f);

            Label descLabel = new Label(descText, game.getSkin());
            descLabel.setFontScale(0.8f);
            descLabel.setColor(Color.LIGHT_GRAY);

            // Add text to the row
            row.add(titleLabel).left().expandX().row();
            row.add(descLabel).left().expandX().padTop(5);

            // Add the row to the big list
            listTable.add(row).growX().padBottom(10).row();
        }

        // 3. Wrap the list in a ScrollPane
        ScrollPane scrollPane = new ScrollPane(listTable, game.getSkin());
        mainTable.add(scrollPane).width(600).height(400).padBottom(20).row();

        // --- BACK BUTTON ---
        TextButton backBtn = UiButtons.button("Back", game.getSkin());
        backBtn.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                game.goToMenu();
            }
        });
        mainTable.add(backBtn).width(200);
    }

    /**
     * Renders the achievements screen.
     *
     * @param delta time step in seconds
     */
    @Override
    public void render(float delta) {
        // Allow ESC to go back
        if (Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE)) {
            game.goToMenu();
        }

        // Clear screen to black
        Gdx.gl.glClearColor(0, 0, 0, 1);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        stage.act(delta);
        stage.draw();
    }

    /**
     * Sets the input processor when the screen is shown.
     */
    @Override
    public void show() { Gdx.input.setInputProcessor(stage); }

    /**
     * Updates the viewport when the window is resized.
     *
     * @param width new width in pixels
     * @param height new height in pixels
     */
    @Override
    public void resize(int width, int height) { stage.getViewport().update(width, height, true); }

    /**
     * Disposes screen resources.
     */
    @Override
    public void dispose() { stage.dispose(); }

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
}
