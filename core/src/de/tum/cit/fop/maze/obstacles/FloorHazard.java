package de.tum.cit.fop.maze.obstacles;

import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;

/**
 * A floor-based hazard that temporarily boosts player movement speed.
 *
 * When the player steps onto the hazard, a short speed boost is activated.
 * The hazard enforces a cooldown before it can trigger again.
 */
public class FloorHazard extends Obstacle {

    
    private static final float SPEED_UP_FACTOR = 1.6f;
    private static final float MAX_BOOST_TIME = 2.5f;

    private float activeBoostTimer = 0f;

    private static final Texture HAZARD_TEX = new Texture("Traps/FloorHazard.png");
    private final TextureRegion hazardSprite;

    private boolean playerWasOn = false;


    /**
     * A floor-based hazard that temporarily boosts player movement speed.
     *
     * When the player steps onto the hazard, a short speed boost is activated.
     * The hazard enforces a cooldown before it can trigger again.
     */
    public FloorHazard(Vector2 spawnPos) {
        super(spawnPos, 0, 2f, 16f, 16f);

        hazardSprite = new TextureRegion(HAZARD_TEX);
    }


    /**
     * Activates the floor hazard effect.
     *
     * Starts the speed boost timer and triggers the internal cooldown.
     */
    @Override
    public void onActivate() {
        activeBoostTimer = MAX_BOOST_TIME;
        startCooldown();
    }


    /**
     * Updates the active speed boost timer.
     *
     * @param deltaTime time elapsed since the last frame
     */
    public void updateEffect(float deltaTime) {
        if (activeBoostTimer > 0f)
            activeBoostTimer -= deltaTime;

    }

    /**
     * Checks whether the player has just stepped onto the hazard.
     *
     * This method returns true only on the first frame the player
     * enters the hazard area.
     *
     * @param playerFeet collision rectangle representing the player's feet
     * @return true if the player entered the hazard this frame
     */
    public boolean justEntered(Rectangle playerFeet) {
        boolean on = collisionBounds.overlaps(playerFeet);
        boolean entered = on && !playerWasOn;

        playerWasOn = on;
        return entered;
    }


    /**
     * Checks whether the player is within activation range of the hazard.
     *
     * @param px player world X position
     * @param py player world Y position
     * @param tileSize size of a single maze tile
     * @return true if the player is within the hazard's effective radius
     */
    public boolean isPlayerInRange(float px, float py, float tileSize) {
        float radius = 3f * tileSize;

        float cx = collisionBounds.x + collisionBounds.width * 0.5f;
        float cy = collisionBounds.y + collisionBounds.height * 0.5f;

        float dx = px - cx;
        float dy = py - cy;

        return dx * dx + dy * dy <= radius * radius;
    }


    /**
     * @return true if the speed boost is currently active
     */
    public boolean isBoostActive() {
        return activeBoostTimer > 0f;
    }

    /**
     * @return movement speed multiplier applied while the boost is active
     */
    public float getSpeedBoost() {
        return SPEED_UP_FACTOR;
    }

    /**
     * @return texture region used to render the floor hazard
     */
    public TextureRegion getSprite() {
        return hazardSprite;
    }

}
