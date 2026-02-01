package de.tum.cit.fop.maze.enemies;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.audio.Sound;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Animation;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.Array;
import de.tum.cit.fop.maze.behavior.BehaviorState;
import de.tum.cit.fop.maze.behavior.BehaviorStateType;
import de.tum.cit.fop.maze.behavior.ChaseBehavior;
import de.tum.cit.fop.maze.behavior.IdleBehavior;
import de.tum.cit.fop.maze.map.Maze;
import de.tum.cit.fop.maze.player.Player;

/**
 * Melee enemy that chases the player and performs close-range attacks.
 *
 * Uses direction-based animations for walking, attacking, and dying.
 */
public class MutantWorker extends Enemy {


    /** Walk animation frame duration. */
    private static final float WALK_FRAME_DURATION = 0.12f;
    /** Attack animation frame duration. */
    private static final float ATTACK_FRAME_DURATION = 0.10f;
    /** Death animation frame duration. */
    private static final float DEATH_FRAME_DURATION = 0.14f;

    /** Attack radius in world units. */
    private static final float ATTACK_RADIUS = 24f;
    /** Minimum safe radius from the player. */
    private static final float MIN_SAFE_RADIUS = 20f;

    /** Cooldown between attacks. */
    private static final float ATTACK_COOLDOWN = 0.6f;
    /** Normalized moment within the attack animation to apply hit. */
    private static final float HIT_MOMENT = 0.45f;

    /** Remaining attack cooldown time. */
    private float attackCooldownTimer = 0f;
    /** Attack animation timer. */
    private float attackTimer = 0f;
    /** Movement animation timer. */
    private float animationTimer = 0f;
    /** Death animation timer. */
    private float deathTimer = 0f;

    /** Whether the enemy is currently attacking. */
    private boolean attacking = false;
    /** Whether the hit has been applied this attack. */
    private boolean hitApplied = false;
    /** Whether the attack sound has played this attack. */
    private boolean attackSoundPlayed = false;
    /** Whether the death sound has played. */
    private boolean deathSoundPlayed = false;

    /** Maximum HP for this enemy type. */
    private static final int MAX_HP = 10;

    /** Cardinal directions for animation selection. */
    private enum Direction { DOWN, LEFT, RIGHT, UP }
    /** Current facing direction. */
    private Direction currentDirection = Direction.DOWN;

    /** Last position used to infer movement direction. */
    private final Vector2 lastPosition = new Vector2();

    /** Walk animation facing down. */
    private static Animation<TextureRegion> walkDown;
    /** Walk animation facing left. */
    private static Animation<TextureRegion> walkLeft;
    /** Walk animation facing right. */
    private static Animation<TextureRegion> walkRight;
    /** Walk animation facing up. */
    private static Animation<TextureRegion> walkUp;

    /** Attack animation facing down. */
    private static Animation<TextureRegion> attackDown;
    /** Attack animation facing left. */
    private static Animation<TextureRegion> attackLeft;
    /** Attack animation facing right. */
    private static Animation<TextureRegion> attackRight;
    /** Attack animation facing up. */
    private static Animation<TextureRegion> attackUp;

    /** Death animation facing left. */
    private static Animation<TextureRegion> deathLeft;
    /** Death animation facing right. */
    private static Animation<TextureRegion> deathRight;

    /** Idle frame used when not aggroed. */
    private TextureRegion idleFrame;

    /** Attack sound effect. */
    private static Sound attackSound;
    /** Death sound effect. */
    private static Sound deathSound;

    /**
     * Creates a MutantWorker enemy.
     *
     * @param startWorldPosition initial world position
     * @param movementSpeed movement speed of the enemy
     * @param targetPlayer player to chase and attack
     */
    public MutantWorker(Vector2 startWorldPosition, float movementSpeed, Player targetPlayer) {
        super(startWorldPosition, movementSpeed, new ChaseBehavior(), targetPlayer);

        initHp(MAX_HP);

        chaseRadius = 250f;
        lastPosition.set(startWorldPosition);
        ensureAnimationsLoaded();
        idleFrame = walkDown.getKeyFrame(0f);
    }

