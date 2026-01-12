package de.tum.cit.fop.maze.enemies;

import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;
import de.tum.cit.fop.maze.behavior.BehaviorState;
import de.tum.cit.fop.maze.behavior.BehaviorStateType;
import de.tum.cit.fop.maze.collision.CollisionHandler;
import de.tum.cit.fop.maze.level.Maze;
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

    private static final float COLLISION_INSET = 2f;

    // AI attributes
    protected float chaseRadius = 200f;
    private float chaseMemoryTimer = 0f;
    private static final float CHASE_MEMORY_DURATION = 2.0f;


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

        this.collisionBounds =
                new Rectangle(startWorldPosition.x, startWorldPosition.y, 32f, 64f);

        this.collisionHandler =
                new CollisionHandler(collisionBounds, COLLISION_INSET);
    }


    // Update enemy logic
    public final void update(float deltaTime, Maze maze) {
        updateAI(deltaTime);
        updateBehavior(deltaTime, maze);
        collisionBounds.setPosition(worldPosition.x, worldPosition.y);
    }


    // AI state switching
    protected void updateAI(float deltaTime) {
        if (targetPlayer == null || activeBehavior == null)
            return;

        float dist2 = worldPosition.dst2(getPlayerPosition());
        boolean playerInRange = dist2 <= chaseRadius * chaseRadius;

        if (playerInRange) {
            chaseMemoryTimer = 0f;

            if (activeBehavior.getType() != BehaviorStateType.CHASE) {
                setCurrentBehavior(createChaseBehavior());
            }
        } else {
            chaseMemoryTimer += deltaTime;

            if (chaseMemoryTimer >= CHASE_MEMORY_DURATION
                    && activeBehavior.getType() == BehaviorStateType.CHASE) {
                setCurrentBehavior(createDefaultBehavior());
            }
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

    public Vector2 getPosition() { return worldPosition; }
    public float getSpeed() { return movementSpeed; }
    public Rectangle getCollisionBox() { return collisionBounds; }
    public CollisionHandler getCollisionHandler() { return collisionHandler; }
    public Vector2 getHomePosition() { return homeWorldPosition; }

    public void setCurrentBehavior(BehaviorState behavior) {
        this.activeBehavior = behavior;
    }
}

