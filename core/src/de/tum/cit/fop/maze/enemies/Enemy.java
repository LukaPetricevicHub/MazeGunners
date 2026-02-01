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

/**
 * Base class for all enemies in the game.
 *
 * Handles movement, health, status effects (freeze, poison),
 * combat locking, collision, and AI behavior switching.
 */
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

    
    protected int health = 3;
    protected int maxHealth = 3;
    protected boolean markedForRemoval = false;
    protected boolean dying = false;
    private float hitFlashTimer = 0f;
    private static final float HIT_FLASH_DURATION = 0.12f;

    
    private boolean frozen = false;
    private float freezeTimer = 0f;

    private float poisonTimer = 0f;
    private float poisonDps = 0f;
    private float poisonTickAccumulator = 0f;

    
    private final Vector2 prevPosition = new Vector2();
    private float stuckTimer = 0f;
    private static final float STUCK_TIME_THRESHOLD = 0.4f;
    private static final float MIN_MOVEMENT_EPSILON = 0.5f;

    
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

    /**
     * Creates a new enemy instance.
     *
     * @param startWorldPosition initial world position
     * @param movementSpeed movement speed in world units
     * @param initialBehavior starting AI behavior
     * @param targetPlayer player this enemy reacts to
     */
    public Enemy(Vector2 startWorldPosition, float movementSpeed, BehaviorState initialBehavior, Player targetPlayer) {
        this.worldPosition = startWorldPosition;
        this.homeWorldPosition = new Vector2(startWorldPosition);
        this.movementSpeed = movementSpeed;
        this.activeBehavior = initialBehavior;
        this.targetPlayer = targetPlayer;

        this.collisionBounds = new Rectangle(startWorldPosition.x + COLLISION_OFFSET, startWorldPosition.y + COLLISION_OFFSET, COLLISION_SIZE, COLLISION_SIZE
        );
        this.collisionHandler = new CollisionHandler(collisionBounds, COLLISION_INSET);
    }

    protected void initHp(int hp) {
        this.maxHealth = Math.max(1, hp);
        this.health = this.maxHealth;
    }

    /**
     * Updates enemy logic for one frame.
     *
     * Handles status effects, AI, movement, collision,
     * combat locking, and stuck recovery.
     *
     * @param deltaTime time since last frame (seconds)
     * @param maze current maze for collision checks
     */
    public final void update(float deltaTime, Maze maze) {
        if (hitFlashTimer > 0f) hitFlashTimer = Math.max(0f, hitFlashTimer - deltaTime);
        prevPosition.set(worldPosition);

        updateStatusEffects(deltaTime);
        if(!dying && frozen) {
            collisionBounds.setPosition(worldPosition.x + COLLISION_OFFSET, worldPosition.y + COLLISION_OFFSET);
            return;
        }

        updateCombatLock(deltaTime);

        if (combatLockState == CombatLockState.NONE)
            updateAI(deltaTime);


        updateBehavior(deltaTime, maze);

        collisionBounds.setPosition(worldPosition.x + COLLISION_OFFSET, worldPosition.y + COLLISION_OFFSET);

        if (prevPosition.dst2(worldPosition) < MIN_MOVEMENT_EPSILON * MIN_MOVEMENT_EPSILON)
            stuckTimer += deltaTime;
        else
            stuckTimer = 0f;

        if (stuckTimer >= STUCK_TIME_THRESHOLD) {
            onStuck(maze);
            stuckTimer = 0f;
        }
    }

    /**
     * Updates AI state based on player distance and aggro rules.
     *
     * Switches between patrol and chase behaviors.
     *
     * @param deltaTime time since last frame
     */
    protected void updateAI(float deltaTime) {
        if (targetPlayer == null || activeBehavior == null) return;

        float dist2 = worldPosition.dst2(getPlayerPosition());
        boolean playerInRange = dist2 <= chaseRadius * chaseRadius;

        if(playerInRange && supportsAggroLock()) {
            aggroLocked = true;
        }

        if (aggroLocked || playerInRange) {
            chaseMemoryTimer = 0f;

            if(activeBehavior.getType() != BehaviorStateType.CHASE) {
                setCurrentBehavior(createChaseBehavior());
            }
            return;
        }

        chaseMemoryTimer += deltaTime;

        if (chaseMemoryTimer >= CHASE_MEMORY_DURATION && activeBehavior.getType() == BehaviorStateType.CHASE) {
            setCurrentBehavior(createDefaultBehavior());
        }
    }

    /**
     * Executes the currently active behavior.
     *
     * @param deltaTime time since last frame
     * @param maze maze used for movement and collision
     */
    protected void updateBehavior(float deltaTime, Maze maze) {
        if (activeBehavior != null) {
            activeBehavior.execute(this, deltaTime, maze);
        }
    }


    protected abstract BehaviorState createChaseBehavior();
    protected abstract BehaviorState createDefaultBehavior();

    /**
     * Deals damage to the enemy.
     *
     * @param amount damage amount
     */
    public void takeDamage(int amount) {
        if (dying) return;

        health -= Math.max(0, amount);
        hitFlashTimer = HIT_FLASH_DURATION;
        if (health <= 0) {
            die();
        }
    }

    /**
     * Triggers the enemy death sequence.
     */
    protected void die() {
        if (dying) return;
        dying = true;
        finalizeDeath();
    }

    public boolean isDying() {
        return dying;
    }

    /**
     * Marks the enemy for removal from the game.
     */
    public void finalizeDeath() {
        markedForRemoval = true;
    }

    public boolean isDead() {
        return markedForRemoval;
    }

    public boolean isHitFlashing() {
        return hitFlashTimer > 0f;
    }

    /**
     * Scales the enemy's health by a multiplier.
     *
     * @param mult health multiplier
     */
    public void scaleHp(float mult) {
        if (mult <= 1f) return;
        maxHealth = Math.max(1, Math.round(maxHealth * mult));
        health = maxHealth;
    }

    public void addHp(int delta) {
        if (delta == 0) return;
        maxHealth = Math.max(1, maxHealth + delta);
        health = maxHealth;
    }

    /**
     * Freezes the enemy for a given duration.
     *
     * @param duration freeze time in seconds
     */
    public void applyFreeze(float duration) {
        if (dying) return;
        frozen = true;
        freezeTimer = Math.max(freezeTimer, duration);
    }

    /**
     * Applies poison damage over time.
     *
     * @param duration poison duration in seconds
     * @param dps damage per second
     */
    public void applyPoison(float duration, float dps) {
        if (dying) return;
        poisonTimer = Math.max(poisonTimer, duration);
        poisonDps = Math.max(0f, dps);
    }

    public boolean isFrozen() {
        return frozen;
    }

    private void updateStatusEffects(float dt) {
        if (dying) return;

        
        if(frozen) {
            freezeTimer -= dt;
            if (freezeTimer <= 0f) {
                freezeTimer = 0f;
                frozen = false;
            }
        }

        
        if (poisonTimer > 0f) {
            poisonTimer -= dt;

            poisonTickAccumulator += poisonDps * dt;

            
            while (poisonTickAccumulator >= 1f) {
                takeDamage(1);
                poisonTickAccumulator -= 1f;
                if (dying)
                    break;
            }

            if (poisonTimer <= 0f) {
                poisonTimer = 0f;
                poisonDps = 0f;
                poisonTickAccumulator = 0f;
            }
        }
    }

    
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

    
    public Rectangle getInteractionRect() {
        interactionRect.set(worldPosition.x - INTERACTION_MARGIN, worldPosition.y - INTERACTION_MARGIN, getRenderWidth() + INTERACTION_MARGIN * 2, INTERACTION_BODY_HEIGHT + INTERACTION_MARGIN * 2);
        return interactionRect;
    }

    /**
     * Pushes enemies apart to prevent overlapping.
     *
     * @param others list of other enemies
     * @param player player reference
     * @param deltaTime time since last frame
     * @param maze maze for collision handling
     */
    public void applyEnemySeparation(List<Enemy> others, Player player, float deltaTime, Maze maze) {
        if (combatLockState == CombatLockState.ATTACKING) return;
        if (isFrozen()) return;

        Vector2 pos = getPosition();
        Rectangle myRect = getInteractionRect();
        Vector2 correction = new Vector2();

        for (Enemy other : others) {
            if (other == this) continue;
            if (ignoresEnemyCollision() || other.ignoresEnemyCollision()) continue;

            Rectangle otherRect = other.getInteractionRect();
            if (!myRect.overlaps(otherRect)) continue;

            float overlapX = Math.min(myRect.x + myRect.width - otherRect.x, otherRect.x + otherRect.width - myRect.x);
            float overlapY = Math.min(myRect.y + myRect.height - otherRect.y, otherRect.y + otherRect.height - myRect.y);

            float tolerance = maxAllowedOverlap();
            if (overlapX < tolerance && overlapY < tolerance) continue;

            if (overlapX < overlapY) {
                correction.x += (myRect.x + myRect.width * 0.5f < otherRect.x + otherRect.width * 0.5f) ? -overlapX : overlapX;
            } else{
                correction.y += (myRect.y + myRect.height * 0.5f < otherRect.y + otherRect.height * 0.5f) ? -overlapY : overlapY;
            }
        }

        if (correction.len2() < 0.0001f) return;

        correction.nor().scl(getSpeed() * 0.95f);
        getCollisionHandler().move(pos, correction, deltaTime, maze);
    }

    
    protected void updateCombatLock(float deltaTime) {
        switch (combatLockState) {

            case NONE -> { }

            case ATTACKING -> {
                if (combatLockTimer > 0f)
                    combatLockTimer -= deltaTime;

                if(!touchingPlayer && combatLockTimer <= 0f) {
                    combatLockState = CombatLockState.POST_ATTACK_DELAY;
                    combatLockTimer = postAttackDelay();
                }
            }

            case POST_ATTACK_DELAY -> {
                combatLockTimer -= deltaTime;
                if (combatLockTimer <= 0f) {
                    combatLockState = CombatLockState.NONE;
                }
            }
        }

        touchingPlayer = false;
    }

    /**
     * Notifies the enemy that it is touching the player.
     */
    public void onPlayerContact() {
        touchingPlayer = true;

        if (combatLockState == CombatLockState.NONE) {
            combatLockState = CombatLockState.ATTACKING;
            combatLockTimer = minAttackCommitTime();
        }
    }

    protected float minAttackCommitTime() { return 0.25f; }
    protected float postAttackDelay() { return 0.25f; }

    public boolean isAggroLocked() { return aggroLocked; }
    public boolean isAttackingLocked() { return combatLockState == CombatLockState.ATTACKING; }

    protected boolean supportsAggroLock() { return false; }
    protected boolean shouldResetPatrolOnReturn() { return true; }
    public boolean canBeHitByMelee() { return true; }

    public boolean ignoresEnemyCollision() { return false; }

    /**
     * Attempts to free the enemy when stuck.
     *
     * @param maze maze used for collision checks
     */
    protected void onStuck(Maze maze) {
        if (isFrozen()) return;

        Vector2 escape = new Vector2((float) (Math.random() * 2 - 1), (float) (Math.random() * 2 - 1));
        if (escape.len2() < 0.001f) return;
        escape.nor().scl(getSpeed() * 0.5f);
        collisionHandler.move(worldPosition, escape, 0.1f, maze);
    }

    /**
     * Checks if the enemy can move to a given position.
     *
     * @param x target x position
     * @param y target y position
     * @param maze maze for wall checks
     * @return true if movement is possible
     */
    protected boolean canMoveTo(float x, float y, Maze maze) {
        float inset = 2f;
        return !maze.isWallWorld(x + inset, y + inset) && !maze.isWallWorld(x + 32f - inset, y + inset) && !maze.isWallWorld(x + inset, y + 32f - inset) && !maze.isWallWorld(x + 32f - inset, y + 32f - inset);
    }

    public int getPathfindingFootprint() { return 2; }
    public float getAttackRadius() { return 0f; }
    public float getSeparationRadius() { return 24f; }
    protected float maxAllowedOverlap() { return 6f; }

    public float getRenderWidth() { return 32f; }
    public float getRenderHeight() { return 32f; }
}
