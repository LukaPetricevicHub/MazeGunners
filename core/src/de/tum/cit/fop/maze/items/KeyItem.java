package de.tum.cit.fop.maze.items;

import de.tum.cit.fop.maze.player.Player;

/**
 * A collectable item that gives the player a key.
 */
public class KeyItem extends Collectable {

    /**
     * Creates a key item at the given position.
     */
    public KeyItem(float x, float y) {
        super(x, y, "key.png");
    }

    /**
     * Gives the key to the player.
     */
    @Override
    public void applyEffect(Player player) {
        player.giveKey();
    }
}