    @Override
    protected BehaviorState createChaseBehavior() {
        return new ChaseBehavior();
    }

    /**
     * Creates the default idle behavior.
     *
     * @return Idle behavior instance
     */
    @Override
    protected BehaviorState createDefaultBehavior() {
        return new IdleBehavior();
    }

    /**
     * Indicates whether this enemy uses aggro lock.
     *
     * @return True if aggro lock is supported
     */
    @Override
    protected boolean supportsAggroLock() {
        return true;
    }

    /**
     * Updates the enemy behavior, including movement, attacks,
     * animations, and death handling.
     *
     * @param delta time passed since last frame
     * @param maze current maze for collision checks
     */
    @Override
    protected void updateBehavior(float delta, Maze maze) {

        if (dying) {
            deathTimer += delta;
            if(getDeathAnim().isAnimationFinished(deathTimer)) {
                finalizeDeath();
            }
            return;
        }

        boolean notAggroLocked = !isAggroLocked();
        boolean notChasing = activeBehavior.getType() != BehaviorStateType.CHASE;

        if (notAggroLocked && notChasing) {
            attacking = false;
            attackTimer = 0f;
            animationTimer = 0f;
            return;
        }

        animationTimer += delta;
        attackCooldownTimer -= delta;

        if (isAttackingLocked()) {
            handleAttackLockedState(delta);
            return;
        }

        if(attacking) {
            handleAttackAnimation(delta);
            return;
        }

        super.updateBehavior(delta, maze);
        updateDirectionFromMovement();
    }


    /**
     * Updates logic while attack is locked to the player.
     *
     * @param delta Time since last frame
     */
    private void handleAttackLockedState(float delta) {

        if (!attacking) {
            attacking = true;
            attackTimer = 0f;
            hitApplied = false;

            if (attackSound != null && !attackSoundPlayed) {
                attackSound.play(0.8f * getSfxVolume());
                attackSoundPlayed = true;
            }
        }

        attackTimer += delta;

        Animation<TextureRegion> attackAnimation = getAttackAnim();
        float animationDuration = attackAnimation.getAnimationDuration();
        float normalizedTime = attackTimer / animationDuration;

        if (!hitApplied && normalizedTime >= HIT_MOMENT) {
            hitApplied = true;
        }

        if (attackAnimation.isAnimationFinished(attackTimer)) {
            attacking = false;
            attackTimer = 0f;
            attackSoundPlayed = false;
        }

        updateDirectionFromPlayer();
    }

    /**
     * Advances the attack animation and timing.
     *
     * @param delta Time since last frame
     */
    private void handleAttackAnimation(float delta) {

        attackTimer += delta;

        Animation<TextureRegion> attackAnimation = getAttackAnim();
        float animationDuration = attackAnimation.getAnimationDuration();
        float normalizedTime = attackTimer / animationDuration;

        if (!hitApplied && normalizedTime >= HIT_MOMENT) {
            hitApplied = true;
        }


        if (attackAnimation.isAnimationFinished(attackTimer)) {
            attacking = false;
            attackTimer = 0f;
            hitApplied = false;
            attackCooldownTimer = ATTACK_COOLDOWN;
            attackSoundPlayed = false;
        }

        updateDirectionFromPlayer();
    }


    /**
     * Attempts to move away from the player.
     *
     * @param delta Time since last frame
     * @param maze Maze for collision checks
     */
    private void moveAwayFromPlayer(float delta, Maze maze) {
        Vector2 position = getPosition();
        Vector2 playerPosition = getPlayerPosition();

        Vector2 awayVector = position.cpy().sub(playerPosition);
        if (awayVector.len2() < 0.001f) return;

        awayVector.nor().scl(getSpeed());
        getCollisionHandler().move(position, awayVector, delta, maze);
    }


