package de.tum.cit.fop.maze.obstacles;

import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Vector2;

public class SpikeTrap extends Obstacle {

    private static final int SPIKE_DAMAGE = 1;
    private static final float RESET_COOLDOWN = 1.5f;
    private static final float HITBOX_SIZE = 16f;
    private static final float FRAME_TIME = 0.15f;

    private static final Texture SPIKE_TEXTURE = new Texture("spikeTrap.png");

    private final TextureRegion[] frames;

    private int currentFrame = 0;
    private float frameTimer = 0f;
    private boolean activating = false;

    public SpikeTrap(Vector2 spawnPosition) {
        super(spawnPosition, SPIKE_DAMAGE, RESET_COOLDOWN, HITBOX_SIZE, HITBOX_SIZE);

        // 1536 / 4 = 384
        TextureRegion[][] split = TextureRegion.split(
                SPIKE_TEXTURE,
                384,
                1024
        );

        frames = split[0]; // frames 0..3

        if (frames.length != 4) {
            throw new IllegalStateException(
                    "spikeTrap.png must have exactly 4 frames, but has " + frames.length
            );
        }
    }

    /** Called every frame */
    public void update(float delta) {
        updateCooldown(delta);
        frameTimer += delta;

        // === EXTENDING ===
        if (activating) {
            if (frameTimer >= FRAME_TIME) {
                frameTimer = 0f;

                if (currentFrame < 2) {
                    currentFrame++; // 0 → 1 → 2
                } else {
                    activating = false;
                    startCooldown(); // IMPORTANT
                }
            }
            return;
        }

        // === RETRACTING ===
        if (cooldownTimer <= 0f && currentFrame > 0) {
            if (frameTimer >= FRAME_TIME) {
                frameTimer = 0f;
                currentFrame--; // 2 → 1 → 0
            }
        }
    }

    /** Triggered by tryActivate() */
    @Override
    public void onActivate() {
        if (cooldownTimer <= 0f && !activating && currentFrame == 0) {
            activating = true;
            frameTimer = 0f;
        }
    }

    /** Used by GameScreen for rendering */
    public TextureRegion getCurrentFrame() {
        return frames[currentFrame];
    }
}


