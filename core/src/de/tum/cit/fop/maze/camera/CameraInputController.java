package de.tum.cit.fop.maze.camera;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.InputAdapter;

public class CameraInputController extends InputAdapter {

    private final GameCamera gameCamera;
    private final de.tum.cit.fop.maze.util.GamePreferences preferences;

    // speed: "steps per second" while holding keys
    private static final float ZOOM_HOLD_SPEED = 2.5f;

    public CameraInputController(GameCamera gameCamera, de.tum.cit.fop.maze.util.GamePreferences preferences) {
        this.gameCamera = gameCamera;
        this.preferences = preferences;
    }

    /** Call once per frame from GameScreen.render(delta). */
    public void update(float delta) {
        // Hold key to zoom in/out
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
