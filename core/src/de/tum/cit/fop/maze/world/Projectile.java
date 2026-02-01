package de.tum.cit.fop.maze.world;

import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;

public class Projectile {
    private Vector2 position;
    private Vector2 velocity;
    private Texture texture;
    private boolean active = true;
    private static final float SPEED = 400f;
    private static final float SIZE = 16f;

    public Projectile(float x, float y, Vector2 direction, Texture texture) {
        this.position = new Vector2(x, y);
        this.texture = texture;
        this.velocity = new Vector2(direction).nor().scl(SPEED);
    }

    public void update(float dt) {
        position.mulAdd(velocity, dt);
    }

    public void render(SpriteBatch batch) {
        batch.draw(texture, position.x, position.y, SIZE, SIZE);
    }

    public Rectangle getBounds() {
        return new Rectangle(position.x, position.y, SIZE, SIZE);
    }

    public boolean isActive() { return active; }
    public void deactivate() { this.active = false; }
}