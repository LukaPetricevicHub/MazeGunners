package de.tum.cit.fop.maze.ui;

import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.ui.*;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.utils.Align;

import de.tum.cit.fop.maze.GameScreen;
import de.tum.cit.fop.maze.MazeRunnerGame;
import de.tum.cit.fop.maze.LevelDiscovery;
import de.tum.cit.fop.maze.player.Player;

public class GameOverWindow extends Window {

    public GameOverWindow(Skin skin,
                          MazeRunnerGame game,
                          int currentLevel,
                          boolean won,
                          FileHandle currentLevelFile,
                          int score,
                          Player player) {

        super(won ? "LEVEL COMPLETED" : "GAME OVER", skin);

        // 1. VISUAL SETUP
        this.getTitleLabel().setAlignment(Align.center);
        this.setMovable(false);
        this.setModal(true);
        this.setResizable(false);

        // --- SPACING FIXES ---
        this.pad(60); // Big internal padding
        this.defaults().spaceBottom(20); // Add gap between every row
        this.defaults().align(Align.center); // Center everything
        // ---------------------

        // 2. Message & Score
        String message = won ? "Great Job! You escaped." : "You died.";
        Label messageLabel = new Label(message, skin);
        messageLabel.setFontScale(1.2f);
        this.add(messageLabel).row();

        Label scoreLabel = new Label("Final Score: " + score, skin);
        scoreLabel.setFontScale(1.1f);
        this.add(scoreLabel).padBottom(30).row();

        // 3. Name Entry
        this.add(new Label("Enter Name:", skin)).spaceBottom(10).row();

        final TextField nameField = new TextField("Player", skin);
        nameField.setAlignment(Align.center);
        this.add(nameField).width(300).row();

        TextButton submitBtn = new TextButton("Submit Score", skin);
        submitBtn.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                String name = nameField.getText();
                if (name == null || name.trim().isEmpty()) name = "Unknown";

                game.getLeaderboardManager().addScore(name, score);

                submitBtn.setText("Saved!");
                submitBtn.setDisabled(true);
            }
        });
        this.add(submitBtn).width(300).padBottom(30).row();

        // 4. Navigation Buttons

        // "Next Level" (Only if you won)
        if (won) {
            TextButton nextBtn = new TextButton("Next Level", skin);
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

        // "Main Menu" (Always visible)
        TextButton menuBtn = new TextButton("Main Menu", skin);
        menuBtn.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                game.goToMenu();
            }
        });
        this.add(menuBtn).width(300).row();

        this.pack();
    }

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