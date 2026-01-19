package de.tum.cit.fop.maze.util;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Preferences;

public class GamePreferences {
    private static final String PREF_MUSIC_VOLUME = "volume";
    private static final String PREF_MUSIC_ENABLED = "music.enabled";
    private static final String PREFS_NAME = "maze_runner_prefs";
    private static final String PREF_SOUND_VOL = "sound_vol";
    private static final String PREF_MAX_LEVEL = "max_level";

    private static final String PREF_KEY_UP = "key_up";
    private static final String PREF_KEY_DOWN = "key_down";
    private static final String PREF_KEY_LEFT = "key_left";
    private static final String PREF_KEY_RIGHT = "key_right";
    private static final String PREF_KEY_PAUSE = "key_pause";

    public int getMaxLevel() {
        return getPrefs().getInteger(PREF_MAX_LEVEL, 1);
    }

    public void setMaxLevel(int level) {
        if (level > getMaxLevel()) {
            getPrefs().putInteger(PREF_MAX_LEVEL, level);
            getPrefs().flush(); // Save to disk
        }
    }

    protected Preferences getPrefs() {
        return Gdx.app.getPreferences(PREFS_NAME);
    }

    public float getMusicVolume() {
        return getPrefs().getFloat(PREF_MUSIC_VOLUME, 0.5f);
    }

    public void setMusicVolume(float volume) {
        getPrefs().putFloat(PREF_MUSIC_VOLUME, volume);
        getPrefs().flush();
    }

    public boolean isMusicEnabled() {
        return getPrefs().getBoolean(PREF_MUSIC_ENABLED, true);
    }

    public void setMusicEnabled(boolean musicEnabled) {
        getPrefs().putBoolean(PREF_MUSIC_ENABLED, musicEnabled);
        getPrefs().flush();
    }

    public float getSoundVolume() {
        return getPrefs().getFloat(PREF_SOUND_VOL, 0.5f);
    }
    public void setSoundVolume(float volume) {
        getPrefs().putFloat(PREF_SOUND_VOL, volume);
        getPrefs().flush();
    }

    public int getKeyUp() {
        return getPrefs().getInteger(PREF_KEY_UP, com.badlogic.gdx.Input.Keys.UP);
    }

    public void setKeyUp(int keycode) {
        getPrefs().putInteger(PREF_KEY_UP, keycode);
        getPrefs().flush();
    }

    public int getKeyDown() {
        return getPrefs().getInteger(PREF_KEY_DOWN, com.badlogic.gdx.Input.Keys.DOWN);
    }

    public void setKeyDown(int keycode) {
        getPrefs().putInteger(PREF_KEY_DOWN, keycode);
        getPrefs().flush();
    }

    public int getKeyLeft() {
        return getPrefs().getInteger(PREF_KEY_LEFT, com.badlogic.gdx.Input.Keys.LEFT);
    }

    public void setKeyLeft(int keycode) {
        getPrefs().putInteger(PREF_KEY_LEFT, keycode);
        getPrefs().flush();
    }

    public int getKeyRight() {
        return getPrefs().getInteger(PREF_KEY_RIGHT, com.badlogic.gdx.Input.Keys.RIGHT);
    }

    public void setKeyRight(int keycode) {
        getPrefs().putInteger(PREF_KEY_RIGHT, keycode);
        getPrefs().flush();
    }
}