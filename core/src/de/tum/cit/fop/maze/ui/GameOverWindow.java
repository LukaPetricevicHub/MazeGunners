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
import de.tum.cit.fop.maze.player.Player;

public class GameOverWindow extends Window {

    // IMPORTANT: Note that 'won' and 'player' are declared here in the arguments
    public GameOverWindow(Skin skin,
                          MazeRunnerGame game,
                          int currentLevel,
                          boolean won,
                          FileHandle currentLevelFile,
                          int score,
                          Player player) {

        // We call super() first, but we DO NOT close the bracket '}' yet!
        super(won ? "LEVEL " + currentLevel + " COMPLETED" : "GAME OVER", skin);

        this.getTitleLabel().setAlignment(1);
        this.setMovable(false);
        this.setModal(true);

        String message = won ? "Great Job!" : "Better luck next time...";
        this.add(new Label(message, skin)).padBottom(10).row();

        // Display Score
        Label scoreLabel = new Label("Final Score: " + score, skin);
        scoreLabel.setFontScale(1.0f);
        this.add(scoreLabel).padBottom(20).row();

        // Next Level Button
        if (won) {
            TextButton nextBtn = new TextButton("Next Level", skin);
            nextBtn.addListener(new ChangeListener() {
                @Override
                public void changed(ChangeEvent event, Actor actor) {
                    FileHandle nextFile = findNextLevelFile(currentLevelFile);

                    // Save stats (Player Level, XP, Gun) to memory
                    if (player != null) {
                        game.handleLevelTransition(player);
                    }

                    if (nextFile != null) {
                        int nextDifficulty = guessDifficulty(nextFile.name());
                        game.setScreen(new GameScreen(game, nextDifficulty, nextFile));
                    } else {
                        game.goToMenu();
                    }
                }
            });
            this.add(nextBtn).width(200).padBottom(10).row();
        }

        // Restart Button
        TextButton restartBtn = new TextButton("Restart Level", skin);
        restartBtn.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                int difficulty = guessDifficulty(currentLevelFile != null ? currentLevelFile.name() : ("level-" + currentLevel + ".properties"));
                game.setScreen(new GameScreen(game, difficulty, currentLevelFile));
            }
        });
        this.add(restartBtn).width(200).padBottom(10).row();

        // Menu Button
        TextButton menuBtn = new TextButton("Main Menu", skin);
        menuBtn.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                game.goToMenu();
            }
        });
        this.add(menuBtn).width(200).row();

        this.pack();
    } // <--- The constructor closes HERE, at the very end.

    private FileHandle findNextLevelFile(FileHandle current) {
        var all = LevelDiscovery.listAllLevels();
        if (all.size == 0) return null;

        if (current == null) return all.get(0);

        for (int i = 0; i < all.size; i++) {
            if (all.get(i).path().equals(current.path())) {
                if (i + 1 < all.size) return all.get(i + 1);
                return null;
            }
        }
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