package de.tum.cit.fop.maze.ui;

import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.ui.Window;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import de.tum.cit.fop.maze.GameScreen;
import de.tum.cit.fop.maze.MazeRunnerGame;

public class GameOverWindow extends Window {

    public GameOverWindow(Skin skin, MazeRunnerGame game, int currentLevel, boolean won) {
        //Set Title
        super(won ? "LEVEL " + currentLevel + " COMPLETED" : "GAME OVER", skin);

        this.getTitleLabel().setAlignment(1);
        this.setMovable(false);
        this.setModal(true);

        //Message
        String message = won ? "Great Job!" : "Better luck next time...";
        this.add(new Label(message, skin)).padBottom(20).row();

        //Next Level Button
        if (won && currentLevel < 5) {
            TextButton nextBtn = new TextButton("Next Level", skin);
            nextBtn.addListener(new ChangeListener() {
                @Override
                public void changed(ChangeEvent event, Actor actor) {
                    game.setScreen(new GameScreen(game, currentLevel + 1));
                }
            });
            this.add(nextBtn).width(200).padBottom(10).row();
        }

        //Restart Button
        TextButton restartBtn = new TextButton("Restart Level", skin);
        restartBtn.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                game.setScreen(new GameScreen(game, currentLevel));
            }
        });
        this.add(restartBtn).width(200).padBottom(10).row();

        //Main Menu Button
        TextButton menuBtn = new TextButton("Main Menu", skin);
        menuBtn.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                game.goToMenu();
            }
        });
        this.add(menuBtn).width(200).row();

        //Pack to fit content
        this.pack();
    }
}