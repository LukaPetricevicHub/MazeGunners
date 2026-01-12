package de.tum.cit.fop.maze;

public class GameStateController {

    public enum State {
        RUNNING,
        WON,
        LOST
    }

    private State state = State.RUNNING;
    private float timer = 0f;
    private static final float EXIT_DELAY = 2f;


    // Update game logic
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


    // When exactly do we return?
    public boolean shouldReturnToMenu() {
        return state != State.RUNNING && timer >= EXIT_DELAY;
    }


    // getter
    public State getState() {
        return state;
    }

    public boolean isRunning() {
        return state == State.RUNNING;
    }
}

