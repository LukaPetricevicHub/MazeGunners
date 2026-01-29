package de.tum.cit.fop.maze.obstacles;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.audio.Sound;
import com.badlogic.gdx.utils.Timer;

public final class ObstacleAudioManager {

    private static Sound floorHazardSound;
    private static Sound spikeTrapSound;
    private static Sound lightningTrapSound;

    private static boolean initialized = false;

    private ObstacleAudioManager() {}

    public static void init() {
        if (initialized) return;

        floorHazardSound = Gdx.audio.newSound(Gdx.files.internal("assets/Traps/FloorHazard.wav"));
        spikeTrapSound = Gdx.audio.newSound(Gdx.files.internal("assets/Traps/SpikeTrap.wav"));
        lightningTrapSound = Gdx.audio.newSound(Gdx.files.internal("assets/Traps/LightningTrap.wav"));

        initialized = true;
    }

    public static void playSpikeTrap() {
        if (spikeTrapSound != null)
            spikeTrapSound.play(1f);
    }

    public static void playLightningTrap() {
        if (lightningTrapSound == null) return;

        long id = lightningTrapSound.play(1f);

        // Stop after 1.5 seconds
        Timer.schedule(new Timer.Task() {
            @Override
            public void run() {
                lightningTrapSound.stop(id);
            }
        }, 1f);
    }

    public static long playFloorHazardLoop(float volume) {
        if (floorHazardSound == null) return -1;
        return floorHazardSound.loop(volume);
    }

    public static void stopFloorHazard(long id) {
        if (floorHazardSound != null && id != -1)
            floorHazardSound.stop(id);
    }

    public static void dispose() {
        if (floorHazardSound != null) floorHazardSound.dispose();
        if (spikeTrapSound != null) spikeTrapSound.dispose();
        if (lightningTrapSound != null) lightningTrapSound.dispose();
    }
}
