package de.tum.cit.fop.maze.ui;

import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.InputListener;
import com.badlogic.gdx.scenes.scene2d.ui.*;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.utils.Align;
import de.tum.cit.fop.maze.player.Player;
import de.tum.cit.fop.maze.ui.UiButtons;

/**
 * UI window displaying the player's skill tree.
 *
 * Allows the player to spend skill points to unlock abilities
 * and upgrades. Skills are grouped into categories and include
 * hover descriptions and dependency checks.
 */
/**
 * In-game window for spending skill points and showing upgrade descriptions.
 */
public class SkillWindow extends Window {
    private final Player player;
    private final Skin skin;
    private final Runnable onClose;
    private Label pointsLabel;

    private Label descriptionLabel;

    /**
     * Creates a new skill tree window.
     *
     * @param skin UI skin
     * @param player Player whose skills are modified
     * @param onClose Callback executed when the window is closed
     */
    public SkillWindow(Skin skin, Player player, Runnable onClose) {
        super("SKILL TREE", skin);
        this.skin = skin;
        this.player = player;
        this.onClose = onClose;

        this.setModal(true);
        this.setMovable(false);
        this.setResizable(false);
        this.getTitleLabel().setAlignment(Align.center);

        this.pad(30);

        refresh();
    }

    /**
     * Rebuilds the skill tree UI based on the player's current state.
     */
    public void refresh() {
        this.clearChildren();

        
        Table headerTable = new Table();
        pointsLabel = new Label("Skill Points Available: " + player.getSkillPoints(), skin);
        pointsLabel.setFontScale(1.1f);
        headerTable.add(pointsLabel).padBottom(15);
        this.add(headerTable).colspan(3).row();

        
        Table content = new Table();
        content.defaults().pad(10).top();

        
        Table colGunner = new Table();
        colGunner.add(new Label("GUNNER", skin)).padBottom(10).row();

        colGunner.add(createSkillButton("Trigger Finger", "Fire rate +40%",
                player.hasRapidFire(), true,
                () -> player.unlockRapidFire())).width(160).padBottom(5).row();

        colGunner.add(createSkillButton("Shotgun", "Shoot 3 bullets spread",
                player.hasShotgun(), player.hasRapidFire(),
                () -> player.unlockShotgun())).width(160).padBottom(5).row();

        colGunner.add(createSkillButton("Piercing", "Bullets pass through enemies",
                player.hasPiercing(), player.hasShotgun(),
                () -> player.unlockPiercing())).width(160).row();

        content.add(colGunner);
        content.add(new Image(skin.newDrawable("white", 1, 1, 1, 0.3f))).width(2).fillY().pad(0, 10, 0, 10);


        
        Table colSpeed = new Table();
        colSpeed.add(new Label("SPEEDSTER", skin)).padBottom(10).row();

        colSpeed.add(createStatButton("Lightweight", "Permanent Speed +5%",
                player.getSkillSpeedLevel(), 5,
                () -> player.upgradeSpeed())).width(160).padBottom(5).row();

        colSpeed.add(createSkillButton("Dash", "Press SHIFT to Dash",
                player.hasDash(), player.getSkillSpeedLevel() > 0,
                () -> player.unlockDash())).width(160).padBottom(5).row();

        colSpeed.add(createSkillButton("Ghost Walker", "Immune to Spikes/Traps",
                player.hasGhost(), player.hasDash(),
                () -> player.unlockGhost())).width(160).row();

        content.add(colSpeed);
        content.add(new Image(skin.newDrawable("white", 1, 1, 1, 0.3f))).width(2).fillY().pad(0, 10, 0, 10);


        
        Table colTank = new Table();
        colTank.add(new Label("TANK", skin)).padBottom(10).row();

        colTank.add(createStatButton("Iron Will", "Max Health +5 HP",
                player.getSkillHealthLevel(), 5,
                () -> player.upgradeHealth())).width(160).padBottom(5).row();

        colTank.add(createSkillButton("Thorns", "Enemies take damage hitting you",
                player.hasThorns(), player.getSkillHealthLevel() > 0,
                () -> player.unlockThorns())).width(160).padBottom(5).row();

        colTank.add(createSkillButton("Repulse", "Press Q to push enemies back",
                player.hasRepulse(), player.hasThorns(),
                () -> player.unlockRepulse())).width(160).row();

        content.add(colTank);
        this.add(content).row();

        
        
        descriptionLabel = new Label("Hover over a skill to see details...", skin);
        descriptionLabel.setAlignment(Align.center);
        descriptionLabel.setWrap(true);
        descriptionLabel.setColor(1f, 1f, 0.7f, 1f); 

        
        this.add(new Image(skin.newDrawable("white", 1, 1, 1, 0.3f))).height(2).fillX().padTop(20).padBottom(10).row();
        this.add(descriptionLabel).width(500).height(50).padBottom(10).row();

        
        TextButton closeBtn = UiButtons.button("Close", skin);
        closeBtn.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                if(onClose != null) onClose.run();
            }
        });
        this.add(closeBtn).width(200);

        this.pack();
    }

    /**
     * Refreshes the window when it becomes visible.
     */
    @Override
    public void setVisible(boolean visible) {
        super.setVisible(visible);
        if (visible) {
            refresh();
        }
    }

    private TextButton createSkillButton(String name, String desc, boolean isUnlocked, boolean isParentUnlocked, Runnable unlockAction) {
        
        String text = name;
        if (isUnlocked) text += "\n[UNLOCKED]";
        else if (!isParentUnlocked) text += "\n[LOCKED]";
        else text += "\nCost: 1 Pt";

        TextButton btn = UiButtons.button(text, skin);
        btn.getLabel().setFontScale(0.85f);
        btn.getLabel().setAlignment(Align.center);

        
        if (isUnlocked) {
            btn.setDisabled(true);
            btn.setColor(0.5f, 1f, 0.5f, 1f);
        } else if (!isParentUnlocked) {
            btn.setDisabled(true);
            btn.setColor(0.3f, 0.3f, 0.3f, 1f);
        } else if (player.getSkillPoints() < 1) {
            btn.setDisabled(true);
            btn.setColor(0.6f, 0.6f, 0.6f, 1f);
        }

        
        if (!btn.isDisabled()) {
            btn.addListener(new ChangeListener() {
                @Override
                public void changed(ChangeEvent event, Actor actor) {
                    if (player.spendSkillPoint()) {
                        unlockAction.run();
                        refresh();
                    }
                }
            });
        }

        
        btn.addListener(new InputListener() {
            @Override
            public void enter(InputEvent event, float x, float y, int pointer, Actor fromActor) {
                
                descriptionLabel.setText(name + ": " + desc);
            }

            @Override
            public void exit(InputEvent event, float x, float y, int pointer, Actor toActor) {
                
                descriptionLabel.setText("Hover over a skill to see details...");
            }
        });

        return btn;
    }

    private TextButton createStatButton(String name, String desc, int currentLevel, int maxLevel, Runnable upgradeAction) {
        
        String text = name + " " + currentLevel + "/" + maxLevel;
        if (currentLevel < maxLevel) text += "\nCost: 1 Pt";
        else text += "\n[MAXED]";

        TextButton btn = UiButtons.button(text, skin);
        btn.getLabel().setFontScale(0.85f);
        btn.getLabel().setAlignment(Align.center);

        if (currentLevel >= maxLevel) {
            btn.setDisabled(true);
            btn.setColor(1f, 0.8f, 0.2f, 1f);
        } else if (player.getSkillPoints() < 1) {
            btn.setDisabled(true);
            btn.setColor(0.6f, 0.6f, 0.6f, 1f);
        }

        
        if (!btn.isDisabled()) {
            btn.addListener(new ChangeListener() {
                @Override
                public void changed(ChangeEvent event, Actor actor) {
                    upgradeAction.run();
                    refresh();
                }
            });
        }

        
        btn.addListener(new InputListener() {
            @Override
            public void enter(InputEvent event, float x, float y, int pointer, Actor fromActor) {
                descriptionLabel.setText(name + ": " + desc);
            }

            @Override
            public void exit(InputEvent event, float x, float y, int pointer, Actor toActor) {
                descriptionLabel.setText("Hover over a skill to see details...");
            }
        });

        return btn;
    }
}

