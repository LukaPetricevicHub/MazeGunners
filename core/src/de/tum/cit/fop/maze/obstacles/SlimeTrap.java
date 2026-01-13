package de.tum.cit.fop.maze.obstacles;

import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Vector2;

public class SlimeTrap extends Obstacle {

    // Attributes
    private static final int DAMAGE = 1;
    private static final float COOLDOWN = 1.5f;
    private static final float SIZE = 16f;
    private static final float SLOW_MULTIPLIER = 0.5f;
    private static final float VISION_MULTIPLIER = 0.5f;
    private static final float EFFECT_DURATION = 3f;
    private float effectTimer = 0f;
    private static final Texture TEXTURE = new Texture("slime.png");
    private final TextureRegion sprite;


    // Constructor
    public SlimeTrap(Vector2 position) {
        super(position, DAMAGE, COOLDOWN, SIZE, SIZE);
        sprite = new TextureRegion(TEXTURE);
    }


    // Trigger trap and start cooldown
    @Override
    public void onActivate() {
        effectTimer = EFFECT_DURATION;
        startCooldown();
    }


    // Update the active effect duration
    public void updateEffect(float dt) {
        if (effectTimer > 0f) {
            effectTimer -= dt;
        }
    }


    // Check if effect still active
    public boolean isEffectActive() {
        return effectTimer > 0f;
    }


    // Get the movement speed penalty
    public float getSlowMultiplier() {
        return SLOW_MULTIPLIER;
    }


    // Get the vision reduction penalty
    public float getVisionMultiplier() {
        return VISION_MULTIPLIER;
    }


    // Return the current trap texture
    public TextureRegion getSprite() {
        return sprite;
    }
}