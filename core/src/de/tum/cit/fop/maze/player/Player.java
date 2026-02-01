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

/**
 * Core player entity handling movement, combat, progression, and rendering.
 */
public class Player {
    // Attributes
    /** Player x-coordinate in world units. */
    private float x;
    /** Player y-coordinate in world units. */
    private float y;

    /** Damage multiplier applied to player attacks. */
    private float damageMult = 1.0f;
    /** Bonus speed multiplier from upgrades or perks. */
    private float bonusSpeedMult = 1.0f;

    /** Whether the player has shoes for trap immunity. */
    private boolean hasShoes = false;
    /** Whether the player has gloves for knockback. */
    private boolean hasGloves = false;

    /** Strength stat affecting damage. */
    private int strengthStat = 5;
    /** Speed stat affecting movement. */
    private int speedStat = 5;
    /** Whether freeze weapon skill is active. */
    private boolean hasFreezeWeapon = false;
    /** Whether poison knuckles skill is active. */
    private boolean hasPoisonKnuckles = false;
    /** Current hit points. */
    private int hp;

    /** Base HP before upgrades. */
    private static final int BASE_HP = 100;
    /** HP gained per upgrade level. */
    private static final int HP_PER_UPGRADE = 5;

    /** Extra hearts granted from progression. */
    private int permanentHeartsBonus = 0;


    // Coins (persisted)
    /** Current coin count. */
    private int coins = 0;
    /** Progression data store for persistent stats. */
    private final Progression progression;
    /** Game reference for asset access and audio. */
    private final MazeRunnerGame gameRef;
    /** HUD notification callback. */
    private transient java.util.function.Consumer<String> hudNotifier;

    /** Whether the player has a key. */
    private boolean hasKey = false;
    /** Whether the player has collected the story item. */
    private boolean hasStoryItem = false;
    /** Current story item display name. */
    private String currentStoryItemName = "";
    /** Current score. */
    private int score = 0;

    // Status
    /** Whether the player is invulnerable. */
    private boolean invulnerable = false;
    /** Remaining invulnerability time. */
    private float invulnTimeLeft = 0f;
    /** Whether a damage flash is active. */
    private boolean damageFlash = false;
    /** Remaining damage flash time. */
    private float flashTimeLeft = 0f;

    // Movement
    /** Base walk speed in world units per second. */
    private static final float WALK_SPEED_BASE = 300f;
    /** Run speed multiplier. */
    private static final float RUN_MULTIPLIER = 1.7f;

    /** Temporary speed multiplier from power-ups and debuffs. */
    private float speedMultiplier = 1f; // temporary speed powerups + slow debuffs
    /** Remaining time for temporary speed boost. */
    private float speedTimer = 0f;

    // Dimensions
    /** Player collision width. */
    private static final float WIDTH = 32f;
    /** Player collision height. */
    private static final float HEIGHT = 32f;
    /** Collision inset to tighten bounds. */
    private static final float COLLISION_INSET = 2f;

    // Animation - Walking
    /** Walking animation facing down. */
    private final Animation<TextureRegion> animDown, animUp, animLeft, animRight;
    /** Idle animation facing down. */
    private final Animation<TextureRegion> idleDown, idleUp, idleLeft, idleRight;
    /** Current body animation. */
    private Animation<TextureRegion> currentAnimation;
    /** Accumulated animation time. */
    private float animTime = 0f;

    // Animation - Shooting
    /** Shooting animation facing left. */
    private final Animation<TextureRegion> shootLeftAnim;
    /** Shooting animation facing right. */
    private final Animation<TextureRegion> shootRightAnim;
    /** Shooting animation facing up. */
    private final Animation<TextureRegion> shootUpAnim;
    /** Shooting animation facing down. */
    private final Animation<TextureRegion> shootDownAnim;
    /** Gun idle/run animation facing down. */
    private final Animation<TextureRegion> gunDownIdleRun;
    /** Gun idle/run animation facing up. */
    private final Animation<TextureRegion> gunUpIdleRun;
    /** Gun idle/run animation facing right. */
    private final Animation<TextureRegion> gunSideIdleRun;
    /** Gun idle/run animation facing left. */
    private final Animation<TextureRegion> gunSideLeftIdleRun;
    /** Gun shooting animation facing down. */
    private final Animation<TextureRegion> gunDownShoot;
    /** Gun shooting animation facing up. */
    private final Animation<TextureRegion> gunUpShoot;
    /** Gun shooting animation facing right. */
    private final Animation<TextureRegion> gunSideShoot;
    /** Gun shooting animation facing left. */
    private final Animation<TextureRegion> gunSideLeftShoot;
    /** Bat attack animation facing down. */
    private final Animation<TextureRegion> batDownAttack;
    /** Bat attack animation facing up. */
    private final Animation<TextureRegion> batUpAttack;
    /** Bat attack animation facing right. */
    private final Animation<TextureRegion> batSideAttack;
    /** Bat attack animation facing left. */
    private final Animation<TextureRegion> batSideLeftAttack;
    /** Whether the player is currently shooting. */
    private boolean shooting = false;
    /** Sound effect for shooting. */
    private Sound shootSound;
    /** Sound effect for reloading. */
    private Sound reloadSound;
    /** Sound effect for sliding. */
    private Sound slipSound;
    private Sound ouchSound;


