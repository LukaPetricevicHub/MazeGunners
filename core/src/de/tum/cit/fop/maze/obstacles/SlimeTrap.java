package de.tum.cit.fop.maze.obstacles;

import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Vector2;

public class SlimeTrap extends Obstacle {

    // Attributes
    private static final int TRAP_DAMAGE = 1;
    private static final float RETRIGGER_COOLDOWN = 1.5f;
    private static final float TRAP_SIZE = 16f;
    private static final float SPEED_PENALTY = 0.5f;
    private static final float VISION_PENALTY = 0.5f;
    private static final float DEBUFF_MAX_DURATION = 3f;
    private float activeDebuffTimer = 0f;
    private static final Texture SLIME_TEX = new Texture("slime.png");
    private final TextureRegion trapSprite;


    // Constructor
    public SlimeTrap(Vector2 spawnPosition) {
        super(spawnPosition, TRAP_DAMAGE, RETRIGGER_COOLDOWN, TRAP_SIZE, TRAP_SIZE);
        trapSprite = new TextureRegion(SLIME_TEX);
    }


    // Trigger trap and start cooldown
    @Override
    public void onActivate() {
        activeDebuffTimer = DEBUFF_MAX_DURATION;
        startCooldown();
    }


    // Update the active effect duration
    public void updateEffect(float deltaTime) {
        if (activeDebuffTimer > 0f)
            activeDebuffTimer -= deltaTime;

    }


    // Check if effect still active
    public boolean isEffectActive() {
        return activeDebuffTimer > 0f;
    }


    // Get the movement speed penalty
    public float getSlowMultiplier() {
        return SPEED_PENALTY;
    }


    // Get the vision reduction penalty
    public float getVisionMultiplier() {
        return VISION_PENALTY;
    }


    // Return the current trap texture
    public TextureRegion getSprite() {
        return trapSprite;
    }
}