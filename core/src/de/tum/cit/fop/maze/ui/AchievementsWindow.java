package de.tum.cit.fop.maze.ui;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.ScrollPane;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.ui.Window;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.utils.Align;
import de.tum.cit.fop.maze.util.Achievement;
import de.tum.cit.fop.maze.ui.UiButtons;

import java.util.Collection;
import de.tum.cit.fop.maze.ui.UiButtons;

public class AchievementsWindow extends Window {

    private final Runnable onClose;

    public AchievementsWindow(Skin skin, Collection<Achievement> achievements, Runnable onClose) {
        super("Achievements", skin);
        this.onClose = onClose;
        this.getTitleLabel().setAlignment(Align.center);
        this.setMovable(false);
        this.setResizable(false);
        this.setModal(true);
        this.pad(20f);

        Table listTable = new Table();
        listTable.top();

        for (Achievement a : achievements) {
            Table row = new Table();
            if (skin.has("white", com.badlogic.gdx.scenes.scene2d.utils.Drawable.class)) {
                row.setBackground(skin.newDrawable("white", 0, 0, 0, 0.5f));
            }
            row.pad(10f);

            String titleText = a.getTitle();
            String descText = a.isUnlocked() ? a.getDescription() : "Locked";
            Color titleColor = a.isUnlocked() ? Color.GREEN : Color.GRAY;

            Label titleLabel = new Label(titleText, skin);
            titleLabel.setColor(titleColor);
            titleLabel.setFontScale(1.1f);

            Label descLabel = new Label(descText, skin);
            descLabel.setFontScale(0.8f);
            descLabel.setColor(Color.LIGHT_GRAY);

            row.add(titleLabel).left().expandX().row();
            row.add(descLabel).left().expandX().padTop(5f);

            listTable.add(row).growX().padBottom(10f).row();
        }

        ScrollPane scrollPane = new ScrollPane(listTable, skin);
        this.add(scrollPane).width(600).height(400).row();

        TextButton closeBtn = UiButtons.button("Close", skin);
        closeBtn.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                remove();
                if (onClose != null) onClose.run();
            }
        });
        this.add(closeBtn).width(200).padTop(10f);

        this.pack();
    }
}

