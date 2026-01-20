package de.tum.cit.fop.maze;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.*;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.ScreenUtils;
import com.badlogic.gdx.utils.viewport.ScreenViewport;

import de.tum.cit.fop.maze.LevelDiscovery;

public class LevelMenuScreen implements Screen {

    private final MazeRunnerGame game;
    private final Stage stage;

    public LevelMenuScreen(MazeRunnerGame game) {
        this.game = game;
        this.stage = new Stage(new ScreenViewport(new OrthographicCamera()), game.getSpriteBatch());
    }

    @Override
    public void show() {
        Gdx.input.setInputProcessor(stage);
        buildUI(); // ✅ rebuild every time you open the screen
    }

    private void buildUI() {
        stage.clear();

        Table root = new Table();
        root.setFillParent(true);
        stage.addActor(root);

        Label title = new Label("Select Level", game.getSkin(), "title");
        title.setAlignment(Align.center);
        root.add(title).padBottom(25).row();

        Table listTable = new Table();
        ScrollPane scroll = new ScrollPane(listTable, game.getSkin());
        scroll.setFadeScrollBars(false);

        var levels = LevelDiscovery.listAllLevels();

        if (levels.size == 0) {
            listTable.add(new Label("No .properties levels found in maps/.", game.getSkin()))
                    .pad(10).row();
        } else {
            for (FileHandle levelFile : levels) {
                String label = levelFile.nameWithoutExtension();

                TextButton btn = new TextButton(label, game.getSkin());
                btn.addListener(new ChangeListener() {
                    @Override
                    public void changed(ChangeEvent event, Actor actor) {
                        int lvl = guessDifficulty(levelFile.name());
                        game.setScreen(new GameScreen(game, lvl, levelFile));
                    }
                });

                listTable.add(btn).width(360).pad(6).row();
            }
        }

        root.add(scroll).width(440).height(460).row();

        TextButton back = new TextButton("Back", game.getSkin());
        back.addListener(new ChangeListener() {
            @Override public void changed(ChangeEvent event, Actor actor) {
                game.setScreen(new MenuScreen(game));
            }
        });
        root.add(back).width(200).padTop(18).row();
    }

    private int guessDifficulty(String filename) {
        String digits = filename.replaceAll("\\D+", "");
        if (digits.isEmpty()) return 1;
        try { return Integer.parseInt(digits); }
        catch (NumberFormatException e) { return 1; }
    }

    @Override
    public void render(float delta) {
        ScreenUtils.clear(0,0,0,1);
        stage.act(delta);
        stage.draw();
    }

    @Override public void resize(int width, int height) { stage.getViewport().update(width, height, true); }
    @Override public void pause() {}
    @Override public void resume() {}
    @Override public void hide() {}
    @Override public void dispose() { stage.dispose(); }
}
