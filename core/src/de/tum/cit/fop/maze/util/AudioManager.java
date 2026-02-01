package de.tum.cit.fop.maze.util;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.audio.Music;

/**
 * Manages background music playback for the game.
 *
 * This class handles loading, playing, pausing, and stopping
 * background music while respecting user audio preferences.
 */
public class AudioManager {
    private Music backgroundMusic;
    private final GamePreferences prefs;
    private String currentTrack;

    /**
     * Creates a new audio manager with default preferences.
     */
    public AudioManager() {
        this.prefs = new GamePreferences();
    }

    /**
     * Starts playing background music from the given file.
     * Any previously playing music is stopped and disposed.
     *
     * @param filePath Path to the music file
     */
    public void playBackgroundMusic(String filePath) {
        if (backgroundMusic != null && filePath != null && filePath.equals(currentTrack)) {
            backgroundMusic.setLooping(true);
            backgroundMusic.setVolume(prefs.getMusicVolume());
            if (prefs.isMusicEnabled() && !backgroundMusic.isPlaying()) {
                backgroundMusic.play();
            }
            return;
        }

        
        if (backgroundMusic != null) {
            backgroundMusic.stop();
            backgroundMusic.dispose();
            backgroundMusic = null;
        }

        
        try {
            if (!Gdx.files.internal(filePath).exists()) {
                System.err.println("ERROR: Music file NOT found at: " + filePath);
                return;
            }

            backgroundMusic = Gdx.audio.newMusic(Gdx.files.internal(filePath));
            backgroundMusic.setLooping(true);
            backgroundMusic.setVolume(prefs.getMusicVolume());
            currentTrack = filePath;

            if (prefs.isMusicEnabled()) {
                backgroundMusic.play();
            }
        } catch (Exception e) {
            System.err.println("FAILED to load music: " + e.getMessage());
        }
    }

    /**
     * Stops the currently playing background music, if any.
     */
    public void stopMusic() {
        if (backgroundMusic != null) {
            backgroundMusic.stop();
        }
    }

    /**
     * Pauses the background music if it is playing.
     */
    public void pauseMusic() {
        if (backgroundMusic != null && backgroundMusic.isPlaying()) {
            backgroundMusic.pause();
        }
    }

    /**
     * Resumes background music if enabled in preferences.
     */
    public void resumeMusic() {
        if (backgroundMusic != null && prefs.isMusicEnabled()) {
            backgroundMusic.play();
        }
    }

    /**
     * Updates the volume of the currently playing music.
     *
     * @param volume new volume in the range [0, 1]
     */
    public void updateMusicVolume(float volume) {
        if (backgroundMusic != null) {
            backgroundMusic.setVolume(volume);
        }
    }

    /**
     * Disposes of music resources held by the manager.
     */
    public void dispose() {
        if (backgroundMusic != null) {
            backgroundMusic.dispose();
        }
    }
}
