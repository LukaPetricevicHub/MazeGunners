package de.tum.cit.fop.maze.items;

import de.tum.cit.fop.maze.player.Player;

public class AmmoItem extends Collectable {
    private static final int AMMO_GAIN = 20;

    public AmmoItem(float x, float y) {
        super(x, y, "bullet.png");
    }

    @Override
    public void applyEffect(Player player) {
        if (player != null) {
            player.addAmmo(AMMO_GAIN);
        }
    }
}
