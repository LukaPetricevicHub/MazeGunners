package de.tum.cit.fop.maze.enemies;

import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;

public class DroneLaser {

    private final Vector2 position;
    private final Vector2 velocity;
    private boolean alive = true;
    private float lifeTime = 1.5f;

    private static final float SPEED = 450f;
    private static final float SIZE = 4f;

    public DroneLaser(float startX, float startY, float targetX, float targetY) {
        this.position = new Vector2(startX, startY);
        this.velocity = new Vector2(targetX - startX, targetY - startY).nor().scl(SPEED);
    }

    public void update(float dt) {
        position.mulAdd(velocity, dt);
        lifeTime -= dt;
        if(lifeTime <= 0f)
            alive = false;

    }


    public Rectangle getBounds() {
        return new Rectangle(position.x, position.y, SIZE, SIZE);
    }

    public boolean isAlive() {
        return alive;
    }

    public void kill() {
        alive = false;
    }

    public Vector2 getPosition() {
        return position;
    }
}

