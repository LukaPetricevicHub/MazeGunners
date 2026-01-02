package de.tum.cit.fop.maze.obstacles;

import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;

public abstract class Obstacle {

    // Obstacle attributes
    protected Vector2 position;
    protected Rectangle collisionBox;
    protected boolean isActive;
    protected int damageToPlayer;
    protected float cooldown;
    protected float cooldownTimer;
    protected boolean playerWasTrapped;

    // Obstacle constructor
    public Obstacle(Vector2 position,int damageToPlayer, float cooldown, float width, float height) {
        this.position = new Vector2(position);
        this.collisionBox = new Rectangle(position.x, position.y, width, height);
        this.damageToPlayer = damageToPlayer;
        this.isActive = true;
        this.cooldown = cooldown;
        this.cooldownTimer = 0f;
        this.playerWasTrapped = false;

    }

    // Player collides with obstacle
    public boolean intersectsWithPlayerBounds(Rectangle playerBounds) { // To replace with actual player bounds (player.getCollisionBounds()
        return collisionBox.overlaps(playerBounds);
    }

    // Obstacle activates
    public boolean activateObstacle(Rectangle playerBounds) {
        boolean playerIsTrapped = intersectsWithPlayerBounds(playerBounds);

        if (playerIsTrapped && !playerWasTrapped && isActive && cooldownTimer <= 0f) {
            playerWasTrapped = true;
            return true;
        }

        playerWasTrapped = playerIsTrapped;
        return false;
    }

    // Obstacle deals damageToPlayer
    public int getDamageToPlayer() {
        return damageToPlayer;
    }

    // Start obstacle cooldown
    public void startObstacleCooldown() {
        cooldownTimer = cooldown;
    }

    // Update cooldownTimer
    public void updateCooldownTimer(float delta) {
        if (cooldownTimer > 0f) {
            cooldownTimer -= delta;
        }
    }

    // Getters
    public Rectangle getCollisionBox() {
        return collisionBox;
    }

    public boolean isActive() {
        return isActive;
    }
}