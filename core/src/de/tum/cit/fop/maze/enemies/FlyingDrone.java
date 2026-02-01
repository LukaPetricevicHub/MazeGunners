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
import de.tum.cit.fop.maze.behavior.PatrolBehavior;
import de.tum.cit.fop.maze.behavior.ReturnToPatrolBehavior;
import de.tum.cit.fop.maze.map.Maze;
import de.tum.cit.fop.maze.player.Player;

/**
 * A flying enemy that patrols rooms and attacks the player with lasers.
 *
 * The drone switches between patrol and chase behavior, keeps distance
 * from the player, and cannot be hit by melee attacks.
 */
public class FlyingDrone extends Enemy {

    /** Render width in world units. */
    public static final float RENDER_WIDTH  = 32f;
    /** Render height in world units. */
    public static final float RENDER_HEIGHT = 32f;

    /** Number of frames for flight animation. */
    private static final int FLY_FRAMES = 5;
    /** Duration of each flight frame. */
    private static final float FLY_FRAME_DURATION = 0.12f;

    /** Hover offsets for bobbing effect. */
    private static final float[] HOVER_Y_OFFSETS = {0f, 4f, 8f, 4f, 0f};

    /** Attack radius in world units. */
    private static final float ATTACK_RADIUS = 120f;
    /** Minimum safe radius from the player. */
    private static final float MIN_SAFE_RADIUS = 80f;

    /** Whether the drone is actively attacking. */
    private boolean attacking = false;
    /** Attack timer used for animation. */
    private float attackTimer = 0f;

    /** Maximum HP for this enemy type. */
    private static final int MAX_HP = 5;

    /** Active laser projectiles. */
    private final Array<DroneLaser> lasers = new Array<>();
    /** Laser lock time for charging effects. */
    private float laserLockTime = 0f;
    /** Maximum lock time. */
    private static final float MAX_LASER_LOCK = 3.0f;

    /** Laser gun x-offset relative to sprite. */
    private static final float LASER_GUN_OFFSET_X = 0f;
    /** Laser gun y-offset relative to sprite. */
    private static final float LASER_GUN_OFFSET_Y = 6f;

    /** Laser fire cooldown. */
    private float laserCooldown = 0f;
    /** Interval between laser shots. */
    private static final float LASER_INTERVAL = 0.02f;

    /** Number of frames for death animation. */
    private static final int DEATH_FRAMES = 6;
    /** Duration of each death frame. */
    private static final float DEATH_FRAME_DURATION = 0.16f;
    /** Death animation sequence. */
    private static Animation<TextureRegion> deathAnim;

    /** Death sound effect. */
    private static Sound deathSound;
    /** Laser loop sound effect. */
    private static Sound laserSound;
    /** Active laser sound instance id. */
    private long laserSoundId = -1;

    /** Minimum laser sound volume. */
    private static final float LASER_VOLUME_MIN = 0.1f;
    /** Maximum laser sound volume. */
    private static final float LASER_VOLUME_MAX = 0.3f;

    /** Death animation timer. */
    private float deathTimer = 0f;
    /** Whether death sound has played. */
    private boolean deathSoundPlayed = false;

    /** Cardinal directions for animation selection. */
    private enum Direction { DOWN, LEFT, RIGHT, UP }
    /** Current facing direction. */
    private Direction currentDirection = Direction.DOWN;

    /** Patrol points for the drone. */
    private final Array<Vector2> patrolPoints;

    /** Idle animation. */
    private static Animation<TextureRegion> idle;
    /** Patrol scan animation. */
    private static Animation<TextureRegion> patrolScan;
    /** Fly left animation. */
    private static Animation<TextureRegion> flyLeft;
    /** Fly right animation. */
    private static Animation<TextureRegion> flyRight;
    /** Scan animation. */
    private static Animation<TextureRegion> scan;

    /** Animation timer for flight/scan animations. */
    private float animationTimer = 0f;

    /** Previous position used to infer direction. */
    private final Vector2 lastPosition = new Vector2();
    /** Whether to snap to tile center on chase start. */
    private boolean snapToCenterOnChase = false;
    /** Whether to snap to tile center on return to patrol. */
    private boolean snapToCenterOnReturn = false;

