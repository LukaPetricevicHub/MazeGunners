package de.tum.cit.fop.maze.hud;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.viewport.Viewport;
import de.tum.cit.fop.maze.MazeRunnerGame;
import de.tum.cit.fop.maze.player.Player;
import de.tum.cit.fop.maze.ui.DevConsole;

public class Hud {

    private static final int MAX_LIVES = 5;
    private float messageTimer = 0f;
    private final Stage stage;

    private final Image[] heartImages;
    private final Texture heartTexture;

    private final Label keyLabel;

    private final Image arrow;
    private final Texture arrowTexture;

    private float exitX;
    private float exitY;
    private boolean hasExitTarget = false;

    private final Label notificationLabel;

    private DevConsole devConsole;


    public Hud(Viewport viewport, com.badlogic.gdx.graphics.g2d.SpriteBatch batch, Skin skin) {
        stage = new Stage(viewport, batch);

        // Hearts
        heartTexture = new Texture("heart.png");
        heartImages = new Image[MAX_LIVES];

        Table heartsTable = new Table();
        heartsTable.left();

        for (int i = 0; i < MAX_LIVES; i++) {
            heartImages[i] = new Image(new TextureRegion(heartTexture));
            heartsTable.add(heartImages[i]).size(16, 16).padRight(4);
        }

        keyLabel = new Label("Key: NO", skin);

        Table mainContainer = new Table();
        mainContainer.setFillParent(true);
        mainContainer.top().left().pad(10);

        mainContainer.add(heartsTable).left().row();
        mainContainer.add(keyLabel).left().padTop(5);

        stage.addActor(mainContainer);

        notificationLabel = new Label("", skin);
        notificationLabel.setFontScale(1.0f);
        notificationLabel.setColor(Color.YELLOW);
        notificationLabel.setAlignment(Align.right);
        notificationLabel.setVisible(false);

        Table notificationTable = new Table();
        notificationTable.setFillParent(true);
        notificationTable.top().right().pad(20);
        notificationTable.add(notificationLabel);

        stage.addActor(notificationTable);

        arrowTexture = new Texture("arrow.png");
        arrow = new Image(arrowTexture);
        arrow.setSize(32, 32);
        arrow.setOrigin(16, 16);
        arrow.setPosition(
                viewport.getWorldWidth() - 48,
                viewport.getWorldHeight() - 48
        );

        stage.addActor(arrow);

        devConsole = new DevConsole(skin, (MazeRunnerGame) Gdx.app.getApplicationListener());
        stage.addActor(devConsole);
    }

    public void update(Player player) {
        int lives = player.getLives();
        for(int i = 0; i < heartImages.length; i++) {
            heartImages[i].setVisible(i < lives);
        }

        keyLabel.setText(player.hasKey() ? "Key: YES" : "Key: NO");

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

        // Timer Logic
        if (messageTimer > 0) {
            messageTimer -= dt;
            if (messageTimer <= 0) {
                notificationLabel.setVisible(false);
            }
        }
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

    public void showNotification(String text) {
        notificationLabel.setText(text);
        notificationLabel.setVisible(true);
        messageTimer = 5.0f;
    }

    public DevConsole getDevConsole() {
        return devConsole;
    }

    public void dispose() {
        stage.dispose();
        heartTexture.dispose();
        arrowTexture.dispose();
    }
}