package de.tum.cit.fop.maze.player;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.g2d.Animation;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;

/**
 * Player entity: handles movement, lives, key possession, and damage flash state.
 */
public class Player {

    private float x;
    private float y;

    private int lives = 5;
    private boolean hasKey = false;

    // Damage / invulnerability
    private boolean invulnerable = false;
    private float invulnTimeLeft = 0f;

    // Flash (for VFX: you can tint red in renderer later)
    private boolean damageFlash = false;
    private float flashTimeLeft = 0f;

    // Movement
    private final float walkSpeed = 220f; // pixels/sec
    private final float runMultiplier = 1.7f;

    // Animation (reuse your existing down animation for now)
    private final Animation<TextureRegion> walkDownAnimation;
    private float animTime = 0f;

    public Player(float startX, float startY, Animation<TextureRegion> walkDownAnimation) {
        this.x = startX;
        this.y = startY;
        this.walkDownAnimation = walkDownAnimation;
    }

    public void update(float dt) {
        animTime += dt;

        // timers
        if (invulnerable) {
            invulnTimeLeft -= dt;
            if (invulnTimeLeft <= 0f) invulnerable = false;
        }
        if (damageFlash) {
            flashTimeLeft -= dt;
            if (flashTimeLeft <= 0f) damageFlash = false;
        }

        // input
        boolean run = Gdx.input.isKeyPressed(Input.Keys.SHIFT_LEFT) || Gdx.input.isKeyPressed(Input.Keys.SHIFT_RIGHT);
        float speed = walkSpeed * (run ? runMultiplier : 1f);
        float move = speed * dt;

        if (Gdx.input.isKeyPressed(Input.Keys.LEFT))  x -= move;
        if (Gdx.input.isKeyPressed(Input.Keys.RIGHT)) x += move;
        if (Gdx.input.isKeyPressed(Input.Keys.UP))    y += move;
        if (Gdx.input.isKeyPressed(Input.Keys.DOWN))  y -= move;
    }

    public void draw(SpriteBatch batch) {
        TextureRegion frame = walkDownAnimation.getKeyFrame(animTime, true);
        // draw bigger so you can see it
        batch.draw(frame, x, y, 32, 64);
    }

    public void takeDamage(int amount) {
        if (invulnerable) return;

        lives = Math.max(0, lives - amount);

        invulnerable = true;
        invulnTimeLeft = 0.8f;

        damageFlash = true;
        flashTimeLeft = 0.25f;
    }

    public void giveKey() {
        hasKey = true;
    }

    // getters for HUD
    public int getLives() { return lives; }
    public boolean hasKey() { return hasKey; }
    public boolean isDamageFlashing() { return damageFlash; }

    public float getX() { return x; }
    public float getY() { return y; }
}
