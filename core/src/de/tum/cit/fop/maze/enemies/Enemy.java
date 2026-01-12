package de.tum.cit.fop.maze.enemies;

import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;
import de.tum.cit.fop.maze.behavior.BehaviorState;
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

        // Enemy owns its collision handling
        this.collisionHandler =
                new CollisionHandler(collisionBounds, COLLISION_INSET);
    }


    // Update enemy logic and collision bounds
    public final void update(float deltaTime, Maze maze) {
        updateBehavior(deltaTime, maze);
        collisionBounds.setPosition(worldPosition.x, worldPosition.y);
    }


    // Execute current behavior logic
    protected void updateBehavior(float deltaTime, Maze maze) {
        if (activeBehavior != null)
            activeBehavior.execute(this, deltaTime, maze);
    }


    // Return player's current position
    public Vector2 getPlayerPosition() {
        if (targetPlayer != null) {
            return new Vector2(targetPlayer.getX(), targetPlayer.getY());
        }
        return new Vector2(0f, 0f);
    }


    // Getters
    public Vector2 getPosition() {
        return worldPosition;
    }

    public float getSpeed() {
        return movementSpeed;
    }

    public Rectangle getCollisionBox() {
        return collisionBounds;
    }

    public CollisionHandler getCollisionHandler() {
        return collisionHandler;
    }

    public Vector2 getHomePosition() {
        return homeWorldPosition;
    }

    public BehaviorState getCurrentBehavior() {
        return activeBehavior;
    }

    public void setCurrentBehavior(BehaviorState behavior) {
        this.activeBehavior = behavior;
    }

    public void setPlayer(Player player) {
        this.targetPlayer = player;
    }
}


