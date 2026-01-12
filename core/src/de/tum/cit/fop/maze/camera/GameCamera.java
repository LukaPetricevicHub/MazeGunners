package de.tum.cit.fop.maze.camera;

import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.math.MathUtils;

public class GameCamera {

    private final OrthographicCamera camera;

    private float worldWidth;
    private float worldHeight;

    private static final float FOLLOW_SMOOTHNESS = 8f;

    public GameCamera(
            float viewportWidth,
            float viewportHeight,
            float worldWidth,
            float worldHeight
    ) {
        this.camera = new OrthographicCamera(viewportWidth, viewportHeight);
        this.camera.position.set(viewportWidth / 2f, viewportHeight / 2f, 0f);
        this.camera.update();

        this.worldWidth = worldWidth;
        this.worldHeight = worldHeight;
    }

    // Smoothly follow player ps
    public void follow(float targetX, float targetY, float deltaTime) {

        float alpha = 1f - (float) Math.exp(-FOLLOW_SMOOTHNESS * deltaTime);

        camera.position.x += (targetX - camera.position.x) * alpha;
        camera.position.y += (targetY - camera.position.y) * alpha;

        clampToWorld();
        camera.update();
    }

    private void clampToWorld() {
        float halfW = camera.viewportWidth * camera.zoom * 0.5f;
        float halfH = camera.viewportHeight * camera.zoom * 0.5f;

        camera.position.x = MathUtils.clamp(
                camera.position.x,
                halfW,
                worldWidth - halfW
        );

        camera.position.y = MathUtils.clamp(
                camera.position.y,
                halfH,
                worldHeight - halfH
        );
    }

    public void resize(float viewportWidth, float viewportHeight) {
        camera.viewportWidth = viewportWidth;
        camera.viewportHeight = viewportHeight;
        camera.update();
    }

    public void setZoom(float zoom) {
        camera.zoom = zoom;
        camera.update();
    }

    public OrthographicCamera getCamera() {
        return camera;
    }
}

