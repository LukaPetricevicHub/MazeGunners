package de.tum.cit.fop.maze.player;

import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;
import de.tum.cit.fop.maze.enemies.Enemy;
import java.util.ArrayList;
import java.util.List;

public class Bullet {
    private Vector2 position;
    private Vector2 velocity;
    private boolean active = true;
    private static final float SPEED = 600f;
    private static final float SIZE = 8f;

    // Track enemies we already pierced so we don't hit them twice ---
    private List<Enemy> hitEnemies = new ArrayList<>();

    public Bullet(float startX, float startY, float targetX, float targetY) {
        this.position = new Vector2(startX, startY);
        this.velocity = new Vector2(targetX - startX, targetY - startY);
        this.velocity.nor().scl(SPEED);
    }

    public void update(float dt) {
        position.mulAdd(velocity, dt);
    }

    public void draw(SpriteBatch batch, TextureRegion texture) {
        batch.draw(texture, position.x, position.y, SIZE, SIZE);
    }

    public Rectangle getBounds() {
        return new Rectangle(position.x, position.y, SIZE, SIZE);
    }

    public float getX() { return position.x; }
    public float getY() { return position.y; }

    // --- NEW HELPERS FOR PIERCING ---
    public boolean hasAlreadyHit(Enemy enemy) {
        return hitEnemies.contains(enemy);
    }

    public void registerHit(Enemy enemy) {
        hitEnemies.add(enemy);
    }

    public boolean isAlive() { return active; }
    public void setDead() { active = false; }
}
