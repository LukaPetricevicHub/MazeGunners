package de.tum.cit.fop.maze.hud;

import de.tum.cit.fop.maze.player.Player;

import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.utils.viewport.Viewport;

/**
 * Basic HUD: lives, key status, and an arrow that points to an exit direction.
 */
public class Hud {

    private final Stage stage;

    private final Label livesLabel;
    private final Label keyLabel;
    private final Image arrow;

    // Exit target (later: set to closest exit)
    private float exitX = 600;
    private float exitY = 400;

    public Hud(Viewport viewport, com.badlogic.gdx.graphics.g2d.SpriteBatch batch, Skin skin, TextureRegion arrowRegion) {
        stage = new Stage(viewport, batch);

        livesLabel = new Label("Lives: 5", skin);
        keyLabel   = new Label("Key: NO", skin);

        // FIX: do not new Texture() here. Use region from atlas/asset manager.
        arrow = new Image(new TextureRegionDrawable(arrowRegion));
        arrow.setSize(32, 32);
        arrow.setOrigin(16, 16);

        // Layout
        Table root = new Table();
        root.setFillParent(true);
        root.top().left().pad(10);

        root.add(livesLabel).left().row();
        root.add(keyLabel).left().row();

        stage.addActor(root);
        stage.addActor(arrow);

        // position arrow initially
        repositionArrow();
    }

    private void repositionArrow() {
        float w = stage.getViewport().getWorldWidth();
        float h = stage.getViewport().getWorldHeight();
        arrow.setPosition(w - 48, h - 48);
    }

    public Stage getStage() {
        return stage;
    }

    public void update(Player player) {
        livesLabel.setText("Lives: " + player.getLives());
        keyLabel.setText("Key: " + (player.hasKey() ? "YES" : "NO"));

        // Rotate arrow towards exit target
        float px = player.getX();
        float py = player.getY();
        float dx = exitX - px;
        float dy = exitY - py;

        float angleDeg = (float) Math.toDegrees(Math.atan2(dy, dx));
        arrow.setRotation(angleDeg);
    }

    public void act(float dt) {
        stage.act(dt);
    }

    public void draw() {
        stage.draw();
    }

    public void resize(int width, int height, boolean centerCamera) {
        stage.getViewport().update(width, height, centerCamera);
        repositionArrow(); // FIX: ensure arrow stays top-right after resize
    }

    public void dispose() {
        stage.dispose();
        // DO NOT dispose arrow texture here because it's managed by AssetManager/atlas outside.
    }

    public void setExitTarget(float x, float y) {
        this.exitX = x;
        this.exitY = y;
    }
}
