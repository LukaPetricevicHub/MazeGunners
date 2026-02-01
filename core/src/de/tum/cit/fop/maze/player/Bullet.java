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
    private Vector2 position;
    private Vector2 velocity;
    private boolean active = true;
    private static final float SPEED = 600f;
    private static final float SIZE = 8f;
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

    public Rectangle getBounds() {
        return new Rectangle(position.x, position.y, SIZE, SIZE);
    }

    public float getX() { return position.x; }
    public float getY() { return position.y; }

    public boolean hasAlreadyHit(Enemy enemy) {
        return hitEnemies.contains(enemy);
    }

    public void registerHit(Enemy enemy) {
        hitEnemies.add(enemy);
    }

    public boolean isAlive() { return active; }
    public void setDead() { active = false; }
}
