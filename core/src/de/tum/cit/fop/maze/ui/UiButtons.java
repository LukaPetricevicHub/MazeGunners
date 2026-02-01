package de.tum.cit.fop.maze.ui;

import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;

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
    public static TextButton button(String text, Skin skin) {
        return new AutoScaleTextButton(text, skin);
    }
}
