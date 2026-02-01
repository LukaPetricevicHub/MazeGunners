package de.tum.cit.fop.maze.ui;

import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.ui.*;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.utils.Align;
import de.tum.cit.fop.maze.ui.UiButtons;

import de.tum.cit.fop.maze.GameScreen;
import de.tum.cit.fop.maze.MazeRunnerGame;
import de.tum.cit.fop.maze.LevelDiscovery;
import de.tum.cit.fop.maze.player.Player;
import de.tum.cit.fop.maze.ui.UiButtons;

/**
 * Window shown when the game ends.
 *
 * Displays the final score, allows the player to submit their name
 * to the leaderboard, and provides navigation options such as
 * continuing to the next level or returning to the main menu.
 */
/**
 * Game-over modal that shows results and handles score submission.
 */
public class GameOverWindow extends Window {

    /**
     * Creates a game over window for both victory and defeat states.
     *
     * @param skin UI skin used for styling
     * @param game Reference to the main game instance
     * @param currentLevel Current level number
     * @param won Whether the player won the level
     * @param currentLevelFile File of the current level
     * @param score Final player score
     * @param player Player instance (used for transitions)
     * @param endlessMode Whether endless mode is active
     * @param floor Current floor in endless mode
     */
    public GameOverWindow(Skin skin, MazeRunnerGame game, int currentLevel, boolean won, FileHandle currentLevelFile, int score, Player player, boolean endlessMode, int floor) {
        super(won ? "LEVEL COMPLETED" : "GAME OVER", skin);

        
        this.getTitleLabel().setAlignment(Align.center);
        this.setMovable(false);
        this.setModal(true);
        this.setResizable(false);
        this.pad(60); 
        this.defaults().spaceBottom(20); 
        this.defaults().align(Align.center); 


        
        String message = won ? "Great Job! You escaped." : "You died.";
        Label messageLabel = new Label(message, skin);
        messageLabel.setFontScale(1.2f);
        this.add(messageLabel).row();

        Label scoreLabel = new Label("Final Score: " + score, skin);
        scoreLabel.setFontScale(1.1f);
        this.add(scoreLabel).padBottom(30).row();

        
        this.add(new Label("Enter Name:", skin)).spaceBottom(10).row();

        final TextField nameField = new TextField("Player", skin);
        nameField.setAlignment(Align.center);
        this.add(nameField).width(300).row();

        TextButton submitBtn = UiButtons.button("Submit Score", skin);
        submitBtn.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                String name = nameField.getText();
                if (name == null || name.trim().isEmpty()) name = "Unknown";

                String boardKey = endlessMode ? "endless" : ("level-" + currentLevel);
                if (endlessMode) {
                    game.getLeaderboardManager().addScore(boardKey, name, floor, score);
                } else {
                    game.getLeaderboardManager().addScore(boardKey, name, score);
                }

                submitBtn.setText("Saved!");
                submitBtn.setDisabled(true);
            }
        });
        this.add(submitBtn).width(300).padBottom(30).row();

        

        
        if (won) {
            TextButton nextBtn = UiButtons.button("Next Level", skin);
            nextBtn.addListener(new ChangeListener() {
                @Override
                public void changed(ChangeEvent event, Actor actor) {
                    FileHandle nextFile = findNextLevelFile(currentLevelFile);
                    if (player != null) game.handleLevelTransition(player);

                    if (nextFile != null) {
                        int nextDifficulty = guessDifficulty(nextFile.name());
                        game.setScreen(new GameScreen(game, nextDifficulty, nextFile));
                    } else {
                        game.goToMenu();
                    }
                }
            });
            this.add(nextBtn).width(300).row();
        }


        TextButton menuBtn = UiButtons.button("Main Menu", skin);
        menuBtn.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                game.resetRunGamble();
                game.goToMenu();
            }
        });
        this.add(menuBtn).width(300).row();

        this.pack();
    }

    /**
     * Finds the next level file in the level list after the current one.
     *
     * @param current Current level file
     * @return Next level file, or null if none exists
     */
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

    /**
     * Attempts to extract the level number from a filename.
     *
     * @param filename File name to parse
     * @return Parsed difficulty, or 1 if no number is found
     */
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

