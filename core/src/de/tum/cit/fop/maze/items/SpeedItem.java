package de.tum.cit.fop.maze.items;

import de.tum.cit.fop.maze.player.Player;

/**
 * A collectable item that temporarily increases the player's movement speed.
 *
 * When picked up, the player receives a speed boost for a short duration.
 */
public class SpeedItem extends Collectable {
    /**
     * Creates a new speed item at the given position.
     *
     * @param x x-coordinate in the game world
     * @param y y-coordinate in the game world
     */
    public SpeedItem(float x, float y) {
        super(x, y, "speed.png");
    }

    /**
     * Applies the speed boost effect to the player.
     *
     * @param player the player who collected the item
     */
    @Override
    public void applyEffect(Player player) {
        player.activateSpeedBoost(6.0f, 2.2f);
    }
}