    // Powerups
    /** Whether the player is shielded. */
    private boolean isShielded = false;
    /** Remaining shield duration. */
    private float shieldTimer = 0f;

    // Debug
    /** Whether god mode is enabled. */
    private boolean godMode = false;
    /** Whether noclip is enabled. */
    private boolean noClip = false;

    // RPG / Leveling
    /** Current experience points. */
    private int experience = 0;
    /** Current player level. */
    private int playerLevel = 1;
    /** Unspent skill points. */
    private int skillPoints = 0;
    /** XP required per level baseline. */
    private static final int XP_PER_LEVEL_BASE = 100;

    // Skills (0 to 5)
    /** Speed skill level. */
    private int skillSpeedLevel = 0;
    /** Health skill level. */
    private int skillHealthLevel = 0;

    // Combat and weapons
    /** Whether the player has a gun. */
    private boolean hasGun = false;
    /** Maximum bullets per clip. */
    private static final int MAX_CLIP_AMMO = 20;
    /** Ammo currently in clip. */
    private int ammoInClip = 0;
    /** Ammo in reserve. */
    private int ammoReserve = 0;

    /** Whether a melee attack is in progress. */
    private boolean isAttacking = false;
    /** Remaining attack time. */
    private float attackTimer = 0f;
    /** Duration of a melee attack. */
    private static final float ATTACK_DURATION = 0.25f;
    /** Cooldown between melee attacks. */
    private static final float ATTACK_COOLDOWN = 0.5f;
    /** Remaining melee cooldown time. */
    private float attackCooldownTimer = 0f;
    /** Active melee hitbox. */
    private Rectangle attackHitbox;


    // Direction
    /** Last facing direction vector. */
    private final Vector2 facingDirection = new Vector2(0, -1);

    // Floor hazard effect
    /** Remaining time of wall damage after hazard slip. */
    private float hazardWallDamageTimer = 0f;
    /** Remaining slide timer. */
    private float slideTimer = 0f;
    /** Duration of sliding after hazard. */
    private static final float SLIDE_DURATION = 3.5f;
    /** Damage applied when hitting walls during hazard. */
    private static final int HAZARD_WALL_DAMAGE = 1;
    /** Control multiplier applied during hazard effects. */
    private float controlMultiplier = 1f;

    /** Velocity used during sliding. */
    private final Vector2 slideVelocity = new Vector2();
    /** Whether the player is sliding. */
    private boolean isSliding = false;
    /** Last non-zero movement direction. */
    private final Vector2 lastMovementDir = new Vector2(0, 1);
    /** Whether wall damage has been applied for current hazard. */
    private boolean hazardWallDamageUsed = false;

    // Lightning trap visuals/debuff
    /** Whether lightning slow is active. */
    private boolean lightningSlowed = false;
    /** Animation timer for lightning effect. */
    private float lightningAnimTime = 0f;
    /** Remaining lightning debuff time. */
    private float lightningDebuffTimer = 0f;

    // Repulse Skill
    /** Whether repulse is active this frame. */
    private boolean isRepulsing = false;
    /** Remaining repulse cooldown time. */
    private float repulseCooldownTimer = 0f;

    /** Cardinal facing directions for animation selection. */
    private enum Facing { UP, DOWN, LEFT, RIGHT }
    /** Current facing direction. */
    private Facing currentFacing = Facing.DOWN;

    // Bullets
    /** Active bullets fired by the player. */
    private final List<Bullet> bullets = new ArrayList<>();
    /** Texture region for bullet rendering. */
    private final TextureRegion bulletRegion;

    // Skill flags
    /** Whether rapid fire is unlocked. */
    private boolean skillRapidFire = false;
    /** Whether shotgun is unlocked. */
    private boolean skillShotgun = false;
    /** Whether piercing shots are unlocked. */
    private boolean skillPiercing = false;

    /** Whether dash is unlocked. */
    private boolean skillDash = false;
    /** Whether ghost walker is unlocked. */
    private boolean skillGhost = false;

    /** Whether thorns is unlocked. */
    private boolean skillThorns = false;
    /** Whether repulse is unlocked. */
    private boolean skillRepulse = false;

    // Dash
    /** Whether a dash is currently active. */
    private boolean isDashing = false;
    /** Remaining dash time. */
    private float dashTimer = 0f;
    /** Remaining dash cooldown time. */
    private float dashCooldown = 0f;
    /** Direction of current dash. */
    private final Vector2 dashDirection = new Vector2();
    /** Dash duration in seconds. */
    private static final float DASH_DURATION = 0.15f;
    /** Dash speed in world units per second. */
    private static final float DASH_SPEED = 1200f;
    /** Max dash cooldown time. */
    private static final float DASH_COOLDOWN_MAX = 1.0f;

    // Shooting cooldown
    /** Remaining time until next allowed shot. */
    private float shootTimer = 0f;

    // Drone laser
    /** Accumulated laser damage for fractional ticks. */
    private float laserDamageAccumulator = 0f;
    /** Current laser heat value. */
    private float laserHeat = 0f;
    /** Permanent strength stat (debug). */
    private int permanentStrength = 5; // starts playable
    /** Permanent speed stat (debug). */
    private int permanentSpeed = 5;

