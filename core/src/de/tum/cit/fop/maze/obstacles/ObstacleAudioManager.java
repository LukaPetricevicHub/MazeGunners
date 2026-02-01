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
            spikeTrapSound.play(1f * getSfxVolume());
    }

    public static void playLightningTrap() {
        if (lightningTrapSound == null) return;

        long id = lightningTrapSound.play(1f * getSfxVolume());

        Timer.schedule(new Timer.Task() {
            @Override
            public void run() {
                lightningTrapSound.stop(id);
            }
        }, 1f);
    }

    public static long playFloorHazardLoop(float volume) {
        return -1;
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

    private static float getSfxVolume() {
        if (Gdx.app == null) return 1f;
        if (Gdx.app.getApplicationListener() instanceof de.tum.cit.fop.maze.MazeRunnerGame game) {
            return game.getPreferences().getSoundVolume();
        }
        return 1f;
    }
}
