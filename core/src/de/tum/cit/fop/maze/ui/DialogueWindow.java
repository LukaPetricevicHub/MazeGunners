package de.tum.cit.fop.maze.ui;

import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.InputListener;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Window;
import com.badlogic.gdx.utils.Align;

/**
 * Simple dialogue box for narrative text and confirmations.
 */
public class DialogueWindow extends Window {

    private final String fullText;
    private final Label textLabel;
    private final Runnable onFinish;

    private float charsPerSecond = 45f;
    private float charProgress = 0f;
    private int visibleChars = 0;
    private boolean finished = false;
    private boolean closed = false;

    /**
     * Creates a dialogue window with a speaker title and text content.
     *
     * @param skin UI skin used for styling
     * @param speaker name shown in the title bar
     * @param text dialogue text to display
     * @param onFinish callback invoked when the window closes
     */
    public DialogueWindow(Skin skin, String speaker, String text, Runnable onFinish) {
        super(speaker == null ? "" : speaker, skin);
        this.fullText = text == null ? "" : text;
        this.onFinish = onFinish;

        this.getTitleLabel().setAlignment(Align.center);
        this.setModal(true);
        this.setMovable(false);
        this.setResizable(false);
        this.pad(30f);

        textLabel = new Label("", skin);
        textLabel.setWrap(true);
        textLabel.setAlignment(Align.left | Align.top);
        this.add(textLabel).expand().fill();

        setSize(700f, 240f);

        addListener(new InputListener() {
            @Override
            public boolean touchDown(InputEvent event, float x, float y, int pointer, int button) {
                handleClick();
                return true;
            }
        });
    }

    /**
     * Advances the typewriter animation.
     *
     * @param delta time step in seconds
     */
    @Override
    public void act(float delta) {
        super.act(delta);
        if (finished) return;

        charProgress += delta * charsPerSecond;
        int newVisible = Math.min(fullText.length(), (int) charProgress);
        if (newVisible != visibleChars) {
            visibleChars = newVisible;
            textLabel.setText(fullText.substring(0, visibleChars));
        }
        if (visibleChars >= fullText.length()) {
            finished = true;
        }
    }

    /**
     * Centers the window on its current stage and sizes it relative to the viewport.
     */
    public void centerOnStage() {
        Stage stage = getStage();
        if (stage == null) return;

        float stageW = stage.getViewport().getWorldWidth();
        float stageH = stage.getViewport().getWorldHeight();

        float targetW = stageW * 0.7f;
        float targetH = stageH * 0.3f;

        setSize(targetW, targetH);
        if (getCell(textLabel) != null) {
            getCell(textLabel).width(targetW - getPadLeft() - getPadRight());
        }
        textLabel.setWidth(targetW - getPadLeft() - getPadRight());
        layout();

        setPosition((stageW - getWidth()) / 2f, (stageH - getHeight()) / 2f);
    }

    /**
     * Handles click behavior to reveal or close the dialogue.
     */
    private void handleClick() {
        if (closed) return;

        if (!finished) {
            finished = true;
            visibleChars = fullText.length();
            textLabel.setText(fullText);
            return;
        }

        closed = true;
        remove();
        if (onFinish != null) {
            onFinish.run();
        }
    }
}
