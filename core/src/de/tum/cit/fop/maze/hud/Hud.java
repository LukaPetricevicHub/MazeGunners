package de.tum.cit.fop.maze.hud;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Group;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.actions.Actions;
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
    private static final int HEARTS_PER_ROW = 5;
    private final Table heartsGrid;
    private static final float TILE_SIZE = 32f;
    private static final float NOTIF_DURATION = 3.0f;

    // --- CORE ---
    private final Stage stage;
    private final Skin skin;

    // --- TEXTURES ---
    private final Texture pixelTexture;
    private final Texture heartTexture;
    private final Texture arrowTexture;

    // --- UI ELEMENTS ---
    private final java.util.ArrayList<Image> heartImages = new java.util.ArrayList<>();

    private final Label floorLabel;
    private final Label hintLabel;
    private final Label keyLabel;

    // ✅ NEW: coins
    private final Label coinsLabel;

    private final Label levelLabel;
    private final ProgressBar xpBar;
    private final Label xpTextLabel;

    private final Label notificationLabel;

    // Arrow pinned top-right
    private final Image arrow;
    private float exitX, exitY;
    private boolean hasExitTarget = false;

    // Boss HUD (overlay group so it never drifts)
    private Group bossGroup;
    private Table bossTable;
    private final Label bossLabel;
    private final ProgressBar bossHpBar;

    // Boss shake
    private float lastBossRatio = -1f;
    private boolean bossShakeEnabled = true;

    // Minimap container pinned bottom-right (optional)
    private final Table minimapContainer;

    // Timer for notifications
    private float messageTimer = 0f;

    // Console
    private final DevConsole devConsole;

    // ✅ FIXED: constructor back to 3 params (no Label passed in)
    public Hud(Viewport viewport, SpriteBatch batch, Skin skin) {
        this.stage = new Stage(viewport, batch);
        this.skin = skin;

        // 1x1 pixel texture (for backgrounds/bars)
        Pixmap pm = new Pixmap(1, 1, Pixmap.Format.RGBA8888);
        pm.setColor(Color.WHITE);
        pm.fill();
        pixelTexture = new Texture(pm);
        pm.dispose();

        heartTexture = new Texture("heart.png");
        arrowTexture = new Texture("arrow.png");

        Drawable panelBg = new TextureRegionDrawable(new TextureRegion(pixelTexture))
                .tint(new Color(0f, 0f, 0f, 0.55f));

        // =========================
        // TOP-LEFT (Hearts + Coins + Key + Floor)
        // =========================
        Table topLeft = new Table();
        topLeft.setBackground(panelBg);
        topLeft.pad(8);
        topLeft.left().top();

        heartsGrid = new Table();
        heartsGrid.left().top();
        topLeft.add(heartsGrid).left().row();


        coinsLabel = new Label("Coins: 0", skin); // ✅ new
        keyLabel = new Label("Key: NO", skin);
        floorLabel = new Label("Floor: 1", skin);

        topLeft.add(heartsGrid).left().row();
        topLeft.add(coinsLabel).left().padTop(4).row(); // ✅ under hearts
        topLeft.add(keyLabel).left().padTop(2).row();
        topLeft.add(floorLabel).left().padTop(2);

        Table topLeftWrap = new Table();
        topLeftWrap.setFillParent(true);
        topLeftWrap.top().left().pad(10);
        topLeftWrap.add(topLeft);
        stage.addActor(topLeftWrap);

        // =========================
        // TOP-CENTER (BOSS HUD) - OVERLAY GROUP (NO LAYOUT DRIFT)
        // =========================
        bossLabel = new Label("BOSS", skin);
        bossLabel.setAlignment(Align.center);
        bossLabel.setColor(Color.WHITE);

        ProgressBar.ProgressBarStyle bossStyle = new ProgressBar.ProgressBarStyle();
        bossStyle.background =
                new TextureRegionDrawable(new TextureRegion(pixelTexture))
                        .tint(new Color(0f, 0f, 0f, 0.70f));
        bossStyle.knobBefore =
                new TextureRegionDrawable(new TextureRegion(pixelTexture))
                        .tint(Color.RED);

        bossHpBar = new ProgressBar(0f, 1f, 0.001f, false, bossStyle);
        bossHpBar.setValue(1f);
        bossHpBar.setAnimateDuration(0.15f);

        bossTable = new Table();
        bossTable.setBackground(
                new TextureRegionDrawable(new TextureRegion(pixelTexture))
                        .tint(new Color(0f, 0f, 0f, 0.60f))
        );
        bossTable.pad(8);
        bossTable.add(bossLabel).padBottom(4).row();
        bossTable.add(bossHpBar).width(700).height(14);
        bossTable.setVisible(false);

        bossGroup = new Group();
        stage.addActor(bossGroup);
        bossGroup.addActor(bossTable);

        layoutBossHud();

        // =========================
        // BOTTOM-LEFT (Level + XP + Hint)
        // =========================
        ProgressBar.ProgressBarStyle xpStyle = new ProgressBar.ProgressBarStyle();
        xpStyle.background = new TextureRegionDrawable(new TextureRegion(pixelTexture)).tint(Color.DARK_GRAY);
        xpStyle.knobBefore = new TextureRegionDrawable(new TextureRegion(pixelTexture)).tint(Color.CYAN);

        xpBar = new ProgressBar(0f, 1f, 0.001f, false, xpStyle);
        xpBar.setValue(0f);

        levelLabel = new Label("LEVEL 1", skin);
        levelLabel.setColor(Color.GOLD);

        xpTextLabel = new Label("0 / 100 XP", skin);
        xpTextLabel.setColor(Color.LIGHT_GRAY);

        hintLabel = new Label("Hint: ???", skin);
        hintLabel.setColor(Color.SKY);

        Table bottomLeft = new Table();
        bottomLeft.setBackground(panelBg);
        bottomLeft.pad(8);
        bottomLeft.left().bottom();

        bottomLeft.add(levelLabel).left().row();
        bottomLeft.add(xpBar).width(260).height(10).left().padTop(4).row();
        bottomLeft.add(xpTextLabel).left().padTop(2).row();
        bottomLeft.add(hintLabel).left().padTop(6);

        Table bottomLeftWrap = new Table();
        bottomLeftWrap.setFillParent(true);
        bottomLeftWrap.bottom().left().pad(10);
        bottomLeftWrap.add(bottomLeft);
        stage.addActor(bottomLeftWrap);

        // =========================
        // TOP-RIGHT (Arrow)
        // =========================
        arrow = new Image(arrowTexture);
        arrow.setSize(80, 80);
        arrow.setOrigin(arrow.getWidth() / 2f, arrow.getHeight() / 2f);
        arrow.setVisible(false);

        Table topRightWrap = new Table();
        topRightWrap.setFillParent(true);
        topRightWrap.top().right().pad(10);
        topRightWrap.add(arrow).size(80, 80);
        stage.addActor(topRightWrap);

        // =========================
        // BOTTOM-RIGHT (Minimap container)
        // =========================
        minimapContainer = new Table();
        minimapContainer.setBackground(panelBg);
        minimapContainer.pad(6);

        Table bottomRightWrap = new Table();
        bottomRightWrap.setFillParent(true);
        bottomRightWrap.bottom().right().pad(10);
        bottomRightWrap.add(minimapContainer);
        stage.addActor(bottomRightWrap);

        // =========================
        // Notification (top-right, under arrow)
        // =========================
        notificationLabel = new Label("", skin);
        notificationLabel.setVisible(false);
        notificationLabel.setAlignment(Align.right);
        notificationLabel.setColor(Color.YELLOW);

        Table notifWrap = new Table();
        notifWrap.setFillParent(true);
        notifWrap.top().right().pad(100, 10, 10, 10);
        notifWrap.add(notificationLabel);
        stage.addActor(notifWrap);

        // Console
        devConsole = new DevConsole(skin, (MazeRunnerGame) Gdx.app.getApplicationListener());
        stage.addActor(devConsole);
    }

    private void layoutBossHud() {
        bossTable.pack();

        float worldW = stage.getViewport().getWorldWidth();
        float worldH = stage.getViewport().getWorldHeight();

        float x = (worldW - bossTable.getWidth()) / 2f;
        float y = worldH - bossTable.getHeight() - 12f;

        bossGroup.setPosition(x, y);
    }

    /**
     * Call this every frame.
     */
    public void update(Player player) {
        // Hearts (map 0..100 lives into 0..5 hearts)
        int currentHearts = (int) Math.ceil(player.getHp() / 2f);
        int maxHearts = player.getMaxHearts();
        rebuildHeartsGrid(currentHearts, maxHearts);


        // ✅ Coins text
        coinsLabel.setText("Coins: " + player.getCoins());

        // Key text
        keyLabel.setText(player.hasKey() ? "Key: YES" : "Key: NO");

        // Level + XP
        levelLabel.setText("LEVEL " + player.getPlayerLevel());

        float currentXp = player.getExperience();
        float maxXp = player.getXpNeededForNextLevel();
        float ratio = (maxXp <= 0f) ? 0f : (currentXp / maxXp);
        xpBar.setValue(MathUtils.clamp(ratio, 0f, 1f));
        xpTextLabel.setText((int) currentXp + " / " + (int) maxXp + " XP");

        // Hint updates continuously
        updateHeatHint(player);

        // Arrow logic
        boolean showArrow = player.hasKey() && hasExitTarget;
        arrow.setVisible(showArrow);

        if (showArrow) {
            float px = player.getX() + 16f;
            float py = player.getY() + 16f;

            float dx = exitX - px;
            float dy = exitY - py;

            float angleDeg = (float) Math.toDegrees(Math.atan2(dy, dx));
            arrow.setRotation(angleDeg);
        }
    }

    /**
     * Hot/Cold hint based on distance to exit.
     */
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
        float distTiles = distWorld / TILE_SIZE;

        String text;
        if (distTiles > 30f) text = "Cold";
        else if (distTiles > 18f) text = "Cool";
        else if (distTiles > 10f) text = "Warm";
        else if (distTiles > 5f) text = "Hot";
        else text = "BOILING";

        float maxTiles = 25f;
        float t = 1f - MathUtils.clamp(distTiles / maxTiles, 0f, 1f);

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
        layoutBossHud();
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

    // Boss control (call from GameScreen)
    public void setBossHp(float current, float max) {
        if (max <= 0f) return;

        float ratio = MathUtils.clamp(current / max, 0f, 1f);
        bossHpBar.setValue(ratio);

        if (bossShakeEnabled && lastBossRatio >= 0f && ratio < lastBossRatio - 0.0001f) {
            triggerBossShake();
        }
        lastBossRatio = ratio;
    }

    public void setBossVisible(boolean visible) {
        bossTable.setVisible(visible);
        if (!visible) {
            lastBossRatio = -1f;
            bossGroup.clearActions();
            layoutBossHud();
            return;
        }
        layoutBossHud();
    }

    private void triggerBossShake() {
        if (bossGroup == null) return;

        final float ox = bossGroup.getX();
        final float oy = bossGroup.getY();

        bossGroup.clearActions();

        float a = 4f;
        float step = 0.025f;

        bossGroup.addAction(Actions.sequence(
                Actions.moveBy(-a, 0f, step),
                Actions.moveBy(a * 2f, 0f, step),
                Actions.moveBy(-a, 0f, step),

                Actions.moveBy(0f, -a, step),
                Actions.moveBy(0f, a * 2f, step),
                Actions.moveBy(0f, -a, step),

                Actions.run(() -> bossGroup.setPosition(ox, oy))
        ));
    }

    private void ensureHeartImages(int needed) {
        while (heartImages.size() < needed) {
            heartImages.add(new Image(new TextureRegion(heartTexture)));
        }
    }

    private void rebuildHeartsGrid(int currentHearts, int maxHearts) {
        heartsGrid.clearChildren();

        ensureHeartImages(maxHearts);

        for (int i = 0; i < maxHearts; i++) {
            Image img = heartImages.get(i);
            img.setVisible(i < currentHearts);

            heartsGrid.add(img).size(16, 16).padRight(4).padBottom(4);

            if ((i + 1) % HEARTS_PER_ROW == 0) heartsGrid.row();
        }
    }


    public void setMinimapActor(Actor minimapActor, float width, float height) {
        minimapContainer.clearChildren();
        minimapContainer.add(minimapActor).size(width, height);
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
