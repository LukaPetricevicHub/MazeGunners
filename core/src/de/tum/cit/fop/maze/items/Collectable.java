package de.tum.cit.fop.maze.items;

import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.Rectangle;
import de.tum.cit.fop.maze.player.Player;

/**
 * Base class for all collectable items in the maze.
 *
 * A collectable has a position, a texture, and can apply an effect
 * to the player when collected.
 */
public abstract class Collectable {
    /** World-space bounds of the collectable. */
    protected Rectangle bounds;
    /** Texture used to render the collectable. */
    protected Texture texture;
    /** Whether the collectable is still active. */
    private boolean active = true;

    /**
     * Creates a collectable at the given world position.
     *
     * @param x x-position in world coordinates
     * @param y y-position in world coordinates
     * @param texturePath path to the item texture
     */
    public Collectable(float x, float y, String texturePath) {
        this.bounds = new Rectangle(x, y, 32, 32);
        this.texture = new Texture(texturePath);
    }

    /**
     * Applies the item's effect to the player.
     *
     * @param player the player collecting the item
     */
    public abstract void applyEffect(Player player);

    /**
     * Renders the item if it is still active.
     *
     * @param batch sprite batch used for rendering
     */
    public void render(SpriteBatch batch) {
        if (active) {
            batch.draw(texture, bounds.x, bounds.y, 32, 32);
        }
    }

    /**
     * Retrieves the world-space bounds of the collectable.
     *
     * @return bounding rectangle
     */
    public Rectangle getBounds() { return bounds; }

    /**
     * Indicates whether the collectable is still active.
     *
     * @return true if active, false if already collected
     */
    public boolean isActive() { return active; }

    /**
     * Marks the collectable as collected and inactive.
     */
    public void collect() { active = false; }

    /**
     * Releases texture resources used by the collectable.
     */
    public void dispose() {
        if (texture != null) texture.dispose();
    }
}
