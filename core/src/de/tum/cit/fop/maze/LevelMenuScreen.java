package de.tum.cit.fop.maze;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.ScrollPane;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.utils.viewport.ScreenViewport;

/**
 * Screen for selecting a level to play (Option A: regenerate every entry).
 */
public class LevelMenuScreen implements Screen {

    private final Stage stage;
    private final Skin skin;

    public LevelMenuScreen(MazeRunnerGame game) {
        this.stage = new Stage(new ScreenViewport(), game.getSpriteBatch());
        this.skin = game.getSkin();

        Gdx.input.setInputProcessor(stage);

        Table mainTable = new Table();
        mainTable.setFillParent(true);
        stage.addActor(mainTable);

        Label title = new Label("Select a Level", skin, "title");
        mainTable.add(title).padBottom(40).row();

        Table buttonTable = new Table();

        // Fixed levels 1..5 (each click regenerates that level)
        for (int level = 1; level <= 5; level++) {
            final int lvl = level;

            TextButton button = new TextButton("Level " + lvl, skin);
            button.addListener(new ChangeListener() {
                @Override
                public void changed(ChangeEvent event, Actor actor) {
                    game.setScreen(new GameScreen(game, lvl));
                }
            });

            buttonTable.add(button).width(300).pad(10).row();
        }
        buttonTable.row().padTop(40);

        TextButton backButton = new TextButton("Back to Menu", skin);
        backButton.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                game.goToMenu();
            }
        });

        buttonTable.add(backButton).width(300).pad(10).row();


        ScrollPane scrollPane = new ScrollPane(buttonTable, skin);
        scrollPane.setFadeScrollBars(false);
        mainTable.add(scrollPane).fill().expand();
    }

    @Override
    public void show() {
        Gdx.input.setInputProcessor(stage);
    }

    @Override
    public void render(float delta) {
        Gdx.gl.glClearColor(0, 0, 0, 1);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        stage.act(delta);
        stage.draw();
    }

    @Override
    public void resize(int width, int height) {
        stage.getViewport().update(width, height, true);
    }

    @Override
    public void dispose() {
        stage.dispose();
    }

    @Override public void hide() {}
    @Override public void pause() {}
    @Override public void resume() {}
}
