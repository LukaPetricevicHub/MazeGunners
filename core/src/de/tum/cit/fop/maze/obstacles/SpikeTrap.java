package de.tum.cit.fop.maze.obstacles;

import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Vector2;

public class SpikeTrap extends Obstacle {

    // Attributes
    private static final int SPIKE_DAMAGE = 1;
    private static final float RESET_COOLDOWN = 1.5f;
    private static final float HITBOX_SIZE = 16f;
    private static final Texture SPIKE_TEX = new Texture("spike.png");
    private final TextureRegion spikeSprite;


    // Constructor
    public SpikeTrap(Vector2 spawnPosition) {
        super(spawnPosition, SPIKE_DAMAGE, RESET_COOLDOWN, HITBOX_SIZE, HITBOX_SIZE);
        spikeSprite = new TextureRegion(SPIKE_TEX);
    }


    // Update the trap logic state
    public void update(float deltaTime) {
        // No animation for now
    }


    // Get the current visual sprite
    public TextureRegion getCurrentFrame() {
        return spikeSprite;
    }
}