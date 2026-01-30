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

public class MutantWorker extends Enemy {

    // --- HP ---
    private static final int MAX_HP = 10; // tweak

    // Constants
    private static final float WALK_FRAME_DURATION = 0.12f;
    private static final float ATTACK_FRAME_DURATION = 0.10f;
    private static final float DEATH_FRAME_DURATION = 0.14f;

    private static final float ATTACK_RADIUS = 24f;
    private static final float MIN_SAFE_RADIUS = 20f;

    private static final float ATTACK_COOLDOWN = 0.6f;
    private static final float HIT_MOMENT = 0.45f;

    private float attackCooldownTimer = 0f;
    private float attackTimer = 0f;
    private float animationTimer = 0f;
    private float deathTimer = 0f;

    private boolean attacking = false;
    private boolean hitApplied = false;
    private boolean attackSoundPlayed = false;
    private boolean deathSoundPlayed = false;

    private enum Direction { DOWN, LEFT, RIGHT, UP }
    private Direction currentDirection = Direction.DOWN;

    private final Vector2 lastPosition = new Vector2();

    private static Animation<TextureRegion> walkDown;
    private static Animation<TextureRegion> walkLeft;
    private static Animation<TextureRegion> walkRight;
    private static Animation<TextureRegion> walkUp;

    private static Animation<TextureRegion> attackDown;
    private static Animation<TextureRegion> attackLeft;
    private static Animation<TextureRegion> attackRight;
    private static Animation<TextureRegion> attackUp;

    private static Animation<TextureRegion> deathLeft;
    private static Animation<TextureRegion> deathRight;

    private TextureRegion idleFrame;

    private static Sound attackSound;
    private static Sound deathSound;

    // Initialize mutant worker
    public MutantWorker(Vector2 startWorldPosition, float movementSpeed, Player targetPlayer) {
        super(startWorldPosition, movementSpeed, new ChaseBehavior(), targetPlayer);

        initHp(MAX_HP); // ✅ NEW

        chaseRadius = 250f;
        lastPosition.set(startWorldPosition);
        ensureAnimationsLoaded();
        idleFrame = walkDown.getKeyFrame(0f);
    }

    @Override
    protected BehaviorState createChaseBehavior() {
        return new ChaseBehavior();
    }

    @Override
    protected BehaviorState createDefaultBehavior() {
        return new IdleBehavior();
    }

    @Override
    protected boolean supportsAggroLock() {
        return true;
    }

    @Override
    protected void updateBehavior(float delta, Maze maze) {

        if (dying) {
            deathTimer += delta;
            if (getDeathAnim().isAnimationFinished(deathTimer)) {
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

        if (attacking) {
            handleAttackAnimation(delta);
            return;
        }

        super.updateBehavior(delta, maze);
        updateDirectionFromMovement();
    }

    // Handle locked attack state logic
    private void handleAttackLockedState(float delta) {

        if (!attacking) {
            attacking = true;
            attackTimer = 0f;
            hitApplied = false;

            if (attackSound != null && !attackSoundPlayed) {
                attackSound.play(0.8f);
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

    // Handle attack animation progression
    private void handleAttackAnimation(float delta) {

        attackTimer += delta;

        Animation<TextureRegion> attackAnimation = getAttackAnim();
        float animationDuration = attackAnimation.getAnimationDuration();
        float normalizedTime = attackTimer / animationDuration;

        if (!hitApplied && normalizedTime >= HIT_MOMENT)
            hitApplied = true;

        if (attackAnimation.isAnimationFinished(attackTimer)) {
            attacking = false;
            attackTimer = 0f;
            hitApplied = false;
            attackCooldownTimer = ATTACK_COOLDOWN;
            attackSoundPlayed = false;
        }

        updateDirectionFromPlayer();
    }

    // Moves enemy away from player (not used, but kept)
    private void moveAwayFromPlayer(float delta, Maze maze) {
        Vector2 position = getPosition();
        Vector2 playerPosition = getPlayerPosition();

        Vector2 awayVector = position.cpy().sub(playerPosition);
        if (awayVector.len2() < 0.001f) return;

        awayVector.nor().scl(getSpeed());
        getCollisionHandler().move(position, awayVector, delta, maze);
    }

    // Attempt sidestep when blocked (not used, but kept)
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

    // Update direction based on player
    private void updateDirectionFromPlayer() {
        Vector2 position = getPosition();
        Vector2 playerPosition = getPlayerPosition();

        float dx = playerPosition.x - position.x;
        float dy = playerPosition.y - position.y;

        if (Math.abs(dx) > Math.abs(dy)) {
            currentDirection = (dx > 0) ? Direction.RIGHT : Direction.LEFT;
        } else {
            currentDirection = (dy > 0) ? Direction.UP : Direction.DOWN;
        }
    }

    // Updates direction from movement delta
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

    // Returns current animation frame
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

    // Returns attack animation by direction
    private Animation<TextureRegion> getAttackAnim() {
        if (currentDirection == Direction.UP) return attackUp;
        if (currentDirection == Direction.LEFT) return attackLeft;
        if (currentDirection == Direction.RIGHT) return attackRight;
        return attackDown;
    }

    // Returns walk animation by direction
    private Animation<TextureRegion> getWalkAnim() {
        if (currentDirection == Direction.UP) return walkUp;
        if (currentDirection == Direction.LEFT) return walkLeft;
        if (currentDirection == Direction.RIGHT) return walkRight;
        return walkDown;
    }

    // Return death animation by direction
    private Animation<TextureRegion> getDeathAnim() {
        if (currentDirection == Direction.LEFT) return deathLeft;
        return deathRight;
    }

    // Overrides
    @Override
    public float getAttackRadius() {
        return ATTACK_RADIUS;
    }

    @Override
    public float getSeparationRadius() {
        return 18f;
    }

    // Animations
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

    // Load animation strip from texture
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

    // Overrides
    @Override
    protected float maxAllowedOverlap() {
        return 5f;
    }


    protected float attackLockDuration() {
        return 0.45f;
    }

    @Override
    protected float postAttackDelay() {
        return 0.2f;
    }

    @Override
    protected void die() {
        if (dying) return;

        dying = true;
        deathTimer = 0f;

        if (deathSound != null && !deathSoundPlayed) {
            deathSound.play(1.0f);
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
