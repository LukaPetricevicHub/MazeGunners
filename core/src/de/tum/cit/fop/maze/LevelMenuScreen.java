package de.tum.cit.fop.maze;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.files.FileHandle;
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

import java.util.Arrays;


/**
 * Screen for selecting a level/map to play.
 */
public class LevelMenuScreen implements Screen {

    private final MazeRunnerGame game;
    private final Stage stage;
    private final Skin skin;

    public LevelMenuScreen(MazeRunnerGame game) {
        this.game = game;
        stage = new Stage(new ScreenViewport(), game.getSpriteBatch());
        Gdx.input.setInputProcessor(stage);

        skin = game.getSkin();

        Table mainTable = new Table();
        mainTable.setFillParent(true);
        stage.addActor(mainTable);

        Label title = new Label("Select a Level", skin, "title");
        mainTable.add(title).padBottom(40).row();

        Table buttonTable = new Table();

        // List all .properties files in "maps" folder
        FileHandle mapDir = Gdx.files.internal("maps");
        FileHandle[] mapFiles = mapDir.list(".properties"); // Replace this with the TiledMazes

        if (mapFiles != null && mapFiles.length > 0) {
            // Sort numerically by file name
            Arrays.sort(mapFiles, (a, b) -> {
                int numA = Integer.parseInt(a.nameWithoutExtension().replaceAll("\\D", ""));
                int numB = Integer.parseInt(b.nameWithoutExtension().replaceAll("\\D", ""));
                return Integer.compare(numA, numB);
            });

            for(FileHandle mapFile : mapFiles){
                TextButton button = new TextButton(mapFile.nameWithoutExtension(), skin);

                button.addListener(new ChangeListener() {
                    @Override
                    public void changed(ChangeEvent event, Actor actor) {
                        game.setScreen(new GameScreen(game, "maps/" + mapFile.name()));
                    }
                });

                buttonTable.add(button).width(300).pad(10).row();
            }
        } else {
            buttonTable.add(new Label("No maps found!", skin));
        }

        ScrollPane scrollPane = new ScrollPane(buttonTable, skin);
        scrollPane.setFadeScrollBars(false);
        mainTable.add(scrollPane).fill().expand();
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

    @Override
    public void show() {
        Gdx.input.setInputProcessor(stage);
    }

    @Override public void hide() {}
    @Override public void pause() {}
    @Override public void resume() {}
}
