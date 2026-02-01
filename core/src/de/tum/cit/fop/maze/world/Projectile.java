package de.tum.cit.fop.maze.world;

import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;

/**
 * Simple projectile with position, velocity, and texture.
 */
public class Projectile {
    /** Current projectile position. */
    private Vector2 position;
    /** Projectile velocity in world units per second. */
    private Vector2 velocity;
    /** Texture used to render the projectile. */
    private Texture texture;
    /** Whether the projectile is still active. */
    private boolean active = true;
    /** Movement speed of the projectile. */
    private static final float SPEED = 400f;
    /** Render size of the projectile. */
    private static final float SIZE = 16f;

    /**
     * Creates a projectile at the given position and direction.
     *
     * @param x World x-coordinate
     * @param y World y-coordinate
     * @param direction Direction vector
     * @param texture Texture used for rendering
     */
    public Projectile(float x, float y, Vector2 direction, Texture texture) {
        this.position = new Vector2(x, y);
        this.texture = texture;
        this.velocity = new Vector2(direction).nor().scl(SPEED);
    }

    /**
     * Updates the projectile position based on velocity.
     *
     * @param dt Delta time in seconds
     */
    public void update(float dt) {
        position.mulAdd(velocity, dt);
    }

    /**
     * Renders the projectile.
     *
     * @param batch Sprite batch used for drawing
     */
    public void render(SpriteBatch batch) {
        batch.draw(texture, position.x, position.y, SIZE, SIZE);
    }

    /**
     * Retrieves the projectile bounding rectangle.
     *
     * @return Bounds rectangle
     */
    public Rectangle getBounds() {
        return new Rectangle(position.x, position.y, SIZE, SIZE);
    }

    /**
     * Checks whether the projectile is active.
     *
     * @return True if active
     */
    public boolean isActive() { return active; }
    /**
     * Marks the projectile as inactive.
     */
    public void deactivate() { this.active = false; }
}
