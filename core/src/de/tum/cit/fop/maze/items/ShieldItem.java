package de.tum.cit.fop.maze.items;

import de.tum.cit.fop.maze.player.Player;

public class ShieldItem extends AbstractCollectable {
    public ShieldItem(float x, float y) {
        super(x, y, "shield.png");
    }

    @Override
    public void applyEffect(Player player) {
        player.activateShield(5.0f);
    }
}