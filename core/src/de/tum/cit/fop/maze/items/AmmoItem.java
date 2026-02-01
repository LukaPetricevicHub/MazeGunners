package de.tum.cit.fop.maze.items;

import de.tum.cit.fop.maze.player.Player;

/**
 * Collectable that restores ammunition for the player.
 */
public class AmmoItem extends Collectable {
    /** Amount of ammo granted when collected. */
    private static final int AMMO_GAIN = 20;

    /**
     * Creates an ammo pickup at the given position.
     *
     * @param x World x-coordinate
     * @param y World y-coordinate
     */
    public AmmoItem(float x, float y) {
        super(x, y, "bullet.png");
    }

    /**
     * Applies the ammo bonus to the player.
     *
     * @param player Player who collected the item
     */
    @Override
    public void applyEffect(Player player) {
        if (player != null) {
            player.addAmmo(AMMO_GAIN);
        }
    }
}
