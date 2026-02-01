package de.tum.cit.fop.maze;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.ScreenUtils;
import com.badlogic.gdx.utils.viewport.ScreenViewport;
import de.tum.cit.fop.maze.ui.UiButtons;

/**
 * Screen that allows the player to select the game mode.
 *
 * The player can start the story mode or unlock and play
 * endless mode after completing the story.
 */
/**
 * Mode selection screen for story and endless gameplay.
 */
public class ModeSelectScreen implements Screen {

    private final MazeRunnerGame game;
    private final Stage stage;

    /**
     * Creates the mode selection screen.
     *
     * @param game Reference to the main game instance
     */
    public ModeSelectScreen(MazeRunnerGame game) {
        this.game = game;
        this.stage = new Stage(new ScreenViewport(new OrthographicCamera()), game.getSpriteBatch());
    }

    @Override
    public void show() {
        Gdx.input.setInputProcessor(stage);
        buildUI();
    }

    /**
     * Builds the UI for game mode selection.
     */
    private void buildUI() {
        stage.clear();

        Table root = new Table();
        root.setFillParent(true);
        stage.addActor(root);

        Label title = new Label("New Game", game.getSkin(), "title");
        title.setAlignment(Align.center);
        root.add(title).padBottom(30).row();

        
        TextButton storyBtn = UiButtons.button("Story Mode", game.getSkin());
        storyBtn.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                game.setScreen(new LevelMenuScreen(game));
            }
        });
        root.add(storyBtn).width(360).padBottom(12).row();

        
        boolean unlocked = game.getProgression().isStoryCompleted();
        TextButton endlessBtn = UiButtons.button(unlocked ? "Endless Mode" : "Endless Mode (Locked)", game.getSkin());
        endlessBtn.setDisabled(!unlocked);
        endlessBtn.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                if (endlessBtn.isDisabled()) return;
                
                game.setScreen(new GameScreen(game, 1, null, true, 1));
            }
        });
        root.add(endlessBtn).width(360).padBottom(8).row();

        if (!unlocked) {
            Label hint = new Label("Complete Story Mode to unlock Endless.", game.getSkin());
            hint.setAlignment(Align.center);
            root.add(hint).padBottom(18).row();
        } else {
            root.row();
        }

        TextButton back = UiButtons.button("Back", game.getSkin());
        back.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                game.goToMenu();
            }
        });
        root.add(back).width(200).padTop(10).row();
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

