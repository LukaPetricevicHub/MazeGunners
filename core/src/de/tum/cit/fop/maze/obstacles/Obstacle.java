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


    // Constructor
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


    // Determine whether obstacle triggers this frame
    public boolean tryActivate(Rectangle playerBounds) {
        boolean isPlayerInside = intersectsPlayer(playerBounds);

        if (isPlayerInside && !wasPlayerInside && isActive && cooldownTimer <= 0f) {
            wasPlayerInside = true;
            return true;
        }

        wasPlayerInside = isPlayerInside;
        return false;
    }


    // Return damage dealt on activation
    public int getDamageToPlayer() {
        return damageToPlayer;
    }


    // Start cooldown timer
    public void startCooldown() {
        cooldownTimer = activationCooldown;
    }


    // Update cooldown timer each frame
    public void updateCooldown(float deltaTime) {
        if (cooldownTimer > 0f) {
            cooldownTimer -= deltaTime;
        }
    }


    // Return collision bounds
    public Rectangle getCollisionBox() {
        return collisionBounds;
    }


    // Return whether obstacle is active
    public boolean isActive() {
        return isActive;
    }
}
