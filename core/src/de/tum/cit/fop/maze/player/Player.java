package de.tum.cit.fop.maze.player;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.Camera;
import com.badlogic.gdx.graphics.g2d.Animation;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.math.Vector3;
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
    private static final int BASE_LIVES = 100;
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

    private boolean isAttacking = false;
    private float attackTimer = 0f;
    private static final float ATTACK_DURATION = 0.25f;
    private static final float ATTACK_COOLDOWN = 0.5f;
    private float attackCooldownTimer = 0f;
    private Rectangle attackHitbox;

    // Direction Logic
    private Vector2 facingDirection = new Vector2(0, -1);

    // Repulse Skill
    private boolean isRepulsing = false; // Flag to tell CombatSystem to push
    private float repulseCooldownTimer = 0f;


    private enum Facing { UP, DOWN, LEFT, RIGHT }
    private Facing currentFacing = Facing.DOWN;

    // Bullet Logic
    private final List<Bullet> bullets = new ArrayList<>();
    private final TextureRegion bulletRegion;

    // --- SKILL FLAGS ---
    // Path 1: Gunner
    private boolean skillRapidFire = false;
    private boolean skillShotgun = false;
    private boolean skillPiercing = false;

    // Path 2: Speedster
    private boolean skillDash = false;
    private boolean skillGhost = false;

    // Path 3: Tank
    private boolean skillThorns = false;
    private boolean skillRepulse = false;

    // Dash Logic variables
    private boolean isDashing = false;
    private float dashTimer = 0f;
    private float dashCooldown = 0f;
    private Vector2 dashDirection = new Vector2();
    private static final float DASH_DURATION = 0.15f;
    private static final float DASH_SPEED = 1200f;
    private static final float DASH_COOLDOWN_MAX = 1.0f;

    // Shooting Cooldown
    private float shootTimer = 0f;

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

        this.animDown = down;
        this.animUp = up;
        this.animLeft = left;
        this.animRight = right;
        this.currentAnimation = down;

        this.shootLeftAnim = game.getShootLeftAnim();
        this.shootRightAnim = game.getShootRightAnim();
        this.shootUpAnim = game.getShootUpAnim();
        this.shootDownAnim = game.getShootDownAnim();

        this.bulletRegion = game.getBulletRegion();

        this.lives = getMaxLives();
    }


    // --- MAIN UPDATE LOOP ---
    public void update(float dt, Maze maze, MazeRunnerGame game, BlockChecker blockChecker, Camera camera) {
        // 1. Timers & Cooldowns
        updateTimers(dt);
        if (attackCooldownTimer > 0) attackCooldownTimer -= dt;
        if (dashCooldown > 0) dashCooldown -= dt;
        if (shootTimer > 0) shootTimer -= dt;

        // --- DASH LOGIC ---
        if (isDashing) {
            dashTimer -= dt;
            attemptMove(dashDirection.x * DASH_SPEED * dt, dashDirection.y * DASH_SPEED * dt, maze, game, null);
            if (dashTimer <= 0) isDashing = false;
            return;
        }
        if (repulseCooldownTimer > 0) repulseCooldownTimer -= dt;
        isRepulsing = false;

        // 2. Input Reading
        MovementInput input = readMovementInput(dt, game);
        boolean moving = (input.dx != 0f || input.dy != 0f);

        // 3. Facing Direction
        if (hasGun) updateFacingToMouse(camera);
        else updateFacingDirection(game);

        // 4. Skills Input (Dash)
        if (skillDash && Gdx.input.isKeyJustPressed(Input.Keys.E) && dashCooldown <= 0 && moving) {
            startDash(input.dx, input.dy);
        }

        if (skillRepulse && Gdx.input.isKeyJustPressed(Input.Keys.Q) && repulseCooldownTimer <= 0) {
            isRepulsing = true;
            repulseCooldownTimer = 5.0f; // 5 Seconds Cooldown
        }

        // --- FIX: RESTORE MELEE INPUT HERE ---
        if (Gdx.input.isKeyJustPressed(Input.Keys.SPACE)) {
            attack();
        }
        // -------------------------------------

        // 5. Shooting Input
        if (Gdx.input.isButtonJustPressed(Input.Buttons.LEFT) && hasGun) {
            if (shootTimer <= 0) {
                spawnBullet(camera);
                shootTimer = skillRapidFire ? 0.2f : 0.4f;
                shooting = true;
            }
        } else {
            shooting = false;
        }

        // 6. Animation Selection
        if (shooting) {
            selectShootingAnimation();
        } else {
            selectWalkAnimation(moving);
        }

        if (moving || shooting) {
            animTime += dt;
        } else {
            animTime = 0f;
        }

        // 7. Movement & Bullets
        if (!isDashing) {
            attemptMove(input.dx, input.dy, maze, game, blockChecker);
        }
        updateBullets(dt);

        // 8. Melee Hitbox Update
        if (isAttacking) {
            attackTimer -= dt;
            if (attackTimer <= 0) isAttacking = false;
        }
        updateAttackHitbox();
    }

    private void startDash(float moveDx, float moveDy) {
        isDashing = true;
        dashTimer = DASH_DURATION;
        dashCooldown = DASH_COOLDOWN_MAX;

        dashDirection.set(moveDx, moveDy).nor();

        if (dashDirection.isZero()) {
            dashDirection.set(1, 0);
        }
        System.out.println("DASH!");
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
            currentFacing = Facing.LEFT;
        } else if (Gdx.input.isKeyPressed(keyRight)) {
            facingDirection.set(1, 0);
            currentFacing = Facing.RIGHT;
        } else if (Gdx.input.isKeyPressed(keyUp)) {
            facingDirection.set(0, 1);
            currentFacing = Facing.UP;
        } else if (Gdx.input.isKeyPressed(keyDown)) {
            facingDirection.set(0, -1);
            currentFacing = Facing.DOWN;
        }
    }

    public boolean spendSkillPoint() {
        if (skillPoints > 0) {
            skillPoints--;
            return true;
        }
        return false;
    }

    private void updateFacingToMouse(Camera camera) {
        Vector3 mousePos = new Vector3(Gdx.input.getX(), Gdx.input.getY(), 0);
        camera.unproject(mousePos);

        float diffX = mousePos.x - (x + WIDTH/2f);
        float diffY = mousePos.y - (y + HEIGHT/2f);

        float angle = MathUtils.atan2(diffY, diffX) * MathUtils.radiansToDegrees;
        if (angle < 0) angle += 360f;

        if (angle >= 45 && angle < 135) currentFacing = Facing.UP;
        else if (angle >= 135 && angle < 225) currentFacing = Facing.LEFT;
        else if (angle >= 225 && angle < 315) currentFacing = Facing.DOWN;
        else currentFacing = Facing.RIGHT;
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

    public boolean attack() {
        if (attackCooldownTimer <= 0 && !isAttacking) {
            System.out.println("Melee Attack Triggered!");
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

    private void spawnBullet(Camera camera) {
        if (!hasGun) return;
        float cx = x + WIDTH / 2f;
        float cy = y + HEIGHT / 2f;

        Vector3 mousePos = new Vector3(Gdx.input.getX(), Gdx.input.getY(), 0);
        camera.unproject(mousePos);

        Vector2 target = new Vector2(mousePos.x, mousePos.y);

        bullets.add(new Bullet(cx, cy, target.x, target.y));

        if (skillShotgun) {
            Vector2 aimVector = new Vector2(target.x - cx, target.y - cy);
            Vector2 leftShot = aimVector.cpy().rotateDeg(15).add(cx, cy);
            Vector2 rightShot = aimVector.cpy().rotateDeg(-15).add(cx, cy);

            bullets.add(new Bullet(cx, cy, leftShot.x, leftShot.y));
            bullets.add(new Bullet(cx, cy, rightShot.x, rightShot.y));
        }
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

    private void selectShootingAnimation() {
        Animation<TextureRegion> next = currentAnimation;
        switch (currentFacing) {
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
        switch (currentFacing) {
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

    public void draw(SpriteBatch batch) {
        for (Bullet b : bullets) {
            b.draw(batch, bulletRegion);
        }

        TextureRegion frame = currentAnimation.getKeyFrame(animTime, true);
        com.badlogic.gdx.graphics.Color oldColor = batch.getColor();

        if (isShielded) {
            batch.setColor(0.5f, 0.5f, 1f, 1f);
        }
        else if (damageFlash) {
            batch.setColor(1f, 0f, 0f, 1f);
        }
        else if (isAttacking) {
            batch.setColor(1f, 1f, 0f, 1f); // YELLOW/ORANGE for Melee Attack
        }

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

    public void activateSpeedBoost(float duration, float multiplier) {
        this.speedTimer = duration;
        this.speedMultiplier = multiplier;
    }
    public void activateShield(float duration) {
        this.shieldTimer = duration;
        this.isShielded = true;
    }
    public void unlockRapidFire() { skillRapidFire = true; }
    public void unlockShotgun() { skillShotgun = true; }
    public void unlockPiercing() { skillPiercing = true; }

    public void unlockDash() { skillDash = true; }
    public void unlockGhost() { skillGhost = true; }

    public void unlockThorns() { skillThorns = true; }
    public void unlockRepulse() { skillRepulse = true; }
    public boolean isRepulseActive() { return isRepulsing; }

    public void setSkillPoints(int points) { this.skillPoints = points; }

    public boolean hasRapidFire() { return skillRapidFire; }
    public boolean hasShotgun() { return skillShotgun; }
    public boolean hasPiercing() { return skillPiercing; }
    public boolean hasDash() { return skillDash; }
    public boolean hasGhost() { return skillGhost; }
    public boolean hasThorns() { return skillThorns; }
    public boolean hasRepulse() { return skillRepulse; }
}