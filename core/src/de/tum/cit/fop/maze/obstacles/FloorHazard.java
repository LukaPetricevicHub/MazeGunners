package de.tum.cit.fop.maze.obstacles;

import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Vector2;

public class FloorHazard extends Obstacle {

    // Attributes
    private static final float SPEED_BOOST = 1.6f;
    private static final float DURATION = 2.5f;
    private float boostTimer = 0f;
    private static final Texture TEXTURE = new Texture("hazard.png");
    private final TextureRegion sprite;


    // Constructor
    public FloorHazard(Vector2 position) {
        super(position, 0, 2f, 16f, 16f);
        sprite = new TextureRegion(TEXTURE); // ✅ FULL IMAGE
    }


    // If activated, boost Player
    @Override
    public void onActivate() {
        boostTimer = DURATION;
        startCooldown();
    }


    // Boost
    public void updateEffect(float dt) {
        if (boostTimer > 0f) {
            boostTimer -= dt;
        }
    }


    public boolean isBoostActive() {
        return boostTimer > 0f;
    }

    public float getSpeedBoost() {
        return SPEED_BOOST;
    }

    public TextureRegion getSprite() {
        return sprite;
    }
}



