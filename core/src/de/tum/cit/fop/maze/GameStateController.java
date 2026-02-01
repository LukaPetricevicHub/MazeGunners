package de.tum.cit.fop.maze;

/**
 * Controls the current game state and transitions between running,
 * win, and loss conditions.
 *
 * This class checks whether the player has won or lost the game
 * and delays the transition back to the menu.
 */
public class GameStateController {

    /**
     * Possible gameplay states for the current run.
     */
    public enum State {
        RUNNING,
        WON,
        LOST
    }

    // Attributes
    private State state = State.RUNNING;
    private float timer = 0f;
    private static final float EXIT_DELAY = 2f;


    /**
     * Updates the current game state based on player conditions.
     *
     * @param delta time passed since last frame
     * @param onExit whether the player is standing on the exit tile
     * @param hasKey whether the player has collected the key
     * @param lives current number of player lives
     */
    public void update(float delta, boolean onExit, boolean hasKey, int lives) {
        if (state == State.RUNNING) {
            if(onExit && hasKey) {
                state = State.WON;
                timer = 0f;
            }
            else if (lives <= 0) {
                state = State.LOST;
                timer = 0f;
            }
        }else {
            timer += delta;
        }
    }


    /**
     * Checks whether the game should return to the main menu.
     *
     * @return true if the game has ended and the exit delay has passed
     */
    public boolean shouldReturnToMenu() {
        return state != State.RUNNING && timer >= EXIT_DELAY;
    }


    /**
     * Retrieves the current state.
     *
     * @return current game state
     */
    public State getState() {
        return state;
    }

    /**
     * Indicates whether the game is currently running.
     *
     * @return true if running, otherwise false
     */
    public boolean isRunning() {
        return state == State.RUNNING;
    }
}

