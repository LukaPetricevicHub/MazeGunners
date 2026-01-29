package de.tum.cit.fop.maze.obstacles;

import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Vector2;

public class SpikeTrap extends Obstacle {

    private static final int SPIKE_DAMAGE = 1;
    private static final float RESET_COOLDOWN = 8f;
    private static final float HITBOX_SIZE = 32f;
    private static final float FRAME_TIME = 0.025f;

    private static final int FRAME_COLS = 14;
    private static final int FRAME_ROWS = 1;

    private static final int DAMAGE_START_FRAME = 7;
    private static final float VISUAL_Y_OFFSET = 4f;

    private boolean playerOnTrap = false;
    private float cooldownTimer = 0f;
    private boolean damageDealtThisCycle = false;



    private static final Texture SPIKE_TEXTURE =
            new Texture("assets/Traps/SpikeTrap.png");

    private final TextureRegion[] frames;

    private int currentFrame = 0;
    private float frameTimer = 0f;
    private boolean activating = false;

    public SpikeTrap(Vector2 spawnPosition) {
        super(spawnPosition, SPIKE_DAMAGE, RESET_COOLDOWN, HITBOX_SIZE, HITBOX_SIZE);

        int frameWidth  = SPIKE_TEXTURE.getWidth() / FRAME_COLS;
        int frameHeight = SPIKE_TEXTURE.getHeight() / FRAME_ROWS;

        TextureRegion[][] split = TextureRegion.split(SPIKE_TEXTURE, frameWidth, frameHeight);

        frames = split[0]; // 14 frames (1 row)

        if (frames.length != FRAME_COLS) {
            throw new IllegalStateException(
                    "SpikeTrap.png must have exactly " + FRAME_COLS +
                            " frames, but has " + frames.length
            );
        }
        System.out.println("[SpikeTrap] texture size = " + SPIKE_TEXTURE.getWidth() + "x" + SPIKE_TEXTURE.getHeight());
    }

    // Called every frame
    public void update(float delta) {
        frameTimer += delta;

        // cooldown always ticks
        // cooldown ticks ONCE
        if (cooldownTimer > 0f) {
            cooldownTimer -= delta;

            if (cooldownTimer <= 0f) {
                cooldownTimer = 0f;
                currentFrame = 0;
                damageDealtThisCycle = false; // 🔥 RESET DAMAGE FLAG
            }
        }




        // activation condition
        if (playerOnTrap && cooldownTimer <= 0f && !activating && currentFrame == 0) {
            activate();
            cooldownTimer = RESET_COOLDOWN;
        }




        // === EXTENDING ===
        if (activating) {
            if (frameTimer >= FRAME_TIME) {
                frameTimer = 0f;

                if (currentFrame < frames.length - 1) {
                    currentFrame++;
                } else {
                    activating = false;
                    currentFrame = frames.length - 1; // STAY extended until next trigger
                }

            }
            return;
        }


    }



    // Activate SpikeTrap
    private void activate() {
        activating = true;
        frameTimer = 0f;
        currentFrame = 0;

        ObstacleAudioManager.playSpikeTrap(); // 🔊 ADD
    }




    // Used by GameScreen for rendering
    public TextureRegion getCurrentFrame() {
        return frames[currentFrame];
    }

    public boolean shouldDamagePlayerThisFrame() {
        if (!playerOnTrap) return false;

        if (damageDealtThisCycle) return false;

        if (currentFrame >= DAMAGE_START_FRAME) {
            damageDealtThisCycle = true;
            return true;
        }

        return false;
    }


    public void setPlayerOnTrap(boolean onTrap) {
        this.playerOnTrap = onTrap;
    }


    public float getRenderX() {
        return getCollisionBox().x;
    }

    public float getRenderY() {
        return getCollisionBox().y + VISUAL_Y_OFFSET;
    }


}
