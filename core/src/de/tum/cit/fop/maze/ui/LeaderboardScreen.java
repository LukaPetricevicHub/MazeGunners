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

public class LeaderboardScreen implements Screen {
    private final MazeRunnerGame game;
    private final Stage stage;

    public LeaderboardScreen(MazeRunnerGame game) {
        this.game = game;
        Viewport viewport = new com.badlogic.gdx.utils.viewport.FitViewport(1280, 720);
        this.stage = new Stage(viewport, game.getSpriteBatch());
        Gdx.input.setInputProcessor(stage);

        Table table = new Table();
        table.setFillParent(true);
        stage.addActor(table);

        // Title
        table.add(new Label("HALL OF FAME", game.getSkin(), "title")).padBottom(20).row();

        // Header
        Table listTable = new Table();
        listTable.add(new Label("Rank", game.getSkin())).width(50);
        listTable.add(new Label("Name", game.getSkin())).width(200);
        listTable.add(new Label("Score", game.getSkin())).width(100).row();

        // Entries
        int rank = 1;
        for (ScoreEntry entry : game.getLeaderboardManager().getScores()) {
            listTable.add(new Label("#" + rank, game.getSkin())).pad(5);
            listTable.add(new Label(entry.getPlayerName(), game.getSkin())).pad(5);
            listTable.add(new Label(String.valueOf(entry.getScore()), game.getSkin())).pad(5).row();
            rank++;
        }
        table.add(listTable).padBottom(30).row();

        // Back Button
        TextButton backBtn = new TextButton("Back to Menu", game.getSkin());
        backBtn.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                game.goToMenu();
            }
        });
        table.add(backBtn).width(200).height(50);
    }

    @Override public void render(float delta) {
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);
        stage.act(delta);
        stage.draw();
    }

    @Override public void resize(int width, int height) { stage.getViewport().update(width, height, true); }
    @Override public void dispose() { stage.dispose(); }
    @Override public void show() {}
    @Override public void hide() {}
    @Override public void pause() {}
    @Override public void resume() {}
}