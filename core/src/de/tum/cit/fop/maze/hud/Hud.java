package de.tum.cit.fop.maze.hud;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.*;
import com.badlogic.gdx.scenes.scene2d.utils.Drawable;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.viewport.Viewport;
import de.tum.cit.fop.maze.MazeRunnerGame;
import de.tum.cit.fop.maze.player.Player;
import de.tum.cit.fop.maze.ui.DevConsole;

public class Hud {

    private final Stage stage;
    private final Skin skin;

    // --- CONFIG ---
    private static final int MAX_LIVES = 5;
    private float messageTimer = 0f;

    // --- UI ELEMENTS ---
    private final Texture pixelTexture;

    // Left Section
    private final Image[] heartImages;
    private final Texture heartTexture;
    private final Label floorLabel; // Displays "Map: Level 1"

    // Center Section
    private final Label scoreLabel;
    private final Label levelLabel; // Displays "Player Lvl"
    private final ProgressBar xpBar;
    private final Label xpTextLabel;

    // Right Section
    private final Label keyLabel;
    private final Label notificationLabel;

    // Navigation
    private final Image arrow;
    private final Texture arrowTexture;
    private float exitX, exitY;
    private boolean hasExitTarget = false;

    // Console
    private final DevConsole devConsole;

    public Hud(Viewport viewport, com.badlogic.gdx.graphics.g2d.SpriteBatch batch, Skin skin) {
        this.stage = new Stage(viewport, batch);
        this.skin = skin;

        // 1. Create a 1x1 white pixel for background/bars
        Pixmap pixmap = new Pixmap(1, 1, Pixmap.Format.RGBA8888);
        pixmap.setColor(Color.WHITE);
        pixmap.fill();
        pixelTexture = new Texture(pixmap);
        pixmap.dispose();

        // 2. MAIN TABLE (The Top Bar)
        Table rootTable = new Table();
        rootTable.top();
        rootTable.setFillParent(true);

        // Background: 60% transparent black
        Drawable bgDrawable = new TextureRegionDrawable(new TextureRegion(pixelTexture))
                .tint(new Color(0f, 0f, 0f, 0.6f));

        Table topBar = new Table();
        topBar.setBackground(bgDrawable);

        // --- LEFT SECTION: HEARTS & FLOOR ---
        Table leftTable = new Table();
        heartTexture = new Texture("heart.png");
        heartImages = new Image[MAX_LIVES];

        Table heartsRow = new Table();
        for (int i = 0; i < MAX_LIVES; i++) {
            heartImages[i] = new Image(new TextureRegion(heartTexture));
            heartsRow.add(heartImages[i]).size(24, 24).padRight(4);
        }

        floorLabel = new Label("Map 1", skin);
        floorLabel.setFontScale(0.8f);
        floorLabel.setColor(Color.LIGHT_GRAY);

        leftTable.add(heartsRow).left().row();
        leftTable.add(floorLabel).left().padTop(4);

        topBar.add(leftTable).padLeft(20).left();

        // --- CENTER SECTION: LEVEL / XP / SCORE ---
        Table centerTable = new Table();

        levelLabel = new Label("LVL 1", skin, "title");
        levelLabel.setFontScale(0.7f);
        levelLabel.setColor(Color.GOLD);

        scoreLabel = new Label("000000", skin);
        scoreLabel.setFontScale(1.0f);

        // Custom XP Bar Style
        ProgressBar.ProgressBarStyle barStyle = new ProgressBar.ProgressBarStyle();
        barStyle.background = new TextureRegionDrawable(new TextureRegion(pixelTexture)).tint(Color.DARK_GRAY);
        barStyle.background.setMinHeight(12);
        barStyle.knobBefore = new TextureRegionDrawable(new TextureRegion(pixelTexture)).tint(Color.CYAN);
        barStyle.knobBefore.setMinHeight(12);

        xpBar = new ProgressBar(0f, 1f, 0.01f, false, barStyle);
        xpBar.setValue(0f);

        xpTextLabel = new Label("0 / 100 XP", skin);
        xpTextLabel.setFontScale(0.7f);
        xpTextLabel.setColor(Color.LIGHT_GRAY);

        centerTable.add(levelLabel).row();
        centerTable.add(scoreLabel).padTop(2).row();
        centerTable.add(xpBar).width(300).padTop(5).row();
        centerTable.add(xpTextLabel).padTop(2);

        topBar.add(centerTable).expandX().padTop(10).padBottom(10);

        // --- RIGHT SECTION: KEY & NOTIFICATIONS ---
        Table rightTable = new Table();

        keyLabel = new Label("KEY 🗝", skin, "title");
        keyLabel.setFontScale(0.8f);
        keyLabel.setColor(Color.DARK_GRAY);

        notificationLabel = new Label("", skin);
        notificationLabel.setFontScale(1.0f);
        notificationLabel.setColor(Color.YELLOW);
        notificationLabel.setAlignment(Align.right);
        notificationLabel.setVisible(false);

        rightTable.add(keyLabel).padRight(20).row();
        rightTable.add(notificationLabel).padRight(20).padTop(10);

        topBar.add(rightTable).right();

        // Finish Layout
        rootTable.add(topBar).growX().row();
        stage.addActor(rootTable);

        // 3. NAVIGATION ARROW (Floating)
        arrowTexture = new Texture("arrow.png");
        arrow = new Image(arrowTexture);
        arrow.setSize(48, 48);
        arrow.setOrigin(24, 24);
        arrow.setPosition(
                viewport.getWorldWidth() - 60,
                viewport.getWorldHeight() - 150
        );
        stage.addActor(arrow);

        // 4. DEV CONSOLE
        devConsole = new DevConsole(skin, (MazeRunnerGame) Gdx.app.getApplicationListener());
        stage.addActor(devConsole);
    }

