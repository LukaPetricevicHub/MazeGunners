package de.tum.cit.fop.maze.items;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.audio.Sound;
import de.tum.cit.fop.maze.player.Player;

/**
 * A collectable item that gives the player a key.
 */
public class KeyItem extends Collectable {
    /** Shared pickup sound for key items. */
    private static Sound pickupSound;

    /**
     * Creates a key item at the given position.
     *
     * @param x World x-coordinate
     * @param y World y-coordinate
     */
    public KeyItem(float x, float y) {
        super(x, y, "key.png");
        if (pickupSound == null) {
            pickupSound = Gdx.audio.newSound(Gdx.files.internal("key.mp3"));
        }
    }

    /**
     * Gives the key to the player.
     *
     * @param player Player who collected the key
     */
    @Override
    public void applyEffect(Player player) {
        player.giveKey();
        if (pickupSound != null) {
            float vol = Gdx.app.getPreferences("maze_runner_prefs").getFloat("sound_vol", 0.5f);
            pickupSound.play(vol);
        }
        collect();
    }

    /**
     * Disposes shared resources and clears the static sound reference.
     */
    @Override
    public void dispose() {
        super.dispose();
        if (pickupSound != null) {
            pickupSound.dispose();
            pickupSound = null;
        }
    }
}
