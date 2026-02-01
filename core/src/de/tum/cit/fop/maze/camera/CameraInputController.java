package de.tum.cit.fop.maze.camera;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.InputAdapter;

/**
 * Handles keyboard input for controlling the game camera.
 *
 * Supports continuous zooming while zoom keys are held down.
 */
public class CameraInputController extends InputAdapter {

    private final GameCamera gameCamera;
    private final de.tum.cit.fop.maze.util.GamePreferences preferences;

    private static final float ZOOM_HOLD_SPEED = 2.5f;

    /**
     * Creates a new camera input controller.
     *
     * @param gameCamera the camera to control
     * @param preferences game preferences containing key bindings
     */
    public CameraInputController(GameCamera gameCamera, de.tum.cit.fop.maze.util.GamePreferences preferences) {
        this.gameCamera = gameCamera;
        this.preferences = preferences;
    }

    /**
     * Updates camera zoom based on held keyboard input.
     *
     * Should be called once per frame.
     *
     * @param delta time since last frame (seconds)
     */
    public void update(float delta) {
        
        int zoomInKey = preferences.getKeyZoomIn();
        int zoomOutKey = preferences.getKeyZoomOut();

        if (Gdx.input.isKeyPressed(zoomInKey) || Gdx.input.isKeyPressed(Input.Keys.PLUS) || Gdx.input.isKeyPressed(Input.Keys.EQUALS)) {
            gameCamera.zoomInContinuous(ZOOM_HOLD_SPEED * delta);
        }
        if (Gdx.input.isKeyPressed(zoomOutKey) || Gdx.input.isKeyPressed(Input.Keys.MINUS)) {
            gameCamera.zoomOutContinuous(ZOOM_HOLD_SPEED * delta);
        }
    }
}
