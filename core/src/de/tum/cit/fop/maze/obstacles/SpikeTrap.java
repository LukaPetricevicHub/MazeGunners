package de.tum.cit.fop.maze.obstacles;

import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Vector2;

/**
 * A floor-mounted spike trap obstacle.
 *
 * Activates when the player steps onto it, plays an animation,
 * deals damage once per activation cycle, and then enters a cooldown
 * before it can trigger again.
 */
public class SpikeTrap extends Obstacle {

    /** Damage dealt by spikes. */
    private static final int SPIKE_DAMAGE = 1;
    /** Cooldown time before the trap can reset. */
    private static final float RESET_COOLDOWN = 8f;
    /** Collision size for the trap. */
    private static final float HITBOX_SIZE = 32f;
    /** Duration of each frame. */
    private static final float FRAME_TIME = 0.025f;

    /** Number of columns in the sprite sheet. */
    private static final int FRAME_COLS = 14;
    /** Number of rows in the sprite sheet. */
    private static final int FRAME_ROWS = 1;

    /** First frame index where damage should be applied. */
    private static final int DAMAGE_START_FRAME = 7;
    /** Visual Y offset for rendering. */
    private static final float VISUAL_Y_OFFSET = 4f;

    /** Whether the player is currently on the trap. */
    private boolean playerOnTrap = false;
    /** Remaining cooldown time. */
    private float cooldownTimer = 0f;
    /** Whether damage has been dealt this cycle. */
    private boolean damageDealtThisCycle = false;

    /** Sprite sheet for the spike trap. */
    private static final Texture SPIKE_TEXTURE = new Texture("assets/Traps/SpikeTrap.png");

    /** Animation frames for the trap. */
    private final TextureRegion[] frames;

    /** Current animation frame index. */
    private int currentFrame = 0;
    /** Accumulated time for frame stepping. */
    private float frameTimer = 0f;
    /** Whether the trap is currently extending. */
    private boolean activating = false;

    /**
     * Creates a new spike trap at the given world position.
     *
     * @param spawnPosition world position where the trap is placed
     */
    public SpikeTrap(Vector2 spawnPosition) {
        super(spawnPosition, SPIKE_DAMAGE, RESET_COOLDOWN, HITBOX_SIZE, HITBOX_SIZE);

        int frameWidth = SPIKE_TEXTURE.getWidth() / FRAME_COLS;
        int frameHeight = SPIKE_TEXTURE.getHeight() / FRAME_ROWS;

        TextureRegion[][] split = TextureRegion.split(SPIKE_TEXTURE, frameWidth, frameHeight);

        frames = split[0]; // 14 frames

        if(frames.length != FRAME_COLS) {
            throw new IllegalStateException("SpikeTrap.png must have exactly " + FRAME_COLS + " frames, but has " + frames.length);
        }

        System.out.println("[SpikeTrap] texture size = " + SPIKE_TEXTURE.getWidth() + "x" + SPIKE_TEXTURE.getHeight());
    }


    /**
     * Updates the spike trap state.
     *
     * Advances animation frames, handles activation timing,
     * and manages cooldown and reset behavior.
     *
     * @param delta time elapsed since the last frame
     */
    public void update(float delta) {

        frameTimer += delta;

        // cooldown ticks
        if(cooldownTimer > 0f) {
            cooldownTimer -= delta;

            if (cooldownTimer <= 0f) {
                cooldownTimer = 0f;
                currentFrame = 0;
                damageDealtThisCycle = false;
            }
        }

        // activation condition
        if(playerOnTrap && cooldownTimer <= 0f && !activating && currentFrame == 0) {
            activate();
            cooldownTimer = RESET_COOLDOWN;
        }


        // extending
        if (activating) {

            if(frameTimer >= FRAME_TIME) {
                frameTimer = 0f;

                if(currentFrame < frames.length - 1) {
                    currentFrame++;
                }
                else {
                    activating = false;
                    currentFrame = frames.length - 1;
                }
            }

            return;
        }

    }


    /**
     * Activates the spike trap.
     *
     * Resets animation state and plays the spike activation sound.
     */
    private void activate() {
        activating = true;
        frameTimer = 0f;
        currentFrame = 0;

        ObstacleAudioManager.playSpikeTrap(); // sound
    }


    /**
     * Returns the current animation frame of the spike trap.
     *
     * @return current spike trap texture frame
     */
    public TextureRegion getCurrentFrame() {
        return frames[currentFrame];
    }


    /**
     * Checks whether the player should take damage this frame.
     *
     * @return True if damage should be applied now
     */
    public boolean shouldDamagePlayerThisFrame() {

        if (!playerOnTrap) return false;
        if (damageDealtThisCycle) return false;

        if(currentFrame >= DAMAGE_START_FRAME) {
            damageDealtThisCycle = true;
            return true;
        }

        return false;
    }


    /**
     * Updates whether the player is currently standing on the trap.
     *
     * @param onTrap true if the player is on the trap
     */
    public void setPlayerOnTrap(boolean onTrap) {
        this.playerOnTrap = onTrap;
    }


    /**
     * Returns the render x-position for the trap sprite.
     *
     * @return Render x-position
     */
    public float getRenderX() {
        return getCollisionBox().x;
    }

    /**
     * Returns the render y-position for the trap sprite.
     *
     * @return Render y-position
     */
    public float getRenderY() {
        return getCollisionBox().y + VISUAL_Y_OFFSET;
    }

}
