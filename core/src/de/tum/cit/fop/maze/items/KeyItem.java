package de.tum.cit.fop.maze.items;

import de.tum.cit.fop.maze.player.Player;

public class KeyItem extends Collectable {
    public KeyItem(float x, float y) {
        super(x, y, "key.png");
    }

    @Override
    public void applyEffect(Player player) {
        player.giveKey();
    }
}
