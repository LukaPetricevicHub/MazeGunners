package de.tum.cit.fop.maze.hud;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Rectangle;
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
    private static final float TILE_SIZE = 32f;
    private static final float NOTIF_DURATION = 3.0f;

    // --- CORE ---
    private final Stage stage;
    private final Skin skin;

    // --- TEXTURES ---
    private final Texture pixelTexture;
    private final Texture keyIconTexture;
    private final Texture arrowTexture;
    private final Texture hpBgTexture;
    private final Texture hpFillTexture;
    private final Texture bossBgTexture;
    private final Texture bossFillTexture;

    // --- UI ELEMENTS ---
    private final Label floorLabel;
    private final Label hintLabel;
    private final Label objectiveLabel;
    private final Image keyIcon;
    private final Label endlessLabel;

    // ✅ NEW: coins
    private final Label coinsLabel;
    private final Label damageLabel;
    private final Label statsLabel;
    private final Label multLabel;

    private final Label levelLabel;
    private final ProgressBar xpBar;
    private final Label xpTextLabel;
    private final ProgressBar hpBar;
    private final Label hpTextLabel;

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

    // Boss sha
    private float lastBossRatio = -1f;
    private boolean bossShakeEnabled = true;

    // Minimap container pinned bottom-right (optional)
    private final Table minimapContainer;

    // Timer for notifications
    private float messageTimer = 0f;
    private float hintMaxTiles = 50f;
    private Rectangle exitRoomWorldRect = null;

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

        hpBgTexture = createSolidTexture(new Color(0.2f, 0f, 0f, 0.8f));
        hpFillTexture = createSolidTexture(Color.RED);
        bossBgTexture = createSolidTexture(new Color(0f, 0f, 0f, 0.70f));
        bossFillTexture = createSolidTexture(Color.RED);

        arrowTexture = new Texture("arrow.png");
        keyIconTexture = new Texture("key.png");

        Drawable panelBg = new TextureRegionDrawable(new TextureRegion(pixelTexture))
                .tint(new Color(0f, 0f, 0f, 0.55f));

        // =========================
        // TOP-LEFT (Hearts + Coins + Key + Floor)
        // =========================
        Table topLeft = new Table();
        topLeft.pad(6);
        topLeft.left().top();

        coinsLabel = new Label("Coins: 0", skin); // ✅ new
        damageLabel = new Label("DMG: bullet 0 | melee 0", skin);
        statsLabel = new Label("Stats: STR 5 | SPD 5", skin);
        multLabel = new Label("Mult: DMG x1.00 | SPD x1.00", skin);
        floorLabel = new Label("Floor: 1", skin);
        endlessLabel = new Label("", skin);
        endlessLabel.setVisible(false);
        keyIcon = new Image(keyIconTexture);
        keyIcon.setVisible(false);
        keyIcon.setSize(16, 16);

        ProgressBar.ProgressBarStyle hpStyle = new ProgressBar.ProgressBarStyle();
        TextureRegionDrawable hpBg = new TextureRegionDrawable(new TextureRegion(hpBgTexture));
        TextureRegionDrawable hpFill = new TextureRegionDrawable(new TextureRegion(hpFillTexture));
        hpBg.setMinHeight(20);
        hpFill.setMinHeight(20);
        hpStyle.background = hpBg;
        hpStyle.knobBefore = hpFill;

        hpBar = new ProgressBar(0f, 1f, 0.001f, false, hpStyle);
        hpBar.setValue(1f);

        hpTextLabel = new Label("HP: 0 / 0", skin);
        hpTextLabel.setColor(Color.SALMON);

        Table hpRow = new Table();
        hpRow.left().top();
        hpRow.add(hpBar).width(140).height(20).left();
        hpRow.add(keyIcon).size(16, 16).left().padLeft(6);
        topLeft.add(hpRow).left().row();
        topLeft.add(hpTextLabel).left().padTop(2).row();

        topLeft.add(coinsLabel).left().padTop(4).row();
        topLeft.add(floorLabel).left().padTop(2);
        topLeft.add(endlessLabel).left().padTop(2).row();

        Table topLeftWrap = new Table();
        topLeftWrap.setFillParent(true);
        topLeftWrap.top().left().pad(4);
        topLeftWrap.add(topLeft);
        stage.addActor(topLeftWrap);

        // =========================
        // TOP-CENTER (BOSS HUD) - OVERLAY GROUP (NO LAYOUT DRIFT)
        // =========================
        bossLabel = new Label("SWORD", skin);
        bossLabel.setAlignment(Align.center);
        bossLabel.setColor(Color.WHITE);

        ProgressBar.ProgressBarStyle bossStyle = new ProgressBar.ProgressBarStyle();
        TextureRegionDrawable bossBg =
                new TextureRegionDrawable(new TextureRegion(bossBgTexture));
        TextureRegionDrawable bossFill =
                new TextureRegionDrawable(new TextureRegion(bossFillTexture));
        bossBg.setMinHeight(30);
        bossFill.setMinHeight(30);
        bossStyle.background = bossBg;
        bossStyle.knobBefore = bossFill;

        bossHpBar = new ProgressBar(0f, 1f, 0.001f, false, bossStyle);
        bossHpBar.setValue(1f);
        bossHpBar.setAnimateDuration(0.15f);

        bossTable = new Table();
        bossTable.pad(8);
        bossTable.add(bossLabel).padBottom(4).row();
        bossTable.add(bossHpBar).width(700).height(30);
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
        objectiveLabel = new Label("Objective: Find the key", skin);
        objectiveLabel.setColor(Color.LIGHT_GRAY);

        Table bottomLeft = new Table();
        bottomLeft.pad(8);
        bottomLeft.left().bottom();

        bottomLeft.add(levelLabel).left().row();
        bottomLeft.add(xpBar).width(260).height(10).left().padTop(4).row();
        bottomLeft.add(xpTextLabel).left().padTop(2).row();
        bottomLeft.add(damageLabel).left().padTop(6).row();
        bottomLeft.add(statsLabel).left().padTop(2).row();
        bottomLeft.add(multLabel).left().padTop(2);

        Table bottomLeftWrap = new Table();
        bottomLeftWrap.setFillParent(true);
        bottomLeftWrap.bottom().left().pad(6);
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
        topRightWrap.top().right().pad(6);
        topRightWrap.add(objectiveLabel).right().padBottom(4).row();
        topRightWrap.add(hintLabel).right().padBottom(6).row();
        topRightWrap.add(arrow).size(80, 80).right();
        stage.addActor(topRightWrap);

        // =========================
        // BOTTOM-RIGHT (Minimap container)
        // =========================
        minimapContainer = new Table();
        minimapContainer.pad(6);

        Table bottomRightWrap = new Table();
        bottomRightWrap.setFillParent(true);
        bottomRightWrap.bottom().right().pad(6);
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
        notifWrap.top().right().pad(90, 6, 6, 6);
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
        int hp = player.getHp();
        int maxHp = player.getMaxHp();
        float hpRatio = (maxHp <= 0) ? 0f : (hp / (float) maxHp);
        hpBar.setValue(MathUtils.clamp(hpRatio, 0f, 1f));
        hpTextLabel.setText("HP: " + hp + " / " + maxHp);


        // ✅ Coins text
        coinsLabel.setText("Coins: " + player.getCoins());

        // Damage + stats + gambling multipliers
        int strength = player.getStrengthStat();
        int strengthBonus = Math.max(0, strength - 5);
        float dmgMult = player.getDamageMult();

        int bulletDamage = Math.max(1, Math.round((5f + strengthBonus) * dmgMult));
        int meleeDamage = Math.max(1, Math.round((1f + strengthBonus * 0.6f) * dmgMult));

        damageLabel.setText("DMG: bullet " + bulletDamage + " | melee " + meleeDamage);
        statsLabel.setText("Stats: STR " + player.getStrengthStat() + " | SPD " + player.getSpeedStat());
        multLabel.setText(String.format("Mult: DMG x%.2f | SPD x%.2f",
                player.getDamageMult(), player.getBonusSpeedMult()));

        keyIcon.setVisible(player.hasKey());

        // Level + XP
        levelLabel.setText("LEVEL " + player.getPlayerLevel());

        float currentXp = player.getExperience();
        float maxXp = player.getXpNeededForNextLevel();
        float ratio = (maxXp <= 0f) ? 0f : (currentXp / maxXp);
        xpBar.setValue(MathUtils.clamp(ratio, 0f, 1f));
        xpTextLabel.setText((int) currentXp + " / " + (int) maxXp + " XP");

        objectiveLabel.setText(player.hasKey() ? "Objective: Find the exit" : "Objective: Find the key");

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

        if (exitRoomWorldRect != null && exitRoomWorldRect.contains(px, py)) {
            hintLabel.setText("Hint: BOILING");
            hintLabel.setColor(1f, 0f, 0f, 1f);
            return;
        }

        float dx = exitX - px;
        float dy = exitY - py;

        float distWorld = (float) Math.sqrt(dx * dx + dy * dy);
        float distTiles = distWorld / TILE_SIZE;

        float maxTiles = Math.max(20f, hintMaxTiles);
        String text;
        if (distTiles > maxTiles * 0.80f) text = "Cold";
        else if (distTiles > maxTiles * 0.55f) text = "Cool";
        else if (distTiles > maxTiles * 0.30f) text = "Warm";
        else if (distTiles > maxTiles * 0.15f) text = "Hot";
        else text = "BOILING";

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

    public void setEndlessInfo(String text) {
        if (text == null || text.isEmpty()) {
            endlessLabel.setVisible(false);
            return;
        }
        endlessLabel.setText(text);
        endlessLabel.setVisible(true);
    }

    public void setExitTarget(float x, float y) {
        this.exitX = x;
        this.exitY = y;
        this.hasExitTarget = true;
    }

    public void setHintMaxTiles(float tiles) {
        hintMaxTiles = Math.max(20f, tiles);
    }

    public void setExitRoomWorldRect(Rectangle rect) {
        this.exitRoomWorldRect = rect;
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
        arrowTexture.dispose();
        keyIconTexture.dispose();
        hpBgTexture.dispose();
        hpFillTexture.dispose();
        bossBgTexture.dispose();
        bossFillTexture.dispose();
    }

    private Texture createSolidTexture(Color color) {
        Pixmap pm = new Pixmap(1, 1, Pixmap.Format.RGBA8888);
        pm.setColor(color);
        pm.fill();
        Texture tex = new Texture(pm);
        pm.dispose();
        return tex;
    }
}