    public void update(Player player) {
        // Hearts
        int lives = player.getLives();
        for (int i = 0; i < heartImages.length; i++) {
            if (i < heartImages.length) {
                heartImages[i].setVisible(i < lives);
            }
        }

        // Stats
        scoreLabel.setText(String.format("%06d", player.getScore()));
        levelLabel.setText("LEVEL " + player.getPlayerLevel());

        // XP
        float currentXp = player.getExperience();
        float maxXp = player.getXpNeededForNextLevel();
        if (maxXp > 0) xpBar.setValue(currentXp / maxXp);
        else xpBar.setValue(0);
        xpTextLabel.setText((int)currentXp + " / " + (int)maxXp + " XP");

        // Key
        if (player.hasKey()) {
            keyLabel.setColor(Color.GOLD);
            keyLabel.setText("KEY FOUND 🗝");
        } else {
            keyLabel.setColor(Color.DARK_GRAY);
            keyLabel.setText("NO KEY 🗝");
        }

        // Arrow
        if (hasExitTarget) {
            float px = player.getX() + 16f;
            float py = player.getY() + 32f;
            float dx = exitX - px;
            float dy = exitY - py;
            float angleDeg = (float) Math.toDegrees(Math.atan2(dy, dx));
            arrow.setRotation(angleDeg);
        }
    }

    public void act(float dt) {
        stage.act(dt);
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
        arrow.setPosition(
                stage.getViewport().getWorldWidth() - 60,
                stage.getViewport().getWorldHeight() - 150
        );
    }

    public void setExitTarget(float x, float y) {
        this.exitX = x;
        this.exitY = y;
        this.hasExitTarget = true;
    }

    public void showNotification(String text) {
        notificationLabel.setText(text);
        notificationLabel.setVisible(true);
        messageTimer = 3.0f;
    }

    public void setFloorText(String text) {
        if (floorLabel != null) floorLabel.setText(text);
    }

    public DevConsole getDevConsole() {
        return devConsole;
    }

    public Stage getStage() {
        return stage;
    }

    public void dispose() {
        stage.dispose();
        heartTexture.dispose();
        arrowTexture.dispose();
        pixelTexture.dispose();
    }
}