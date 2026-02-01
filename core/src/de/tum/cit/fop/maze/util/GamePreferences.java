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
    /** Preference key for music volume. */
    private static final String PREF_MUSIC_VOLUME = "volume";
    /** Preference key for music enabled flag. */
    private static final String PREF_MUSIC_ENABLED = "music.enabled";
    /** Preferences file name. */
    private static final String PREFS_NAME = "maze_runner_prefs";
    /** Preference key for sound effects volume. */
    private static final String PREF_SOUND_VOL = "sound_vol";
    /** Preference key for maximum unlocked level. */
    private static final String PREF_MAX_LEVEL = "max_level";

    /** Preference key for move up. */
    private static final String PREF_KEY_UP = "key_up";
    /** Preference key for move down. */
    private static final String PREF_KEY_DOWN = "key_down";
    /** Preference key for move left. */
    private static final String PREF_KEY_LEFT = "key_left";
    /** Preference key for move right. */
    private static final String PREF_KEY_RIGHT = "key_right";
    /** Preference key for pause. */
    private static final String PREF_KEY_PAUSE = "key_pause";
    /** Preference key for map toggle. */
    private static final String PREF_KEY_MAP = "key_map";
    /** Preference key for melee attack. */
    private static final String PREF_KEY_MELEE = "key_melee";
    /** Preference key for shoot action. */
    private static final String PREF_KEY_SHOOT = "key_shoot";
    /** Preference key for zoom in. */
    private static final String PREF_KEY_ZOOM_IN = "key_zoom_in";
    /** Preference key for zoom out. */
    private static final String PREF_KEY_ZOOM_OUT = "key_zoom_out";
    /** Preference key for dash. */
    private static final String PREF_KEY_DASH = "key_dash";
    /** Preference key for repulse. */
    private static final String PREF_KEY_REPULSE = "key_repulse";

    /**
     * Retrieves the highest unlocked level.
     *
     * @return Maximum unlocked level number
     */
    public int getMaxLevel() {
        return getPrefs().getInteger(PREF_MAX_LEVEL, 1);
    }

    /**
     * Updates the highest unlocked level if the new value is higher.
     *
     * @param level Level number to store
     */
    public void setMaxLevel(int level) {
        if (level > getMaxLevel()) {
            getPrefs().putInteger(PREF_MAX_LEVEL, level);
            getPrefs().flush(); // Save to disk
        }
    }

    /**
     * Retrieves the underlying LibGDX preferences store.
     *
     * @return Preferences instance
     */
    protected Preferences getPrefs() {
        return Gdx.app.getPreferences(PREFS_NAME);
    }

    /**
     * Retrieves the music volume setting.
     *
     * @return Music volume from 0.0 to 1.0
     */
    public float getMusicVolume() {
        return getPrefs().getFloat(PREF_MUSIC_VOLUME, 0.5f);
    }

    /**
     * Updates the music volume setting.
     *
     * @param volume Music volume from 0.0 to 1.0
     */
    public void setMusicVolume(float volume) {
        getPrefs().putFloat(PREF_MUSIC_VOLUME, volume);
        getPrefs().flush();
    }

    /**
     * Checks whether music playback is enabled.
     *
     * @return True if music is enabled
     */
    public boolean isMusicEnabled() {
        return getPrefs().getBoolean(PREF_MUSIC_ENABLED, true);
    }

    /**
     * Updates the music enabled flag.
     *
     * @param musicEnabled True to enable music
     */
    public void setMusicEnabled(boolean musicEnabled) {
        getPrefs().putBoolean(PREF_MUSIC_ENABLED, musicEnabled);
        getPrefs().flush();
    }

    /**
     * Gets and sets the sound effects volume.
     *
     * @return Sound effects volume from 0.0 to 1.0
     */
    public float getSoundVolume() {
        return getPrefs().getFloat(PREF_SOUND_VOL, 0.5f);
    }
    /**
     * Updates the sound effects volume.
     *
     * @param volume Sound effects volume from 0.0 to 1.0
     */
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

    // Movement
    /**
     * Retrieves the key binding for moving up.
     *
     * @return Keycode for move up
     */
    public int getKeyUp() {
        return getPrefs().getInteger(PREF_KEY_UP, com.badlogic.gdx.Input.Keys.UP);
    }

    /**
     * Updates the key binding for moving up.
     *
     * @param keycode Keycode to assign
     */
    public void setKeyUp(int keycode) {
        getPrefs().putInteger(PREF_KEY_UP, keycode);
        getPrefs().flush();
    }

    /**
     * Retrieves the key binding for moving down.
     *
     * @return Keycode for move down
     */
    public int getKeyDown() {
        return getPrefs().getInteger(PREF_KEY_DOWN, com.badlogic.gdx.Input.Keys.DOWN);
    }

    /**
     * Updates the key binding for moving down.
     *
     * @param keycode Keycode to assign
     */
    public void setKeyDown(int keycode) {
        getPrefs().putInteger(PREF_KEY_DOWN, keycode);
        getPrefs().flush();
    }

    /**
     * Retrieves the key binding for moving left.
     *
     * @return Keycode for move left
     */
    public int getKeyLeft() {
        return getPrefs().getInteger(PREF_KEY_LEFT, com.badlogic.gdx.Input.Keys.LEFT);
    }

    /**
     * Updates the key binding for moving left.
     *
     * @param keycode Keycode to assign
     */
    public void setKeyLeft(int keycode) {
        getPrefs().putInteger(PREF_KEY_LEFT, keycode);
        getPrefs().flush();
    }

    /**
     * Retrieves the key binding for moving right.
     *
     * @return Keycode for move right
     */
    public int getKeyRight() {
        return getPrefs().getInteger(PREF_KEY_RIGHT, com.badlogic.gdx.Input.Keys.RIGHT);
    }

    /**
     * Updates the key binding for moving right.
     *
     * @param keycode Keycode to assign
     */
    public void setKeyRight(int keycode) {
        getPrefs().putInteger(PREF_KEY_RIGHT, keycode);
        getPrefs().flush();
    }

    // Gameplay & UI
    /**
     * Retrieves the key binding for pause.
     *
     * @return Keycode for pause
     */
    public int getKeyPause() {
        return getPrefs().getInteger(PREF_KEY_PAUSE, com.badlogic.gdx.Input.Keys.ESCAPE);
    }

    /**
     * Updates the key binding for pause.
     *
     * @param keycode Keycode to assign
     */
    public void setKeyPause(int keycode) {
        getPrefs().putInteger(PREF_KEY_PAUSE, keycode);
        getPrefs().flush();
    }

    /**
     * Retrieves the key binding for the map toggle.
     *
     * @return Keycode for map toggle
     */
    public int getKeyMap() {
        return getPrefs().getInteger(PREF_KEY_MAP, com.badlogic.gdx.Input.Keys.M);
    }

    /**
     * Updates the key binding for the map toggle.
     *
     * @param keycode Keycode to assign
     */
    public void setKeyMap(int keycode) {
        getPrefs().putInteger(PREF_KEY_MAP, keycode);
        getPrefs().flush();
    }

    /**
     * Retrieves the key binding for melee attack.
     *
     * @return Keycode for melee attack
     */
    public int getKeyMelee() {
        return getPrefs().getInteger(PREF_KEY_MELEE, com.badlogic.gdx.Input.Keys.SPACE);
    }

    /**
     * Updates the key binding for melee attack.
     *
     * @param keycode Keycode to assign
     */
    public void setKeyMelee(int keycode) {
        getPrefs().putInteger(PREF_KEY_MELEE, keycode);
        getPrefs().flush();
    }

    /**
     * Retrieves the key binding for shooting.
     *
     * @return Keycode for shooting
     */
    public int getKeyShoot() {
        return getPrefs().getInteger(PREF_KEY_SHOOT, com.badlogic.gdx.Input.Keys.F);
    }

    /**
     * Updates the key binding for shooting.
     *
     * @param keycode Keycode to assign
     */
    public void setKeyShoot(int keycode) {
        getPrefs().putInteger(PREF_KEY_SHOOT, keycode);
        getPrefs().flush();
    }

    // Camera & Skills
    /**
     * Retrieves the key binding for zoom in.
     *
     * @return Keycode for zoom in
     */
    public int getKeyZoomIn() {
        return getPrefs().getInteger(PREF_KEY_ZOOM_IN, com.badlogic.gdx.Input.Keys.E);
    }

    /**
     * Updates the key binding for zoom in.
     *
     * @param keycode Keycode to assign
     */
    public void setKeyZoomIn(int keycode) {
        getPrefs().putInteger(PREF_KEY_ZOOM_IN, keycode);
        getPrefs().flush();
    }

    /**
     * Retrieves the key binding for zoom out.
     *
     * @return Keycode for zoom out
     */
    public int getKeyZoomOut() {
        return getPrefs().getInteger(PREF_KEY_ZOOM_OUT, com.badlogic.gdx.Input.Keys.Q);
    }

    /**
     * Updates the key binding for zoom out.
     *
     * @param keycode Keycode to assign
     */
    public void setKeyZoomOut(int keycode) {
        getPrefs().putInteger(PREF_KEY_ZOOM_OUT, keycode);
        getPrefs().flush();
    }

    /**
     * Retrieves the key binding for dash.
     *
     * @return Keycode for dash
     */
    public int getKeyDash() {
        return getPrefs().getInteger(PREF_KEY_DASH, com.badlogic.gdx.Input.Keys.SHIFT_LEFT);
    }

    /**
     * Updates the key binding for dash.
     *
     * @param keycode Keycode to assign
     */
    public void setKeyDash(int keycode) {
        getPrefs().putInteger(PREF_KEY_DASH, keycode);
        getPrefs().flush();
    }

    /**
     * Retrieves the key binding for repulse.
     *
     * @return Keycode for repulse
     */
    public int getKeyRepulse() {
        return getPrefs().getInteger(PREF_KEY_REPULSE, com.badlogic.gdx.Input.Keys.R);
    }

    /**
     * Updates the key binding for repulse.
     *
     * @param keycode Keycode to assign
     */
    public void setKeyRepulse(int keycode) {
        getPrefs().putInteger(PREF_KEY_REPULSE, keycode);
        getPrefs().flush();
    }
}
