package de.tum.cit.fop.maze.camera;

import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.math.MathUtils;

public class GameCamera {

    // Attributes
    private final OrthographicCamera camera;
    private float worldWidth;
    private float worldHeight;
    private float baseZoom = 1f;
    private float zoomMultiplier = 1f;
    private static final float FOLLOW_SMOOTHNESS = 8f;


    // Constructor
    public GameCamera(float viewportWidth, float viewportHeight, float worldWidth, float worldHeight) {
        this.camera = new OrthographicCamera(viewportWidth, viewportHeight);
        this.camera.position.set(viewportWidth / 2f, viewportHeight / 2f, 0f);
        this.camera.update();
        this.worldWidth = worldWidth;
        this.worldHeight = worldHeight;
        this.baseZoom = camera.zoom;
    }


    // Camera follows the player position
    public void follow(float targetX, float targetY, float deltaTime) {
        float alpha = 1f - (float) Math.exp(-FOLLOW_SMOOTHNESS * deltaTime);
        camera.position.x += (targetX - camera.position.x) * alpha;
        camera.position.y += (targetY - camera.position.y) * alpha;
        clampToWorld();
        camera.update();
    }


    // Keep camera inside world bounds
    private void clampToWorld() {
        float halfW = camera.viewportWidth * camera.zoom * 0.5f;
        float halfH = camera.viewportHeight * camera.zoom * 0.5f;
        camera.position.x = MathUtils.clamp(camera.position.x, halfW, worldWidth - halfW);
        camera.position.y = MathUtils.clamp(camera.position.y, halfH, worldHeight - halfH);
    }


    // Update camera on window resize
    public void resize(float viewportWidth, float viewportHeight) {
        camera.viewportWidth = viewportWidth;
        camera.viewportHeight = viewportHeight;
        camera.update();
    }


    // Set the base zoom level
    public void setZoom(float zoom) {
        baseZoom = zoom;
        camera.zoom = baseZoom * zoomMultiplier;
        camera.update();
    }


    // Apply a temporary zoom multiplier
    public void setZoomMultiplier(float multiplier) {
        zoomMultiplier = multiplier;
        camera.zoom = baseZoom * zoomMultiplier;
        camera.update();
    }


    // Reset zoom to default state
    public void resetZoom() {
        zoomMultiplier = 1f;
        camera.zoom = baseZoom;
        camera.update();
    }


    // Return the internal camera object
    public OrthographicCamera getCamera() {
        return camera;
    }
}