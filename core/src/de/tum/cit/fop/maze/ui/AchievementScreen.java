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

/**
 * Screen displaying all available achievements.
 *
 * Shows unlocked and locked achievements in a scrollable list
 * and allows returning to the main menu.
 */
public class AchievementScreen implements Screen {

    private final MazeRunnerGame game;
    private final Stage stage;

    /**
     * Creates the achievement overview screen.
     *
     * @param game Reference to the main game instance
     */
    public AchievementScreen(MazeRunnerGame game) {
        this.game = game;
        this.stage = new Stage(new ScreenViewport(), game.getSpriteBatch());

        Table mainTable = new Table();
        mainTable.setFillParent(true);
        stage.addActor(mainTable);

        // Title
        mainTable.add(new Label("Achievements", game.getSkin(), "title")).padBottom(30).row();

        // SCROLLABLE LIST
        Table listTable = new Table();
        listTable.top();

        for (Achievement a : game.getAchievementManager().getAchievements().values()) {

            Table row = new Table();
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

        // BACK BUTTON
        TextButton backBtn = new TextButton("Back", game.getSkin());
        backBtn.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                game.goToMenu();
            }
        });
        mainTable.add(backBtn).width(200);
    }

    /**
     * Updates and renders the achievement screen.
     *
     * Handles input for returning to the menu and draws the UI.
     *
     * @param delta Time elapsed since the last frame
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
     * Sets the input processor when this screen becomes active.
     */
    @Override public void show() { Gdx.input.setInputProcessor(stage); }

    /**
     * Updates the viewport when the window size changes.
     *
     * @param width New window width
     * @param height New window height
     */
    @Override public void resize(int width, int height) { stage.getViewport().update(width, height, true); }

    /**
     * Disposes resources used by this screen.
     */
    @Override public void dispose() { stage.dispose(); }

    @Override public void pause() {}
    @Override public void resume() {}
    @Override public void hide() {}
}