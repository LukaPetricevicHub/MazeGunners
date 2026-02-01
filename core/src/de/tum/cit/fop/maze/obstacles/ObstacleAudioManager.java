package de.tum.cit.fop.maze.obstacles;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.audio.Sound;
import com.badlogic.gdx.utils.Timer;

/**
 * Centralized audio manager for obstacle and trap sound effects.
 *
 * Handles loading, playing, looping, and disposing of obstacle-related sounds
 * such as floor hazards, spike traps, and lightning traps.
 *
 * This class is static-only and must be initialized via {@link #init()}.
 */
public final class ObstacleAudioManager {

    private static Sound spikeTrapSound;
    private static Sound lightningTrapSound;

    private static boolean initialized = false;

    /**
     * Prevents instantiation of this static utility class.
     */
    private ObstacleAudioManager() {}

    /**
     * Initializes all obstacle-related sound effects.
     *
     * This method is safe to call multiple times and will only
     * load sounds once.
     */
    public static void init() {
        if (initialized) return;

        spikeTrapSound = Gdx.audio.newSound(Gdx.files.internal("assets/Traps/SpikeTrap.wav"));
        lightningTrapSound = Gdx.audio.newSound(Gdx.files.internal("assets/Traps/LightningTrap.wav"));

        initialized = true;
    }

    /**
     * Plays the spike trap sound effect once.
     */
    public static void playSpikeTrap() {
        if (spikeTrapSound != null) {
            spikeTrapSound.play(getSfxVolume());
        }
    }

    /**
     * Plays the lightning trap sound effect once and stops it automatically
     * after a short duration.
     */
    public static void playLightningTrap() {
        if (lightningTrapSound == null) return;

        long id = lightningTrapSound.play(getSfxVolume());

        Timer.schedule(new Timer.Task() {
            @Override
            public void run() {
                lightningTrapSound.stop(id);
            }
        }, 1f);
    }

    /**
     * Starts looping the floor hazard sound effect.
     *
     * @param volume playback volume (0.0 to 1.0)
     * @return sound instance ID, or -1 if playback failed
     */
    /**
     * Disposes all loaded obstacle sound effects.
     *
     * Should be called when the game shuts down to free audio resources.
     */
    public static void dispose() {
        if (spikeTrapSound != null) spikeTrapSound.dispose();
        if (lightningTrapSound != null) lightningTrapSound.dispose();
    }

    private static float getSfxVolume() {
        if (Gdx.app == null) return 1f;
        if (Gdx.app.getApplicationListener() instanceof de.tum.cit.fop.maze.MazeRunnerGame game) {
            return game.getPreferences().getSoundVolume();
        }
        return 1f;
    }
}
