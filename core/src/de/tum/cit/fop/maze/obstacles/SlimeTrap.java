package de.tum.cit.fop.maze.obstacles;

import com.badlogic.gdx.math.Vector2;

public class SlimeTrap extends Obstacle {

    private static final int DAMAGE = 1;
    private static final float COOLDOWN = 1.5f;
    private static final float SIZE = 16f;

    private static final float SLOW_MULTIPLIER = 0.5f;
    private static final float VISION_MULTIPLIER = 0.6f;
    private static final float EFFECT_DURATION = 3f;

    private float effectTimer = 0f;

    public SlimeTrap(Vector2 position) {
        super(position, DAMAGE, COOLDOWN, SIZE, SIZE);
    }

    @Override
    public void onActivate() {
        effectTimer = EFFECT_DURATION;
        startCooldown();
    }

    public void updateEffect(float dt) {
        if (effectTimer > 0f) {
            effectTimer -= dt;
        }
    }

    public boolean isEffectActive() {
        return effectTimer > 0f;
    }

    public float getSlowMultiplier() {
        return SLOW_MULTIPLIER;
    }

    public float getVisionMultiplier() {
        return VISION_MULTIPLIER;
    }
}