    /** Whether freeze weapon is enabled for this run. */
    private boolean freezeWeapon = false;
    /** Whether poison knuckles is enabled for this run. */
    private boolean poisonKnuckles = false;

    /**
     * Interface for checking blocking geometry beyond maze walls.
     */
    @FunctionalInterface
    public interface BlockChecker {
        /**
         * Checks whether a rectangle is blocked.
         *
         * @param playerRect Rectangle to test
         * @return True if blocked
         */
        boolean isBlocked(Rectangle playerRect);
    }

    // Constructor
    /**
     * Creates a new player instance with starting position and animations.
     *
     * @param startX Initial x-coordinate
     * @param startY Initial y-coordinate
     * @param progression Progression data source
     * @param down Downward walk animation
     * @param up Upward walk animation
     * @param left Left walk animation
     * @param right Right walk animation
     * @param game Game instance for assets and audio
     */
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
        this.batDownAttack = game.getBatDownAttack();
        this.batUpAttack = game.getBatUpAttack();
        this.batSideAttack = game.getBatSideAttack();
        this.batSideLeftAttack = game.getBatSideLeftAttack();

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

    // If permanent hearts updated while running
    /**
     * Refreshes permanent progression values while a run is active.
     *
     * @param game Game instance providing progression data
     */
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

    /**
     * Applies run-specific gambling bonuses and modifiers.
     *
     * @param run Run state holding gambling modifiers
     */
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

    // Main update loop
    /**
     * Updates the player state each frame.
     *
     * @param dt Delta time in seconds
     * @param maze Current maze
     * @param game Game instance for preferences and audio
     * @param blockChecker Additional collision checker, or null
     * @param camera Camera used for mouse aiming
     */
    public void update(float dt, Maze maze, MazeRunnerGame game, BlockChecker blockChecker, Camera camera) {
        updateTimers(dt);

        if (attackCooldownTimer > 0) attackCooldownTimer -= dt;
        if (dashCooldown > 0) dashCooldown -= dt;
        if (shootTimer > 0) shootTimer -= dt;
        if (repulseCooldownTimer > 0) repulseCooldownTimer -= dt;

        // Dash
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

        // Sliding
        if (isSliding) {
            if (input.dx != 0f || input.dy != 0f) {
                Vector2 inputVelocity = new Vector2(input.dx / dt, input.dy / dt);
                slideVelocity.lerp(inputVelocity, 0.08f);
            }
            moveDx = slideVelocity.x * dt;
            moveDy = slideVelocity.y * dt;
        }

        // Dash skill
        if (skillDash && Gdx.input.isKeyJustPressed(game.getPreferences().getKeyDash()) && dashCooldown <= 0 && moving) {
            startDash(input.dx, input.dy);
        }

        // Repulse skill
        if (skillRepulse && Gdx.input.isKeyJustPressed(game.getPreferences().getKeyRepulse()) && repulseCooldownTimer <= 0) {
            isRepulsing = true;
            repulseCooldownTimer = 5.0f;
        }

        // Melee
        if (!isParalyzed() && Gdx.input.isKeyJustPressed(game.getPreferences().getKeyMelee())) {
            attack();
        }

        // Shooting
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

        // Anim
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

        // Movement
        attemptMove(moveDx, moveDy, maze, game, blockChecker);

        // Bullets
        updateBullets(dt);

        // Melee window
        if (isAttacking) {
            attackTimer -= dt;
            if (attackTimer <= 0) isAttacking = false;
        }
        updateAttackHitbox();
    }

    /**
     * Starts a dash in the given direction.
     *
     * @param moveDx Horizontal movement direction
     * @param moveDy Vertical movement direction
     */
    private void startDash(float moveDx, float moveDy) {
        isDashing = true;
        dashTimer = DASH_DURATION;
        dashCooldown = DASH_COOLDOWN_MAX;

        dashDirection.set(moveDx, moveDy).nor();
        if (dashDirection.isZero()) dashDirection.set(1, 0);
    }

    /**
     * Advances internal timers and temporary effects.
     *
     * @param dt Delta time in seconds
     */
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

    /**
     * Checks whether the player is slowed by lightning debuff.
     *
     * @return True if lightning debuff is active
     */
    public boolean isLightningDebuffed() { return lightningDebuffTimer > 0f; }
    /**
     * Checks whether the player is paralyzed (used for lightning debuff).
     *
     * @return True if paralyzed
     */
    public boolean isParalyzed() { return lightningDebuffTimer > 0f; }

    /**
     * Movement input snapshot for this frame.
     *
     * @param dx Movement delta x
     * @param dy Movement delta y
     */
    private record MovementInput(float dx, float dy) {}

    /**
     * Reads movement input and computes movement deltas.
     *
     * @param dt Delta time in seconds
     * @param game Game instance providing key bindings
     * @return Movement input for this frame
     */
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

    /**
     * Updates facing direction based on movement keys.
     *
     * @param game Game instance providing key bindings
     */
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

    /**
     * Updates facing direction based on mouse position.
     *
     * @param camera Camera used to unproject the mouse position
     */
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

    /**
     * Attempts to move the player while checking collisions.
     *
     * @param dx X movement delta
     * @param dy Y movement delta
     * @param maze Maze reference
     * @param game Game instance for audio
     * @param blockChecker Optional extra collision checker
     */
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

