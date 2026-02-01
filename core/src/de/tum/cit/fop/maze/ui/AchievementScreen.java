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
 * Full-screen achievement browser for the main menu.
 */
public class AchievementScreen implements Screen {

    private final MazeRunnerGame game;
    private final Stage stage;

    public AchievementScreen(MazeRunnerGame game) {
        this.game = game;
        this.stage = new Stage(new ScreenViewport(), game.getSpriteBatch());

        Table mainTable = new Table();
        mainTable.setFillParent(true);
        stage.addActor(mainTable);

        
        mainTable.add(new Label("Achievements", game.getSkin(), "title")).padBottom(30).row();

        
        Table listTable = new Table();
        listTable.top();

        
        
        for (Achievement a : game.getAchievementManager().getAchievements().values()) {

            Table row = new Table();
            
            
            if (game.getSkin().has("white", com.badlogic.gdx.scenes.scene2d.utils.Drawable.class)) {
                row.setBackground(game.getSkin().newDrawable("white", 0, 0, 0, 0.5f));
            }

            row.pad(10);

            
            String titleText = a.getTitle();
            String descText = a.isUnlocked() ? a.getDescription() : "Locked";
            Color titleColor = a.isUnlocked() ? Color.GREEN : Color.GRAY;

            Label titleLabel = new Label(titleText, game.getSkin());
            titleLabel.setColor(titleColor);
            titleLabel.setFontScale(1.2f);

            Label descLabel = new Label(descText, game.getSkin());
            descLabel.setFontScale(0.8f);
            descLabel.setColor(Color.LIGHT_GRAY);

            
            row.add(titleLabel).left().expandX().row();
            row.add(descLabel).left().expandX().padTop(5);

            
            listTable.add(row).growX().padBottom(10).row();
        }

        
        ScrollPane scrollPane = new ScrollPane(listTable, game.getSkin());
        mainTable.add(scrollPane).width(600).height(400).padBottom(20).row();

        
        TextButton backBtn = UiButtons.button("Back", game.getSkin());
        backBtn.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                game.goToMenu();
            }
        });
        mainTable.add(backBtn).width(200);
    }

    @Override
    public void render(float delta) {
        
        if (Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE)) {
            game.goToMenu();
        }

        
        Gdx.gl.glClearColor(0, 0, 0, 1);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        stage.act(delta);
        stage.draw();
    }

    @Override public void show() { Gdx.input.setInputProcessor(stage); }
    @Override public void resize(int width, int height) { stage.getViewport().update(width, height, true); }
    @Override public void dispose() { stage.dispose(); }
    @Override public void pause() {}
    @Override public void resume() {}
    @Override public void hide() {}
}
