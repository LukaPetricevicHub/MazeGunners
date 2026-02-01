package de.tum.cit.fop.maze.ui;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.audio.Sound;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;

/**
 * Factory helpers for creating UI buttons with consistent styling.
 */
public final class UiButtons {
    /**
     * Prevents instantiation of utility class.
     */
    private UiButtons() {}

    /**
     * Creates a text button using the auto-scale style.
     *
     * @param text button label text
     * @param skin UI skin to apply
     * @return configured text button
     */
    private static Sound clickSound;

    public static TextButton button(String text, Skin skin) {
        TextButton btn = new AutoScaleTextButton(text, skin);
        btn.addListener(new ClickListener() {
            @Override public void clicked(InputEvent event, float x, float y) {
                playClick();
            }
        });
        return btn;
    }

    private static void playClick() {
        if (clickSound == null) {
            if (Gdx.files.internal("audio/click.mp3").exists()) {
                clickSound = Gdx.audio.newSound(Gdx.files.internal("audio/click.mp3"));
            } else if (Gdx.files.internal("audio/pickup.mp3").exists()) {
                clickSound = Gdx.audio.newSound(Gdx.files.internal("audio/pickup.mp3"));
            } else {
                return;
            }
        }
        float vol = Gdx.app.getPreferences("maze_runner_prefs").getFloat("sound_vol", 0.5f);
        clickSound.play(vol);
    }
}
