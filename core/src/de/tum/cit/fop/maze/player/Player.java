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

    private float x;
    private float y;

    private int lives = 1000;
    private boolean hasKey = false;

    private boolean invulnerable = false;
    private float invulnTimeLeft = 0f;

    private boolean damageFlash = false;
    private float flashTimeLeft = 0f;

    private static final float WALK_SPEED = 450f;
    private static final float RUN_MULTIPLIER = 1.7f;

    private float speedMultiplier = 1f;
    private float speedTimer = 0f;

    private static final float WIDTH = 32f;
    private static final float HEIGHT = 32f;

    private static final float COLLISION_INSET = 2f;

    private final Animation<TextureRegion> walkDownAnimation;
    private float animTime = 0f;

    private boolean isShielded = false;
    private float shieldTimer = 0f;

    private boolean godMode = false;
    private boolean noClip = false;

    private int experience = 0;
    private int playerLevel = 1;
    private int skillPoints = 0;

    private int skillSpeedLevel = 0;
    private int skillHealthLevel = 0;
    private int skillDamageLevel = 0;
    private final Rectangle interactionRect = new Rectangle();
    private static final float INTERACTION_MARGIN = 2f;
    private static final float INTERACTION_HEIGHT = 40f;

    private static final int XP_PER_LEVEL_BASE = 100;

    @FunctionalInterface
    public interface BlockChecker {
        boolean isBlocked(Rectangle playerRect);
    }

    public Player(float startX, float startY, Animation<TextureRegion> walkDownAnimation) {
        this.x = startX;
        this.y = startY;
        this.walkDownAnimation = walkDownAnimation;
    }

    public void update(float dt, Maze maze, MazeRunnerGame game, BlockChecker blockChecker) {
        if (speedTimer > 0) {
            speedTimer -= dt;
            if (speedTimer <= 0) {
                speedMultiplier = 1.0f;
            }
        }
        if (shieldTimer > 0) {
            shieldTimer -= dt;
            if (shieldTimer <= 0) {
                isShielded = false;
            }
        }

        animTime += dt;
        updateTimers(dt);

        MovementInput input = readMovementInput(dt);
        attemptMove(input.dx, input.dy, maze, game, blockChecker);
    }

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

    private MovementInput readMovementInput(float dt) {
        int keyUp = Gdx.app.getPreferences("maze_runner_prefs").getInteger("key_up", Input.Keys.UP);
        int keyDown = Gdx.app.getPreferences("maze_runner_prefs").getInteger("key_down", Input.Keys.DOWN);
        int keyLeft = Gdx.app.getPreferences("maze_runner_prefs").getInteger("key_left", Input.Keys.LEFT);
        int keyRight = Gdx.app.getPreferences("maze_runner_prefs").getInteger("key_right", Input.Keys.RIGHT);

        boolean run = Gdx.input.isKeyPressed(Input.Keys.SHIFT_LEFT)
                || Gdx.input.isKeyPressed(Input.Keys.SHIFT_RIGHT);
        float speed = WALK_SPEED * (run ? RUN_MULTIPLIER : 1f) * speedMultiplier;
        float move = speed * dt;

        float dx = 0f;
        float dy = 0f;

        if (Gdx.input.isKeyPressed(keyLeft)) dx -= move;
        if (Gdx.input.isKeyPressed(keyRight)) dx += move;
        if (Gdx.input.isKeyPressed(keyUp)) dy += move;
        if (Gdx.input.isKeyPressed(keyDown)) dy -= move;

        return new MovementInput(dx, dy);
    }

    public void addExperience(int amount) {
        this.experience += amount;
        checkLevelUp();
    }

    private void checkLevelUp() {
        int xpNeeded = playerLevel * XP_PER_LEVEL_BASE;

        while (experience >= xpNeeded) {
            experience -= xpNeeded;
            playerLevel++;
            skillPoints++;
            lives = Math.min(lives + 1, 5);
            xpNeeded = playerLevel * XP_PER_LEVEL_BASE;
        }
    }

    public boolean upgradeSpeed() {
        if (skillPoints > 0 && skillSpeedLevel < 5) {
            skillPoints--;
            skillSpeedLevel++;
            return true;
        }
        return false;
    }

    public boolean upgradeHealth() {
        if (skillPoints > 0 && skillHealthLevel < 5) {
            skillPoints--;
            skillHealthLevel++;
            if (lives < getMaxLives()) lives++;
            return true;
        }
        return false;
    }

    private void attemptMove(float dx, float dy, Maze maze, MazeRunnerGame game, BlockChecker blockChecker) {
        boolean hitWallX = false;
        boolean hitWallY = false;

        if (dx != 0f) {
            float nextX = x + dx;
            if (canOccupy(nextX, y, maze)) {
                if (blockChecker == null || !blockChecker.isBlocked(makeRect(nextX, y))) {
                    x = nextX;
                } else {
                    hitWallX = true;
                }
            } else {
                hitWallX = true;
            }
        }

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
        if (noClip) return true;

        float left = testX + COLLISION_INSET;
        float right = testX + WIDTH - COLLISION_INSET;
        float bottom = testY + COLLISION_INSET;
        float top = testY + 40.0f;

        return !isBlocked(left, bottom, maze)
                && !isBlocked(right, bottom, maze)
                && !isBlocked(left, top, maze)
                && !isBlocked(right, top, maze);
    }

    public void addLife() {
        if (lives < getMaxLives()) {
            lives++;
        }
    }

    public void activateSpeedBoost(float duration, float multiplier) {
        this.speedTimer = duration;
        this.speedMultiplier = multiplier;
    }

    public void activateShield(float duration) {
        this.shieldTimer = duration;
        this.isShielded = true;
    }

    public boolean isShielded() {
        return isShielded;
    }

    public void draw(SpriteBatch batch) {
        TextureRegion frame = walkDownAnimation.getKeyFrame(animTime, true);
        com.badlogic.gdx.graphics.Color oldColor = batch.getColor();

        if (isShielded) {
            batch.setColor(0.5f, 0.5f, 1f, 1f);
        } else if (damageFlash) {
            batch.setColor(1f, 0f, 0f, 1f);
        }

        batch.draw(frame, x, y, 52f, 52f);
        batch.setColor(oldColor);
    }

    public void takeDamage(int amount) {
        if (godMode) return;
        if (isShielded) return;
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

    public int getLives() { return lives; }
    public boolean hasKey() { return hasKey; }
    public boolean isDamageFlashing() { return damageFlash; }

    public float getX() { return x; }
    public float getY() { return y; }

    public void setGodMode(boolean enabled) {
        this.godMode = enabled;
    }

    public void setNoClip(boolean enabled) {
        this.noClip = enabled;
    }

    public Rectangle getInteractionRect() {
        interactionRect.set(
                x - INTERACTION_MARGIN,
                y - INTERACTION_MARGIN,
                WIDTH + INTERACTION_MARGIN * 2,
                INTERACTION_HEIGHT + INTERACTION_MARGIN * 2
        );
        return interactionRect;
    }

    public int getMaxLives() {
        return 3 + skillHealthLevel;
    }

    public float getWalkSpeed() {
        return WALK_SPEED + (skillSpeedLevel * 50f);
    }

    public int getExperience() { return experience; }
    public int getPlayerLevel() { return playerLevel; }
    public int getSkillPoints() { return skillPoints; }
    public int getXpToNextLevel() { return playerLevel * XP_PER_LEVEL_BASE; }

    private record MovementInput(float dx, float dy) {}

    private boolean isBlocked(float worldX, float worldY, Maze maze) {
        int cx = (int) (worldX / maze.getTileSize());
        int cy = (int) (worldY / maze.getTileSize());

        if (maze.isWallCell(cx, cy)) return true;
        if (maze.isBlockedExit(cx, cy, hasKey)) return true;

        return false;
    }

    private Rectangle makeRect(float px, float py) {
        return new Rectangle(px, py, WIDTH, HEIGHT);
    }
}

