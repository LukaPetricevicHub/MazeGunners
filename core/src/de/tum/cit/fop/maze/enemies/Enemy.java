package de.tum.cit.fop.maze.enemies;

import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;
import de.tum.cit.fop.maze.behavior.BehaviorState;
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


    // Constructor
    public Enemy(Vector2 startWorldPosition, float movementSpeed, BehaviorState initialBehavior, Player targetPlayer) {
        this.worldPosition = startWorldPosition;
        this.homeWorldPosition = new Vector2(startWorldPosition);
        this.movementSpeed = movementSpeed;
        this.collisionBounds = new Rectangle(startWorldPosition.x, startWorldPosition.y, 1f, 1f);
        this.activeBehavior = initialBehavior;
        this.targetPlayer = targetPlayer;
    }


    // Update enemy logic and collision bounds
    public final void update(float deltaTime, Maze maze) {
        if (movementSpeed == 0f) {
            com.badlogic.gdx.Gdx.app.log("ENEMY", "Speed is 0 for " + getClass().getSimpleName());
        }
        updateBehavior(deltaTime, maze);
        collisionBounds.setPosition(worldPosition.x, worldPosition.y);
    }


    // Execute current behavior logic
    protected void updateBehavior(float deltaTime, Maze maze) {
        if (activeBehavior != null) {
            Vector2 p = getPlayerPosition();
            if (p.x == 0f && p.y == 0f) {
                com.badlogic.gdx.Gdx.app.log("ENEMY", "targetPlayer is NULL -> chasing (0,0)");
            }
            activeBehavior.execute(this, deltaTime, maze);
        }
    }



    // Move enemy in given direction
    public void moveEnemy(Vector2 direction, float deltaTime, Maze maze) {
        if (direction.len2() < 0.0001f) return;
        Vector2 dir = new Vector2(direction).nor();
        worldPosition.mulAdd(dir, movementSpeed * deltaTime);
    }

    // Return player's current position
    public Vector2 getPlayerPosition() {
        if(targetPlayer != null) {
            return new Vector2(targetPlayer.getX(), targetPlayer.getY());
        }
        return new Vector2(0f, 0f);
    }

    // Getters & Setters
    public Vector2 getPosition() {
        return worldPosition;
    }

    public void setPosition(Vector2 worldPosition) {
        this.worldPosition = worldPosition;
    }

    public float getSpeed() {
        return movementSpeed;
    }

    public void setSpeed(float movementSpeed) {
        this.movementSpeed = movementSpeed;
    }

    public Rectangle getCollisionBox() {
        return collisionBounds;
    }

    public void setCollisionBox(Rectangle collisionBounds) {
        this.collisionBounds = collisionBounds;
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

    public Vector2 getHomePosition() {
        return homeWorldPosition;
    }

}