    /**
     * Attempts a side step around the player.
     *
     * @param delta Time since last frame
     * @param maze Maze for collision checks
     */
    private void trySideStep(float delta, Maze maze) {
        Vector2 position = getPosition();
        Vector2 playerPosition = getPlayerPosition();

        Vector2 toPlayer = playerPosition.cpy().sub(position);
        if (toPlayer.len2() < 0.001f) return;

        Vector2 left = new Vector2(-toPlayer.y, toPlayer.x).nor();
        Vector2 right = new Vector2(toPlayer.y, -toPlayer.x).nor();

        Vector2 chosenDirection = Math.random() < 0.5 ? left : right;
        chosenDirection.scl(getSpeed());

        getCollisionHandler().move(position, chosenDirection, delta, maze);
    }


    /**
     * Updates facing direction toward the player.
     */
    private void updateDirectionFromPlayer() {
        Vector2 position = getPosition();
        Vector2 playerPosition = getPlayerPosition();

        float dx = playerPosition.x - position.x;
        float dy = playerPosition.y - position.y;

        if (Math.abs(dx) > Math.abs(dy)) {
            currentDirection = (dx > 0) ? Direction.RIGHT : Direction.LEFT;
        } else{
            currentDirection = (dy > 0) ? Direction.UP : Direction.DOWN;
        }
    }


    /**
     * Updates facing direction based on movement delta.
     */
    private void updateDirectionFromMovement() {
        Vector2 position = getPosition();
        float dx = position.x - lastPosition.x;
        float dy = position.y - lastPosition.y;

        if (Math.abs(dx) > Math.abs(dy)) {
            if (dx > 0.01f) currentDirection = Direction.RIGHT;
            else if (dx < -0.01f) currentDirection = Direction.LEFT;
        } else {
            if (dy > 0.01f) currentDirection = Direction.UP;
            else if (dy < -0.01f) currentDirection = Direction.DOWN;
        }

        lastPosition.set(position);
    }

    /**
     * Returns the current animation frame based on state and direction.
     *
     * @return texture region to render
     */
    public TextureRegion getCurrentFrame() {

        if (dying) {
            return getDeathAnim().getKeyFrame(deathTimer, false);
        }

        boolean notAggroLocked = !isAggroLocked();
        boolean notChasing = activeBehavior.getType() != BehaviorStateType.CHASE;

        if (notAggroLocked && notChasing) {
            return idleFrame;
        }

        if (attacking)
            return getAttackAnim().getKeyFrame(attackTimer, false);

        return getWalkAnim().getKeyFrame(animationTimer, true);
    }


    private Animation<TextureRegion> getAttackAnim() {
        if (currentDirection == Direction.UP) return attackUp;
        if (currentDirection == Direction.LEFT) return attackLeft;
        if (currentDirection == Direction.RIGHT) return attackRight;
        return attackDown;
    }


    /**
     * Returns the walking animation for the current direction.
     *
     * @return Walk animation
     */
    private Animation<TextureRegion> getWalkAnim() {
        if (currentDirection == Direction.UP) return walkUp;
        if (currentDirection == Direction.LEFT) return walkLeft;
        if (currentDirection == Direction.RIGHT) return walkRight;
        return walkDown;
    }


    /**
     * Returns the death animation for the current direction.
     *
     * @return Death animation
     */
    private Animation<TextureRegion> getDeathAnim() {
        if (currentDirection == Direction.LEFT) return deathLeft;
        return deathRight;
    }

    /**
     * Returns the melee attack radius.
     *
     * @return attack radius in world units
     */
    @Override
    public float getAttackRadius() {
        return ATTACK_RADIUS;
    }

    @Override
    public float getSeparationRadius() {
        return 18f;
    }


