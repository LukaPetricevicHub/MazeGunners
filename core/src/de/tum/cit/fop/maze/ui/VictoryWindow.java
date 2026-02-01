package de.tum.cit.fop.maze.ui;

import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.ui.TextField;
import com.badlogic.gdx.scenes.scene2d.ui.Window;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.utils.Align;
import de.tum.cit.fop.maze.GameScreen;
import de.tum.cit.fop.maze.MazeRunnerGame;

/**
 * Window shown when the player completes the final story level.
 */
public class VictoryWindow extends Window {

    /**
     * Creates a victory window with score submission options.
     *
     * @param skin UI skin to apply
     * @param game main game instance
     * @param level completed level number
     * @param score final score value
     */
    public VictoryWindow(Skin skin, MazeRunnerGame game, int level, int score) {
        super("VICTORY", skin);

        this.getTitleLabel().setAlignment(Align.center);
        this.setMovable(false);
        this.setModal(true);
        this.setResizable(false);
        this.pad(40f);

        Label messageLabel = new Label("You escaped the maze!", skin);
        messageLabel.setAlignment(Align.center);
        this.add(messageLabel).padBottom(20).row();

        Label scoreLabel = new Label("Final Score: " + score, skin);
        scoreLabel.setAlignment(Align.center);
        this.add(scoreLabel).padBottom(30).row();

        this.add(new Label("Enter Name:", skin)).padBottom(6).row();
        final TextField nameField = new TextField("Player", skin);
        nameField.setAlignment(Align.center);
        this.add(nameField).width(300).padBottom(10).row();

        TextButton submitBtn = new TextButton("Submit Score", skin);
        submitBtn.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                String name = nameField.getText();
                if (name == null || name.trim().isEmpty()) name = "Unknown";
                String boardKey = "level-" + level;
                game.getLeaderboardManager().addScore(boardKey, name, score);
                submitBtn.setText("Saved!");
                submitBtn.setDisabled(true);
            }
        });
        this.add(submitBtn).width(300).padBottom(20).row();

        TextButton menuBtn = new TextButton("Main Menu", skin);
        menuBtn.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                game.goToMenu();
            }
        });
        this.add(menuBtn).width(260).row();

        TextButton endlessBtn = new TextButton("Continue Endless", skin);
        endlessBtn.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                game.setScreen(new GameScreen(game, 1, null, true, 1));
            }
        });
        this.add(endlessBtn).width(260).padTop(10).row();

        this.pack();
    }
}
