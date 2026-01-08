package de.tum.cit.fop.maze.camera;

import com.badlogic.gdx.graphics.OrthographicCamera;

public class GameCamera {
    private final OrthographicCamera camera;

    private float worldWidth;
    private float worldHeight;

    public GameCamera(float viewportWidth, float viewportHeight, float worldWidth, float worldHeight) {
        this.camera = new OrthographicCamera(viewportWidth, viewportHeight);
        this.worldWidth = worldWidth;
        this.worldHeight = worldHeight;
    }

    public void follow(float targetX, float targetY, float lerp){
        camera.position.x += (targetX - camera.position.x) * lerp;
        camera.position.y += (targetY - camera.position.y) * lerp;
        clamp();
        camera.update();
    }

    private void clamp(){
        float halfW = camera.viewportWidth * camera.zoom / 2f;
        float halfH = camera.viewportHeight * camera.zoom / 2f;

        camera.position.x = Math.max(halfW, Math.min(camera.position.x, worldWidth - halfW));
        camera.position.y = Math.max(halfH, Math.min(camera.position.y, worldHeight - halfH));
    }

    public void resize(float width, float height){
        camera.viewportWidth = width;
        camera.viewportHeight = height;
        camera.update();
    }

    public OrthographicCamera getCamera() {
        return camera;
    }

    public void setZoom(float zoom){
        camera.zoom = Math.max(0.5f, Math.min(zoom, 2.5f));
    }
}



