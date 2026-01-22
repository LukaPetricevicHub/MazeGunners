package de.tum.cit.fop.maze.ui;

import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.ui.*;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import de.tum.cit.fop.maze.player.Player;

public class SkillWindow extends Window {

    private final Player player;
    private final Label pointsLabel;
    private final Label speedLabel;
    private final Label healthLabel;

    public SkillWindow(Skin skin, Player player, Runnable onBack) {
        super("Skill Tree", skin);
        this.player = player;

        setModal(true); // Blocks input to things behind it
        setMovable(false);
        pad(20);

        // 1. Points Display
        pointsLabel = new Label("Skill Points: " + player.getSkillPoints(), skin);
        pointsLabel.setFontScale(1.2f);
        add(pointsLabel).colspan(3).padBottom(20).row();

        // 2. Speed Row
        add(new Label("Movement Speed", skin)).left();
        speedLabel = new Label(String.format("%.0f", player.getWalkSpeed()), skin);
        add(speedLabel).padLeft(20).padRight(20);

        TextButton buySpeedBtn = new TextButton("+ Speed (1 SP)", skin);
        buySpeedBtn.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                if (player.upgradeSpeed()) {
                    updateLabels(); // Refresh UI
                }
            }
        });
        add(buySpeedBtn).width(150).padBottom(10).row();

        // 3. Health Row
        add(new Label("Max Health", skin)).left();
        healthLabel = new Label(String.valueOf(player.getMaxLives()), skin);
        add(healthLabel).padLeft(20).padRight(20);

        TextButton buyHealthBtn = new TextButton("+ Health (1 SP)", skin);
        buyHealthBtn.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                if (player.upgradeHealth()) {
                    updateLabels(); // Refresh UI
                }
            }
        });
        add(buyHealthBtn).width(150).padBottom(30).row();

        // 4. Back Button
        TextButton backBtn = new TextButton("Close", skin);
        backBtn.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                onBack.run();
            }
        });
        add(backBtn).colspan(3).width(200);

        pack(); // Auto-size window to fit content
    }

    private void updateLabels() {
        pointsLabel.setText("Skill Points: " + player.getSkillPoints());
        speedLabel.setText(String.format("%.0f", player.getWalkSpeed()));
        healthLabel.setText(String.valueOf(player.getMaxLives()));
    }

    // Call this whenever opening the window to ensure data is fresh
    public void refresh() {
        updateLabels();
    }
}