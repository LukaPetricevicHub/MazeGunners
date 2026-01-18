package de.tum.cit.fop.maze.camera;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.InputAdapter;

public class CameraInputController extends InputAdapter {

    private final GameCamera gameCamera;

    // speed: "steps per second" while holding keys
    private static final float ZOOM_HOLD_SPEED = 2.5f;

    public CameraInputController(GameCamera gameCamera) {
        this.gameCamera = gameCamera;
    }

    /** Call once per frame from GameScreen.render(delta). */
    public void update(float delta) {
        // Hold E to zoom in, hold Q to zoom out
        if (Gdx.input.isKeyPressed(Input.Keys.E) || Gdx.input.isKeyPressed(Input.Keys.PLUS) || Gdx.input.isKeyPressed(Input.Keys.EQUALS)) {
            gameCamera.zoomInContinuous(ZOOM_HOLD_SPEED * delta);
        }
        if (Gdx.input.isKeyPressed(Input.Keys.Q) || Gdx.input.isKeyPressed(Input.Keys.MINUS)) {
            gameCamera.zoomOutContinuous(ZOOM_HOLD_SPEED * delta);
        }
    }
}
