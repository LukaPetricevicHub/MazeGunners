package de.tum.cit.fop.maze.hud;

import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.utils.viewport.Viewport;
import de.tum.cit.fop.maze.player.Player;

public class Hud {

    private static final int MAX_LIVES = 5;

    private final Stage stage;

    private final Image[] heartImages;
    private final Texture heartTexture;

    private final Label keyLabel;

    private final Image arrow;
    private final Texture arrowTexture;

    private float exitX;
    private float exitY;
    private boolean hasExitTarget = false;

    public Hud(Viewport viewport, com.badlogic.gdx.graphics.g2d.SpriteBatch batch, Skin skin) {
        stage = new Stage(viewport, batch);

        // === Hearts ===
        heartTexture = new Texture("heart.png");
        heartImages = new Image[MAX_LIVES];

        Table heartsTable = new Table();
        heartsTable.left(); // Align content to the left

        for (int i = 0; i < MAX_LIVES; i++) {
            heartImages[i] = new Image(new TextureRegion(heartTexture));
            // Set the cell size to 16x16 and add padding
            heartsTable.add(heartImages[i]).size(16, 16).padRight(4);
        }

        // === Key label ===
        keyLabel = new Label("Key: NO", skin);


        // === Main HUD table ===
        Table mainContainer = new Table();
        mainContainer.setFillParent(true);
        mainContainer.top().left().pad(10); // Positions the whole UI in top-left

        mainContainer.add(heartsTable).left().row(); // Add hearts row
        mainContainer.add(keyLabel).left().padTop(5); // Add key row with slight gap

        stage.addActor(mainContainer);

        // === Exit arrow ===
        arrowTexture = new Texture("arrow.png");
        arrow = new Image(arrowTexture);
        arrow.setSize(32, 32);
        arrow.setOrigin(16, 16);
        arrow.setPosition(
                viewport.getWorldWidth() - 48,
                viewport.getWorldHeight() - 48
        );

        stage.addActor(arrow);
    }

    public void update(Player player) {
        // Update hearts
        int lives = player.getLives();
        for (int i = 0; i < heartImages.length; i++) {
            heartImages[i].setVisible(i < lives);
        }

        // Update key
        keyLabel.setText(player.hasKey() ? "Key: YES" : "Key: NO");

        // Update arrow rotation
        if (hasExitTarget) {
            float px = player.getX() + 16f;
            float py = player.getY() + 32f;

            float dx = exitX - px;
            float dy = exitY - py;

            float angleDeg = (float) Math.toDegrees(Math.atan2(dy, dx));
            arrow.setRotation(angleDeg);
        }
    }

    public Stage getStage() {
        return stage;
    }

    public void act(float dt) {
        stage.act(dt);
    }

    public void draw() {
        stage.draw();
    }

    public void resize(int width, int height, boolean centerCamera) {
        stage.getViewport().update(width, height, centerCamera);
    }

    public void setExitTarget(float x, float y) {
        this.exitX = x;
        this.exitY = y;
        this.hasExitTarget = true;
    }

    public void dispose() {
        stage.dispose();
        heartTexture.dispose();
        arrowTexture.dispose();
    }
}


