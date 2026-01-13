package de.tum.cit.fop.maze.obstacles;

import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Vector2;

public class SpikeTrap extends Obstacle {

    // Attributes
    private static final int DAMAGE = 1;
    private static final float COOLDOWN = 1.5f;
    private static final float SIZE = 16f;
    private static final Texture TEXTURE = new Texture("spike.png");
    private final TextureRegion sprite;


    // Constructor
    public SpikeTrap(Vector2 position) {
        super(position, DAMAGE, COOLDOWN, SIZE, SIZE);
        sprite = new TextureRegion(TEXTURE);
    }


    // Update the trap logic state
    public void update(float dt) {
        // No animation for now
    }


    // Get the current visual sprite
    public TextureRegion getCurrentFrame() {
        return sprite;
    }
}