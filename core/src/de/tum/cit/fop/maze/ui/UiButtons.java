package de.tum.cit.fop.maze.ui;

import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;

public final class UiButtons {
    private UiButtons() {}

    public static TextButton button(String text, Skin skin) {
        return new AutoScaleTextButton(text, skin);
    }
}