    /**
     * Lazily loads animations and sounds for this enemy type.
     */
    private static void ensureAnimationsLoaded() {

        if (walkDown != null) return;

        walkDown = loadStrip("Enemies/MutantWorker/MutantWorkerDown.png", 8, WALK_FRAME_DURATION, true);
        walkLeft = loadStrip("Enemies/MutantWorker/MutantWorkerLeft.png", 8, WALK_FRAME_DURATION, true);
        walkRight = loadStrip("Enemies/MutantWorker/MutantWorkerRight.png", 8, WALK_FRAME_DURATION, true);
        walkUp = loadStrip("Enemies/MutantWorker/MutantWorkerUp.png", 8, WALK_FRAME_DURATION, true);

        attackDown = loadStrip("Enemies/MutantWorker/MutantWorkerDownAttack.png", 7, ATTACK_FRAME_DURATION, false);
        attackLeft = loadStrip("Enemies/MutantWorker/MutantWorkerLeftAttack.png", 7, ATTACK_FRAME_DURATION, false);
        attackRight = loadStrip("Enemies/MutantWorker/MutantWorkerRightAttack.png", 7, ATTACK_FRAME_DURATION, false);
        attackUp = loadStrip("Enemies/MutantWorker/MutantWorkerUpAttack.png", 7, ATTACK_FRAME_DURATION, false);

        deathLeft = loadStrip("Enemies/MutantWorker/MutantWorkerLeftDeath.png", 6, DEATH_FRAME_DURATION, false);
        deathRight = loadStrip("Enemies/MutantWorker/MutantWorkerRightDeath.png", 6, DEATH_FRAME_DURATION, false);

        attackSound = Gdx.audio.newSound(Gdx.files.internal("Enemies/MutantWorker/MutantWorkerAttack.wav"));
        deathSound = Gdx.audio.newSound(Gdx.files.internal("Enemies/MutantWorker/MutantWorkerDeath.wav"));
    }


    /**
     * Loads an animation strip from a texture.
     *
     * @param path Texture path
     * @param frames Number of frames
     * @param duration Frame duration
     * @param loop Whether animation loops
     * @return Built animation
     */
    private static Animation<TextureRegion> loadStrip(String path, int frames, float duration, boolean loop) {

        Texture texture = new Texture(path);
        texture.setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest);

        int frameWidth = texture.getWidth() / frames;
        int frameHeight = texture.getHeight();

        Array<TextureRegion> regions = new Array<>(frames);
        for (int i = 0; i < frames; i++) {
            regions.add(new TextureRegion(texture, i * frameWidth, 0, frameWidth, frameHeight));
        }

        Animation<TextureRegion> animation = new Animation<>(duration, regions);
        animation.setPlayMode(loop ? Animation.PlayMode.LOOP : Animation.PlayMode.NORMAL);
        return animation;
    }

    /**
     * Retrieves SFX volume from preferences.
     *
     * @return SFX volume
     */
    private float getSfxVolume() {
        if (Gdx.app == null) return 1f;
        return Gdx.app.getPreferences("maze_runner_prefs").getFloat("sound_vol", 0.5f);
    }


    @Override
    protected float maxAllowedOverlap() {
        return 5f;
    }


    /**
     * Returns the duration of the attack lock.
     *
     * @return Attack lock duration
     */
    protected float attackLockDuration() {
        return 0.45f;
    }

    /**
     * Returns the post-attack delay.
     *
     * @return Post-attack delay duration
     */
    @Override
    protected float postAttackDelay() {
        return 0.2f;
    }

    /**
     * Triggers the death state of the enemy.
     *
     * Starts the death animation, plays the death sound,
     * stops attacking, and locks the facing direction
     * for the death animation.
     */
    @Override
    protected void die() {
        if (dying) return;

        dying = true;
        deathTimer = 0f;

        if (deathSound != null && !deathSoundPlayed) {
            deathSound.play(1.0f * getSfxVolume());
            deathSoundPlayed = true;
        }

        attacking = false;
        attackTimer = 0f;

        boolean verticalDirection = currentDirection == Direction.UP || currentDirection == Direction.DOWN;
        if (verticalDirection) {
            float dx = getPosition().x - getPlayerPosition().x;
            currentDirection = (dx >= 0) ? Direction.RIGHT : Direction.LEFT;
        }
    }
}
