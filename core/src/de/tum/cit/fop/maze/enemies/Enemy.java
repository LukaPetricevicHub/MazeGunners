package de.tum.cit.fop.maze.enemies;

import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;
import de.tum.cit.fop.maze.behavior.BehaviorState;
import de.tum.cit.fop.maze.behavior.BehaviorStateType;
import de.tum.cit.fop.maze.behavior.PatrolBehavior;
import de.tum.cit.fop.maze.collision.CollisionHandler;
import de.tum.cit.fop.maze.map.Maze;
import de.tum.cit.fop.maze.player.Player;

public abstract class Enemy {

    // Attributes
    protected Vector2 worldPosition;
    protected float movementSpeed;
    protected Rectangle collisionBounds;
    protected BehaviorState activeBehavior;
    protected Player targetPlayer;
    protected final Vector2 homeWorldPosition;
    protected final CollisionHandler collisionHandler;
    private static final float COLLISION_INSET = 1f;
    protected float chaseRadius = 200f;
    private float chaseMemoryTimer = 0f;
    private static final float CHASE_MEMORY_DURATION = 2.0f;
    protected boolean aggroLocked = false;
    private final Vector2 prevPosition = new Vector2();
    private float stuckTimer = 0f;
    private static final float STUCK_TIME_THRESHOLD = 0.4f;
    private static final float MIN_MOVEMENT_EPSILON = 0.5f;



    // Constructor
    public Enemy(
            Vector2 startWorldPosition,
            float movementSpeed,
            BehaviorState initialBehavior,
            Player targetPlayer
    ) {
        this.worldPosition = startWorldPosition;
        this.homeWorldPosition = new Vector2(startWorldPosition);
        this.movementSpeed = movementSpeed;
        this.activeBehavior = initialBehavior;
        this.targetPlayer = targetPlayer;
        this.collisionBounds = new Rectangle(startWorldPosition.x, startWorldPosition.y, 32f, 32f);
        this.collisionHandler = new CollisionHandler(collisionBounds, COLLISION_INSET);
    }


    // Update enemy logic
    public final void update(float deltaTime, Maze maze) {
        prevPosition.set(worldPosition);

        updateAI(deltaTime);
        updateBehavior(deltaTime, maze);

        collisionBounds.setPosition(worldPosition.x, worldPosition.y);

        // ---- STUCK DETECTION ----
        if (prevPosition.dst2(worldPosition) < MIN_MOVEMENT_EPSILON * MIN_MOVEMENT_EPSILON) {
            stuckTimer += deltaTime;
        } else {
            stuckTimer = 0f;
        }

        if (stuckTimer >= STUCK_TIME_THRESHOLD) {
            onStuck(maze);
            stuckTimer = 0f;
        }
    }




    // AI state switching
    protected void updateAI(float deltaTime) {
        if (targetPlayer == null || activeBehavior == null)
            return;

        float dist2 = worldPosition.dst2(getPlayerPosition());
        boolean playerInRange = dist2 <= chaseRadius * chaseRadius;

        // 1) Activation for aggro-lock enemies (ONCE)
        if (playerInRange && supportsAggroLock()) {
            aggroLocked = true;
        }

        // 2) Decide whether we should chase
        if (aggroLocked || playerInRange) {
            chaseMemoryTimer = 0f;

            if (activeBehavior.getType() != BehaviorStateType.CHASE) {
                setCurrentBehavior(createChaseBehavior());
            }
            return; // IMPORTANT: stop here
        }

        // 3) Only non-aggro enemies reach this point
        chaseMemoryTimer += deltaTime;

        if (chaseMemoryTimer >= CHASE_MEMORY_DURATION
                && activeBehavior.getType() == BehaviorStateType.CHASE) {
            setCurrentBehavior(createDefaultBehavior());
        }
    }



    // Execute current behavior
    protected void updateBehavior(float deltaTime, Maze maze) {
        if (activeBehavior != null)
            activeBehavior.execute(this, deltaTime, maze);
    }


    // Behavior factories
    protected abstract BehaviorState createChaseBehavior();
    protected abstract BehaviorState createDefaultBehavior();


    // Utility
    public Vector2 getPlayerPosition() {
        if (targetPlayer != null)
            return new Vector2(targetPlayer.getX(), targetPlayer.getY());
        return new Vector2(0f, 0f);
    }


    // Getters
    public Vector2 getPosition() { return worldPosition; }
    public float getSpeed() { return movementSpeed; }
    public Rectangle getCollisionBox() { return collisionBounds; }
    public CollisionHandler getCollisionHandler() { return collisionHandler; }
    public Vector2 getHomePosition() { return homeWorldPosition; }

    public void setCurrentBehavior(BehaviorState behavior) {
        if (behavior instanceof PatrolBehavior patrol && shouldResetPatrolOnReturn()) {
            patrol.resetToClosest(worldPosition);
        }
        this.activeBehavior = behavior;
    }



    public boolean isAggroLocked() {
        return aggroLocked;
    }


    protected boolean supportsAggroLock() {
        return false;
    }


    protected boolean usesAISwitching() {
        return true;
    }

    protected boolean shouldResetPatrolOnReturn() {
        return true;
    }

    protected void onStuck(Maze maze) {
        // Default: do nothing
    }

    protected boolean canMoveTo(float x, float y, Maze maze) {
        float inset = 2f;

        return !maze.isWallWorld(x + inset, y + inset)
                && !maze.isWallWorld(x + 32f - inset, y + inset)
                && !maze.isWallWorld(x + inset, y + 32f - inset)
                && !maze.isWallWorld(x + 32f - inset, y + 32f - inset);
    }

    public int getPathfindingFootprint() {
        return 2;
    }


}

