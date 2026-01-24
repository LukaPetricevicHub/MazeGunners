package de.tum.cit.fop.maze.enemies;

import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;
import de.tum.cit.fop.maze.behavior.BehaviorState;
import de.tum.cit.fop.maze.behavior.BehaviorStateType;
import de.tum.cit.fop.maze.behavior.PatrolBehavior;
import de.tum.cit.fop.maze.collision.CollisionHandler;
import de.tum.cit.fop.maze.map.Maze;
import de.tum.cit.fop.maze.player.Player;

import java.util.List;

public abstract class Enemy {

    protected Vector2 worldPosition;
    protected float movementSpeed;
    protected Rectangle collisionBounds;
    protected BehaviorState activeBehavior;
    protected Player targetPlayer;
    protected final Vector2 homeWorldPosition;
    protected final CollisionHandler collisionHandler;

    private static final float COLLISION_INSET = 8f;
    private static final float COLLISION_SIZE = 20f;
    private static final float COLLISION_OFFSET = (32f - COLLISION_SIZE) * 0.5f;

    protected float chaseRadius = 200f;
    private float chaseMemoryTimer = 0f;
    private static final float CHASE_MEMORY_DURATION = 2.0f;
    protected boolean aggroLocked = false;

    // =========================
    // HEALTH / DAMAGE
    // =========================
    protected int health = 3;
    protected boolean markedForRemoval = false;

    // =========================
    // STUCK DETECTION
    // =========================
    private final Vector2 prevPosition = new Vector2();
    private float stuckTimer = 0f;
    private static final float STUCK_TIME_THRESHOLD = 0.4f;
    private static final float MIN_MOVEMENT_EPSILON = 0.5f;

    // =========================
    // INTERACTION / COMBAT LOCK
    // =========================
    private final Rectangle interactionRect = new Rectangle();
    protected static final float INTERACTION_MARGIN = 2f;
    protected static final float INTERACTION_BODY_HEIGHT = 32f;

    protected enum CombatLockState {
        NONE,
        ATTACKING,
        POST_ATTACK_DELAY
    }

    protected boolean touchingPlayer = false;
    protected CombatLockState combatLockState = CombatLockState.NONE;
    protected float combatLockTimer = 0f;

    // =========================
    // CONSTRUCTOR
    // =========================
    public Enemy(Vector2 startWorldPosition,
                 float movementSpeed,
                 BehaviorState initialBehavior,
                 Player targetPlayer) {

        this.worldPosition = startWorldPosition;
        this.homeWorldPosition = new Vector2(startWorldPosition);
        this.movementSpeed = movementSpeed;
        this.activeBehavior = initialBehavior;
        this.targetPlayer = targetPlayer;

        this.collisionBounds = new Rectangle(
                startWorldPosition.x + COLLISION_OFFSET,
                startWorldPosition.y + COLLISION_OFFSET,
                COLLISION_SIZE,
                COLLISION_SIZE
        );

        this.collisionHandler = new CollisionHandler(collisionBounds, COLLISION_INSET);
    }

    // =========================
    // UPDATE
    // =========================
    public final void update(float deltaTime, Maze maze) {
        prevPosition.set(worldPosition);

        updateCombatLock(deltaTime);

        if (combatLockState == CombatLockState.NONE) {
            updateAI(deltaTime);
        }

        updateBehavior(deltaTime, maze);

        collisionBounds.setPosition(
                worldPosition.x + COLLISION_OFFSET,
                worldPosition.y + COLLISION_OFFSET
        );

        if (prevPosition.dst2(worldPosition) < MIN_MOVEMENT_EPSILON * MIN_MOVEMENT_EPSILON)
            stuckTimer += deltaTime;
        else
            stuckTimer = 0f;

        if (stuckTimer >= STUCK_TIME_THRESHOLD) {
            onStuck(maze);
            stuckTimer = 0f;
        }
    }

    // =========================
    // AI
    // =========================
    protected void updateAI(float deltaTime) {
        if (targetPlayer == null || activeBehavior == null)
            return;

        float dist2 = worldPosition.dst2(getPlayerPosition());
        boolean playerInRange = dist2 <= chaseRadius * chaseRadius;

        if (playerInRange && supportsAggroLock()) {
            aggroLocked = true;
        }

        if (aggroLocked || playerInRange) {
            chaseMemoryTimer = 0f;

            if (activeBehavior.getType() != BehaviorStateType.CHASE) {
                setCurrentBehavior(createChaseBehavior());
            }
            return;
        }

        chaseMemoryTimer += deltaTime;

        if (chaseMemoryTimer >= CHASE_MEMORY_DURATION
                && activeBehavior.getType() == BehaviorStateType.CHASE) {
            setCurrentBehavior(createDefaultBehavior());
        }
    }

    protected void updateBehavior(float deltaTime, Maze maze) {
        if (activeBehavior != null)
            activeBehavior.execute(this, deltaTime, maze);
    }

    protected abstract BehaviorState createChaseBehavior();
    protected abstract BehaviorState createDefaultBehavior();

    // =========================
    // HEALTH
    // =========================
    public void takeDamage(int amount) {
        health -= amount;
        if (health <= 0) {
            die();
        }
    }

    protected void die() {
        markedForRemoval = true;
    }

    public boolean isDead() {
        return markedForRemoval;
    }

    // =========================
    // ACCESSORS
    // =========================
    public Vector2 getPosition() { return worldPosition; }
    public float getSpeed() { return movementSpeed; }
    public Rectangle getCollisionBox() { return collisionBounds; }
    public CollisionHandler getCollisionHandler() { return collisionHandler; }
    public Vector2 getHomePosition() { return homeWorldPosition; }