    /**
     * Creates a flying drone enemy.
     *
     * @param startWorldPosition initial world position
     * @param movementSpeed movement speed
     * @param patrolWorldPositions patrol path
     * @param targetPlayer player to target
     */
    public FlyingDrone(Vector2 startWorldPosition, float movementSpeed, Array<Vector2> patrolWorldPositions, Player targetPlayer) {
        super(startWorldPosition, movementSpeed, new PatrolBehavior(patrolWorldPositions), targetPlayer);

        initHp(MAX_HP);

        patrolPoints = new Array<>(patrolWorldPositions);
        chaseRadius = 200f;

        lastPosition.set(startWorldPosition);
        ensureAnimationsLoaded();
    }

    /**
     * Loads all drone animations and sounds once.
     *
     * Assets are shared between all FlyingDrone instances.
     */
    private static void ensureAnimationsLoaded() {
        if (idle != null) return;

        Texture idleTex = new Texture("Enemies/FlyingDrone/FlyingDroneIdle.png");
        Texture patrolScanTex = new Texture("Enemies/FlyingDrone/FlyingDronePatrolScan.png");
        Texture leftTex = new Texture("Enemies/FlyingDrone/FlyingDroneLeft.png");
        Texture rightTex = new Texture("Enemies/FlyingDrone/FlyingDroneRight.png");
        Texture scanTex = new Texture("Enemies/FlyingDrone/FlyingDroneScan.png");

        setNearest(idleTex);
        setNearest(patrolScanTex);
        setNearest(leftTex);
        setNearest(rightTex);
        setNearest(scanTex);

        idle = buildStripAnim(idleTex, 4, FLY_FRAME_DURATION);
        flyLeft = buildStripAnim(leftTex, 4, FLY_FRAME_DURATION);
        flyRight = buildStripAnim(rightTex, 4, FLY_FRAME_DURATION);
        patrolScan = buildStripAnim(patrolScanTex, 8, FLY_FRAME_DURATION);
        scan = buildStripAnim(scanTex, 8, FLY_FRAME_DURATION);

        Texture deathTex = new Texture("Enemies/FlyingDrone/FlyingDroneDeath.png");
        setNearest(deathTex);

        deathAnim = buildStripAnim(deathTex, DEATH_FRAMES, DEATH_FRAME_DURATION);
        deathAnim.setPlayMode(Animation.PlayMode.NORMAL);

        laserSound = Gdx.audio.newSound(Gdx.files.internal("Enemies/FlyingDrone/FlyingDroneLaser.wav"));
        deathSound = Gdx.audio.newSound(Gdx.files.internal("Enemies/FlyingDrone/FlyingDroneDeath.wav"));
    }


