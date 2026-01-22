package de.tum.cit.fop.maze.player;

import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Rectangle;

public class Bullet {

    // Enum for direction
    public enum Dir { LEFT, RIGHT, UP, DOWN }

    private float x, y;
    private final Dir dir;
    private final float speed = 600f; // Speed of bullet
    private float lifeTime = 2.0f;    // Bullet disappears after 2 seconds
    private boolean alive = true;
    private final float width = 10f;
    private final float height = 10f;

    public Bullet(float x, float y, Dir dir) {
        this.x = x;
        this.y = y;
        this.dir = dir;
    }

    public void update(float dt) {
        switch (dir) {
            case LEFT -> x -= speed * dt;
            case RIGHT -> x += speed * dt;
            case UP -> y += speed * dt;
            case DOWN -> y -= speed * dt;
        }

        lifeTime -= dt;
        if (lifeTime <= 0) alive = false;
    }

    public void draw(SpriteBatch batch, TextureRegion region) {
        batch.draw(region, x, y, width, height);
    }

    public boolean isAlive() { return alive; }
    public Rectangle getBounds() { return new Rectangle(x, y, width, height); }
    public void setDead() { this.alive = false; }
}