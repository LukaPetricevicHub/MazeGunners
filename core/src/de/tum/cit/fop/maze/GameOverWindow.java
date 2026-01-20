package de.tum.cit.fop.maze;

import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.ui.Window;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;

import de.tum.cit.fop.maze.GameScreen;
import de.tum.cit.fop.maze.MazeRunnerGame;
import de.tum.cit.fop.maze.LevelDiscovery;

public class GameOverWindow extends Window {

    public GameOverWindow(Skin skin,
                          MazeRunnerGame game,
                          int currentLevel,
                          boolean won,
                          FileHandle currentLevelFile) {

        super(won ? "LEVEL " + currentLevel + " COMPLETED" : "GAME OVER", skin);

        this.getTitleLabel().setAlignment(1);
        this.setMovable(false);
        this.setModal(true);

        String message = won ? "Great Job!" : "Better luck next time...";
        this.add(new Label(message, skin)).padBottom(20).row();

        // Next Level Button
        if (won) {
            TextButton nextBtn = new TextButton("Next Level", skin);
            nextBtn.addListener(new ChangeListener() {
                @Override
                public void changed(ChangeEvent event, Actor actor) {
                    FileHandle nextFile = findNextLevelFile(currentLevelFile);
                    if (nextFile != null) {
                        int nextDifficulty = guessDifficulty(nextFile.name());
                        game.setScreen(new GameScreen(game, nextDifficulty, nextFile));
                    } else {
                        // If no next file exists, go back to menu
                        game.goToMenu();
                    }
                }
            });
            this.add(nextBtn).width(200).padBottom(10).row();
        }

        // Restart Button (reopen same file)
        TextButton restartBtn = new TextButton("Restart Level", skin);
        restartBtn.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                int difficulty = guessDifficulty(currentLevelFile != null ? currentLevelFile.name() : ("level-" + currentLevel + ".properties"));
                game.setScreen(new GameScreen(game, difficulty, currentLevelFile));
            }
        });
        this.add(restartBtn).width(200).padBottom(10).row();

        TextButton menuBtn = new TextButton("Main Menu", skin);
        menuBtn.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                game.goToMenu();
            }
        });
        this.add(menuBtn).width(200).row();

        this.pack();
    }

    private FileHandle findNextLevelFile(FileHandle current) {
        var all = LevelDiscovery.listAllLevels();
        if (all.size == 0) return null;

        // If we don't know current file, just take the first one
        if (current == null) return all.get(0);

        // Find current in the list and return the next one
        for (int i = 0; i < all.size; i++) {
            if (all.get(i).path().equals(current.path())) {
                if (i + 1 < all.size) return all.get(i + 1);
                return null;
            }
        }

        // current not found -> return first
        return all.get(0);
    }

    private int guessDifficulty(String filename) {
        String digits = filename.replaceAll("\\D+", "");
        if (digits.isEmpty()) return 1;
        try {
            return Integer.parseInt(digits);
        } catch (NumberFormatException e) {
            return 1;
        }
    }
}
