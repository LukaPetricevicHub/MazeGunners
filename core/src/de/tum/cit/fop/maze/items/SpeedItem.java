package de.tum.cit.fop.maze.items;

import de.tum.cit.fop.maze.player.Player;

public class SpeedItem extends AbstractCollectable {
    public SpeedItem(float x, float y) {
        super(x, y, "speed.png");
    }

    @Override
    public void applyEffect(Player player) {
        player.activateSpeedBoost(5.0f, 1.8f);
    }
}