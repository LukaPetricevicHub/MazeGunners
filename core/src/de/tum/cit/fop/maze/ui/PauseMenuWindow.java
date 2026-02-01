package de.tum.cit.fop.maze.ui;

import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.ui.Window;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;

/**
 * Pause menu window displayed while the game is paused.
 *
 * Provides buttons to resume the game, open settings, open the skill tree,
 * or exit back to the main menu.
 */
public class PauseMenuWindow extends Window {

    /**
     * Creates a pause menu with callback actions for each button.
     *
     * @param skin UI skin used for styling
     * @param onResume Action executed when resuming the game
     * @param onSettings Action executed when opening settings
     * @param onSkills Action executed when opening the skill tree
     * @param onExit Action executed when exiting to the main menu
     */
    public PauseMenuWindow(Skin skin, Runnable onResume, Runnable onSettings, Runnable onSkills, Runnable onExit) {
        super("Paused", skin);
        this.getTitleLabel().setAlignment(1);
        this.setMovable(false);
        this.setModal(true);

        // RESUME BUTTON
        TextButton resumeBtn = new TextButton("Resume", skin);
        resumeBtn.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                onResume.run();
            }
        });
        this.add(resumeBtn).width(200).pad(10).row();

        // SKILL TREE BUTTON
        TextButton skillsBtn = new TextButton("Skill Tree", skin);
        skillsBtn.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                onSkills.run();
            }
        });
        add(skillsBtn).width(200).padBottom(10).row();

        // SETTINGS BUTTON
        TextButton settingsBtn = new TextButton("Settings", skin);
        settingsBtn.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                onSettings.run();
            }
        });
        this.add(settingsBtn).width(200).pad(10).row();

        // EXIT BUTTON
        TextButton menuBtn = new TextButton("Exit to Menu", skin);
        menuBtn.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                onExit.run();
            }
        });
        this.add(menuBtn).width(200).pad(10).row();

        this.pack();
    }
}