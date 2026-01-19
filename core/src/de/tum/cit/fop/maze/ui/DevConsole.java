package de.tum.cit.fop.maze.ui;

import com.badlogic.gdx.Input;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.InputListener;
import com.badlogic.gdx.scenes.scene2d.ui.*;
import de.tum.cit.fop.maze.MazeRunnerGame;
import de.tum.cit.fop.maze.GameScreen;
import de.tum.cit.fop.maze.player.Player;

public class DevConsole extends Table {

    private final TextField inputField;
    private final Label logLabel;
    private final MazeRunnerGame game;
    private Player player;

    public DevConsole(Skin skin, MazeRunnerGame game) {
        super(skin);
        this.game = game;

        this.setFillParent(true);
        this.top();
        this.setVisible(false);

        Table container = new Table(skin);
        container.setBackground(skin.newDrawable("white", 0, 0, 0, 0.8f)); // 80% transparent black

        logLabel = new Label("Dev Console Ready", skin);
        logLabel.setFontScale(0.8f);
        container.add(logLabel).expandX().left().pad(5).row();

        inputField = new TextField("", skin);
        inputField.setMessageText("Enter command...");
        container.add(inputField).fillX().expandX().pad(5).row();

        this.add(container).fillX().expandX();

        inputField.addListener(new InputListener() {
            @Override
            public boolean keyDown(InputEvent event, int keycode) {
                if (keycode == Input.Keys.ENTER) {
                    executeCommand(inputField.getText());
                    inputField.setText(""); // Clear input
                    return true;
                }

                if (keycode == Input.Keys.ESCAPE) {
                    setVisible(false);
                    return true;
                }
                return false;
            }
        });
    }

    public void setPlayer(Player player) {
        this.player = player;
    }

    public void toggle() {
        boolean visible = !isVisible();
        setVisible(visible);
        if (visible) {
            getStage().setKeyboardFocus(inputField);
        } else {
            getStage().setKeyboardFocus(null);
        }
    }

    private void executeCommand(String rawCommand) {
        if (rawCommand.trim().isEmpty()) return;

        String[] parts = rawCommand.toLowerCase().trim().split(" ");
        String command = parts[0];

        try {
            switch (command) {
                case "god":
                    player.setGodMode(true);
                    log("God Mode ON");
                    break;
                case "ungod":
                    player.setGodMode(false);
                    log("God Mode OFF");
                    break;

                case "noclip":
                    player.setNoClip(true);
                    log("No Clip ON (Walk through walls)");
                    break;
                case "clip":
                    player.setNoClip(false);
                    log("No Clip OFF");
                    break;

                case "key":
                    player.giveKey();
                    log("Key added to inventory.");
                    break;

                case "speed":
                    if (parts.length > 1) {
                        float amount = Float.parseFloat(parts[1]);
                        player.setSpeedMultiplier(amount);
                        log("Speed multiplier set to " + amount);
                    } else {
                        log("Usage: speed <value> (e.g. 1.5)");
                    }
                    break;

                case "heal":
                    player.addLife();
                    log("Player healed.");
                    break;

                case "level":
                    if (parts.length > 1) {
                        int lvl = Integer.parseInt(parts[1]);
                        log("Teleporting to Level " + lvl + "...");
                        game.setScreen(new GameScreen(game, lvl));
                    }
                    break;

                case "help":
                    log("cmds: god, ungod, noclip, clip, key, speed <x>, level <x>");
                    break;

                default:
                    log("Unknown command: " + command);
                    break;
            }
        } catch (Exception e) {
            log("Error executing command: " + e.getMessage());
        }
    }

    private void log(String message) {
        logLabel.setText(message);
        System.out.println("[DEV] " + message);
    }
}