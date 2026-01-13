package de.tum.cit.fop.maze.obstacles;

import com.badlogic.gdx.math.Vector2;

public class FloorHazard extends Obstacle {

    private static final float SPEED_BOOST = 1.6f;
    private static final float DURATION = 2.5f;

    private float boostTimer = 0f;

    public FloorHazard(Vector2 position) {
        super(position, 0, 2f, 16f, 16f);
    }

    @Override
    public void onActivate() {
        boostTimer = DURATION;
        startCooldown();
    }

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
}

