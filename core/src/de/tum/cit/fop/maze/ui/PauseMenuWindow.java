package de.tum.cit.fop.maze.ui;

import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.ui.Window;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;

public class PauseMenuWindow extends Window {

    // ADD 'Runnable onSkills' HERE vvv
    public PauseMenuWindow(Skin skin, Runnable onResume, Runnable onSettings, Runnable onSkills, Runnable onAchievements, Runnable onExit) {
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
                // Now this works because we added it to the constructor
                onSkills.run();
            }
        });
        add(skillsBtn).width(200).padBottom(10).row();

        // ACHIEVEMENTS BUTTON
        TextButton achievementsBtn = new TextButton("Achievements", skin);
        achievementsBtn.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                onAchievements.run();
            }
        });
        add(achievementsBtn).width(200).padBottom(10).row();

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
