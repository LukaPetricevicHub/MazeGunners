package de.tum.cit.fop.maze.obstacles;

import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;

public abstract class Obstacle {

    // Attributes
    protected Vector2 worldPosition;
    protected Rectangle collisionBounds;
    protected boolean isActive;
    protected int damageToPlayer;
    protected float activationCooldown;
    protected float cooldownTimer;
    protected boolean wasPlayerInside;
    protected boolean blocksMovement = false;


    // Constructor For Traps
    public Obstacle(Vector2 startWorldPosition, int damageToPlayer, float activationCooldown, float width, float height) {
        this.worldPosition = new Vector2(startWorldPosition);
        this.collisionBounds = new Rectangle(startWorldPosition.x, startWorldPosition.y, width, height);
        this.damageToPlayer = damageToPlayer;
        this.isActive = true;
        this.activationCooldown = activationCooldown;
        this.cooldownTimer = 0f;
        this.wasPlayerInside = false;
    }


    // Check collision with player bounds
    public boolean intersectsPlayer(Rectangle playerBounds) {
        return collisionBounds.overlaps(playerBounds);
    }


    // Handle logic for obstacle activation
    public boolean tryActivate(Rectangle playerBounds) {
        boolean isPlayerInside = intersectsPlayer(playerBounds);
        if (isPlayerInside && !wasPlayerInside && isActive && cooldownTimer <= 0f) {
            wasPlayerInside = true;
            return true;
        }
        wasPlayerInside = isPlayerInside;
        return false;
    }

    public boolean blocksMovement() {
        return blocksMovement;
    }


    // Perform specific obstacle activation logic
    public void onActivate() {}


    // Get damage dealt to player
    public int getDamageToPlayer() {
        return damageToPlayer;
    }


    // Begin the internal cooldown timer
    public void startCooldown() {
        cooldownTimer = activationCooldown;
    }


    // Reduce cooldown timer by delta
    public void updateCooldown(float deltaTime) {
        if (cooldownTimer > 0f) {
            cooldownTimer -= deltaTime;
        }
    }


    // Return the obstacle collision box
    public Rectangle getCollisionBox() {
        return collisionBounds;
    }


    // Check if obstacle is active
    public boolean isActive() {
        return isActive;
    }


    // Return width of the obstacle
    public float getWidth() {
        return collisionBounds.width;
    }


    // Return height of the obstacle
    public float getHeight() {
        return collisionBounds.height;
    }
}

