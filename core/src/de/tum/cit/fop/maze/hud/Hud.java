package de.tum.cit.fop.maze.hud;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.MathUtils;
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

    // --- CONFIG ---
    private static final int MAX_LIVES = 5;
    private static final float NOTIF_DURATION = 3.0f;

    // --- CORE ---
    private final Stage stage;
    private final Skin skin;

    // --- TEXTURES ---
    private final Texture pixelTexture;
    private final Texture heartTexture;
    private final Texture arrowTexture;

    // --- UI ELEMENTS ---
    private final Image[] heartImages;

    private final Label floorLabel;       // "Floor: 1" or "Round: 1"
    private final Label hintLabel;        // "Hint: Cold/Warm/Hot"
    private final Label keyLabel;         // "Key: YES/NO"

    private final Label scoreLabel;       // "000123"
    private final Label levelLabel;       // "LEVEL 3"
    private final ProgressBar xpBar;      // 0..1
    private final Label xpTextLabel;      // "50 / 100 XP"

    private final Label notificationLabel;

    // Arrow pinned top-right (always inside screen)
    private final Image arrow;
    private float exitX, exitY;
    private boolean hasExitTarget = false;

    // Timer for notifications
    private float messageTimer = 0f;

    // Console
    private final DevConsole devConsole;

    public Hud(Viewport viewport, com.badlogic.gdx.graphics.g2d.SpriteBatch batch, Skin skin) {
        this.stage = new Stage(viewport, batch);
        this.skin = skin;

        // 1x1 pixel texture (for backgrounds/bars)
        Pixmap pm = new Pixmap(1, 1, Pixmap.Format.RGBA8888);
        pm.setColor(Color.WHITE);
        pm.fill();
        pixelTexture = new Texture(pm);
        pm.dispose();

        // Textures
        heartTexture = new Texture("heart.png");
        arrowTexture = new Texture("arrow.png");

        // Root container
        Table root = new Table();
        root.setFillParent(true);
        root.top();
        stage.addActor(root);

        // Top bar background
        Drawable bg = new TextureRegionDrawable(new TextureRegion(pixelTexture))
                .tint(new Color(0f, 0f, 0f, 0.6f));

        Table topBar = new Table();
        topBar.setBackground(bg);
        topBar.pad(8);
        root.add(topBar).growX();

        // ---------------- LEFT: Hearts + Key + Floor + Hint ----------------
        Table left = new Table();
        left.left();

        heartImages = new Image[MAX_LIVES];
        Table heartsRow = new Table();
        for (int i = 0; i < MAX_LIVES; i++) {
            heartImages[i] = new Image(new TextureRegion(heartTexture));
            heartsRow.add(heartImages[i]).size(16, 16).padRight(4);
        }

        keyLabel = new Label("Key: NO", skin);
        floorLabel = new Label("Floor: 1", skin);
        hintLabel = new Label("Hint: ???", skin);
        hintLabel.setColor(Color.SKY);

        left.add(heartsRow).left().row();
        left.add(keyLabel).left().padTop(3).row();
        left.add(floorLabel).left().padTop(3).row();
        left.add(hintLabel).left().padTop(3);

        topBar.add(left).left().padLeft(8);

        // ---------------- CENTER: Level + Score + XP ----------------
        Table center = new Table();
        center.center();

        levelLabel = new Label("LEVEL 1", skin);
        levelLabel.setColor(Color.GOLD);

        scoreLabel = new Label("000000", skin);

        ProgressBar.ProgressBarStyle barStyle = new ProgressBar.ProgressBarStyle();
        barStyle.background = new TextureRegionDrawable(new TextureRegion(pixelTexture)).tint(Color.DARK_GRAY);
        barStyle.knobBefore = new TextureRegionDrawable(new TextureRegion(pixelTexture)).tint(Color.CYAN);

        xpBar = new ProgressBar(0f, 1f, 0.001f, false, barStyle);
        xpBar.setValue(0f);

        xpTextLabel = new Label("0 / 100 XP", skin);
        xpTextLabel.setColor(Color.LIGHT_GRAY);

        center.add(levelLabel).padBottom(2).row();
        center.add(scoreLabel).padBottom(4).row();
        center.add(xpBar).width(260).height(10).row();
        center.add(xpTextLabel).padTop(2);

        topBar.add(center).expandX().center();

        // ---------------- RIGHT: Notification ----------------
        Table right = new Table();
        right.right();

        notificationLabel = new Label("", skin);
        notificationLabel.setVisible(false);
        notificationLabel.setAlignment(Align.right);
        notificationLabel.setColor(Color.YELLOW);

        right.add(notificationLabel).right().padRight(8);

        topBar.add(right).right();

        // ---------------- Arrow pinned inside top-right ----------------
        arrow = new Image(arrowTexture);
        arrow.setSize(80, 80);
        arrow.setOrigin(arrow.getWidth() / 2f, arrow.getHeight() / 2f);
        arrow.setVisible(false);

        Table arrowOverlay = new Table();
        arrowOverlay.setFillParent(true);
        arrowOverlay.top().right().pad(10);
        arrowOverlay.add(arrow).size(80, 80);
        stage.addActor(arrowOverlay);

        // Console
        devConsole = new DevConsole(skin, (MazeRunnerGame) Gdx.app.getApplicationListener());
        stage.addActor(devConsole);
    }

    public void update(Player player) {
        // Hearts
        int lives = player.getLives();
        for (int i = 0; i < heartImages.length; i++) {
            heartImages[i].setVisible(i < lives);
        }

        // Key text
        keyLabel.setText(player.hasKey() ? "Key: YES" : "Key: NO");

        // ----- These require your Player to have these methods -----
        // If your Player doesn't have score/xpNeeded methods, tell me your actual methods and I’ll adapt.
        scoreLabel.setText(String.format("%06d", player.getScore()));
        levelLabel.setText("LEVEL " + player.getPlayerLevel());

        float currentXp = player.getExperience();
        float maxXp = player.getXpNeededForNextLevel(); // must exist in your Player
        float ratio = (maxXp <= 0f) ? 0f : (currentXp / maxXp);
        xpBar.setValue(MathUtils.clamp(ratio, 0f, 1f));
        xpTextLabel.setText((int) currentXp + " / " + (int) maxXp + " XP");

        // Arrow logic
        boolean showArrow = player.hasKey() && hasExitTarget;
        arrow.setVisible(showArrow);

        if (showArrow) {
            float px = player.getX() + 16f;
            float py = player.getY() + 16f;

            float dx = exitX - px;
            float dy = exitY - py;

            float angleDeg = (float) Math.toDegrees(Math.atan2(dy, dx));

            // Your arrow.png points RIGHT by default -> offset 0
            arrow.setRotation(angleDeg);
        }
    }

    public void updateHeatHint(Player player) {
        if (!player.hasKey() || !hasExitTarget) {
            hintLabel.setText("Hint: ???");
            hintLabel.setColor(Color.SKY);
            return;
        }

        float px = player.getX() + 16f;
        float py = player.getY() + 16f;

        float dx = exitX - px;
        float dy = exitY - py;

        float distWorld = (float) Math.sqrt(dx * dx + dy * dy);
        float distTiles = distWorld / 32f;

        String text;
        if (distTiles > 30f) text = "Cold";
        else if (distTiles > 18f) text = "Cool";
        else if (distTiles > 10f) text = "Warm";
        else if (distTiles > 5f) text = "Hot";
        else text = "BOILING";

        float maxTiles = 25f;
        float t = 1f - MathUtils.clamp(distTiles / maxTiles, 0f, 1f); // 0=far(blue) -> 1=close(red)

        float r = t;
        float g = 0f;
        float b = 1f - t;

        hintLabel.setText("Hint: " + text);
        hintLabel.setColor(r, g, b, 1f);
    }

    public void act(float dt) {
        stage.act(dt);

        if (messageTimer > 0f) {
            messageTimer -= dt;
            if (messageTimer <= 0f) {
                notificationLabel.setVisible(false);
            }
        }
    }

    public void draw() {
        stage.draw();
    }

    public void resize(int width, int height, boolean centerCamera) {
        stage.getViewport().update(width, height, centerCamera);
        // Arrow is in an overlay table, so it stays INSIDE automatically.
    }

    public void showNotification(String text) {
        notificationLabel.setText(text);
        notificationLabel.setVisible(true);
        messageTimer = NOTIF_DURATION;
    }

    public void setFloorText(String text) {
        floorLabel.setText(text);
    }

    public void setExitTarget(float x, float y) {
        this.exitX = x;
        this.exitY = y;
        this.hasExitTarget = true;
    }

    public void clearExitTarget() {
        this.hasExitTarget = false;
        arrow.setVisible(false);
    }

    public Stage getStage() {
        return stage;
    }

    public DevConsole getDevConsole() {
        return devConsole;
    }

    public void dispose() {
        stage.dispose();
        pixelTexture.dispose();
        heartTexture.dispose();
        arrowTexture.dispose();
    }
}
