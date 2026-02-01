package de.tum.cit.fop.maze.player;

import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;
import de.tum.cit.fop.maze.enemies.Enemy;
import java.util.ArrayList;
import java.util.List;

/**
 * Represents a projectile fired by the player.
 *
 * Bullets move in a straight line and can optionally
 * pierce multiple enemies.
 */
public class Bullet {
    /** Current bullet position. */
    private Vector2 position;
    /** Bullet velocity in world units per second. */
    private Vector2 velocity;
    /** Whether the bullet is still active. */
    private boolean active = true;
    /** Bullet travel speed. */
    private static final float SPEED = 600f;
    /** Render size of the bullet. */
    private static final float SIZE = 8f;
    /** Enemies already hit by this bullet. */
    private List<Enemy> hitEnemies = new ArrayList<>();


    /**
     * Creates a new bullet flying towards a target position.
     *
     * @param startX starting X position
     * @param startY starting Y position
     * @param targetX target X position
     * @param targetY target Y position
     */
    public Bullet(float startX, float startY, float targetX, float targetY) {
        this.position = new Vector2(startX, startY);
        this.velocity = new Vector2(targetX - startX, targetY - startY);
        this.velocity.nor().scl(SPEED);
    }

    /**
     * Advances the bullet position based on its velocity.
     *
     * @param dt time step in seconds
     */
    public void update(float dt) {
        position.mulAdd(velocity, dt);
    }

    /**
     * Draws the bullet.
     *
     * @param batch sprite batch used for rendering
     * @param texture bullet texture
     */
    public void draw(SpriteBatch batch, TextureRegion texture) {
        batch.draw(texture, position.x, position.y, SIZE, SIZE);
    }

    /**
     * Computes the bullet's axis-aligned bounds.
     *
     * @return bounding rectangle of the bullet
     */
    public Rectangle getBounds() {
        return new Rectangle(position.x, position.y, SIZE, SIZE);
    }

    /**
     * Retrieves the bullet's X coordinate.
     *
     * @return X position in world units
     */
    public float getX() { return position.x; }

    /**
     * Retrieves the bullet's Y coordinate.
     *
     * @return Y position in world units
     */
    public float getY() { return position.y; }

    /**
     * Checks whether this bullet has already hit the given enemy.
     *
     * @param enemy enemy to test against
     * @return true if the enemy was already hit by this bullet
     */
    public boolean hasAlreadyHit(Enemy enemy) {
        return hitEnemies.contains(enemy);
    }

    /**
     * Records that the given enemy was hit by this bullet.
     *
     * @param enemy enemy that was hit
     */
    public void registerHit(Enemy enemy) {
        hitEnemies.add(enemy);
    }

    /**
     * Indicates whether the bullet is still active.
     *
     * @return true if active, false if destroyed
     */
    public boolean isAlive() { return active; }

    /**
     * Marks the bullet as inactive.
     */
    public void setDead() { active = false; }
}
