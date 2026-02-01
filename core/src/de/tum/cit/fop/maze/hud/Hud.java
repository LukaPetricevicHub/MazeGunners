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

/**
 * Heads-up display that renders player stats, objectives, and notifications.
 */
public class Hud {

    // --- CONFIG ---
    /** Size of a tile in world units. */
    private static final float TILE_SIZE = 32f;
    /** Duration for top-right notifications. */
    private static final float NOTIF_DURATION = 3.0f;

    // --- CORE ---
    /** Stage hosting the HUD UI. */
    private final Stage stage;
    /** Skin used for HUD widgets. */
    private final Skin skin;

    // --- TEXTURES ---
    /** 1x1 pixel texture for tintable UI backgrounds. */
    private final Texture pixelTexture;
    /** Key icon texture. */
    private final Texture keyIconTexture;
    /** Arrow texture used for exit guidance. */
    private final Texture arrowTexture;
    /** Bullet icon texture. */
    private final Texture bulletIconTexture;
    /** Background texture for HP bar. */
    private final Texture hpBgTexture;
    /** Fill texture for HP bar. */
    private final Texture hpFillTexture;
    /** Background texture for boss HP bar. */
    private final Texture bossBgTexture;
    /** Fill texture for boss HP bar. */
    private final Texture bossFillTexture;

    // --- UI ELEMENTS ---
    /** Label showing current floor. */
    private final Label floorLabel;
    /** Label showing the hint text. */
    private final Label hintLabel;
    /** Label showing current objective. */
    private final Label objectiveLabel;
    /** Key icon shown when player has a key. */
    private final Image keyIcon;
    /** Label for endless mode floor info. */
    private final Label endlessLabel;

    // ✅ NEW: coins
    /** Label showing coin count. */
    private final Label coinsLabel;
    /** Label showing damage values. */
    private final Label damageLabel;
    /** Label showing current stats. */
    private final Label statsLabel;
    /** Label showing current ammo counts. */
    private final Label ammoLabel;
    /** Label showing multiplier values. */
    private final Label multLabel;
    /** Bullet icon image. */
    private final Image bulletIcon;

    /** Label showing player level. */
    private final Label levelLabel;
    /** XP progress bar. */
    private final ProgressBar xpBar;
    /** XP text label. */
    private final Label xpTextLabel;
    /** HP progress bar. */
    private final ProgressBar hpBar;
    /** HP text label. */
    private final Label hpTextLabel;

    /** Notification label anchored near top-right. */
    private final Label notificationLabel;
    /** Center screen notification label. */
    private final Label centerNotificationLabel;

    // Arrow pinned top-right
    /** Arrow image pointing to exit. */
    private final Image arrow;
    /** Exit target x-coordinate in world units. */
    private float exitX, exitY;
    /** Whether a valid exit target is available. */
    private boolean hasExitTarget = false;

    // Boss HUD (overlay group so it never drifts)
    /** Group containing the boss HUD elements. */
    private Group bossGroup;
    /** Table containing boss HUD content. */
    private Table bossTable;
    /** Boss name label. */
    private final Label bossLabel;
    /** Boss HP progress bar. */
    private final ProgressBar bossHpBar;

    // Boss sha
    /** Last observed boss HP ratio for shake detection. */
    private float lastBossRatio = -1f;
    /** Whether boss HUD shake is enabled on damage. */
    private boolean bossShakeEnabled = true;

    // Minimap container pinned bottom-right (optional)
    /** Container for the minimap actor. */
    private final Table minimapContainer;

    // Timer for notifications
    /** Timer for top-right notifications. */
    private float messageTimer = 0f;
    /** Max hint distance in tiles. */
    private float hintMaxTiles = 50f;
    /** Exit room bounds in world coordinates. */
    private Rectangle exitRoomWorldRect = null;

    // Console
    /** Developer console overlay. */
    private final DevConsole devConsole;
    /** Current achievement popup window. */
    private Window achievementPopup;

    // ✅ FIXED: constructor back to 3 params (no Label passed in)
    /**
     * Creates a HUD for the given viewport and batch.
     *
     * @param viewport Viewport for the HUD stage
     * @param batch Sprite batch used by the stage
     * @param skin UI skin used for widgets
     */
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
        bulletIconTexture = new Texture("bullet.png");

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
        ammoLabel = new Label("Ammo: 0/0", skin);
        multLabel = new Label("Mult: DMG x1.00 | SPD x1.00", skin);
        floorLabel = new Label("Floor: 1", skin);
        endlessLabel = new Label("", skin);
        endlessLabel.setVisible(false);
        keyIcon = new Image(keyIconTexture);
        keyIcon.setVisible(false);
        keyIcon.setSize(16, 16);
        bulletIcon = new Image(bulletIconTexture);
        bulletIcon.setSize(20, 20);

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
        Table ammoRow = new Table();
        ammoRow.add(bulletIcon).size(20, 20).left().padRight(6);
        ammoRow.add(ammoLabel).left();
        bottomLeft.add(ammoRow).left().padTop(2).row();

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

