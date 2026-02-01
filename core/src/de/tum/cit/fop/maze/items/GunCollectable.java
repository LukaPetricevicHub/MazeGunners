package de.tum.cit.fop.maze.items;

import de.tum.cit.fop.maze.player.Player;

/**
 * A collectable gun item.
 *
 * When picked up, the player receives a gun and bonus score.
 */
public class GunCollectable extends Collectable {

    /**
     * Creates a gun collectable at the given world position.
     *
     * @param x x-position in world coordinates
     * @param y y-position in world coordinates
     */
    public GunCollectable(float x, float y) {
        super(x, y, "gun.png");
    }

    /**
     * Marks the gun as collected.
     */
    @Override
    public void collect() {
        super.collect();
        System.out.println("GUN COLLECTED!");
    }

    /**
     * Gives the player a gun and adds score.
     *
     * @param player the player collecting the gun
     */
    @Override
    public void applyEffect(Player player) {
        player.equipGun();
        player.addScore(200);
    }
}