    /**
     * Handles wall collision behavior.
     *
     * @param game Game instance for audio
     */
    private void onWallHit(MazeRunnerGame game) {
        game.playWallHitSound();

        if (hazardWallDamageTimer > 0f && !hazardWallDamageUsed) {
            takeDamage(HAZARD_WALL_DAMAGE);
            hazardWallDamageUsed = true;
        }
    }

    /**
     * Checks whether the player can occupy a position in the maze.
     *
     * @param testX Candidate x-coordinate
     * @param testY Candidate y-coordinate
     * @param maze Maze reference
     * @return True if the position is not blocked
     */
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

    /**
     * Tests if a world coordinate is blocked by walls or exit rules.
     *
     * @param worldX World x-coordinate
     * @param worldY World y-coordinate
     * @param maze Maze reference
     * @return True if blocked
     */
    private boolean isBlocked(float worldX, float worldY, Maze maze) {
        int cx = (int) (worldX / maze.getTileSize());
        int cy = (int) (worldY / maze.getTileSize());
        if (maze.isWallCell(cx, cy)) return true;
        return maze.isBlockedExit(cx, cy, hasKey, hasStoryItem);
    }

    /**
     * Creates a collision rectangle at the given position.
     *
     * @param px X position
     * @param py Y position
     * @return Rectangle for collision checks
     */
    private Rectangle makeRect(float px, float py) {
        return new Rectangle(px, py, WIDTH, HEIGHT);
    }

    // Close combat logic
    /**
     * Starts a melee attack if off cooldown.
     *
     * @return True if the attack started
     */
    public boolean attack() {
        if (attackCooldownTimer <= 0 && !isAttacking) {
            isAttacking = true;
            attackTimer = ATTACK_DURATION;
            attackCooldownTimer = ATTACK_COOLDOWN;
            return true;
        }
        return false;
    }

    /**
     * Updates the melee hitbox based on the player position.
     */
    private void updateAttackHitbox() {
        float range = 40f;
        if (attackHitbox == null) attackHitbox = new Rectangle();
        attackHitbox.set(x - range / 2f, y - range / 2f, WIDTH + range, HEIGHT + range);
    }

    /**
     * Checks whether a melee attack is active.
     *
     * @return True if attacking
     */
    public boolean isAttacking() { return isAttacking; }
    /**
     * Returns the current melee hitbox.
     *
     * @return Attack hitbox, or null if not initialized
     */
    public Rectangle getAttackHitbox() { return attackHitbox; }

    // Bullets
    /**
     * Spawns a bullet toward the mouse cursor.
     *
     * @param camera Camera used to unproject the cursor
     */
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

    /**
     * Updates all bullets and removes inactive ones.
     *
     * @param dt Delta time in seconds
     */
    private void updateBullets(float dt) {
        Iterator<Bullet> it = bullets.iterator();
        while (it.hasNext()) {
            Bullet b = it.next();
            b.update(dt);
            if (!b.isAlive()) it.remove();
        }
    }

    /**
     * Returns active bullets fired by the player.
     *
     * @return Bullet list
     */
    public List<Bullet> getBullets() { return bullets; }

    /**
     * Selects the shooting animation based on facing direction.
     */
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

    /**
     * Selects a walk or idle animation based on movement.
     *
     * @param moving Whether the player is moving
     */
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


    // Dmg, traps and laser
    /**
     * Applies damage to the player if not protected.
     *
     * @param amount Damage amount
     */
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

    /**
     * Checks whether trap damage can be ignored.
     *
     * @return True if trap damage is ignored
     */
    public boolean canIgnoreTrapDamage() {
        return hasShoes;
    }

    /**
     * Applies damage over time from drone laser hits.
     *
     * @param chargeTime Current laser charge time
     * @param dt Delta time in seconds
     */
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

    /**
     * Resets laser heat and accumulated damage.
     */
    public void resetLaserHeat() {
        laserHeat = 0f;
        laserDamageAccumulator = 0f;
    }

    /**
     * Computes alpha for laser glow effect.
     *
     * @return Glow alpha value
     */
    private float computeLaserGlowAlpha() {
        float chargeTime = laserHeat * 3f;

        if (chargeTime < 1f) return 0f;
        if (chargeTime < 2f) return 0.5f + 0.5f * MathUtils.sin(animTime * MathUtils.PI);
        if (chargeTime < 3f) return 0.5f + 0.5f * MathUtils.sin(animTime * MathUtils.PI * 3f);
        return 1f;
    }

