package de.tum.cit.fop.maze.items;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.audio.Sound;
import de.tum.cit.fop.maze.player.Player;

public class AmmoItem extends Collectable {
    private static final int AMMO_GAIN = 20;
    private static Sound pickupSound;

    public AmmoItem(float x, float y) {
        super(x, y, "bullet.png");
        if (pickupSound == null) {
            pickupSound = Gdx.audio.newSound(Gdx.files.internal("coinpickup.mp3"));
        }
    }

    @Override
    public void applyEffect(Player player) {
        if (player != null) {
            player.addAmmo(AMMO_GAIN);
        }
        if (pickupSound != null) {
            float vol = Gdx.app.getPreferences("maze_runner_prefs").getFloat("sound_vol", 0.5f);
            pickupSound.play(vol);
        }
        collect();
    }

    @Override
    public void dispose() {
        super.dispose();
        if (pickupSound != null) {
            pickupSound.dispose();
            pickupSound = null;
        }
    }
}
