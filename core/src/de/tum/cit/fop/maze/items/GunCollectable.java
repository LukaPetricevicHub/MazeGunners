package de.tum.cit.fop.maze.items;

import de.tum.cit.fop.maze.player.Player;

public class GunCollectable extends AbstractCollectable {

    public GunCollectable(float x, float y) {
        super(x, y, "gun.png");
    }

    @Override
    public void collect() {
        super.collect();
        System.out.println("GUN COLLECTED!");
    }

    @Override
    public void applyEffect(Player player) {
        player.equipGun();
        player.addScore(200);
    }
}