    // Player render
    /**
     * Renders the player, weapons, and bullets.
     *
     * @param batch Sprite batch used for drawing
     */
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
        }

        float drawW = 88f;
        float drawH = 88f;
        float drawX = x - (drawW - WIDTH) / 2f;
        float drawY = y - (drawH - HEIGHT) / 2f;
        if (hasGun && currentFacing == Facing.UP) {
            drawGun(batch);
        }
        if (!hasGun && isAttacking) {
            drawMelee(batch);
        }
        batch.draw(frame, drawX, drawY, drawW, drawH);
        if (hasGun && currentFacing != Facing.UP) {
            drawGun(batch);
        }
        if (!hasGun && isAttacking && currentFacing != Facing.UP) {
            drawMelee(batch);
        }
        batch.setColor(oldColor);
    }

    /**
     * Draws the gun overlay for the current facing direction.
     *
     * @param batch Sprite batch used for drawing
     */
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

    /**
     * Chooses the gun animation based on facing and shooting.
     *
     * @return Selected gun animation
     */
    private Animation<TextureRegion> pickGunAnimation() {
        boolean useShoot = shooting;
        return switch (currentFacing) {
            case LEFT -> useShoot ? gunSideLeftShoot : gunSideLeftIdleRun;
            case RIGHT -> useShoot ? gunSideShoot : gunSideIdleRun;
            case UP -> useShoot ? gunUpShoot : gunUpIdleRun;
            case DOWN -> useShoot ? gunDownShoot : gunDownIdleRun;
        };
    }

    /**
     * Draws the melee weapon animation.
     *
     * @param batch Sprite batch used for drawing
     */
    private void drawMelee(SpriteBatch batch) {
        Animation<TextureRegion> melee = pickBatAnimation();
        if (melee == null) return;

        TextureRegion frame = melee.getKeyFrame(animTime, true);
        float scale = 2.0f;
        float xOffset = 6f;
        float yOffset = 0f;
        if (currentFacing == Facing.UP) {
            yOffset = 4f;
        } else if (currentFacing == Facing.DOWN) {
            yOffset = -4f;
        }

        float drawX = x + xOffset;
        float drawY = y + yOffset;

        float w = frame.getRegionWidth() * scale;
        float h = frame.getRegionHeight() * scale;
        batch.draw(frame, drawX, drawY, w, h);
    }

    /**
     * Chooses the bat attack animation based on facing.
     *
     * @return Selected bat animation
     */
    private Animation<TextureRegion> pickBatAnimation() {
        return switch (currentFacing) {
            case LEFT -> batSideLeftAttack;
            case RIGHT -> batSideAttack;
            case UP -> batUpAttack;
            case DOWN -> batDownAttack;
        };
    }

    // powerups
    /**
     * Activates a temporary speed boost.
     *
     * @param duration Boost duration in seconds
     * @param multiplier Speed multiplier
     */
    public void activateSpeedBoost(float duration, float multiplier) {
        this.speedTimer = duration;
        this.speedMultiplier = multiplier;
    }

    /**
     * Activates a temporary shield.
     *
     * @param duration Shield duration in seconds
     */
    public void activateShield(float duration) {
        this.shieldTimer = duration;
        this.isShielded = true;
    }

    /**
     * Applies a lightning debuff to the player.
     *
     * @param duration Debuff duration in seconds
     */
    public void applyLightningDebuff(float duration) {
        lightningDebuffTimer = Math.max(lightningDebuffTimer, duration);
        lightningSlowed = true;
        lightningAnimTime = 0f;
    }

    /**
     * Activates floor hazard slide and speed effect.
     *
     * @param speedDuration Duration of speed boost
     * @param speedBoost Speed multiplier
     */
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

    // =========================
    // LEVELING
    // =========================
    /**
     * Adds experience and handles level-ups.
     *
     * @param amount XP to add
     */
    public void addExperience(int amount) {
        experience += amount;
        checkLevelUp();
    }

    /**
     * Checks for level-ups and applies stat gains.
     */
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

    /**
     * Spends one skill point if available.
     *
     * @return True if a point was spent
     */
    public boolean spendSkillPoint() {
        if (skillPoints > 0) {
            skillPoints--;
            return true;
        }
        return false;
    }

    /**
     * Upgrades speed skill if possible.
     *
     * @return True if upgraded
     */
    public boolean upgradeSpeed() {
        if (skillPoints > 0 && skillSpeedLevel < 5) {
            skillPoints--;
            skillSpeedLevel++;
            return true;
        }
        return false;
    }

    /**
     * Upgrades health skill if possible.
     *
     * @return True if upgraded
     */
    public boolean upgradeHealth() {
        if (skillPoints > 0 && skillHealthLevel < 5) {
            skillPoints--;
            skillHealthLevel++;
            hp++;
            return true;
        }
        return false;
    }

    /**
     * Adds permanent strength (debug/progression use).
     *
     * @param delta Strength delta
     */
    public void addPermanentStrength(int delta) {
        permanentStrength = MathUtils.clamp(permanentStrength + delta, 0, 99);
    }

    /**
     * Adds permanent speed (debug/progression use).
     *
     * @param delta Speed delta
     */
    public void addPermanentSpeed(int delta) {
        permanentSpeed = MathUtils.clamp(permanentSpeed + delta, 0, 99);
    }

    // Gambling buff
    /**
     * Retrieves the damage multiplier.
     *
     * @return Damage multiplier
     */
    public float getDamageMult() { return damageMult; }

    // Dev override (no clamp, not persisted)
    /**
     * Sets the damage multiplier directly without clamping.
     *
     * @param v Damage multiplier value
     */
    public void setDamageMultRaw(float v) {
        damageMult = v;
    }

    /**
     * Adds to the damage multiplier and persists it.
     *
     * @param delta Delta to add
     */
    public void addDamageMult(float delta) {
        damageMult = MathUtils.clamp(damageMult + delta, 0.5f, 3.0f);
        if (progression != null) progression.setPermanentDamageMult(damageMult);
    }

    /**
     * Retrieves the bonus speed multiplier.
     *
     * @return Bonus speed multiplier
     */
    public float getBonusSpeedMult() { return bonusSpeedMult; }

    /**
     * Adds to the bonus speed multiplier and persists it.
     *
     * @param delta Delta to add
     */
    public void addBonusSpeedMult(float delta) {
        bonusSpeedMult = MathUtils.clamp(bonusSpeedMult + delta, 0.5f, 3.0f);
        if (progression != null) progression.setPermanentSpeedMult(bonusSpeedMult);
    }

    /**
     * Checks whether the player has shoes.
     *
     * @return True if shoes are owned
     */
    public boolean hasShoes() { return hasShoes; }
    /**
     * Grants shoes and updates speed multiplier.
     */
    public void giveShoes() {
        hasShoes = true;
        bonusSpeedMult = Math.max(bonusSpeedMult, 1.2f);
        if (progression != null) progression.setPermanentSpeedMult(bonusSpeedMult);
    }

    /**
     * Checks whether the player has gloves.
     *
     * @return True if gloves are owned
     */
    public boolean hasGloves() { return hasGloves; }
    /**
     * Grants gloves to the player.
     */
    public void giveGloves() { hasGloves = true; }

    /**
     * Retrieves knockback strength from gloves.
     *
     * @return Knockback strength
     */
    public float getGloveKnockbackStrength() {
        return hasGloves ? 250f : 0f;
    }

    // Slot stats
    /**
     * Retrieves the strength stat value.
     *
     * @return Strength stat
     */
    public int getStrengthStat() { return strengthStat; }
    /**
     * Retrieves the speed stat value.
     *
     * @return Speed stat
     */
    public int getSpeedStat() { return speedStat; }

    /**
     * Sets the strength stat and persists it.
     *
     * @param v Strength value
     */
    public void setStrengthStat(int v) {
        strengthStat = MathUtils.clamp(v, 1, 99);
        if (progression != null) progression.setPermanentStrength(strengthStat);
    }

    // Dev override (no clamp, not persisted)
    /**
     * Sets the strength stat directly without clamping.
     *
     * @param v Strength value
     */
    public void setStrengthStatRaw(int v) {
        strengthStat = v;
    }

    /**
     * Sets the speed stat and persists it.
     *
     * @param v Speed value
     */
    public void setSpeedStat(int v) {
        speedStat = MathUtils.clamp(v, 1, 99);
        if (progression != null) progression.setPermanentSpeedStat(speedStat);
    }

    /**
     * Adds to the strength stat.
     *
     * @param delta Strength delta
     */
    public void addStrengthStat(int delta) { setStrengthStat(strengthStat + delta); }
    /**
     * Adds to the speed stat.
     *
     * @param delta Speed delta
     */
    public void addSpeedStat(int delta) { setSpeedStat(speedStat + delta); }

    /**
     * Checks if freeze weapon is active.
     *
     * @return True if freeze weapon is active
     */
    public boolean hasFreezeWeapon() { return hasFreezeWeapon; }
    /**
     * Checks if poison knuckles is active.
     *
     * @return True if poison knuckles is active
     */
    public boolean hasPoisonKnuckles() { return hasPoisonKnuckles; }

    /**
     * Unlocks freeze weapon and persists it.
     */
    public void unlockFreezeWeapon() {
        hasFreezeWeapon = true;
        if (progression != null) progression.setHasFreezeWeapon(true);
    }

    /**
     * Unlocks poison knuckles and persists it.
     */
    public void unlockPoisonKnuckles() {
        hasPoisonKnuckles = true;
        if (progression != null) progression.setHasPoisonKnuckles(true);
    }

    // Coins
    /**
     * Adds coins to the player and persists the value.
     *
     * @param amount Coins to add
     */
    public void addCoins(int amount) {
        if (amount <= 0) return;
        coins = Math.max(0, coins + amount);
        if (progression != null) progression.setCoins(coins);
    }

    /**
     * Spends coins if available.
     *
     * @param amount Coins to spend
     * @return True if the spend succeeded
     */
    public boolean spendCoins(int amount) {
        if (amount <= 0) return true;
        if (coins < amount) return false;
        coins -= amount;
        if (progression != null) progression.setCoins(coins);
        return true;
    }

    // Getters & Setters
    /**
     * Retrieves current coin count.
     *
     * @return Coin count
     */
    public int getCoins() { return coins; }

    /**
     * Calculates the number of heart containers currently filled.
     *
     * @return Heart count
     */
    public int getHeartsCount() {
        return MathUtils.clamp((int) Math.ceil(hp / (float) HP_PER_UPGRADE), 0, getMaxHearts());
    }


    /**
     * Calculates maximum number of heart containers.
     *
     * @return Max hearts
     */
    public int getMaxHearts() {
        return (BASE_HP / HP_PER_UPGRADE) + skillHealthLevel + permanentHeartsBonus;
    }

    /**
     * Calculates maximum HP based on upgrades.
     *
     * @return Max HP
     */
    public int getMaxHp() {
        return BASE_HP + (skillHealthLevel + permanentHeartsBonus) * HP_PER_UPGRADE;
    }

    /**
     * Retrieves current HP.
     *
     * @return Current HP
     */
    public int getHp() { return hp; }

    /**
     * Sets current HP within valid range.
     *
     * @param hp HP value to set
     */
    public void setHp(int hp) {
        this.hp = MathUtils.clamp(hp, 0, getMaxHp());
    }

    /**
     * Heals the player by a given amount.
     *
     * @param amount Healing amount
     */
    public void healHp(int amount) {
        if (amount <= 0) return;
        setHp(this.hp + amount);
    }

    /**
     * Adds permanent heart containers.
     *
     * @param hearts Number of containers to add
     */
    public void addHeartContainer(int hearts) {
        if (hearts <= 0) return;
        permanentHeartsBonus += hearts;
        hp = getMaxHp();
    }

    /**
     * Retrieves current lives (mapped to HP).
     *
     * @return Lives value
     */
    public int getLives() {
        return getHp();
    }

    /**
     * Sets lives (mapped to HP).
     *
     * @param lives Lives value
     */
    public void setLives(int lives) {
        setHp(lives);
    }

    /**
     * Adds one life container worth of HP.
     */
    public void addLife() {
        healHp(HP_PER_UPGRADE);
    }
    /**
     * Sets the player level.
     *
     * @param level Level to set
     */
    public void setPlayerLevel(int level) { this.playerLevel = Math.max(1, level); }
    /**
     * Retrieves current player level.
     *
     * @return Player level
     */
    public int getPlayerLevel() { return playerLevel; }

    /**
     * Sets experience points.
     *
     * @param experience XP value
     */
    public void setExperience(int experience) { this.experience = Math.max(0, experience); }
    /**
     * Retrieves current experience points.
     *
     * @return Experience value
     */
    public int getExperience() { return experience; }

    /**
     * Retrieves XP needed for the next level.
     *
     * @return XP required
     */
    public int getXpNeededForNextLevel() { return playerLevel * XP_PER_LEVEL_BASE; }

    /**
     * Sets the speed skill level.
     *
     * @param level Skill level
     */
    public void setSkillSpeedLevel(int level) { this.skillSpeedLevel = MathUtils.clamp(level, 0, 5); }
    /**
     * Retrieves the speed skill level.
     *
     * @return Speed skill level
     */
    public int getSkillSpeedLevel() { return skillSpeedLevel; }

    /**
     * Sets the health skill level.
     *
     * @param level Skill level
     */
    public void setSkillHealthLevel(int level) { this.skillHealthLevel = MathUtils.clamp(level, 0, 5); }
    /**
     * Retrieves the health skill level.
     *
     * @return Health skill level
     */
    public int getSkillHealthLevel() { return skillHealthLevel; }

    /**
     * Retrieves available skill points.
     *
     * @return Skill points
     */
    public int getSkillPoints() { return skillPoints; }
    /**
     * Sets available skill points.
     *
     * @param points Skill points to set
     */
    public void setSkillPoints(int points) { this.skillPoints = Math.max(0, points); }

    /**
     * Calculates base walk speed with upgrades.
     *
     * @return Walk speed
     */
    public float getWalkSpeed() { return WALK_SPEED_BASE + (skillSpeedLevel * 50f); }

    /**
     * Sets the temporary speed multiplier.
     *
     * @param m Speed multiplier
     */
    public void setSpeedMultiplier(float m) { this.speedMultiplier = Math.max(0.1f, m); }
    /**
     * Retrieves the current speed multiplier.
     *
     * @return Speed multiplier
     */
    public float getSpeedMultiplier() { return speedMultiplier; }
    /**
     * Resets speed multiplier to 1.0.
     */
    public void resetSpeedMultiplier() { this.speedMultiplier = 1f; }

    /**
     * Sets the player position.
     *
     * @param x X-coordinate
     * @param y Y-coordinate
     */
    public void setPosition(float x, float y) { this.x = x; this.y = y; }
    /**
     * Retrieves the player x-coordinate.
     *
     * @return X-coordinate
     */
    public float getX() { return x; }
    /**
     * Retrieves the player y-coordinate.
     *
     * @return Y-coordinate
     */
    public float getY() { return y; }

    /**
     * Checks whether the player has a key.
     *
     * @return True if key is owned
     */
    public boolean hasKey() { return hasKey; }
    /**
     * Grants the key to the player.
     */
    public void giveKey() { hasKey = true; }
    /**
     * Removes the key from the player.
     */
    public void clearKey() { hasKey = false; }

    /**
     * Checks whether the player has the story item.
     *
     * @return True if story item is owned
     */
    public boolean hasStoryItem() { return hasStoryItem; }
    /**
     * Retrieves the current story item name.
     *
     * @return Story item name
     */
    public String getCurrentStoryItemName() { return currentStoryItemName; }
    /**
     * Sets the current story item name.
     *
     * @param name Story item name
     */
    public void setCurrentStoryItemName(String name) {
        currentStoryItemName = name == null ? "" : name;
    }
    /**
     * Grants the story item to the player.
     *
     * @param itemName Story item name
     */
    public void giveStoryItem(String itemName) {
        hasStoryItem = true;
        if (itemName != null && !itemName.isEmpty()) {
            currentStoryItemName = itemName;
        }
    }
    /**
     * Clears the story item flag.
     */
    public void resetStoryItem() {
        hasStoryItem = false;
    }

    /**
     * Sets the HUD notification callback.
     *
     * @param notifier Notification consumer
     */
    public void setHudNotifier(java.util.function.Consumer<String> notifier) {
        this.hudNotifier = notifier;
    }

    /**
     * Sends a notification to the HUD if available.
     *
     * @param text Message to send
     */
    public void notifyHud(String text) {
        if (hudNotifier != null && text != null && !text.isEmpty()) {
            hudNotifier.accept(text);
        }
    }

    /**
     * Sets the score value.
     *
     * @param score Score to set
     */
    public void setScore(int score) { this.score = score; }
    /**
     * Adds to the current score.
     *
     * @param amount Score amount to add
     */
    public void addScore(int amount) { this.score += amount; }
    /**
     * Retrieves the current score.
     *
     * @return Score value
     */
    public int getScore() { return score; }

    /**
     * Equips the gun and initializes ammo if needed.
     */
    public void equipGun() {
        hasGun = true;
        if (ammoInClip == 0 && ammoReserve == 0) {
            ammoInClip = MAX_CLIP_AMMO;
        }
    }
    /**
     * Checks whether the player has a gun.
     *
     * @return True if gun is equipped
     */
    public boolean hasGun() { return hasGun; }
    /**
     * Retrieves current ammo in clip.
     *
     * @return Ammo in clip
     */
    public int getAmmoInClip() { return ammoInClip; }
    /**
     * Retrieves ammo in reserve.
     *
     * @return Ammo reserve
     */
    public int getAmmoReserve() { return ammoReserve; }
    /**
     * Sets ammo counts.
     *
     * @param clip Ammo in clip
     * @param reserve Ammo in reserve
     */
    public void setAmmo(int clip, int reserve) {
        ammoInClip = Math.max(0, Math.min(MAX_CLIP_AMMO, clip));
        ammoReserve = Math.max(0, reserve);
    }
    /**
     * Adds ammo to the reserve and reloads if needed.
     *
     * @param amount Ammo amount to add
     */
    public void addAmmo(int amount) {
        if (amount <= 0) return;
        ammoReserve += amount;
        if (hasGun && ammoInClip == 0) {
            reload();
        }
    }
    /**
     * Reloads the weapon from reserve ammo.
     */
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

    /**
     * Checks whether damage flash is active.
     *
     * @return True if damage flash is active
     */
    public boolean isDamageFlashing() { return damageFlash; }

    /**
     * Checks whether god mode is enabled.
     *
     * @return True if god mode is enabled
     */
    public boolean isGodMode() { return godMode; }
    /**
     * Checks whether noclip is enabled.
     *
     * @return True if noclip is enabled
     */
    public boolean isNoClip() { return noClip; }
    /**
     * Enables or disables god mode.
     *
     * @param v True to enable god mode
     */
    public void setGodMode(boolean v) { godMode = v; }
    /**
     * Enables or disables noclip.
     *
     * @param v True to enable noclip
     */
    public void setNoClip(boolean v) { noClip = v; }

    /**
     * Checks whether repulse is active this frame.
     *
     * @return True if repulse is active
     */
    public boolean isRepulseActive() { return isRepulsing; }

    /**
     * Unlocks rapid fire.
     */
    public void unlockRapidFire() { skillRapidFire = true; }
    /**
     * Unlocks shotgun.
     */
    public void unlockShotgun() { skillShotgun = true; }
    /**
     * Unlocks piercing shots.
     */
    public void unlockPiercing() { skillPiercing = true; }
    /**
     * Disposes player audio resources.
     */
    public void dispose() {
        if (shootSound != null) shootSound.dispose();
        if (reloadSound != null) reloadSound.dispose();
        if (slipSound != null) slipSound.dispose();
        if (ouchSound != null) ouchSound.dispose();
    }

    /**
     * Unlocks dash.
     */
    public void unlockDash() { skillDash = true; }
    /**
     * Unlocks ghost walker.
     */
    public void unlockGhost() { skillGhost = true; }

    /**
     * Unlocks thorns.
     */
    public void unlockThorns() { skillThorns = true; }
    /**
     * Unlocks repulse.
     */
    public void unlockRepulse() { skillRepulse = true; }

    /**
     * Checks whether rapid fire is unlocked.
     *
     * @return True if rapid fire is unlocked
     */
    public boolean hasRapidFire() { return skillRapidFire; }
    /**
     * Checks whether shotgun is unlocked.
     *
     * @return True if shotgun is unlocked
     */
    public boolean hasShotgun() { return skillShotgun; }
    /**
     * Checks whether piercing is unlocked.
     *
     * @return True if piercing is unlocked
     */
    public boolean hasPiercing() { return skillPiercing; }
    /**
     * Checks whether dash is unlocked.
     *
     * @return True if dash is unlocked
     */
    public boolean hasDash() { return skillDash; }
    /**
     * Checks whether ghost walker is unlocked.
     *
     * @return True if ghost walker is unlocked
     */
    public boolean hasGhost() { return skillGhost; }
    /**
     * Checks whether thorns is unlocked.
     *
     * @return True if thorns is unlocked
     */
    public boolean hasThorns() { return skillThorns; }
    /**
     * Checks whether repulse is unlocked.
     *
     * @return True if repulse is unlocked
     */
    public boolean hasRepulse() { return skillRepulse; }
}
