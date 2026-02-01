package de.tum.cit.fop.maze.ui;

import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.ui.Window;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import de.tum.cit.fop.maze.ui.UiButtons;

/**
 * Pause menu window displayed while the game is paused.
 *
 * Provides buttons to resume the game, open settings, open the skill tree,
 * or exit back to the main menu.
 */
/**
 * Pause menu overlay shown during gameplay.
 */
public class PauseMenuWindow extends Window {

    /** Callback executed when opening achievements. */
    private final Runnable onAchievements;

    /**
     * Creates a pause menu with callback actions for each button.
     *
     * @param skin UI skin used for styling
     * @param onResume Action executed when resuming the game
     * @param onSettings Action executed when opening settings
     * @param onSkills Action executed when opening the skill tree
     * @param onAchievements Action executed when opening achievements
     * @param onExit Action executed when exiting to the main menu
     */
    public PauseMenuWindow(Skin skin, Runnable onResume, Runnable onSettings, Runnable onSkills, Runnable onAchievements, Runnable onExit) {

        super("Paused", skin);
        this.getTitleLabel().setAlignment(1);
        this.setMovable(false);
        this.setModal(true);
        this.onAchievements = onAchievements;


        
        TextButton resumeBtn = UiButtons.button("Resume", skin);
        resumeBtn.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                onResume.run();
            }
        });
        this.add(resumeBtn).width(200).pad(10).row();

        
        TextButton skillsBtn = UiButtons.button("Skill Tree", skin);
        skillsBtn.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                onSkills.run();
            }
        });
        add(skillsBtn).width(200).padBottom(10).row();

        
        TextButton achievementsBtn = UiButtons.button("Achievements", skin);
        achievementsBtn.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                onAchievements.run();
            }
        });
        add(achievementsBtn).width(200).padBottom(10).row();

        
        TextButton settingsBtn = UiButtons.button("Settings", skin);
        settingsBtn.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                onSettings.run();
            }
        });
        this.add(settingsBtn).width(200).pad(10).row();

        
        TextButton menuBtn = UiButtons.button("Exit to Menu", skin);
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

