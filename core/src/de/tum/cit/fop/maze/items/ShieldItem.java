package de.tum.cit.fop.maze.items;

import de.tum.cit.fop.maze.player.Player;

/**
 * A collectable item that gives the player a temporary shield.
 */
public class ShieldItem extends Collectable {
    /**
     * Creates a shield item at the given position.
     *
     * @param x World x-coordinate
     * @param y World y-coordinate
     */
    public ShieldItem(float x, float y) {
        super(x, y, "shield.png");
    }

    /**
     * Activates the shield effect on the player.
     *
     * @param player Player who collected the item
     */
    @Override
    public void applyEffect(Player player) {
        player.activateShield(5.0f);
    }
}
