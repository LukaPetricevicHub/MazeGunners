package de.tum.cit.fop.maze.player;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.g2d.Animation;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import de.tum.cit.fop.maze.MazeRunnerGame;
import de.tum.cit.fop.maze.map.Maze;

public class Player {

    // Attributes
    private float x;
    private float y;

    private int lives = 100;
    private boolean hasKey = false;

    private boolean invulnerable = false;
    private float invulnTimeLeft = 0f;

    private boolean damageFlash = false;
    private float flashTimeLeft = 0f;

    private static final float WALK_SPEED = 220f;
    private static final float RUN_MULTIPLIER = 1.7f;

    private static final float WIDTH = 32f;
    private static final float HEIGHT = 64f;

    private static final float COLLISION_INSET = 2f;

    private final Animation<TextureRegion> walkDownAnimation;
    private float animTime = 0f;


    // Constructor
    public Player(float startX, float startY, Animation<TextureRegion> walkDownAnimation) {
        this.x = startX;
        this.y = startY;
        this.walkDownAnimation = walkDownAnimation;
    }


    // Main Update
    public void update(float dt, Maze maze, MazeRunnerGame game) {
        animTime += dt;

        updateTimers(dt);

        MovementInput input = readMovementInput(dt);
        attemptMove(input.dx, input.dy, maze, game);
    }


    // Timers & State
    private void updateTimers(float dt) {
        if (invulnerable) {
            invulnTimeLeft -= dt;
            if (invulnTimeLeft <= 0f)
                invulnerable = false;

        }

        if (damageFlash) {
            flashTimeLeft -= dt;
            if (flashTimeLeft <= 0f)
                damageFlash = false;

        }
    }


    // Keyboard input
    private MovementInput readMovementInput(float dt) {
        boolean run = Gdx.input.isKeyPressed(Input.Keys.SHIFT_LEFT) || Gdx.input.isKeyPressed(Input.Keys.SHIFT_RIGHT);

        float speed = WALK_SPEED * (run ? RUN_MULTIPLIER : 1f);
        float move = speed * dt;

        float dx = 0f;
        float dy = 0f;

        if (Gdx.input.isKeyPressed(Input.Keys.LEFT))  dx -= move;
        if (Gdx.input.isKeyPressed(Input.Keys.RIGHT)) dx += move;
        if (Gdx.input.isKeyPressed(Input.Keys.UP))    dy += move;
        if (Gdx.input.isKeyPressed(Input.Keys.DOWN))  dy -= move;

        return new MovementInput(dx, dy);
    }


    // Collision & Movement
    private void attemptMove(float dx, float dy, Maze maze, MazeRunnerGame game) {

        boolean hitWallX = false;
        boolean hitWallY = false;

        if (dx != 0f) {
            float nextX = x + dx;

            if (canOccupy(nextX, y, maze)) {
                x = nextX;
            } else {
                hitWallX = true;
            }
        }

        if (dy != 0f) {
            float nextY = y + dy;

            if (canOccupy(x, nextY, maze))
                y = nextY;
            else
                hitWallY = true;

        }

        if (hitWallX || hitWallY) {
            game.playWallHitSound();
        }
    }

    private boolean canOccupy(float testX, float testY, Maze maze) {
        float left   = testX + COLLISION_INSET;
        float right  = testX + WIDTH - COLLISION_INSET;
        float bottom = testY + COLLISION_INSET;
        float top    = testY + 40.0f;

        return !isBlocked(left, bottom, maze)
                && !isBlocked(right, bottom, maze)
                && !isBlocked(left, top, maze)
                && !isBlocked(right, top, maze);
    }



    // Rendering
    public void draw(SpriteBatch batch) {
        TextureRegion frame = walkDownAnimation.getKeyFrame(animTime, true);
        batch.draw(frame, x, y, 52f, 52f);
    }


    // Player takes damage
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


    // Getters
    public int getLives() { return lives; }
    public boolean hasKey() { return hasKey; }
    public boolean isDamageFlashing() { return damageFlash; }

    public float getX() { return x; }
    public float getY() { return y; }


    // Internal helper
    private record MovementInput(float dx, float dy) {}
    private boolean isBlocked(float worldX, float worldY, Maze maze) {
        int cx = (int) (worldX / maze.getTileSize());
        int cy = (int) (worldY / maze.getTileSize());

        if (maze.isWallCell(cx, cy)) return true;
        if (maze.isBlockedExit(cx, cy, hasKey)) return true;

        return false;
    }
}
