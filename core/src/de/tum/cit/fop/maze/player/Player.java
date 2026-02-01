package de.tum.cit.fop.maze.player;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.Camera;
import com.badlogic.gdx.audio.Sound;
import com.badlogic.gdx.graphics.g2d.Animation;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.math.Vector3;
import de.tum.cit.fop.maze.MazeRunnerGame;
import de.tum.cit.fop.maze.map.Maze;
import de.tum.cit.fop.maze.gambling.Progression;
import de.tum.cit.fop.maze.gambling.RunGambleState;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class Player {
    
    private float x;
    private float y;

    private float damageMult = 1.0f;
    private float bonusSpeedMult = 1.0f;

    private boolean hasShoes = false;
    private boolean hasGloves = false;

    private int strengthStat = 5;
    private int speedStat = 5;
    private boolean hasFreezeWeapon = false;
    private boolean hasPoisonKnuckles = false;
    private int hp;

    private static final int BASE_HP = 100;
    private static final int HP_PER_UPGRADE = 5;

    private int permanentHeartsBonus = 0;


    
    private int coins = 0;
    private final Progression progression;
    private final MazeRunnerGame gameRef;


    private boolean hasKey = false;
    private int score = 0;

    
    private boolean invulnerable = false;
    private float invulnTimeLeft = 0f;
    private boolean damageFlash = false;
    private float flashTimeLeft = 0f;

    
    private static final float WALK_SPEED_BASE = 300f;
    private static final float RUN_MULTIPLIER = 1.7f;

    private float speedMultiplier = 1f; 
    private float speedTimer = 0f;

    
    private static final float WIDTH = 32f;
    private static final float HEIGHT = 32f;
    private static final float COLLISION_INSET = 2f;

    
    private final Animation<TextureRegion> animDown, animUp, animLeft, animRight;
    private final Animation<TextureRegion> idleDown, idleUp, idleLeft, idleRight;
    private Animation<TextureRegion> currentAnimation;
    private float animTime = 0f;

    
    private final Animation<TextureRegion> shootLeftAnim;
    private final Animation<TextureRegion> shootRightAnim;
    private final Animation<TextureRegion> shootUpAnim;
    private final Animation<TextureRegion> shootDownAnim;
    private final Animation<TextureRegion> gunDownIdleRun;
    private final Animation<TextureRegion> gunUpIdleRun;
    private final Animation<TextureRegion> gunSideIdleRun;
    private final Animation<TextureRegion> gunSideLeftIdleRun;
    private final Animation<TextureRegion> gunDownShoot;
    private final Animation<TextureRegion> gunUpShoot;
    private final Animation<TextureRegion> gunSideShoot;
    private final Animation<TextureRegion> gunSideLeftShoot;
    private boolean shooting = false;
    private Sound shootSound;
    private Sound reloadSound;
    private Sound slipSound;
    private Sound ouchSound;


    
    private boolean isShielded = false;
    private float shieldTimer = 0f;

    
    private boolean godMode = false;
    private boolean noClip = false;

    
    private int experience = 0;
    private int playerLevel = 1;
    private int skillPoints = 0;
    private static final int XP_PER_LEVEL_BASE = 100;

    
    private int skillSpeedLevel = 0;
    private int skillHealthLevel = 0;

    
    private boolean hasGun = false;
    private static final int MAX_CLIP_AMMO = 20;
    private int ammoInClip = 0;
    private int ammoReserve = 0;

    private boolean isAttacking = false;
    private float attackTimer = 0f;
    private static final float ATTACK_DURATION = 0.25f;
    private static final float ATTACK_COOLDOWN = 0.5f;
    private float attackCooldownTimer = 0f;
    private Rectangle attackHitbox;


    
    private final Vector2 facingDirection = new Vector2(0, -1);

    
    private float hazardWallDamageTimer = 0f;
    private float slideTimer = 0f;
    private static final float SLIDE_DURATION = 3.5f;
    private static final int HAZARD_WALL_DAMAGE = 1;
    private float controlMultiplier = 1f;

    private final Vector2 slideVelocity = new Vector2();
    private boolean isSliding = false;
    private final Vector2 lastMovementDir = new Vector2(0, 1);
    private boolean hazardWallDamageUsed = false;

    
    private boolean lightningSlowed = false;
    private float lightningAnimTime = 0f;
    private float lightningDebuffTimer = 0f;

    
    private boolean isRepulsing = false;
    private float repulseCooldownTimer = 0f;

    private enum Facing { UP, DOWN, LEFT, RIGHT }
    private Facing currentFacing = Facing.DOWN;

    
    private final List<Bullet> bullets = new ArrayList<>();
    private final TextureRegion bulletRegion;

    
    private boolean skillRapidFire = false;
    private boolean skillShotgun = false;
    private boolean skillPiercing = false;

    private boolean skillDash = false;
    private boolean skillGhost = false;

    private boolean skillThorns = false;
    private boolean skillRepulse = false;

    
    private boolean isDashing = false;
    private float dashTimer = 0f;
    private float dashCooldown = 0f;
    private final Vector2 dashDirection = new Vector2();
    private static final float DASH_DURATION = 0.15f;
    private static final float DASH_SPEED = 1200f;
    private static final float DASH_COOLDOWN_MAX = 1.0f;

    
    private float shootTimer = 0f;

    
    private float laserDamageAccumulator = 0f;
    private float laserHeat = 0f;
    private int permanentStrength = 5; 
    private int permanentSpeed = 5;

    private boolean freezeWeapon = false;
    private boolean poisonKnuckles = false;

    @FunctionalInterface
    public interface BlockChecker {
        boolean isBlocked(Rectangle playerRect);
    }

    
    public Player(float startX, float startY, Progression progression, Animation<TextureRegion> down, Animation<TextureRegion> up, Animation<TextureRegion> left, Animation<TextureRegion> right, MazeRunnerGame game) {

        this.x = startX;
        this.y = startY;

        this.animDown = down;
        this.animUp = up;
        this.animLeft = left;
        this.animRight = right;
        this.idleDown = game.getIdleDown();
        this.idleUp = game.getIdleUp();
        this.idleLeft = game.getIdleLeft();
        this.idleRight = game.getIdleRight();
        this.currentAnimation = down;

        this.shootLeftAnim = game.getShootLeftAnim();
        this.shootRightAnim = game.getShootRightAnim();
        this.shootUpAnim = game.getShootUpAnim();
        this.shootDownAnim = game.getShootDownAnim();
        this.gunDownIdleRun = game.getGunDownIdleRun();
        this.gunUpIdleRun = game.getGunUpIdleRun();
        this.gunSideIdleRun = game.getGunSideIdleRun();
        this.gunSideLeftIdleRun = game.getGunSideLeftIdleRun();
        this.gunDownShoot = game.getGunDownShoot();
        this.gunUpShoot = game.getGunUpShoot();
        this.gunSideShoot = game.getGunSideShoot();
        this.gunSideLeftShoot = game.getGunSideLeftShoot();

        this.bulletRegion = game.getBulletRegion();
        this.shootSound = Gdx.audio.newSound(Gdx.files.internal("audio/PlayerShooting.wav"));
        this.reloadSound = Gdx.audio.newSound(Gdx.files.internal("reload.mp3"));
        this.slipSound = Gdx.audio.newSound(Gdx.files.internal("slip.mp3"));
        if (Gdx.files.internal("ouch.mp3").exists()) {
            this.ouchSound = Gdx.audio.newSound(Gdx.files.internal("ouch.mp3"));
        }

        this.progression = progression;
        this.gameRef = game;

        if (this.progression != null) {
            this.coins = this.progression.getCoins();
            this.permanentHeartsBonus = this.progression.getPermanentHearts();
            this.skillPoints += this.progression.getPermanentSkillPoints();
            this.damageMult = this.progression.getPermanentDamageMult();
            this.bonusSpeedMult = this.progression.getPermanentSpeedMult();

            this.strengthStat = this.progression.getPermanentStrength();
            this.speedStat = this.progression.getPermanentSpeedStat();
            this.hasFreezeWeapon = this.progression.hasFreezeWeapon();
            this.hasPoisonKnuckles = this.progression.hasPoisonKnuckles();
        }

        this.damageMult = MathUtils.clamp(this.damageMult, 0.5f, 3.0f);
        this.bonusSpeedMult = MathUtils.clamp(this.bonusSpeedMult, 0.5f, 3.0f);
        this.strengthStat = MathUtils.clamp(this.strengthStat, 1, 99);
        this.speedStat = MathUtils.clamp(this.speedStat, 1, 99);

        this.hp = getMaxHp();

    }

    
    public void refreshPermanentProgression(MazeRunnerGame game) {
        if (game.getProgression() == null) return;

        this.permanentHeartsBonus = game.getProgression().getPermanentHearts();

        this.strengthStat = MathUtils.clamp(game.getProgression().getPermanentStrength(), 1, 99);
        this.speedStat = MathUtils.clamp(game.getProgression().getPermanentSpeedStat(), 1, 99);
        this.hasFreezeWeapon = game.getProgression().hasFreezeWeapon();
        this.hasPoisonKnuckles = game.getProgression().hasPoisonKnuckles();

        int max = getMaxHp();
        if (hp > max) hp = max;
    }

    public void applyRunGamble(RunGambleState run) {
        if (run == null) return;
        this.strengthStat = MathUtils.clamp(run.strength, 1, 99);
        this.speedStat = MathUtils.clamp(run.speed, 1, 99);
        this.damageMult = MathUtils.clamp(run.damageMult, 0.5f, 3.0f);
        this.bonusSpeedMult = MathUtils.clamp(run.speedMult, 0.5f, 3.0f);
        this.permanentHeartsBonus = Math.max(0, run.bonusHearts);
        this.hasFreezeWeapon = run.freezeWeapon;
        this.hasPoisonKnuckles = run.poisonKnuckles;
        this.hp = getMaxHp();
    }

    
    public void update(float dt, Maze maze, MazeRunnerGame game, BlockChecker blockChecker, Camera camera) {
        updateTimers(dt);

        if (attackCooldownTimer > 0) attackCooldownTimer -= dt;
        if (dashCooldown > 0) dashCooldown -= dt;
        if (shootTimer > 0) shootTimer -= dt;
        if (repulseCooldownTimer > 0) repulseCooldownTimer -= dt;

        
        if (isDashing) {
            dashTimer -= dt;
            attemptMove(dashDirection.x * DASH_SPEED * dt, dashDirection.y * DASH_SPEED * dt, maze, game, null);
            if (dashTimer <= 0) isDashing = false;
            return;
        }

        isRepulsing = false;

        MovementInput input = readMovementInput(dt, game);
        boolean moving = (input.dx != 0f || input.dy != 0f);

        float moveDx = input.dx;
        float moveDy = input.dy;

        if (moveDx != 0f || moveDy != 0f) {
            lastMovementDir.set(moveDx, moveDy).nor();
        }

        if (hasGun) updateFacingToMouse(camera);
        else updateFacingDirection(game);

        
        if (isSliding) {
            if (input.dx != 0f || input.dy != 0f) {
                Vector2 inputVelocity = new Vector2(input.dx / dt, input.dy / dt);
                slideVelocity.lerp(inputVelocity, 0.08f);
            }
            moveDx = slideVelocity.x * dt;
            moveDy = slideVelocity.y * dt;
        }

        
        if (skillDash && Gdx.input.isKeyJustPressed(game.getPreferences().getKeyDash()) && dashCooldown <= 0 && moving) {
            startDash(input.dx, input.dy);
        }

        
        if (skillRepulse && Gdx.input.isKeyJustPressed(game.getPreferences().getKeyRepulse()) && repulseCooldownTimer <= 0) {
            isRepulsing = true;
            repulseCooldownTimer = 5.0f;
        }

        
        if (!isParalyzed() && Gdx.input.isKeyJustPressed(game.getPreferences().getKeyMelee())) {
            attack();
        }

        
        boolean shootPressed = Gdx.input.isButtonJustPressed(Input.Buttons.LEFT)
                || Gdx.input.isKeyJustPressed(game.getPreferences().getKeyShoot());
        boolean reloadPressed = Gdx.input.isKeyJustPressed(Input.Keys.R);
        if (reloadPressed && hasGun) {
            reload();
        }
        if (!isParalyzed() && shootPressed && hasGun) {
            if (shootTimer <= 0 && ammoInClip > 0) {
                spawnBullet(camera);
                shootTimer = skillRapidFire ? 0.2f : 0.4f;
                shooting = true;
            }
        } else {
            shooting = false;
        }

        
        if (shooting) selectShootingAnimation();
        else selectWalkAnimation(moving);

        if (moving
                || shooting
                || currentAnimation == idleDown
                || currentAnimation == idleUp
                || currentAnimation == idleLeft
                || currentAnimation == idleRight) {
            animTime += dt;
        } else {
            animTime = 0f;
        }

        
        attemptMove(moveDx, moveDy, maze, game, blockChecker);

        
        updateBullets(dt);

        
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
        if (dashDirection.isZero()) dashDirection.set(1, 0);
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
        if (hazardWallDamageTimer > 0f) {
            hazardWallDamageTimer -= dt;
            if (hazardWallDamageTimer <= 0f) controlMultiplier = 1f;
        }

        if (isSliding) {
            slideVelocity.scl(0.98f);
            if (slideVelocity.len() < 50f) {
                slideVelocity.setZero();
                isSliding = false;
            }
            if (slideTimer > 0f) {
                slideTimer -= dt;
                if (slideTimer <= 0f) {
                    slideVelocity.setZero();
                    isSliding = false;
                }
            }
        }

        if (lightningDebuffTimer > 0f) {
            lightningDebuffTimer -= dt;
            lightningAnimTime += dt;
            lightningSlowed = true;
        } else {
            lightningSlowed = false;
        }
    }

    public boolean isLightningDebuffed() { return lightningDebuffTimer > 0f; }
    public boolean isParalyzed() { return lightningDebuffTimer > 0f; }

    private record MovementInput(float dx, float dy) {}

    private MovementInput readMovementInput(float dt, MazeRunnerGame game) {
        int keyUp = game.getPreferences().getKeyUp();
        int keyDown = game.getPreferences().getKeyDown();
        int keyLeft = game.getPreferences().getKeyLeft();
        int keyRight = game.getPreferences().getKeyRight();

        boolean run = Gdx.input.isKeyPressed(Input.Keys.SHIFT_LEFT) || Gdx.input.isKeyPressed(Input.Keys.SHIFT_RIGHT);


        float statSpeedBonus = (speedStat - 5) * 15f;

        float currentSpeed = (getWalkSpeed() + statSpeedBonus) * (run ? RUN_MULTIPLIER : 1f) * speedMultiplier * bonusSpeedMult * controlMultiplier;

        float move = currentSpeed * dt;

        float dx = 0f, dy = 0f;
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

    private void updateFacingToMouse(Camera camera) {
        Vector3 mousePos = new Vector3(Gdx.input.getX(), Gdx.input.getY(), 0);
        camera.unproject(mousePos);

        float diffX = mousePos.x - (x + WIDTH / 2f);
        float diffY = mousePos.y - (y + HEIGHT / 2f);

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
                if (blockChecker == null || !blockChecker.isBlocked(makeRect(nextX, y))) x = nextX;
                else onWallHit(game);
            } else onWallHit(game);
        }

        if (dy != 0f) {
            float nextY = y + dy;
            if (canOccupy(x, nextY, maze)) {
                if (blockChecker == null || !blockChecker.isBlocked(makeRect(x, nextY))) y = nextY;
                else onWallHit(game);
            } else onWallHit(game);
        }
    }

    private void onWallHit(MazeRunnerGame game) {
        game.playWallHitSound();

        if (hazardWallDamageTimer > 0f && !hazardWallDamageUsed) {
            takeDamage(HAZARD_WALL_DAMAGE);
            hazardWallDamageUsed = true;
        }
    }

    private boolean canOccupy(float testX, float testY, Maze maze) {
        if (noClip) return true;
        float left = testX + COLLISION_INSET;
        float right = testX + WIDTH - COLLISION_INSET;
        float bottom = testY + COLLISION_INSET;
        float top = testY + HEIGHT - COLLISION_INSET;

        return !isBlocked(left, bottom, maze)
                && !isBlocked(right, bottom, maze)
                && !isBlocked(left, top, maze)
                && !isBlocked(right, top, maze);
    }

    private boolean isBlocked(float worldX, float worldY, Maze maze) {
        int cx = (int) (worldX / maze.getTileSize());
        int cy = (int) (worldY / maze.getTileSize());
        if (maze.isWallCell(cx, cy)) return true;
        return maze.isBlockedExit(cx, cy, hasKey);
    }

    private Rectangle makeRect(float px, float py) {
        return new Rectangle(px, py, WIDTH, HEIGHT);
    }

    
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
        attackHitbox.set(x - range / 2f, y - range / 2f, WIDTH + range, HEIGHT + range);
    }

    public boolean isAttacking() { return isAttacking; }
    public Rectangle getAttackHitbox() { return attackHitbox; }

    
    private void spawnBullet(Camera camera) {
        if (!hasGun) return;
        if (ammoInClip <= 0) return;
        ammoInClip--;

        float cx = x + WIDTH / 2f;
        float cy = y + HEIGHT / 2f;

        Vector3 mousePos = new Vector3(Gdx.input.getX(), Gdx.input.getY(), 0);
        camera.unproject(mousePos);

        Vector2 target = new Vector2(mousePos.x, mousePos.y);

        bullets.add(new Bullet(cx, cy, target.x, target.y));

        if (shootSound != null) {
            shootSound.play(0.7f * gameRef.getPreferences().getSoundVolume());
        }

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
        if (!moving) {
            switch (currentFacing) {
                case LEFT -> next = idleLeft;
                case RIGHT -> next = idleRight;
                case UP -> next = idleUp;
                case DOWN -> next = idleDown;
            }
        } else {
            switch (currentFacing) {
                case LEFT -> next = animLeft;
                case RIGHT -> next = animRight;
                case UP -> next = animUp;
                case DOWN -> next = animDown;
            }
        }
        if (next != currentAnimation) {
            currentAnimation = next;
            animTime = 0f;
        }
    }


    
    public void takeDamage(int amount) {
        if (godMode || isShielded || invulnerable) return;

        int before = hp;
        hp = Math.max(0, hp - amount);
        if (ouchSound != null && hp < before) {
            ouchSound.play(gameRef.getPreferences().getSoundVolume());
        }

        invulnerable = true;
        invulnTimeLeft = 0.8f;
        damageFlash = true;
        flashTimeLeft = 0.25f;
    }

    public boolean canIgnoreTrapDamage() {
        return hasShoes;
    }

    public void onLaserHit(float chargeTime, float dt) {
        if (godMode || isShielded) return;

        float dps;
        if (chargeTime < 1f) dps = 0f;
        else if (chargeTime < 3f) dps = 0.5f;
        else dps = 1f;

        laserDamageAccumulator += dps * dt;
        laserHeat = MathUtils.clamp(chargeTime / 3f, 0f, 1f);

        if (laserDamageAccumulator >= 1f) {
            takeDamage(1);
            laserDamageAccumulator -= 1f;
        }
    }

    public void resetLaserHeat() {
        laserHeat = 0f;
        laserDamageAccumulator = 0f;
    }

    private float computeLaserGlowAlpha() {
        float chargeTime = laserHeat * 3f;

        if (chargeTime < 1f) return 0f;
        if (chargeTime < 2f) return 0.5f + 0.5f * MathUtils.sin(animTime * MathUtils.PI);
        if (chargeTime < 3f) return 0.5f + 0.5f * MathUtils.sin(animTime * MathUtils.PI * 3f);
        return 1f;
    }

    
    public void draw(SpriteBatch batch) {
        for (Bullet b : bullets) b.draw(batch, bulletRegion);

        TextureRegion frame = currentAnimation.getKeyFrame(animTime, true);
        com.badlogic.gdx.graphics.Color oldColor = batch.getColor();

        if (isShielded) {
            batch.setColor(0.5f, 0.5f, 1f, 1f);
        } else if (laserHeat > 0f) {
            float glow = computeLaserGlowAlpha();
            if (glow > 0f) {
                batch.setColor(1f,
                        MathUtils.lerp(1f, 0.2f, glow),
                        MathUtils.lerp(1f, 0.2f, glow),
                        1f);
            }
        } else if (lightningSlowed) {
            float pulse = 0.6f + 0.4f * MathUtils.sin(lightningAnimTime * MathUtils.PI * 6f);
            batch.setColor(0.5f, 0.8f, 1.0f, pulse);
        } else if (damageFlash) {
            batch.setColor(1f, 0f, 0f, 1f);
        } else if (isAttacking) {
            batch.setColor(1f, 1f, 0f, 1f);
        }

        float drawW = 88f;
        float drawH = 88f;
        float drawX = x - (drawW - WIDTH) / 2f;
        float drawY = y - (drawH - HEIGHT) / 2f;
        if (hasGun && currentFacing == Facing.UP) {
            drawGun(batch);
        }
        batch.draw(frame, drawX, drawY, drawW, drawH);
        if (hasGun && currentFacing != Facing.UP) {
            drawGun(batch);
        }
        batch.setColor(oldColor);
    }

    private void drawGun(SpriteBatch batch) {
        Animation<TextureRegion> gunAnim = pickGunAnimation();
        if (gunAnim == null) return;

        float xOffset = 10f;
        float yOffset = 4f;
        if (currentFacing == Facing.DOWN) {
            yOffset = -4f;
        }
        float gunX = x + xOffset;
        float gunY = y + yOffset;

        if (currentFacing == Facing.LEFT) {
            gunX = x - 4f;
        }

        TextureRegion gunFrame = gunAnim.getKeyFrame(animTime, true);
        float scale = 2.0f;
        float gunW = gunFrame.getRegionWidth() * scale;
        float gunH = gunFrame.getRegionHeight() * scale;
        batch.draw(gunFrame, gunX, gunY, gunW, gunH);
    }

    private Animation<TextureRegion> pickGunAnimation() {
        boolean useShoot = shooting;
        return switch (currentFacing) {
            case LEFT -> useShoot ? gunSideLeftShoot : gunSideLeftIdleRun;
            case RIGHT -> useShoot ? gunSideShoot : gunSideIdleRun;
            case UP -> useShoot ? gunUpShoot : gunUpIdleRun;
            case DOWN -> useShoot ? gunDownShoot : gunDownIdleRun;
        };
    }

    
    public void activateSpeedBoost(float duration, float multiplier) {
        this.speedTimer = duration;
        this.speedMultiplier = multiplier;
    }

    public void activateShield(float duration) {
        this.shieldTimer = duration;
        this.isShielded = true;
    }

    public void applyLightningDebuff(float duration) {
        lightningDebuffTimer = Math.max(lightningDebuffTimer, duration);
        lightningSlowed = true;
        lightningAnimTime = 0f;
    }

    public void activateFloorHazard(float speedDuration, float speedBoost) {
        this.speedTimer = speedDuration;
        this.speedMultiplier = speedBoost;
        this.hazardWallDamageTimer = 3f;
        this.hazardWallDamageUsed = false;

        slideVelocity.set(lastMovementDir).nor().scl(getWalkSpeed() * speedBoost);
        isSliding = true;
        slideTimer = SLIDE_DURATION;
        if (slipSound != null) {
            slipSound.play(gameRef.getPreferences().getSoundVolume());
        }
    }

    
    
    
    public void addExperience(int amount) {
        experience += amount;
        checkLevelUp();
    }

    private void checkLevelUp() {
        int xpNeeded = playerLevel * XP_PER_LEVEL_BASE;
        while (experience >= xpNeeded) {
            experience -= xpNeeded;
            playerLevel++;
            skillPoints++;
            hp = getMaxHp();
            xpNeeded = playerLevel * XP_PER_LEVEL_BASE;
        }
    }

    public boolean spendSkillPoint() {
        if (skillPoints > 0) {
            skillPoints--;
            return true;
        }
        return false;
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
            hp++;
            return true;
        }
        return false;
    }

    public void addPermanentStrength(int delta) {
        permanentStrength = MathUtils.clamp(permanentStrength + delta, 0, 99);
    }

    public void addPermanentSpeed(int delta) {
        permanentSpeed = MathUtils.clamp(permanentSpeed + delta, 0, 99);
    }

    
    public float getDamageMult() { return damageMult; }

    
    public void setDamageMultRaw(float v) {
        damageMult = v;
    }

    public void addDamageMult(float delta) {
        damageMult = MathUtils.clamp(damageMult + delta, 0.5f, 3.0f);
        if (progression != null) progression.setPermanentDamageMult(damageMult);
    }

    public float getBonusSpeedMult() { return bonusSpeedMult; }

    public void addBonusSpeedMult(float delta) {
        bonusSpeedMult = MathUtils.clamp(bonusSpeedMult + delta, 0.5f, 3.0f);
        if (progression != null) progression.setPermanentSpeedMult(bonusSpeedMult);
    }

    public boolean hasShoes() { return hasShoes; }
    public void giveShoes() {
        hasShoes = true;
        bonusSpeedMult = Math.max(bonusSpeedMult, 1.2f);
        if (progression != null) progression.setPermanentSpeedMult(bonusSpeedMult);
    }

    public boolean hasGloves() { return hasGloves; }
    public void giveGloves() { hasGloves = true; }

    public float getGloveKnockbackStrength() {
        return hasGloves ? 250f : 0f;
    }

    
    public int getStrengthStat() { return strengthStat; }
    public int getSpeedStat() { return speedStat; }

    public void setStrengthStat(int v) {
        strengthStat = MathUtils.clamp(v, 1, 99);
        if (progression != null) progression.setPermanentStrength(strengthStat);
    }

    
    public void setStrengthStatRaw(int v) {
        strengthStat = v;
    }

    public void setSpeedStat(int v) {
        speedStat = MathUtils.clamp(v, 1, 99);
        if (progression != null) progression.setPermanentSpeedStat(speedStat);
    }

    public void addStrengthStat(int delta) { setStrengthStat(strengthStat + delta); }
    public void addSpeedStat(int delta) { setSpeedStat(speedStat + delta); }

    public boolean hasFreezeWeapon() { return hasFreezeWeapon; }
    public boolean hasPoisonKnuckles() { return hasPoisonKnuckles; }

    public void unlockFreezeWeapon() {
        hasFreezeWeapon = true;
        if (progression != null) progression.setHasFreezeWeapon(true);
    }

    public void unlockPoisonKnuckles() {
        hasPoisonKnuckles = true;
        if (progression != null) progression.setHasPoisonKnuckles(true);
    }

    
    public void addCoins(int amount) {
        if (amount <= 0) return;
        coins = Math.max(0, coins + amount);
        if (progression != null) progression.setCoins(coins);
    }

    public boolean spendCoins(int amount) {
        if (amount <= 0) return true;
        if (coins < amount) return false;
        coins -= amount;
        if (progression != null) progression.setCoins(coins);
        return true;
    }

    
    public int getCoins() { return coins; }

    public int getHeartsCount() {
        return MathUtils.clamp((int) Math.ceil(hp / (float) HP_PER_UPGRADE), 0, getMaxHearts());
    }


    public int getMaxHearts() {
        return (BASE_HP / HP_PER_UPGRADE) + skillHealthLevel + permanentHeartsBonus;
    }

    public int getMaxHp() {
        return BASE_HP + (skillHealthLevel + permanentHeartsBonus) * HP_PER_UPGRADE;
    }

    public int getHp() { return hp; }

    public void setHp(int hp) {
        this.hp = MathUtils.clamp(hp, 0, getMaxHp());
    }

    public void healHp(int amount) {
        if (amount <= 0) return;
        setHp(this.hp + amount);
    }

    public void addHeartContainer(int hearts) {
        if (hearts <= 0) return;
        permanentHeartsBonus += hearts;
        hp = getMaxHp();
    }

    public int getLives() {
        return getHp();
    }

    public void setLives(int lives) {
        setHp(lives);
    }

    public void addLife() {
        healHp(HP_PER_UPGRADE);
    }
    public void setPlayerLevel(int level) { this.playerLevel = Math.max(1, level); }
    public int getPlayerLevel() { return playerLevel; }

    public void setExperience(int experience) { this.experience = Math.max(0, experience); }
    public int getExperience() { return experience; }

    public int getXpNeededForNextLevel() { return playerLevel * XP_PER_LEVEL_BASE; }

    public void setSkillSpeedLevel(int level) { this.skillSpeedLevel = MathUtils.clamp(level, 0, 5); }
    public int getSkillSpeedLevel() { return skillSpeedLevel; }

    public void setSkillHealthLevel(int level) { this.skillHealthLevel = MathUtils.clamp(level, 0, 5); }
    public int getSkillHealthLevel() { return skillHealthLevel; }

    public int getSkillPoints() { return skillPoints; }
    public void setSkillPoints(int points) { this.skillPoints = Math.max(0, points); }

    public float getWalkSpeed() { return WALK_SPEED_BASE + (skillSpeedLevel * 50f); }

    public void setSpeedMultiplier(float m) { this.speedMultiplier = Math.max(0.1f, m); }
    public float getSpeedMultiplier() { return speedMultiplier; }
    public void resetSpeedMultiplier() { this.speedMultiplier = 1f; }

    public void setPosition(float x, float y) { this.x = x; this.y = y; }
    public float getX() { return x; }
    public float getY() { return y; }

    public boolean hasKey() { return hasKey; }
    public void giveKey() { hasKey = true; }
    public void clearKey() { hasKey = false; }

    public void setScore(int score) { this.score = score; }
    public void addScore(int amount) { this.score += amount; }
    public int getScore() { return score; }

    public void equipGun() {
        hasGun = true;
        if (ammoInClip == 0 && ammoReserve == 0) {
            ammoInClip = MAX_CLIP_AMMO;
        }
    }
    public boolean hasGun() { return hasGun; }
    public int getAmmoInClip() { return ammoInClip; }
    public int getAmmoReserve() { return ammoReserve; }
    public void setAmmo(int clip, int reserve) {
        ammoInClip = Math.max(0, Math.min(MAX_CLIP_AMMO, clip));
        ammoReserve = Math.max(0, reserve);
    }
    public void addAmmo(int amount) {
        if (amount <= 0) return;
        ammoReserve += amount;
        if (hasGun && ammoInClip == 0) {
            reload();
        }
    }
    private void reload() {
        if (ammoInClip >= MAX_CLIP_AMMO) return;
        if (ammoReserve <= 0) return;
        int needed = MAX_CLIP_AMMO - ammoInClip;
        int taken = Math.min(needed, ammoReserve);
        ammoInClip += taken;
        ammoReserve -= taken;
        if (reloadSound != null) {
            reloadSound.play(gameRef.getPreferences().getSoundVolume());
        }
    }
    public boolean isDamageFlashing() { return damageFlash; }

    public boolean isGodMode() { return godMode; }
    public boolean isNoClip() { return noClip; }
    public void setGodMode(boolean v) { godMode = v; }
    public void setNoClip(boolean v) { noClip = v; }

    public boolean isRepulseActive() { return isRepulsing; }

    public void unlockRapidFire() { skillRapidFire = true; }
    public void unlockShotgun() { skillShotgun = true; }
    public void unlockPiercing() { skillPiercing = true; }
    public void dispose() {
        if (shootSound != null) shootSound.dispose();
        if (reloadSound != null) reloadSound.dispose();
        if (slipSound != null) slipSound.dispose();
        if (ouchSound != null) ouchSound.dispose();
    }

    public void unlockDash() { skillDash = true; }
    public void unlockGhost() { skillGhost = true; }

    public void unlockThorns() { skillThorns = true; }
    public void unlockRepulse() { skillRepulse = true; }

    public boolean hasRapidFire() { return skillRapidFire; }
    public boolean hasShotgun() { return skillShotgun; }
    public boolean hasPiercing() { return skillPiercing; }
    public boolean hasDash() { return skillDash; }
    public boolean hasGhost() { return skillGhost; }
    public boolean hasThorns() { return skillThorns; }
    public boolean hasRepulse() { return skillRepulse; }
}