    private static void setNearest(Texture texture) {
        texture.setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest);
    }


    private static Animation<TextureRegion> buildStripAnim(Texture tex, int frames, float frameDuration) {
        int frameWidth = tex.getWidth() / frames;
        int frameHeight = tex.getHeight();

        Array<TextureRegion> regions = new Array<>(frames);
        for (int i = 0; i < frames; i++) {
            regions.add(new TextureRegion(tex, i * frameWidth, 0, frameWidth, frameHeight));
        }

        Animation<TextureRegion> anim = new Animation<>(frameDuration, regions);
        anim.setPlayMode(Animation.PlayMode.LOOP);
        return anim;
    }

    /**
     * Switches between patrol and chase depending on player distance.
     */
    @Override
    protected void updateAI(float deltaTime) {
        float dist2 = worldPosition.dst2(getPlayerPosition());
        boolean playerInRange = dist2 <= chaseRadius * chaseRadius;

        if (playerInRange) {
            if(activeBehavior.getType() != BehaviorStateType.CHASE) {
                snapToCenterOnChase = true;
                setCurrentBehavior(createChaseBehavior());
            }
        } else{
            if (activeBehavior.getType() == BehaviorStateType.CHASE) {
                snapToCenterOnReturn = true;
                setCurrentBehavior(new ReturnToPatrolBehavior());
            }
        }
    }

    @Override
    protected BehaviorState createChaseBehavior() {
        return new ChaseBehavior();
    }

    @Override
    protected BehaviorState createDefaultBehavior() {
        return new PatrolBehavior(patrolPoints);
    }

    /**
     * Returns the nearest patrol point to the drone.
     *
     * @return Closest patrol point
     */
    public Vector2 getClosestPatrolPoint() {
        Vector2 pos = getPosition();
        Vector2 closest = patrolPoints.first();
        float bestDist = pos.dst2(closest);

        for (Vector2 p : patrolPoints) {
            float d = pos.dst2(p);
            if (d < bestDist) {
                bestDist = d;
                closest = p;
            }
        }
        return closest;
    }

    /**
     * Creates a patrol behavior for this drone.
     *
     * @return Patrol behavior instance
     */
    public BehaviorState createPatrolBehavior() {
        return new PatrolBehavior(patrolPoints);
    }


    /**
     * Updates movement, attacks, animations, and death logic.
     *
     * Handles laser firing, hovering, and snapping to tile centers.
     */
    @Override
    protected void updateBehavior(float deltaTime, Maze maze) {

        if (dying) {
            deathTimer += deltaTime;

            if (!deathSoundPlayed) {
                if(deathSound != null) deathSound.play(1f * getSfxVolume());
                deathSoundPlayed = true;
            }

            if (deathAnim.isAnimationFinished(deathTimer))
                finalizeDeath();

            return;
        }

        if (snapToCenterOnChase || snapToCenterOnReturn) {
            float tileSize = maze.getTileSize();
            Vector2 pos = getPosition();

            int tx = (int) (pos.x / tileSize);
            int ty = (int) (pos.y / tileSize);

            if (maze.isRoomCell(tx, ty)) {
                pos.set(tx * tileSize + tileSize * 0.5f, ty * tileSize + tileSize * 0.5f);
            }

            snapToCenterOnChase = false;
            snapToCenterOnReturn = false;
        }

        animationTimer += deltaTime;

        if (isPlayerInAttackRange()) {
            float dist2 = getPosition().dst2(getPlayerPosition());

            if (dist2 <= ATTACK_RADIUS * ATTACK_RADIUS) {

                if (!attacking) startLaserSound();

                attacking = true;
                attackTimer += deltaTime;

                updateLaserSoundVolume();

                laserCooldown -= deltaTime;
                if (laserCooldown <= 0f) {
                    fireLaserAtPlayer();
                    laserCooldown = LASER_INTERVAL;
                }

                if (dist2 < MIN_SAFE_RADIUS * MIN_SAFE_RADIUS) {
                    moveAwayFromPlayer(deltaTime, maze);
                    updateDirectionFromMovement();
                }

                return;
            }
        }

        if (attacking) stopLaserSound();

        attacking = false;
        attackTimer = 0f;

        super.updateBehavior(deltaTime, maze);
        updateDirectionFromMovement();
    }

    /**
     * Fires a laser projectile toward the player.
     */

    private void fireLaserAtPlayer() {
        if (targetPlayer == null) return;

        Vector2 start = getLaserStart();

        float px = targetPlayer.getX() + 16f;
        float py = targetPlayer.getY() + 32f;

        lasers.add(new DroneLaser(start.x, start.y, px, py));
    }

    /**
     * Moves away from the player when too close.
     *
     * @param deltaTime Time since last frame
     * @param maze Maze for collision checks
     */
    private void moveAwayFromPlayer(float deltaTime, Maze maze) {
        Vector2 pos = getPosition();
        Vector2 playerPos = getPlayerPosition();

        Vector2 awayDir = pos.cpy().sub(playerPos);
        if (awayDir.len2() < 0.001f) return;

        awayDir.nor().scl(getSpeed());
        getCollisionHandler().move(pos, awayDir, deltaTime, maze);
    }


    private void updateDirectionFromMovement() {
        Vector2 pos = getPosition();

        float dx = pos.x - lastPosition.x;
        float dy = pos.y - lastPosition.y;

        if (Math.abs(dx) > Math.abs(dy)) {
            if (dx > 0.01f) currentDirection = Direction.RIGHT;
            else if (dx < -0.01f) currentDirection = Direction.LEFT;
        } else{
            if (dy > 0.01f) currentDirection = Direction.UP;
            else if (dy < -0.01f) currentDirection = Direction.DOWN;
        }

        lastPosition.set(pos);
    }

    /**
     * Returns the current animation frame for rendering.
     */

    public TextureRegion getCurrentFrame() {
        if (dying) {
            return deathAnim.getKeyFrame(deathTimer, false);
        }

        boolean isChasing = activeBehavior.getType() == BehaviorStateType.CHASE;

        if (attacking)
            return idle.getKeyFrame(attackTimer, true);

        if (isChasing) {
            if (currentDirection == Direction.LEFT)
                return flyLeft.getKeyFrame(animationTimer, true);
            if (currentDirection == Direction.RIGHT)
                return flyRight.getKeyFrame(animationTimer, true);

            return idle.getKeyFrame(animationTimer, true);
        }

        return patrolScan.getKeyFrame(animationTimer, true);
    }

    /**
     * Returns the hover Y offset for bobbing effect.
     *
     * @return Hover offset in world units
     */
    public float getHoverYOffset() {
        int frameIndex = patrolScan.getKeyFrameIndex(animationTimer);
        return HOVER_Y_OFFSETS[frameIndex % HOVER_Y_OFFSETS.length];
    }

    /**
     * Returns active laser projectiles.
     *
     * @return Laser list
     */
    public Array<DroneLaser> getLasers() {
        return lasers;
    }

    /**
     * Checks whether laser attack is active.
     *
     * @return True if attacking
     */
    public boolean isLaserActive() {
        return attacking;
    }

    /**
     * Computes the laser start point.
     *
     * @return Laser start position
     */
    public Vector2 getLaserStart() {
        return new Vector2(getX() + getRenderWidth() * 0.5f + LASER_GUN_OFFSET_X, getY() + LASER_GUN_OFFSET_Y);
    }

    /**
     * Computes the laser end point (player position).
     *
     * @return Laser end position
     */
    public Vector2 getLaserEnd() {
        if (targetPlayer == null) return getLaserStart();
        return new Vector2(targetPlayer.getX() + 16f, targetPlayer.getY() + 32f);
    }

    /**
     * Increases laser lock time up to the max.
     *
     * @param dt Delta time
     */
    public void increaseLaserLock(float dt) {
        laserLockTime = Math.min(laserLockTime + dt, MAX_LASER_LOCK);
    }

    /**
     * Resets the laser lock time.
     */
    public void resetLaserLock() {
        laserLockTime = 0f;
    }

    /**
     * Returns the normalized laser charge.
     *
     * @return Charge value from 0 to 1
     */
    public float getLaserCharge() {
        return laserLockTime / MAX_LASER_LOCK;
    }

    /**
     * Returns the current laser lock time.
     *
     * @return Laser lock time in seconds
     */
    public float getLaserLockTime() {
        return laserLockTime;
    }

    /**
     * Checks whether the player is within attack range.
     *
     * @return True if in range
     */
    private boolean isPlayerInAttackRange() {
        if (targetPlayer == null) return false;
        float dist2 = getPosition().dst2(getPlayerPosition());
        return dist2 <= ATTACK_RADIUS * ATTACK_RADIUS;
    }

    /**
     * Stops all active drone sounds.
     */
    private void startLaserSound() {
        if (laserSound == null) return;
        if (laserSoundId != -1) return;
        laserSoundId = laserSound.loop(LASER_VOLUME_MIN * getSfxVolume());
    }

    /**
     * Updates laser sound volume based on charge.
     */
    private void updateLaserSoundVolume() {
        if (laserSound == null) return;
        if (laserSoundId == -1) return;

        float heat = getLaserCharge();
        float intensity = heat * heat;

        float volume = (LASER_VOLUME_MIN + (LASER_VOLUME_MAX - LASER_VOLUME_MIN) * intensity) * getSfxVolume();
        laserSound.setVolume(laserSoundId, volume);
    }

    /**
     * Stops the looping laser sound.
     */
    private void stopLaserSound() {
        if (laserSound == null) return;
        if (laserSoundId == -1) return;

        laserSound.stop(laserSoundId);
        laserSoundId = -1;
    }

    /**
     * Stops all looping drone sounds.
     */
    public void stopAllSounds() {
        stopLaserSound();
    }

    @Override
    protected boolean shouldResetPatrolOnReturn() {
        return false;
    }

    @Override
    public int getPathfindingFootprint() {
        return 1;
    }

    @Override
    public float getRenderWidth() {
        return 28f;
    }

    @Override
    public float getRenderHeight() {
        return 28f;
    }

    @Override
    public float getAttackRadius() {
        return ATTACK_RADIUS;
    }

    @Override
    public boolean ignoresEnemyCollision() {
        return true;
    }

    @Override
    public boolean canBeHitByMelee() {
        return false;
    }

    /**
     * Starts the death sequence for the flying drone.
     *
     * Stops laser attacks, resets death state, and plays the death animation
     * and sound before removed.
     */
    @Override
    protected void die() {
        if (dying) return;

        dying = true;
        deathTimer = 0f;
        deathSoundPlayed = false;

        stopLaserSound();
        lasers.clear();
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
}
