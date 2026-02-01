package de.tum.cit.fop.maze.ui;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.InputListener;
import com.badlogic.gdx.scenes.scene2d.ui.*;

import de.tum.cit.fop.maze.MazeRunnerGame;
import de.tum.cit.fop.maze.GameScreen;
import de.tum.cit.fop.maze.player.Player;

/**
 * Developer console for debugging and testing during runtime.
 *
 * Allows execution of cheat commands such as stat modification,
 * god mode, level loading, and currency manipulation.
 * Intended for development and testing purposes only.
 */
public class DevConsole extends Table {

    /** Input field used to enter console commands. */
    private final TextField inputField;
    /** Label used to display feedback messages. */
    private final Label logLabel;
    /** Main game instance for navigation and shared systems. */
    private final MazeRunnerGame game;
    /** Active player instance for player-affecting commands. */
    private Player player;

    /**
     * Creates a developer console UI.
     *
     * @param skin UI skin used for styling
     * @param game Reference to the main game instance
     */
    public DevConsole(Skin skin, MazeRunnerGame game) {
        super(skin);
        this.game = game;

        this.setFillParent(true);
        this.top();
        this.setVisible(false);

        Table container = new Table(skin);
        container.setBackground(skin.newDrawable("white", 0, 0, 0, 0.8f));

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
                    inputField.setText("");
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

    /**
     * Assigns the active player to this console.
     *
     * Required for commands that modify player state.
     *
     * @param player Current player instance
     */
    public void setPlayer(Player player) {
        this.player = player;
    }

    /**
     * Toggles the visibility of the developer console.
     *
     * When shown, keyboard focus is moved to the input field.
     */
    public void toggle() {
        boolean visible = !isVisible();
        setVisible(visible);
        if (visible) {
            getStage().setKeyboardFocus(inputField);
        } else {
            getStage().setKeyboardFocus(null);
        }
    }

    /**
     * Parses and executes a developer command entered by the user.
     *
     * Supports commands for modifying player stats, toggling debug
     * modes, loading levels, and adjusting currencies.
     *
     * @param rawCommand Command string entered in the console
     */
    private void executeCommand(String rawCommand) {
        if (rawCommand == null || rawCommand.trim().isEmpty()) return;

        String[] parts = rawCommand.toLowerCase().trim().split("\\s+");
        String command = parts[0];

        try {
            switch (command) {
                case "god":
                    requirePlayer();
                    player.setGodMode(true);
                    log("God Mode ON");
                    break;

                case "ungod":
                    requirePlayer();
                    player.setGodMode(false);
                    log("God Mode OFF");
                    break;

                case "noclip":
                    requirePlayer();
                    player.setNoClip(true);
                    log("No Clip ON (Walk through walls)");
                    break;

                case "clip":
                    requirePlayer();
                    player.setNoClip(false);
                    log("No Clip OFF");
                    break;

                case "key":
                    requirePlayer();
                    player.giveKey();
                    log("Key added to inventory.");
                    break;

                case "gun":
                    requirePlayer();
                    player.equipGun();
                    log("Gun equipped.");
                    break;

                case "boost":
                    requirePlayer();
                    if (parts.length > 1) {
                        int delta = Integer.parseInt(parts[1]);
                        player.addStrengthStat(delta);
                        player.addSpeedStat(delta);
                        log("Increased STR and SPD by " + delta);
                    } else {
                        log("Usage: boost <amount> (e.g. 2)");
                    }
                    break;

                case "str":
                    requirePlayer();
                    if (parts.length > 1) {
                        int delta = Integer.parseInt(parts[1]);
                        player.addStrengthStat(delta);
                        log("Increased STR by " + delta);
                    } else {
                        log("Usage: str <amount> (e.g. 2)");
                    }
                    break;

                case "strraw":
                    requirePlayer();
                    if (parts.length > 1) {
                        int value = Integer.parseInt(parts[1]);
                        player.setStrengthStatRaw(value);
                        log("STR override set to " + value);
                    } else {
                        log("Usage: strraw <value> (e.g. 1000)");
                    }
                    break;

                case "spd":
                    requirePlayer();
                    if (parts.length > 1) {
                        int delta = Integer.parseInt(parts[1]);
                        player.addSpeedStat(delta);
                        log("Increased SPD by " + delta);
                    } else {
                        log("Usage: spd <amount> (e.g. 2)");
                    }
                    break;

                case "dmgmult":
                    requirePlayer();
                    if (parts.length > 1) {
                        float target = Float.parseFloat(parts[1]);
                        float delta = target - player.getDamageMult();
                        player.addDamageMult(delta);
                        log("Damage multiplier set to " + player.getDamageMult());
                    } else {
                        log("Usage: dmgmult <value> (e.g. 2.5)");
                    }
                    break;

                case "dmgmultraw":
                    requirePlayer();
                    if (parts.length > 1) {
                        float target = Float.parseFloat(parts[1]);
                        player.setDamageMultRaw(target);
                        log("Damage multiplier override set to " + player.getDamageMult());
                    } else {
                        log("Usage: dmgmultraw <value> (e.g. 10)");
                    }
                    break;

                case "speed":
                    requirePlayer();
                    if (parts.length > 1) {
                        float amount = Float.parseFloat(parts[1]);
                        player.setSpeedMultiplier(amount);
                        log("Speed multiplier set to " + amount);
                    } else {
                        log("Usage: speed <value> (e.g. 1.5)");
                    }
                    break;

                case "heal":
                    requirePlayer();
                    player.addLife();
                    log("Player healed.");
                    break;

                case "xp":
                    requirePlayer();
                    if (parts.length > 1) {
                        int amount = Integer.parseInt(parts[1]);
                        player.addExperience(amount);
                        log("Gave " + amount + " XP");
                    } else {
                        log("Usage: xp <amount>");
                    }
                    break;

                case "level":
                    if (parts.length > 1) {
                        int lvl = Integer.parseInt(parts[1]);
                        FileHandle file = resolveLevelFile(lvl);

                        if (file != null && file.exists()) {
                            log("Loading level " + lvl + " from " + file.path());
                            game.setScreen(new GameScreen(game, lvl, file));
                        } else {
                            // If file missing: still open GameScreen with null file
                            // -> your GameScreen initializeMaze() can generate a local file fallback
                            log("Level file not found. Generating/loading fallback for level " + lvl + "...");
                            game.setScreen(new GameScreen(game, lvl, null));
                        }
                    } else {
                        log("Usage: level <number>");
                    }
                    break;

                case "help":
                    log("cmds: god, ungod, noclip, clip, key, gun, boost <n>, str <n>, strraw <n>, spd <n>, dmgmult <x>, dmgmultraw <x>, speed <x>, heal, xp <n>, level <n>, coins [n], setcoins <n>, bankcoins [n], setbankcoins <n>");
                    break;

                case "coins":
                    requirePlayer();
                    int amountCoins = (parts.length > 1) ? Integer.parseInt(parts[1]) : 1_000_000;
                    player.addCoins(amountCoins);
                    log("Gave player " + amountCoins + " coins. Player now has: " + player.getCoins());
                    break;

                case "setcoins":
                    requirePlayer();
                    if (parts.length > 1) {
                        int target = Integer.parseInt(parts[1]);
                        int delta = target - player.getCoins();
                        player.addCoins(delta);
                        log("Set player coins to " + target);
                    } else {
                        log("Usage: setcoins <amount>");
                    }
                    break;

                case "bankcoins":
                    int bankAmount = (parts.length > 1) ? Integer.parseInt(parts[1]) : 1_000_000;
                    game.getProgression().addCoins(bankAmount);
                    log("Added " + bankAmount + " coins to BANK. Bank now has: " + game.getProgression().getCoins());
                    break;

                case "setbankcoins":
                    if (parts.length > 1) {
                        int target = Integer.parseInt(parts[1]);
                        int current = game.getProgression().getCoins();
                        int delta = target - current;
                        if (delta >= 0) game.getProgression().addCoins(delta);
                        else game.getProgression().spendCoins(-delta);
                        log("Set BANK coins to " + target);
                    } else {
                        log("Usage: setbankcoins <amount>");
                    }
                    break;

                default:
                    log("Unknown command: " + command);
                    break;
            }
        } catch (Exception e) {
            log("Error executing command: " + e.getMessage());
        }
    }

    /**
     * Ensures a player instance is available for player-only commands.
     */
    private void requirePlayer() {
        if (player == null) throw new IllegalStateException("Player not set in DevConsole.");
    }

    /**
     * Looks for a level file in:
     *  1) internal/maps/level-X.properties (shipped maps)
     *  2) local/maps/level-X.properties    (generated/custom maps)
     *
     * @param lvl Level number to resolve
     * @return File handle to the level file, or null if not found
     */
    private FileHandle resolveLevelFile(int lvl) {
        String name = "maps/level-" + lvl + ".properties";

        FileHandle internal = Gdx.files.internal(name);
        if (internal.exists()) return internal;

        FileHandle local = Gdx.files.local(name);
        if (local.exists()) return local;

        return null;
    }

    /**
     * Writes a message to the console log and stdout.
     *
     * @param message Message to display
     */
    private void log(String message) {
        logLabel.setText(message);
        System.out.println("[DEV] " + message);
    }
}
