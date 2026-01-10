package de.tum.cit.fop.maze.player;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.g2d.Animation;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import de.tum.cit.fop.maze.MazeRunnerGame;
import de.tum.cit.fop.maze.level.Maze;

public class Player {

    // Attributes
    private float x;
    private float y;

    private int lives = 5;
    private boolean hasKey = false;

    private boolean invulnerable = false;
    private float invulnTimeLeft = 0f;

    private boolean damageFlash = false;
    private float flashTimeLeft = 0f;

    private final float walkSpeed = 220f;
    private final float runMultiplier = 1.7f;

    private final Animation<TextureRegion> walkDownAnimation;
    private float animTime = 0f;


    // Constructor
    public Player(float startX, float startY, Animation<TextureRegion> walkDownAnimation) {
        this.x = startX;
        this.y = startY;
        this.walkDownAnimation = walkDownAnimation;
    }


    // Update player logic and movement
    public void update(float dt, Maze maze, MazeRunnerGame game) {
        animTime += dt;

        if (invulnerable) {
            invulnTimeLeft -= dt;
            if (invulnTimeLeft <= 0f) invulnerable = false;
        }

        if (damageFlash) {
            flashTimeLeft -= dt;
            if(flashTimeLeft <= 0f) damageFlash = false;
        }

        boolean run = Gdx.input.isKeyPressed(Input.Keys.SHIFT_LEFT) || Gdx.input.isKeyPressed(Input.Keys.SHIFT_RIGHT);
        float speed = walkSpeed * (run ? runMultiplier : 1f);
        float move = speed * dt;

        float dx = 0f;
        float dy = 0f;

        if (Gdx.input.isKeyPressed(Input.Keys.LEFT))  dx -= move;
        if (Gdx.input.isKeyPressed(Input.Keys.RIGHT)) dx += move;
        if (Gdx.input.isKeyPressed(Input.Keys.UP))    dy += move;
        if (Gdx.input.isKeyPressed(Input.Keys.DOWN))  dy -= move;

        float nextX = x + dx;
        float nextY = y + dy;

        float w = 16f;
        float h = 32f;
        float inset = 2f;

        float left   = nextX + inset;
        float right  = nextX + w - inset;
        float bottom = y + inset;
        float top    = y + h - inset;

        if (!maze.isWallWorld(left, bottom) && !maze.isWallWorld(right, bottom) && !maze.isWallWorld(left, top) && !maze.isWallWorld(right, top)) {
            x = nextX;
        }

        left   = x + inset;
        right  = x + w - inset;
        bottom = nextY + inset;
        top    = nextY + h - inset;

        if (!maze.isWallWorld(left, bottom) && !maze.isWallWorld(right, bottom) && !maze.isWallWorld(left, top) && !maze.isWallWorld(right, top)) {
            y = nextY;
        }

        boolean blocked = false;

        if (dx != 0f) {
            float testX = x + dx;
            if (!maze.isWallWorld(testX, y))
                x = testX;
            else
                blocked = true;

        }

        if(dy != 0f) {
            float testY = y + dy;
            if (!maze.isWallWorld(x, testY)) {
                y = testY;
            }
        }

        if (blocked) {
            game.playWallHitSound();
        }
    }


    // Draw player sprite
    public void draw(SpriteBatch batch) {
        TextureRegion frame = walkDownAnimation.getKeyFrame(animTime, true);
        batch.draw(frame, x, y, 32, 64);
    }


    // Apply damage and triggers effects
    public void takeDamage(int amount) {
        if (invulnerable) return;

        lives = Math.max(0, lives - amount);

        invulnerable = true;
        invulnTimeLeft = 0.8f;

        damageFlash = true;
        flashTimeLeft = 0.25f;
    }


    // Set player position
    public void setPosition(float x, float y) {
        this.x = x;
    }


    // Grant exit key
    public void giveKey() {
        hasKey = true;
    }


    // HUD getters
    public int getLives() { return lives; }
    public boolean hasKey() { return hasKey; }
    public boolean isDamageFlashing() { return damageFlash; }

    public float getX() { return x; }
    public float getY() { return y; }
}

