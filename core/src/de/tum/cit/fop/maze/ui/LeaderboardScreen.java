package de.tum.cit.fop.maze.ui;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.utils.viewport.ScreenViewport;
import com.badlogic.gdx.utils.viewport.Viewport;
import de.tum.cit.fop.maze.MazeRunnerGame;
import de.tum.cit.fop.maze.score.ScoreEntry;
import de.tum.cit.fop.maze.ui.UiButtons;

/**
 * Screen displaying the game's leaderboards.
 *
 * Allows the player to view high scores for individual levels
 * as well as endless mode.
 */
/**
 * Screen that displays per-level and endless leaderboards.
 */
public class LeaderboardScreen implements Screen {
    /** Main game instance used for navigation and assets. */
    private final MazeRunnerGame game;
    /** Stage hosting the leaderboard UI. */
    private final Stage stage;
    /** Table used to render the score list. */
    private final Table listTable;
    /** Currently selected leaderboard key. */
    private String activeBoard = null;

    /**
     * Creates the leaderboard screen and initializes the user interface.
     *
     * @param game Reference to the main game instance
     */
    public LeaderboardScreen(MazeRunnerGame game) {
        this.game = game;
        Viewport viewport = new com.badlogic.gdx.utils.viewport.FitViewport(1280, 720);
        this.stage = new Stage(viewport, game.getSpriteBatch());
        Gdx.input.setInputProcessor(stage);

        Table table = new Table();
        table.setFillParent(true);
        stage.addActor(table);

        
        table.add(new Label("HALL OF FAME", game.getSkin(), "title")).padBottom(20).row();

        Table buttons = new Table();
        buttons.defaults().pad(6).width(180);

        buttons.add(makeBoardButton("Level 1", "level-1"));
        buttons.add(makeBoardButton("Level 2", "level-2"));
        buttons.add(makeBoardButton("Level 3", "level-3"));
        buttons.row();
        buttons.add(makeBoardButton("Level 4", "level-4"));
        buttons.add(makeBoardButton("Level 5", "level-5"));
        buttons.add(makeBoardButton("Endless", "endless"));

        table.add(buttons).padBottom(20).row();

        listTable = new Table();
        table.add(listTable).padBottom(30).row();
        showSelectHint();

        
        TextButton backBtn = UiButtons.button("Back to Menu", game.getSkin());
        backBtn.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                game.goToMenu();
            }
        });
        table.add(backBtn).width(200).height(50);
    }

    /**
     * Renders and updates the leaderboard screen.
     *
     * @param delta Time since last frame
     */
    @Override public void render(float delta) {
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);
        stage.act(delta);
        stage.draw();
    }

    /**
     * Updates the viewport when the window size changes.
     *
     * @param width New window width
     * @param height New window height
     */
    @Override public void resize(int width, int height) { stage.getViewport().update(width, height, true); }

    /**
     * Disposes all UI resources used by the screen.
     */
    @Override public void dispose() { stage.dispose(); }
    /** Sets up input processing when the screen becomes active. */
    @Override public void show() {}
    /** Clears any temporary state when the screen is hidden. */
    @Override public void hide() {}
    /** Pauses UI updates when the app is backgrounded. */
    @Override public void pause() {}
    /** Resumes UI updates after a pause. */
    @Override public void resume() {}

    /**
     * Creates a button that selects a leaderboard to display.
     *
     * @param label Button label
     * @param boardKey Leaderboard key to load
     * @return Configured leaderboard button
     */
    private TextButton makeBoardButton(String label, String boardKey) {
        TextButton btn = UiButtons.button(label, game.getSkin());
        btn.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                activeBoard = boardKey;
                rebuildList();
            }
        });
        return btn;
    }

    /**
     * Rebuilds the score list for the active leaderboard.
     */
    private void rebuildList() {
        listTable.clearChildren();
        if (activeBoard == null) {
            showSelectHint();
            return;
        }
        listTable.add(new Label("Rank", game.getSkin())).width(120).pad(6);
        listTable.add(new Label("Name", game.getSkin())).width(320).pad(6);
        if (activeBoard.equals("endless")) {
            listTable.add(new Label("Floor", game.getSkin())).width(140).pad(6);
            listTable.add(new Label("Score", game.getSkin())).width(180).pad(6).row();
        } else {
            listTable.add(new Label("Score", game.getSkin())).width(180).pad(6).row();
        }

        int rank = 1;
        for (ScoreEntry entry : game.getLeaderboardManager().getScores(activeBoard)) {
            listTable.add(new Label("#" + rank, game.getSkin())).pad(6);
            listTable.add(new Label(entry.getPlayerName(), game.getSkin())).pad(6);
            if (activeBoard.equals("endless")) {
                listTable.add(new Label(String.valueOf(entry.getScore()), game.getSkin())).pad(6);
                listTable.add(new Label(String.valueOf(entry.getExtra()), game.getSkin())).pad(6).row();
            } else {
                listTable.add(new Label(String.valueOf(entry.getScore()), game.getSkin())).pad(6).row();
            }
            rank++;
        }
    }

    /**
     * Displays a prompt when no leaderboard is selected.
     */
    private void showSelectHint() {
        listTable.clearChildren();
        listTable.add(new Label("Select a leaderboard above.", game.getSkin())).pad(10);
    }
}

