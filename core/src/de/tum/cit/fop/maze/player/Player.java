package de.tum.cit.fop.maze.player;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.g2d.Animation;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;

/**
 * Player entity: handles movement, lives, key possession, damage flash, and facing direction.
 */
public class Player {

    public enum Direction { UP, DOWN, LEFT, RIGHT }

    private float x;
    private float y;

    private int lives = 5;
    private boolean hasKey = false;

    // Damage / invulnerability
    private boolean invulnerable = false;
    private float invulnTimeLeft = 0f;

    // Flash (for VFX: renderer can tint red if isDamageFlashing() == true)
    private boolean damageFlash = false;
    private float flashTimeLeft = 0f;

    // Movement
    private final float walkSpeed = 220f; // pixels/sec
    private final float runMultiplier = 1.7f;

    // Movement state (NEW)
    private boolean moving = false;
    private Direction facing = Direction.DOWN;

    // Animation (UPDATED: 4-direction)
    private final Animation<TextureRegion> walkDownAnimation;
    private final Animation<TextureRegion> walkUpAnimation;
    private final Animation<TextureRegion> walkLeftAnimation;
    private final Animation<TextureRegion> walkRightAnimation;

    private float animTime = 0f;

    // Collision hook (NEW): maze system will implement this
    public interface CollisionMap {
        /**
         * @param worldX player's next x in world pixels
         * @param worldY player's next y in world pixels
         * @return true if player can stand/move there (not a wall)
         */
        boolean isWalkable(float worldX, float worldY);
    }

    private final CollisionMap collisionMap; // can be null for now

    public Player(
            float startX, float startY,
            Animation<TextureRegion> walkDownAnimation,
            Animation<TextureRegion> walkUpAnimation,
            Animation<TextureRegion> walkLeftAnimation,
            Animation<TextureRegion> walkRightAnimation,
            CollisionMap collisionMap // pass null if not ready
    ) {
        this.x = startX;
        this.y = startY;

        this.walkDownAnimation = walkDownAnimation;
        this.walkUpAnimation = walkUpAnimation;
        this.walkLeftAnimation = walkLeftAnimation;
        this.walkRightAnimation = walkRightAnimation;

        this.collisionMap = collisionMap;
    }

    public void update(float dt) {
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

        boolean left  = Gdx.input.isKeyPressed(Input.Keys.LEFT);
        boolean right = Gdx.input.isKeyPressed(Input.Keys.RIGHT);
        boolean up    = Gdx.input.isKeyPressed(Input.Keys.UP);
        boolean down  = Gdx.input.isKeyPressed(Input.Keys.DOWN);

        // Direction vector (NEW: diagonal normalization)
        float dirX = 0f;
        float dirY = 0f;

        if (left)  dirX -= 1f;
        if (right) dirX += 1f;
        if (up)    dirY += 1f;
        if (down)  dirY -= 1f;

        moving = (dirX != 0f || dirY != 0f);

        // update facing based on last intent (simple priority)
        if (moving) {
            // choose facing based on strongest axis or last pressed rule
            if (Math.abs(dirX) > Math.abs(dirY)) {
                facing = (dirX > 0) ? Direction.RIGHT : Direction.LEFT;
            } else {
                facing = (dirY > 0) ? Direction.UP : Direction.DOWN;
            }
        }

        // advance animation time only when moving (NEW)
        if (moving) {
            animTime += dt;
        } else {
            // optional: keep animTime as-is, or reset to 0 for a consistent idle frame
            // animTime = 0f;
        }

        // normalize to fix diagonal speed (NEW)
        if (moving) {
            float len = (float) Math.sqrt(dirX * dirX + dirY * dirY);
            dirX /= len;
            dirY /= len;

            float nextX = x + dirX * move;
            float nextY = y + dirY * move;

            // collision (NEW): if maze not ready, allow movement
            if (collisionMap == null || collisionMap.isWalkable(nextX, y)) {
                x = nextX;
            }
            if (collisionMap == null || collisionMap.isWalkable(x, nextY)) {
                y = nextY;
            }
        }
    }

    public void draw(SpriteBatch batch) {
        Animation<TextureRegion> anim = getCurrentAnimation();
        TextureRegion frame;

        if (moving) {
            frame = anim.getKeyFrame(animTime, true);
        } else {
            // idle = first frame of facing direction (simple)
            frame = anim.getKeyFrames()[0];
        }

        // NOTE: tint red in outer renderer if you want:
        // if (isDamageFlashing()) batch.setColor(1f, 0.3f, 0.3f, 1f);
        batch.draw(frame, x, y, 32, 64);
        // batch.setColor(1f,1f,1f,1f); // reset in renderer
    }

    private Animation<TextureRegion> getCurrentAnimation() {
        return switch (facing) {
            case UP -> walkUpAnimation;
            case DOWN -> walkDownAnimation;
            case LEFT -> walkLeftAnimation;
            case RIGHT -> walkRightAnimation;
        };
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
    public boolean isInvulnerable() { return invulnerable; }

    public float getX() { return x; }
    public float getY() { return y; }

    public Direction getFacing() { return facing; }
    public boolean isMoving() { return moving; }
}
