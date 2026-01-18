package de.tum.cit.fop.maze.camera;

import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.math.MathUtils;

public class GameCamera {

    private static final float FOLLOW_SMOOTHNESS = 8f;

    private static final float MIN_ZOOM = 0.5f;
    private static final float MAX_ZOOM = 2.5f;

    private static final float ZOOM_SMOOTHNESS = 12f;

    private final OrthographicCamera camera;

    private final float worldWidth;
    private final float worldHeight;

    private float baseZoom = 1f;

    private float zoomMultiplier = 1f;
    private float targetZoomMultiplier = 1f;

    public GameCamera(float viewportWidth, float viewportHeight, float worldWidth, float worldHeight) {
        this.camera = new OrthographicCamera(viewportWidth, viewportHeight);
        this.camera.position.set(viewportWidth / 2f, viewportHeight / 2f, 0f);
        this.camera.update();

        this.worldWidth = worldWidth;
        this.worldHeight = worldHeight;

        this.baseZoom = camera.zoom;
    }

    /** Move camera towards target (NO camera.update here). */
    public void follow(float targetX, float targetY, float deltaTime) {
        // Clamp target to avoid jitter at edges
        float halfW = camera.viewportWidth * camera.zoom * 0.5f;
        float halfH = camera.viewportHeight * camera.zoom * 0.5f;

        float clampedX;
        float clampedY;

        if (halfW * 2f >= worldWidth) clampedX = worldWidth / 2f;
        else clampedX = MathUtils.clamp(targetX, halfW, worldWidth - halfW);

        if (halfH * 2f >= worldHeight) clampedY = worldHeight / 2f;
        else clampedY = MathUtils.clamp(targetY, halfH, worldHeight - halfH);

        float alpha = 1f - (float) Math.exp(-FOLLOW_SMOOTHNESS * deltaTime);
        camera.position.x += (clampedX - camera.position.x) * alpha;
        camera.position.y += (clampedY - camera.position.y) * alpha;
    }

    /** Call once per frame AFTER follow() */
    public void update(float deltaTime) {
        float alpha = 1f - (float) Math.exp(-ZOOM_SMOOTHNESS * deltaTime);

        zoomMultiplier += (targetZoomMultiplier - zoomMultiplier) * alpha;
        camera.zoom = baseZoom * zoomMultiplier;

        clampToWorld();
        camera.update();
    }

    /** Continuous zoom changes (good for holding keys). amount is "multiplier delta". */
    public void zoomInContinuous(float amount) {
        targetZoomMultiplier = MathUtils.clamp(targetZoomMultiplier - amount, MIN_ZOOM, MAX_ZOOM);
    }

    public void zoomOutContinuous(float amount) {
        targetZoomMultiplier = MathUtils.clamp(targetZoomMultiplier + amount, MIN_ZOOM, MAX_ZOOM);
    }

    /** Optional: step zoom if you still want press-to-zoom. */
    public void zoomInStep(float step) {
        zoomInContinuous(step);
    }

    public void zoomOutStep(float step) {
        zoomOutContinuous(step);
    }

    public void setZoom(float zoom) {
        baseZoom = zoom;
        camera.zoom = baseZoom * zoomMultiplier;
        clampToWorld();
        camera.update();
    }

    public void resize(float viewportWidth, float viewportHeight) {
        camera.viewportWidth = viewportWidth;
        camera.viewportHeight = viewportHeight;
        clampToWorld();
        camera.update();
    }

    private void clampToWorld() {
        float halfW = camera.viewportWidth * camera.zoom * 0.5f;
        float halfH = camera.viewportHeight * camera.zoom * 0.5f;

        if (halfW * 2f >= worldWidth) camera.position.x = worldWidth / 2f;
        else camera.position.x = MathUtils.clamp(camera.position.x, halfW, worldWidth - halfW);

        if (halfH * 2f >= worldHeight) camera.position.y = worldHeight / 2f;
        else camera.position.y = MathUtils.clamp(camera.position.y, halfH, worldHeight - halfH);
    }

    public OrthographicCamera getCamera() {
        return camera;
    }

    public void setZoomMultiplier(float visibilityFactor) {
    }
}
