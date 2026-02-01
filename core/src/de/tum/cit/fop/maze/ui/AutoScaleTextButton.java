package de.tum.cit.fop.maze.ui;

import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;

public class AutoScaleTextButton extends TextButton {
    private final float baseScaleX;
    private final float baseScaleY;

    public AutoScaleTextButton(String text, Skin skin) {
        super(text, skin);
        Label label = getLabel();
        label.setWrap(false);
        baseScaleX = label.getFontScaleX();
        baseScaleY = label.getFontScaleY();
    }

    @Override
    public void layout() {
        Label label = getLabel();
        label.setWrap(false);
        label.setFontScale(baseScaleX, baseScaleY);

        float availW = getWidth() - getPadLeft() - getPadRight();
        float availH = getHeight() - getPadTop() - getPadBottom();

        float scale = 1f;
        float prefW = label.getPrefWidth();
        float prefH = label.getPrefHeight();

        if (prefW > 0f && availW > 0f) {
            scale = Math.min(scale, availW / prefW);
        }
        if (prefH > 0f && availH > 0f) {
            scale = Math.min(scale, availH / prefH);
        }

        if (scale > 1f) scale = 1f;
        label.setFontScale(baseScaleX * scale, baseScaleY * scale);

        super.layout();
    }
}
