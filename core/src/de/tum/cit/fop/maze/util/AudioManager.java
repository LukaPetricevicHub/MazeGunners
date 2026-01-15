package de.tum.cit.fop.maze.util;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.audio.Music;

public class AudioManager {
    private Music backgroundMusic;
    private final GamePreferences prefs;

    public AudioManager() {
        this.prefs = new GamePreferences();
    }

    public void playBackgroundMusic(String filePath) {
        // 1. Stop remove old music
        if (backgroundMusic != null) {
            backgroundMusic.stop();
            backgroundMusic.dispose();
            backgroundMusic = null;
        }

        // 2. Try to load the new file safely
        try {
            // Check if the file actually exists
            if (!Gdx.files.internal(filePath).exists()) {
                System.err.println("ERROR: Music file NOT found at: " + filePath);
                return; // Exit safely, do not crash!
            }

            backgroundMusic = Gdx.audio.newMusic(Gdx.files.internal(filePath));
            backgroundMusic.setLooping(true);
            backgroundMusic.setVolume(prefs.getMusicVolume());

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