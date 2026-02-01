package de.tum.cit.fop.maze.items;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.audio.Sound;
import de.tum.cit.fop.maze.player.Player;

/**
 * A collectable item that gives the player a temporary shield.
 */
public class ShieldItem extends Collectable {
    private static Sound powerupSound;
    /**
     * Creates a shield item at the given position.
     */
    public ShieldItem(float x, float y) {
        super(x, y, "shield.png");
        if (powerupSound == null) {
            powerupSound = Gdx.audio.newSound(Gdx.files.internal("powerups.mp3"));
        }
    }

    /**
     * Activates the shield effect on the player.
     */
    @Override
    public void applyEffect(Player player) {
        player.activateShield(5.0f);
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
