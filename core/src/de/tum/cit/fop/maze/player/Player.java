package de.tum.cit.fop.maze.player;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.g2d.Animation;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;
import de.tum.cit.fop.maze.MazeRunnerGame;
import de.tum.cit.fop.maze.map.Maze;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class Player {

    // --- ATTRIBUTES ---
    private float x;
    private float y;

    // Stats
    private int lives;
    private static final int BASE_LIVES = 3;
    private boolean hasKey = false;
    private int score = 0;

    // Status Effects
    private boolean invulnerable = false;
    private float invulnTimeLeft = 0f;
    private boolean damageFlash = false;
    private float flashTimeLeft = 0f;

    // Movement
    private static final float WALK_SPEED_BASE = 450f;
    private static final float RUN_MULTIPLIER = 1.7f;
    private float speedMultiplier = 1f;
    private float speedTimer = 0f;

    // Dimensions
    private static final float WIDTH = 32f;
    private static final float HEIGHT = 32f;
    private static final float COLLISION_INSET = 2f;

    // Animation - Walking
    private final Animation<TextureRegion> animDown, animUp, animLeft, animRight;
    private Animation<TextureRegion> currentAnimation;
    private float animTime = 0f;

    // Animation - Shooting
    private final Animation<TextureRegion> shootLeftAnim;
    private final Animation<TextureRegion> shootRightAnim;
    private final Animation<TextureRegion> shootUpAnim;
    private final Animation<TextureRegion> shootDownAnim;
    private boolean shooting = false;

    // Powerups
    private boolean isShielded = false;
    private float shieldTimer = 0f;

    // Debug
    private boolean godMode = false;
    private boolean noClip = false;

    // RPG / Leveling
    private int experience = 0;
    private int playerLevel = 1;
    private int skillPoints = 0;
    private static final int XP_PER_LEVEL_BASE = 100;

    // Skills (0 to 5)
    private int skillSpeedLevel = 0;
    private int skillHealthLevel = 0;

    // --- COMBAT & WEAPONS ---
    private boolean hasGun = false;

    // >> MISSING MELEE VARIABLES RESTORED HERE <<
    private boolean isAttacking = false;
    private float attackTimer = 0f;
    private static final float ATTACK_DURATION = 0.25f;
    private static final float ATTACK_COOLDOWN = 0.5f;
    private float attackCooldownTimer = 0f;
    private Rectangle attackHitbox;

    // Direction Logic
    private Vector2 facingDirection = new Vector2(0, -1);
    private Bullet.Dir facingEnum = Bullet.Dir.DOWN;

    // Bullet Logic
    private final List<Bullet> bullets = new ArrayList<>();
    private final TextureRegion bulletRegion;

    @FunctionalInterface
    public interface BlockChecker {
        boolean isBlocked(Rectangle playerRect);
    }

    // --- CONSTRUCTOR ---
    public Player(float startX, float startY,
                  Animation<TextureRegion> down, Animation<TextureRegion> up,
                  Animation<TextureRegion> left, Animation<TextureRegion> right,
                  MazeRunnerGame game) {
        this.x = startX;
        this.y = startY;

        // Walk Anims
        this.animDown = down;
        this.animUp = up;
        this.animLeft = left;
        this.animRight = right;
        this.currentAnimation = down;

        // Shoot Anims
        this.shootLeftAnim = game.getShootLeftAnim();
        this.shootRightAnim = game.getShootRightAnim();
        this.shootUpAnim = game.getShootUpAnim();
        this.shootDownAnim = game.getShootDownAnim();

        this.bulletRegion = game.getBulletRegion();

        this.lives = getMaxLives();
    }


    // --- MAIN UPDATE LOOP ---
    public void update(float dt, Maze maze, MazeRunnerGame game, BlockChecker blockChecker) {
        // 1. Timers
        updateTimers(dt);

        // 2. Input Reading
        MovementInput input = readMovementInput(dt, game);
        boolean moving = (input.dx != 0f || input.dy != 0f);

        // 3. Update Direction
        updateFacingDirection(game);

        // 4. Melee Combat Timers (The variables used here are now defined above)
        if (attackCooldownTimer > 0) attackCooldownTimer -= dt;
        if (isAttacking) {
            attackTimer -= dt;
            if (attackTimer <= 0) isAttacking = false;
        }
        updateAttackHitbox(); // Keeps hitbox attached to player

        // 5. Melee Input (Space)
        if (Gdx.input.isKeyJustPressed(Input.Keys.SPACE)) {
            attack();
        }

        // 6. Shooting Logic (Press S)
        if (Gdx.input.isKeyPressed(Input.Keys.S) && hasGun) {
            shooting = true;
            if (Gdx.input.isKeyJustPressed(Input.Keys.S)) {
                spawnBullet();
            }
        } else {
            shooting = false;
        }

        // 7. Animation Selection
        if (shooting) {
            selectShootingAnimation();
        } else {
            selectWalkAnimation(moving);
        }

        if (moving || shooting) animTime += dt;
        else animTime = 0f;

        // 8. Movement Physics
        attemptMove(input.dx, input.dy, maze, game, blockChecker);

        // 9. Update Bullets
        updateBullets(dt);
    }

    private void updateTimers(float dt) {
        if (speedTimer > 0) {
            speedTimer -= dt;
            if (speedTimer <= 0) speedMultiplier = 1.0f;
        }
        if (shieldTimer > 0) {
            shieldTimer -= dt;
            if (shieldTimer <= 0) isShielded = false;
        }
        if (invulnerable) {
            invulnTimeLeft -= dt;
            if (invulnTimeLeft <= 0f) invulnerable = false;
        }
        if (damageFlash) {
            flashTimeLeft -= dt;
            if (flashTimeLeft <= 0f) damageFlash = false;
        }
    }

    // --- MOVEMENT & DIRECTION ---
    private record MovementInput(float dx, float dy) {}

    private MovementInput readMovementInput(float dt, MazeRunnerGame game) {
        int keyUp = game.getPreferences().getKeyUp();
        int keyDown = game.getPreferences().getKeyDown();
        int keyLeft = game.getPreferences().getKeyLeft();
        int keyRight = game.getPreferences().getKeyRight();

        boolean run = Gdx.input.isKeyPressed(Input.Keys.SHIFT_LEFT) || Gdx.input.isKeyPressed(Input.Keys.SHIFT_RIGHT);

        float currentSpeed = getWalkSpeed() * (run ? RUN_MULTIPLIER : 1f) * speedMultiplier;
        float move = currentSpeed * dt;

        float dx = 0f;
        float dy = 0f;

        if (Gdx.input.isKeyPressed(keyLeft))  dx -= move;
        if (Gdx.input.isKeyPressed(keyRight)) dx += move;
        if (Gdx.input.isKeyPressed(keyUp))    dy += move;
        if (Gdx.input.isKeyPressed(keyDown))  dy -= move;

        return new MovementInput(dx, dy);
    }

    private void updateFacingDirection(MazeRunnerGame game) {
        int keyUp = game.getPreferences().getKeyUp();
        int keyDown = game.getPreferences().getKeyDown();
        int keyLeft = game.getPreferences().getKeyLeft();
        int keyRight = game.getPreferences().getKeyRight();

        if (Gdx.input.isKeyPressed(keyLeft)) {
            facingDirection.set(-1, 0);
            facingEnum = Bullet.Dir.LEFT;
        } else if (Gdx.input.isKeyPressed(keyRight)) {
            facingDirection.set(1, 0);
            facingEnum = Bullet.Dir.RIGHT;
        } else if (Gdx.input.isKeyPressed(keyUp)) {
            facingDirection.set(0, 1);
            facingEnum = Bullet.Dir.UP;
        } else if (Gdx.input.isKeyPressed(keyDown)) {
            facingDirection.set(0, -1);
            facingEnum = Bullet.Dir.DOWN;
        }
    }

    private void attemptMove(float dx, float dy, Maze maze, MazeRunnerGame game, BlockChecker blockChecker) {
        if (dx != 0f) {
            float nextX = x + dx;
            if (canOccupy(nextX, y, maze)) {
                if (blockChecker == null || !blockChecker.isBlocked(makeRect(nextX, y))) {
                    x = nextX;
                } else game.playWallHitSound();
            } else game.playWallHitSound();
        }
        if (dy != 0f) {
            float nextY = y + dy;
            if (canOccupy(x, nextY, maze)) {
                if (blockChecker == null || !blockChecker.isBlocked(makeRect(x, nextY))) {
                    y = nextY;
                } else game.playWallHitSound();
            } else game.playWallHitSound();
        }
    }

    private boolean canOccupy(float testX, float testY, Maze maze) {
        if (noClip) return true;
        float left   = testX + COLLISION_INSET;
        float right  = testX + WIDTH - COLLISION_INSET;
        float bottom = testY + COLLISION_INSET;
        float top    = testY + HEIGHT - COLLISION_INSET;

        return !isBlocked(left, bottom, maze) && !isBlocked(right, bottom, maze)
                && !isBlocked(left, top, maze) && !isBlocked(right, top, maze);
    }

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

    // --- MELEE COMBAT METHODS (RESTORED) ---

    public boolean attack() {
        if (attackCooldownTimer <= 0 && !isAttacking) {
            isAttacking = true;
            attackTimer = ATTACK_DURATION;
            attackCooldownTimer = ATTACK_COOLDOWN;
            return true;
        }
        return false;
    }

    private void updateAttackHitbox() {
        float range = 40f;
        if (attackHitbox == null) attackHitbox = new Rectangle();
        attackHitbox.set(x - range/2, y - range/2, WIDTH + range, HEIGHT + range);
    }

    public boolean isAttacking() { return isAttacking; }
    public Rectangle getAttackHitbox() { return attackHitbox; }
    public Vector2 getFacingDirection() { return facingDirection; }


    // --- BULLET LOGIC ---

    private void spawnBullet() {
        float cx = x + WIDTH / 2f;
        float cy = y + HEIGHT / 2f;
        float offset = 18f;
        float bx = cx, by = cy;

        switch (facingEnum) {
            case LEFT -> bx -= offset;
            case RIGHT -> bx += offset;
            case UP -> by += offset;
            case DOWN -> by -= offset;
        }
        bullets.add(new Bullet(bx, by, facingEnum));
    }

    private void updateBullets(float dt) {
        Iterator<Bullet> it = bullets.iterator();
        while (it.hasNext()) {
            Bullet b = it.next();
            b.update(dt);
            if (!b.isAlive()) it.remove();
        }
    }

    public List<Bullet> getBullets() { return bullets; }


    // --- ANIMATIONS ---
    private void selectShootingAnimation() {
        Animation<TextureRegion> next = currentAnimation;
        switch (facingEnum) {
            case LEFT -> next = shootLeftAnim;
            case RIGHT -> next = shootRightAnim;
            case UP -> next = shootUpAnim;
            case DOWN -> next = shootDownAnim;
        }
        if (next != currentAnimation) {
            currentAnimation = next;
            animTime = 0f;
        }
    }

    private void selectWalkAnimation(boolean moving) {
        Animation<TextureRegion> next = currentAnimation;

        switch (facingEnum) {
            case LEFT -> next = animLeft;
            case RIGHT -> next = animRight;
            case UP -> next = animUp;
            case DOWN -> next = animDown;
        }

        if (next != currentAnimation) {
            currentAnimation = next;
            animTime = 0f;
        }
    }

    // --- RPG & SKILLS SYSTEM ---

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
            lives = getMaxLives();
            System.out.println("LEVEL UP! Lv " + playerLevel);
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
            lives++;
            return true;
        }
        return false;
    }

    public void takeDamage(int amount) {
        if (godMode || isShielded || invulnerable) return;
        lives = Math.max(0, lives - amount);
        invulnerable = true;
        invulnTimeLeft = 0.8f;
        damageFlash = true;
        flashTimeLeft = 0.25f;
    }

    public void addLife() {
        if (lives < getMaxLives()) lives++;
    }

    // --- DRAWING ---
    public void draw(SpriteBatch batch) {
        // Draw Bullets first
        for (Bullet b : bullets) {
            b.draw(batch, bulletRegion);
        }

        TextureRegion frame = currentAnimation.getKeyFrame(animTime, true);
        com.badlogic.gdx.graphics.Color oldColor = batch.getColor();

        if (isShielded) batch.setColor(0.5f, 0.5f, 1f, 1f);
        else if (damageFlash) batch.setColor(1f, 0f, 0f, 1f);

        // Draw Player (Size 52x52 to match your visual preference)
        batch.draw(frame, x, y, 52f, 52f);

        batch.setColor(oldColor);
    }

    // --- GETTERS & SETTERS ---

    public void setPlayerLevel(int level) { this.playerLevel = level; }
    public int getPlayerLevel() { return playerLevel; }

    public void setScore(int score) { this.score = score; }
    public void addScore(int amount) { this.score += amount; }
    public int getScore() { return score; }

    public void setLives(int lives) { this.lives = lives; }
    public int getLives() { return lives; }
    public int getMaxLives() { return BASE_LIVES + skillHealthLevel; }

    public void setExperience(int experience) { this.experience = experience; }
    public int getExperience() { return experience; }
    public int getXpNeededForNextLevel() { return playerLevel * XP_PER_LEVEL_BASE; }

    public void setSkillSpeedLevel(int level) { this.skillSpeedLevel = level; }
    public int getSkillSpeedLevel() { return skillSpeedLevel; }

    public void setSkillHealthLevel(int level) { this.skillHealthLevel = level; }
    public int getSkillHealthLevel() { return skillHealthLevel; }

    public void setPosition(float x, float y) { this.x = x; this.y = y; }
    public float getX() { return x; }
    public float getY() { return y; }

    public boolean hasKey() { return hasKey; }
    public void giveKey() { hasKey = true; }
    public void clearKey() { hasKey = false; }

    public void equipGun() {
        this.hasGun = true;
        System.out.println("Gun Equipped! Press S to shoot.");
    }
    public boolean hasGun() { return hasGun; }

    public boolean isDamageFlashing() { return damageFlash; }
    public boolean isGodMode() { return godMode; }
    public boolean isNoClip() { return noClip; }
    public void setGodMode(boolean v) { godMode = v; }
    public void setNoClip(boolean v) { noClip = v; }
    public int getSkillPoints() { return skillPoints; }

    public float getWalkSpeed() { return WALK_SPEED_BASE + (skillSpeedLevel * 50f); }
    public void setSpeedMultiplier(float m) { this.speedMultiplier = m; }
    public void resetSpeedMultiplier() { this.speedMultiplier = 1f; }

    // Powerups
    public void activateSpeedBoost(float duration, float multiplier) {
        this.speedTimer = duration;
        this.speedMultiplier = multiplier;
    }
    public void activateShield(float duration) {
        this.shieldTimer = duration;
        this.isShielded = true;
    }
}