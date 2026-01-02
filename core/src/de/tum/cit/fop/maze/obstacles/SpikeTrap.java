package de.tum.cit.fop.maze.obstacles;

import com.badlogic.gdx.math.Vector2;

public class SpikeTrap extends Obstacle {

    // SpikeTrap constants
    private static final int damage = 25; // 1/4 of player life
    private static final float cooldown = 1.5f;
    private static final float width = 32f;
    private static final float height = 32f;

    // SpikeTrap constructor
    public SpikeTrap (Vector2 position) {
        super(position, damage, cooldown, width, height);
    }

    // SpikeTrap animation

}
