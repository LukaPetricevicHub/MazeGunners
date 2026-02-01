package de.tum.cit.fop.maze.items;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.audio.Sound;
import de.tum.cit.fop.maze.player.Player;

/**
 * A collectable gun item.
 *
 * When picked up, the player receives a gun and bonus score.
 */
public class GunCollectable extends Collectable {
    /** Shared pickup sound for gun items. */
    private static Sound pickupSound;

    /**
     * Creates a gun collectable at the given world position.
     *
     * @param x x-position in world coordinates
     * @param y y-position in world coordinates
     */
    public GunCollectable(float x, float y) {
        super(x, y, "gun.png");
        if (pickupSound == null) {
            pickupSound = Gdx.audio.newSound(Gdx.files.internal("audio/pickup.mp3"));
        }
    }

    /**
     * Marks the gun as collected.
     */
    @Override
    public void collect() {
        super.collect();
        System.out.println("GUN COLLECTED!");
    }

    /**
     * Gives the player a gun and adds score.
     *
     * @param player the player collecting the gun
     */
    @Override
    public void applyEffect(Player player) {
        player.equipGun();
        player.addScore(200);
        if (pickupSound != null) {
            float vol = Gdx.app.getPreferences("maze_runner_prefs").getFloat("sound_vol", 0.5f);
            pickupSound.play(vol);
        }
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
