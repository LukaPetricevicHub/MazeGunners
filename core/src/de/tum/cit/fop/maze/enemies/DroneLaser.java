package de.tum.cit.fop.maze.enemies;

import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;

/**
 * Simple projectile fired by a FlyingDrone.
 *
 * Moves in a straight line toward the player and disappears
 * after a short lifetime or when killed.
 */

public class DroneLaser {

    /** Current laser position. */
    private final Vector2 position;
    /** Laser velocity in world units per second. */
    private final Vector2 velocity;
    /** Whether the laser is still alive. */
    private boolean alive = true;
    /** Remaining lifetime in seconds. */
    private float lifeTime = 1.5f;

    /** Laser travel speed. */
    private static final float SPEED = 450f;
    /** Laser collision size. */
    private static final float SIZE = 4f;

    /**
     * Creates a laser projectile moving from a start position
     * toward a target position.
     *
     * @param startX laser start x position
     * @param startY laser start y position
     * @param targetX target x position
     * @param targetY target y position
     */
    public DroneLaser(float startX, float startY, float targetX, float targetY) {
        this.position = new Vector2(startX, startY);
        this.velocity = new Vector2(targetX - startX, targetY - startY).nor().scl(SPEED);
    }

    /**
     * Updates the laser position and lifetime.
     *
     * @param dt time passed since last frame
     */
    public void update(float dt) {
        position.mulAdd(velocity, dt);
        lifeTime -= dt;
        if(lifeTime <= 0f)
            alive = false;

    }


    /**
     * Returns the collision bounds of the laser.
     *
     * @return rectangle used for collision checks
     */
    public Rectangle getBounds() {
        return new Rectangle(position.x, position.y, SIZE, SIZE);
    }

    /**
     * Checks whether the laser is alive.
     *
     * @return True if alive
     */
    public boolean isAlive() {
        return alive;
    }

    /**
     * Marks the laser as dead.
     */
    public void kill() {
        alive = false;
    }

    /**
     * Retrieves the current laser position.
     *
     * @return Position vector
     */
    public Vector2 getPosition() {
        return position;
    }
}

