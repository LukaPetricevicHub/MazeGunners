package de.tum.cit.fop.maze.ui;

import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.ui.Window;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;

public class PauseMenuWindow extends Window {
    public PauseMenuWindow(Skin skin, Runnable onResume, Runnable onSettings, Runnable onExit) {
        super("Paused", skin);
        this.getTitleLabel().setAlignment(1);
        this.setMovable(false);
        this.setModal(true);

        //RESUME BUTTON
        TextButton resumeBtn = new TextButton("Resume", skin);
        resumeBtn.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                onResume.run();
            }
        });
        this.add(resumeBtn).width(200).pad(10).row();

        //SETTINGS BUTTON
        TextButton settingsBtn = new TextButton("Settings", skin);
        settingsBtn.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                onSettings.run();
            }
        });
        this.add(settingsBtn).width(200).pad(10).row();

        //EXIT BUTTON
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