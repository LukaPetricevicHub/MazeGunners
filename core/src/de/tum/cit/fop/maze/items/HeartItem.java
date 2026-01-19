package de.tum.cit.fop.maze.items;

import de.tum.cit.fop.maze.player.Player;

public class HeartItem extends AbstractCollectable {
    public HeartItem(float x, float y) {
        super(x, y, "heart.png");
    }

    @Override
    public void applyEffect(Player player) {
        player.addLife();
    }
}