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

        // 1. Stop remove old music
        if (backgroundMusic != null) {
            backgroundMusic.stop();
            backgroundMusic.dispose();
            backgroundMusic = null;
        }

        // 2. Try to load the new file safely
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

    public void resumeMusic() {
        if (backgroundMusic != null && prefs.isMusicEnabled()) {
            backgroundMusic.play();
        }
    }

    /**
     * Updates the volume of the currently playing music.
     */
    public void updateMusicVolume(float volume) {
        if (backgroundMusic != null) {
            backgroundMusic.setVolume(volume);
        }
    }

    public void dispose() {
        if (backgroundMusic != null) {
            backgroundMusic.dispose();
        }
    }
}
