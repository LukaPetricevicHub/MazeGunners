package de.tum.cit.fop.maze.player;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.g2d.Animation;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Rectangle;
import de.tum.cit.fop.maze.MazeRunnerGame;
import de.tum.cit.fop.maze.map.Maze;

public class Player {

    // Attributes
    private float x;
    private float y;

    private int lives = 1000;
    private boolean hasKey = false;

    private boolean invulnerable = false;
    private float invulnTimeLeft = 0f;

    private boolean damageFlash = false;
    private float flashTimeLeft = 0f;

    private static final float WALK_SPEED = 750f;
    private static final float RUN_MULTIPLIER = 1.7f;

    private float speedMultiplier = 1f;

    private static final float WIDTH = 32f;
    private static final float HEIGHT = 64f;

    private static final float COLLISION_INSET = 2f;

    private final Animation<TextureRegion> walkDownAnimation;
    private float animTime = 0f;


    @FunctionalInterface
    public interface BlockChecker {
        boolean isBlocked(Rectangle playerRect);
    }


    // Constructor
    public Player(float startX, float startY, Animation<TextureRegion> walkDownAnimation) {
        this.x = startX;
        this.y = startY;
        this.walkDownAnimation = walkDownAnimation;
    }


    // Main Update
    public void update(float dt, Maze maze, MazeRunnerGame game, BlockChecker blockChecker) {

        animTime += dt;

        updateTimers(dt);

        MovementInput input = readMovementInput(dt);
     attemptMove(input.dx, input.dy, maze, game, blockChecker);

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
        boolean run = Gdx.input.isKeyPressed(Input.Keys.SHIFT_LEFT)
                || Gdx.input.isKeyPressed(Input.Keys.SHIFT_RIGHT);

        float speed = WALK_SPEED * (run ? RUN_MULTIPLIER : 1f) * speedMultiplier;
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
    private void attemptMove(float dx, float dy, Maze maze, MazeRunnerGame game, BlockChecker blockChecker) {

        boolean hitWallX = false;
        boolean hitWallY = false;

        // --- move X ---
        if (dx != 0f) {
            float nextX = x + dx;

            // first check tiles (walls/exit)
            if (canOccupy(nextX, y, maze)) {

                // then check blocking obstacles (props)
                if (blockChecker == null || !blockChecker.isBlocked(makeRect(nextX, y))) {
                    x = nextX;
                } else {
                    hitWallX = true;
                }

            } else {
                hitWallX = true;
            }
        }

        // --- move Y ---
        if (dy != 0f) {
            float nextY = y + dy;

            if (canOccupy(x, nextY, maze)) {

                if (blockChecker == null || !blockChecker.isBlocked(makeRect(x, nextY))) {
                    y = nextY;
                } else {
                    hitWallY = true;
                }

            } else {
                hitWallY = true;
            }
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


    public void setSpeedMultiplier(float multiplier) {
        speedMultiplier = multiplier;
    }

    public void resetSpeedMultiplier() {
        speedMultiplier = 1f;
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

    private Rectangle makeRect(float px, float py) {
        // Use SAME collision "top" you used in canOccupy (40px tall)
        // so the rectangle matches your actual collision height.
        return new Rectangle(px, py, WIDTH, 40.0f);
    }

}
