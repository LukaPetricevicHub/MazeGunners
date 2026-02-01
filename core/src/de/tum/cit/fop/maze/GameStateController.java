package de.tum.cit.fop.maze;

/**
 * Controls the current game state and transitions between running,
 * win, and loss conditions.
 *
 * This class checks whether the player has won or lost the game
 * and delays the transition back to the menu.
 */
public class GameStateController {

    public enum State {
        RUNNING,
        WON,
        LOST
    }

    
    private State state = State.RUNNING;
    private float timer = 0f;
    private static final float EXIT_DELAY = 2f;


    /**
     * Updates the current game state based on player conditions.
     *
     * @param delta   Time passed since last frame
     * @param onExit  Whether the player is standing on the exit tile
     * @param hasKey  Whether the player has collected the key
     * @param lives   Current number of player lives
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


    
    public State getState() {
        return state;
    }

    public boolean isRunning() {
        return state == State.RUNNING;
    }
}

