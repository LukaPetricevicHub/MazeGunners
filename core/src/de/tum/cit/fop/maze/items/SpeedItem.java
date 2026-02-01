package de.tum.cit.fop.maze.items;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.audio.Sound;
import de.tum.cit.fop.maze.player.Player;

/**
 * A collectable item that temporarily increases the player's movement speed.
 *
 * When picked up, the player receives a speed boost for a short duration.
 */
public class SpeedItem extends Collectable {
    private static Sound powerupSound;
    /**
     * Creates a new speed item at the given position.
     *
     * @param x x-coordinate in the game world
     * @param y y-coordinate in the game world
     */
    public SpeedItem(float x, float y) {
        super(x, y, "speed.png");
        if (powerupSound == null) {
            powerupSound = Gdx.audio.newSound(Gdx.files.internal("powerups.mp3"));
        }
    }

    /**
     * Applies the speed boost effect to the player.
     *
     * @param player the player who collected the item
     */
    @Override
    public void applyEffect(Player player) {
        player.activateSpeedBoost(5.0f, 1.8f);
        if (powerupSound != null) {
            float vol = Gdx.app.getPreferences("maze_runner_prefs").getFloat("sound_vol", 0.5f);
            powerupSound.play(vol);
        }
        collect();
    }

    @Override
    public void dispose() {
        super.dispose();
        if (powerupSound != null) {
            powerupSound.dispose();
            powerupSound = null;
        }
    }
}
