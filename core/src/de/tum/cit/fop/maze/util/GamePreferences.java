package de.tum.cit.fop.maze.util;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Preferences;

/**
 * Manages persistent game settings and key bindings.
 *
 * This class wraps LibGDX preferences and provides typed
 * access to audio settings, unlocked progress, and controls.
 */
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
    private static final String PREF_KEY_MAP = "key_map";
    private static final String PREF_KEY_MELEE = "key_melee";
    private static final String PREF_KEY_SHOOT = "key_shoot";
    private static final String PREF_KEY_ZOOM_IN = "key_zoom_in";
    private static final String PREF_KEY_ZOOM_OUT = "key_zoom_out";
    private static final String PREF_KEY_DASH = "key_dash";
    private static final String PREF_KEY_REPULSE = "key_repulse";

    public int getMaxLevel() {
        return getPrefs().getInteger(PREF_MAX_LEVEL, 1);
    }

    /**
     * Updates the highest unlocked level if the new value is higher.
     */
    public void setMaxLevel(int level) {
        if (level > getMaxLevel()) {
            getPrefs().putInteger(PREF_MAX_LEVEL, level);
            getPrefs().flush(); 
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

    /**
     * Gets and sets the sound effects volume.
     */
    public float getSoundVolume() {
        return getPrefs().getFloat(PREF_SOUND_VOL, 0.5f);
    }
    public void setSoundVolume(float volume) {
        getPrefs().putFloat(PREF_SOUND_VOL, volume);
        getPrefs().flush();
    }

    /**
     * Gets and sets all configurable key bindings.
     *
     * These methods control player movement, combat actions,
     * camera controls, and special abilities.
     */

    
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

    
    public int getKeyPause() {
        return getPrefs().getInteger(PREF_KEY_PAUSE, com.badlogic.gdx.Input.Keys.ESCAPE);
    }

    public void setKeyPause(int keycode) {
        getPrefs().putInteger(PREF_KEY_PAUSE, keycode);
        getPrefs().flush();
    }

    public int getKeyMap() {
        return getPrefs().getInteger(PREF_KEY_MAP, com.badlogic.gdx.Input.Keys.M);
    }

    public void setKeyMap(int keycode) {
        getPrefs().putInteger(PREF_KEY_MAP, keycode);
        getPrefs().flush();
    }

    public int getKeyMelee() {
        return getPrefs().getInteger(PREF_KEY_MELEE, com.badlogic.gdx.Input.Keys.SPACE);
    }

    public void setKeyMelee(int keycode) {
        getPrefs().putInteger(PREF_KEY_MELEE, keycode);
        getPrefs().flush();
    }

    public int getKeyShoot() {
        return getPrefs().getInteger(PREF_KEY_SHOOT, com.badlogic.gdx.Input.Keys.F);
    }

    public void setKeyShoot(int keycode) {
        getPrefs().putInteger(PREF_KEY_SHOOT, keycode);
        getPrefs().flush();
    }

    
    public int getKeyZoomIn() {
        return getPrefs().getInteger(PREF_KEY_ZOOM_IN, com.badlogic.gdx.Input.Keys.E);
    }

    public void setKeyZoomIn(int keycode) {
        getPrefs().putInteger(PREF_KEY_ZOOM_IN, keycode);
        getPrefs().flush();
    }

    public int getKeyZoomOut() {
        return getPrefs().getInteger(PREF_KEY_ZOOM_OUT, com.badlogic.gdx.Input.Keys.Q);
    }

    public void setKeyZoomOut(int keycode) {
        getPrefs().putInteger(PREF_KEY_ZOOM_OUT, keycode);
        getPrefs().flush();
    }

    public int getKeyDash() {
        return getPrefs().getInteger(PREF_KEY_DASH, com.badlogic.gdx.Input.Keys.SHIFT_LEFT);
    }

    public void setKeyDash(int keycode) {
        getPrefs().putInteger(PREF_KEY_DASH, keycode);
        getPrefs().flush();
    }

    public int getKeyRepulse() {
        return getPrefs().getInteger(PREF_KEY_REPULSE, com.badlogic.gdx.Input.Keys.R);
    }

    public void setKeyRepulse(int keycode) {
        getPrefs().putInteger(PREF_KEY_REPULSE, keycode);
        getPrefs().flush();
    }
}
