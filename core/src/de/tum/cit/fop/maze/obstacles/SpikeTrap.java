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

    private static final Texture SPIKE_TEXTURE = new Texture("assets/Traps/SpikeTrap.png");

    private final TextureRegion[] frames;

    private int currentFrame = 0;
    private float frameTimer = 0f;
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

        frames = split[0]; 

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

        
        if(cooldownTimer > 0f) {
            cooldownTimer -= delta;

            if (cooldownTimer <= 0f) {
                cooldownTimer = 0f;
                currentFrame = 0;
                damageDealtThisCycle = false;
            }
        }

        
        if(playerOnTrap && cooldownTimer <= 0f && !activating && currentFrame == 0) {
            activate();
            cooldownTimer = RESET_COOLDOWN;
        }


        
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

        ObstacleAudioManager.playSpikeTrap(); 
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
     * Returns the current animation frame of the spike trap.
     *
     * @return current spike trap texture frame
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


    public float getRenderX() {
        return getCollisionBox().x;
    }

    public float getRenderY() {
        return getCollisionBox().y + VISUAL_Y_OFFSET;
    }

}