    public float getX() { return worldPosition.x; }
    public float getY() { return worldPosition.y; }

    public Vector2 getPlayerPosition() {
        if (targetPlayer != null)
            return new Vector2(targetPlayer.getX(), targetPlayer.getY());
        return new Vector2();
    }

    public void setCurrentBehavior(BehaviorState behavior) {
        if (behavior instanceof PatrolBehavior patrol && shouldResetPatrolOnReturn()) {
            patrol.resetToClosest(worldPosition);
        }
        this.activeBehavior = behavior;
    }

    // =========================
    // SEPARATION
    // =========================
    public Rectangle getInteractionRect() {
        interactionRect.set(
                worldPosition.x - INTERACTION_MARGIN,
                worldPosition.y - INTERACTION_MARGIN,
                getRenderWidth() + INTERACTION_MARGIN * 2,
                INTERACTION_BODY_HEIGHT + INTERACTION_MARGIN * 2
        );
        return interactionRect;
    }

    public void applyEnemySeparation(List<Enemy> others,
                                     Player player,
                                     float deltaTime,
                                     Maze maze) {

        if (combatLockState == CombatLockState.ATTACKING)
            return;

        Vector2 pos = getPosition();
        Rectangle myRect = getInteractionRect();
        Vector2 correction = new Vector2();

        for (Enemy other : others) {
            if (other == this) continue;
            if (ignoresEnemyCollision() || other.ignoresEnemyCollision()) continue;

            Rectangle otherRect = other.getInteractionRect();
            if (!myRect.overlaps(otherRect)) continue;

            float overlapX = Math.min(
                    myRect.x + myRect.width - otherRect.x,
                    otherRect.x + otherRect.width - myRect.x
            );

            float overlapY = Math.min(
                    myRect.y + myRect.height - otherRect.y,
                    otherRect.y + otherRect.height - myRect.y
            );

            float tolerance = maxAllowedOverlap();
            if (overlapX < tolerance && overlapY < tolerance)
                continue;

            if (overlapX < overlapY) {
                correction.x += (myRect.x + myRect.width * 0.5f <
                        otherRect.x + otherRect.width * 0.5f)
                        ? -overlapX : overlapX;
            } else {
                correction.y += (myRect.y + myRect.height * 0.5f <
                        otherRect.y + otherRect.height * 0.5f)
                        ? -overlapY : overlapY;
            }
        }

        if (correction.len2() < 0.0001f)
            return;

        correction.nor().scl(getSpeed() * 0.95f);
        getCollisionHandler().move(pos, correction, deltaTime, maze);
    }

    // =========================
    // COMBAT LOCK
    // =========================
    protected void updateCombatLock(float deltaTime) {

        switch (combatLockState) {

            case NONE -> { }

            case ATTACKING -> {
                if (combatLockTimer > 0f) {
                    combatLockTimer -= deltaTime;
                }

                if (!touchingPlayer && combatLockTimer <= 0f) {
                    combatLockState = CombatLockState.POST_ATTACK_DELAY;
                    combatLockTimer = postAttackDelay();
                }
            }

            case POST_ATTACK_DELAY -> {
                combatLockTimer -= deltaTime;
                if (combatLockTimer <= 0f)
                    combatLockState = CombatLockState.NONE;
            }
        }

        touchingPlayer = false;
    }

    public void onPlayerContact() {
        touchingPlayer = true;

        if (combatLockState == CombatLockState.NONE) {
            combatLockState = CombatLockState.ATTACKING;
            combatLockTimer = minAttackCommitTime();
        }
    }

    protected float minAttackCommitTime() { return 0.25f; }
    protected float attackLockDuration() { return 0.4f; }
    protected float postAttackDelay() { return 0.25f; }

    // =========================
    // OVERRIDES / QUERIES
    // =========================
    public boolean isAggroLocked() { return aggroLocked; }
    public boolean isAttackingLocked() { return combatLockState == CombatLockState.ATTACKING; }

    protected boolean supportsAggroLock() { return false; }
    protected boolean usesAISwitching() { return true; }
    protected boolean shouldResetPatrolOnReturn() { return true; }

    public boolean ignoresEnemyCollision() { return false; }

    public boolean isChasing() {
        return activeBehavior != null
                && activeBehavior.getType() == BehaviorStateType.CHASE
                && (aggroLocked || isAggroLocked());
    }

    protected void onStuck(Maze maze) {
        Vector2 escape = new Vector2(
                (float) (Math.random() * 2 - 1),
                (float) (Math.random() * 2 - 1)
        );
        if (escape.len2() < 0.001f) return;
        escape.nor().scl(getSpeed() * 0.5f);
        collisionHandler.move(worldPosition, escape, 0.1f, maze);
    }

    protected boolean canMoveTo(float x, float y, Maze maze) {
        float inset = 2f;
        return !maze.isWallWorld(x + inset, y + inset)
                && !maze.isWallWorld(x + 32f - inset, y + inset)
                && !maze.isWallWorld(x + inset, y + 32f - inset)
                && !maze.isWallWorld(x + 32f - inset, y + 32f - inset);
    }

    public int getPathfindingFootprint() { return 2; }
    public float getAttackRadius() { return 0f; }
    public float getSeparationRadius() { return 24f; }
    protected float maxAllowedOverlap() { return 6f; }

    public float getRenderWidth() { return 32f; }
    public float getRenderHeight() { return 32f; }
}
