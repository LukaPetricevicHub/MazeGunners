package de.tum.cit.fop.maze.obstacles;

import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;

/**
 * Base class for all maze obstacles and traps.
 *
 * An obstacle defines a rectangular collision area that can interact with
 * the player, optionally deal damage, trigger effects, and enforce
 * activation cooldowns.
 *
 * Subclasses implement specific behavior by overriding {@link #onActivate()}.
 */
public abstract class Obstacle {

    // Attributes
    /** World position of the obstacle. */
    protected Vector2 worldPosition;
    /** Collision bounds for the obstacle. */
    protected Rectangle collisionBounds;
    /** Whether the obstacle is active. */
    protected boolean isActive;
    /** Damage dealt to the player on activation. */
    protected int damageToPlayer;
    /** Cooldown duration between activations. */
    protected float activationCooldown;
    /** Remaining cooldown time. */
    protected float cooldownTimer;
    /** Whether the player was inside on the previous frame. */
    protected boolean wasPlayerInside;
    /** Whether this obstacle blocks movement. */
    protected boolean blocksMovement = false;


    /**
     * Creates a new obstacle at the given world position.
     *
     * @param startWorldPosition world-space position of the obstacle
     * @param damageToPlayer damage dealt to the player on activation
     * @param activationCooldown cooldown time before the obstacle can trigger again
     * @param width collision width of the obstacle
     * @param height collision height of the obstacle
     */
    public Obstacle(Vector2 startWorldPosition, int damageToPlayer, float activationCooldown, float width, float height) {
        this.worldPosition = new Vector2(startWorldPosition);
        this.collisionBounds = new Rectangle(startWorldPosition.x, startWorldPosition.y, width, height);
        this.damageToPlayer = damageToPlayer;
        this.isActive = true;
        this.activationCooldown = activationCooldown;
        this.cooldownTimer = 0f;
        this.wasPlayerInside = false;
    }


    /**
     * Checks whether this obstacle intersects the player's collision bounds.
     *
     * @param playerBounds bounding rectangle of the player
     * @return true if the player overlaps this obstacle
     */
    public boolean intersectsPlayer(Rectangle playerBounds) {
        return collisionBounds.overlaps(playerBounds);
    }


    /**
     * Attempts to activate the obstacle based on player interaction.
     *
     * An activation occurs when the player enters the obstacle's area,
     * the obstacle is active, and its cooldown has expired.
     *
     * @param playerBounds bounding rectangle of the player
     * @return true if the obstacle was successfully activated
     */
    public boolean tryActivate(Rectangle playerBounds) {
        boolean isPlayerInside = intersectsPlayer(playerBounds);
        if (isPlayerInside && !wasPlayerInside && isActive && cooldownTimer <= 0f) {
            wasPlayerInside = true;
            return true;
        }
        wasPlayerInside = isPlayerInside;
        return false;
    }

    /**
     * Indicates whether this obstacle blocks player movement.
     *
     * @return true if the obstacle acts as a movement blocker
     */
    public boolean blocksMovement() {
        return blocksMovement;
    }


    /**
     * Called when the obstacle is activated.
     *
     * Subclasses should override this method to implement
     * obstacle-specific effects or behavior.
     */
    public void onActivate() {}


    /**
     * Returns the amount of damage dealt to the player on activation.
     *
     * @return damage value
     */
    public int getDamageToPlayer() {
        return damageToPlayer;
    }


    /**
     * Starts the obstacle's activation cooldown.
     */
    public void startCooldown() {
        cooldownTimer = activationCooldown;
    }


    /**
     * Updates the internal cooldown timer.
     *
     * @param deltaTime time elapsed since last update
     */
    public void updateCooldown(float deltaTime) {
        if (cooldownTimer > 0f) {
            cooldownTimer -= deltaTime;
        }
    }


    /**
     * Returns the obstacle collision box.
     *
     * @return Collision bounds
     */
    public Rectangle getCollisionBox() {
        return collisionBounds;
    }


    /**
     * Checks whether the obstacle is active.
     *
     * @return True if active
     */
    public boolean isActive() {
        return isActive;
    }


    /**
     * Returns the obstacle width.
     *
     * @return Width in world units
     */
    public float getWidth() {
        return collisionBounds.width;
    }


    /**
     * Returns the obstacle height.
     *
     * @return Height in world units
     */
    public float getHeight() {
        return collisionBounds.height;
    }
}

