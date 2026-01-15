package de.tum.cit.fop.maze.obstacles;

import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Vector2;

public class FloorHazard extends Obstacle {

    // Attributes
    private static final float SPEED_UP_FACTOR = 1.6f;
    private static final float MAX_BOOST_TIME = 2.5f;
    private float activeBoostTimer = 0f;
    private static final Texture HAZARD_TEX = new Texture("hazard.png");
    private final TextureRegion hazardSprite;


    // Constructor
    public FloorHazard(Vector2 spawnPos) {
        super(spawnPos, 0, 2f, 16f, 16f);
        hazardSprite = new TextureRegion(HAZARD_TEX); // ✅ FULL IMAGE
    }


    // If activated, boost Player
    @Override
    public void onActivate() {
        activeBoostTimer = MAX_BOOST_TIME;
        startCooldown();
    }


    // Boost
    public void updateEffect(float deltaTime) {
        if (activeBoostTimer > 0f)
            activeBoostTimer -= deltaTime;

    }


    public boolean isBoostActive() {
        return activeBoostTimer > 0f;
    }

    public float getSpeedBoost() {
        return SPEED_UP_FACTOR;
    }

    public TextureRegion getSprite() {
        return hazardSprite;
    }
}