        // =========================
        // Center Notification (round popups)
        // =========================
        centerNotificationLabel = new Label("", skin);
        centerNotificationLabel.setVisible(false);
        centerNotificationLabel.setAlignment(Align.center);
        centerNotificationLabel.setColor(Color.WHITE);

        Table centerWrap = new Table();
        centerWrap.setFillParent(true);
        centerWrap.center();
        centerWrap.add(centerNotificationLabel).width(800);
        stage.addActor(centerWrap);


        // Console
        devConsole = new DevConsole(skin, (MazeRunnerGame) Gdx.app.getApplicationListener());
        stage.addActor(devConsole);
    }

    /**
     * Positions the boss HUD at the top center of the viewport.
     */
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
     *
     * @param player Player instance used to update HUD values
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

        int totalSpeed = Math.max(1, Math.round(player.getSpeedStat() * player.getBonusSpeedMult()));
        damageLabel.setText("Damage: Bullet " + bulletDamage + " | Melee " + meleeDamage);
        statsLabel.setText("Speed: " + totalSpeed);
        if (player.hasGun()) {
            ammoLabel.setText("Ammo: " + player.getAmmoInClip() + "/" + player.getAmmoReserve());
        } else {
            ammoLabel.setText("Ammo: -");
        }
        multLabel.setVisible(false);

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
     *
     * @param player Player instance used for position
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

    /**
     * Advances the HUD stage and timers.
     *
     * @param dt Delta time in seconds
     */
    public void act(float dt) {
        stage.act(dt);

        if (messageTimer > 0f) {
            messageTimer -= dt;
            if (messageTimer <= 0f) {
                notificationLabel.setVisible(false);
            }
        }
    }

    /**
     * Draws the HUD stage.
     */
    public void draw() {
        stage.draw();
    }

    /**
     * Updates the HUD viewport and repositions elements.
     *
     * @param width New window width
     * @param height New window height
     * @param centerCamera Whether to center the camera
     */
    public void resize(int width, int height, boolean centerCamera) {
        stage.getViewport().update(width, height, centerCamera);
        layoutBossHud();
    }

    /**
     * Shows a temporary notification near the top-right.
     *
     * @param text Notification text
     */
    public void showNotification(String text) {
        notificationLabel.setText(text);
        notificationLabel.setVisible(true);
        messageTimer = NOTIF_DURATION;
    }

    /**
     * Shows a centered notification that fades out after a duration.
     *
     * @param text Message to display
     * @param seconds Duration to display
     */
    public void showCenterNotification(String text, float seconds) {
        float duration = Math.max(0.5f, seconds);
        float fade = Math.min(0.6f, duration * 0.35f);

        centerNotificationLabel.clearActions();
        centerNotificationLabel.setText(text);
        centerNotificationLabel.setVisible(true);
        centerNotificationLabel.getColor().a = 1f;

        centerNotificationLabel.addAction(Actions.sequence(
                Actions.delay(Math.max(0f, duration - fade)),
                Actions.fadeOut(fade),
                Actions.run(() -> centerNotificationLabel.setVisible(false))
        ));
    }

    /**
     * Shows a small achievement popup that slides in and out.
     *
     * @param title Achievement title
     * @param description Achievement description
     */
    public void showAchievementPopup(String title, String description) {
        if (achievementPopup != null) {
            achievementPopup.remove();
        }

        achievementPopup = new Window("ACHIEVEMENT", skin);
        achievementPopup.setMovable(false);
        achievementPopup.setResizable(false);
        achievementPopup.pad(10f);

        Label titleLabel = new Label(title, skin);
        titleLabel.setAlignment(Align.left);
        Label descLabel = new Label(description, skin);
        descLabel.setAlignment(Align.left);

        achievementPopup.add(titleLabel).left().row();
        achievementPopup.add(descLabel).left().padTop(4f);
        achievementPopup.pack();

        float width = Math.min(achievementPopup.getWidth(), stage.getViewport().getWorldWidth() * 0.6f);
        achievementPopup.setWidth(width);
        achievementPopup.setPosition(
                (stage.getViewport().getWorldWidth() - achievementPopup.getWidth()) / 2f,
                stage.getViewport().getWorldHeight()
        );

        float targetY = stage.getViewport().getWorldHeight() - achievementPopup.getHeight() - 10f;
        achievementPopup.addAction(com.badlogic.gdx.scenes.scene2d.actions.Actions.sequence(
                com.badlogic.gdx.scenes.scene2d.actions.Actions.moveTo(achievementPopup.getX(), targetY, 0.25f),
                com.badlogic.gdx.scenes.scene2d.actions.Actions.delay(3f),
                com.badlogic.gdx.scenes.scene2d.actions.Actions.moveTo(achievementPopup.getX(), stage.getViewport().getWorldHeight(), 0.25f),
                com.badlogic.gdx.scenes.scene2d.actions.Actions.run(() -> achievementPopup.remove())
        ));

        stage.addActor(achievementPopup);
    }

    /**
     * Updates the floor label text.
     *
     * @param text Text to display
     */
    public void setFloorText(String text) {
        floorLabel.setText(text);
    }

    /**
     * Updates the endless mode info label.
     *
     * @param text Text to display, or empty to hide
     */
    public void setEndlessInfo(String text) {
        if (text == null || text.isEmpty()) {
            endlessLabel.setVisible(false);
            return;
        }
        endlessLabel.setText(text);
        endlessLabel.setVisible(true);
    }

    /**
     * Sets the exit target location for hinting and arrow guidance.
     *
     * @param x Exit x-coordinate
     * @param y Exit y-coordinate
     */
    public void setExitTarget(float x, float y) {
        this.exitX = x;
        this.exitY = y;
        this.hasExitTarget = true;
    }

    /**
     * Sets the maximum hint distance in tiles.
     *
     * @param tiles Max distance in tiles
     */
    public void setHintMaxTiles(float tiles) {
        hintMaxTiles = Math.max(20f, tiles);
    }

    /**
     * Sets the world bounds for the exit room.
     *
     * @param rect Exit room rectangle in world coordinates
     */
    public void setExitRoomWorldRect(Rectangle rect) {
        this.exitRoomWorldRect = rect;
    }

    /**
     * Clears the current exit target and hides the arrow.
     */
    public void clearExitTarget() {
        this.hasExitTarget = false;
        arrow.setVisible(false);
    }

    // Boss control (call from GameScreen)
    /**
     * Updates the boss HP bar and triggers shake on damage.
     *
     * @param current Current boss HP
     * @param max Maximum boss HP
     */
    public void setBossHp(float current, float max) {
        if (max <= 0f) return;

        float ratio = MathUtils.clamp(current / max, 0f, 1f);
        bossHpBar.setValue(ratio);

        if (bossShakeEnabled && lastBossRatio >= 0f && ratio < lastBossRatio - 0.0001f) {
            triggerBossShake();
        }
        lastBossRatio = ratio;
    }

    /**
     * Shows or hides the boss HUD.
     *
     * @param visible True to show the boss HUD
     */
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

    /**
     * Triggers a short shake animation on the boss HUD.
     */
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

    /**
     * Sets the minimap actor and size.
     *
     * @param minimapActor Actor to display
     * @param width Desired width
     * @param height Desired height
     */
    public void setMinimapActor(Actor minimapActor, float width, float height) {
        minimapContainer.clearChildren();
        minimapContainer.add(minimapActor).size(width, height);
    }

    /**
     * Returns the HUD stage for input processing.
     *
     * @return HUD stage
     */
    public Stage getStage() {
        return stage;
    }

    /**
     * Returns the developer console instance.
     *
     * @return Developer console
     */
    public DevConsole getDevConsole() {
        return devConsole;
    }

    /**
     * Disposes HUD resources and textures.
     */
    public void dispose() {
        stage.dispose();
        pixelTexture.dispose();
        arrowTexture.dispose();
        keyIconTexture.dispose();
        bulletIconTexture.dispose();
        hpBgTexture.dispose();
        hpFillTexture.dispose();
        bossBgTexture.dispose();
        bossFillTexture.dispose();
    }

    /**
     * Creates a 1x1 solid color texture.
     *
     * @param color Color to use
     * @return Created texture
     */
    private Texture createSolidTexture(Color color) {
        Pixmap pm = new Pixmap(1, 1, Pixmap.Format.RGBA8888);
        pm.setColor(color);
        pm.fill();
        Texture tex = new Texture(pm);
        pm.dispose();
        return tex;
    }
}
