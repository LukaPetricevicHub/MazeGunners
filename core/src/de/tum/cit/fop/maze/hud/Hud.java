package de.tum.cit.fop.maze.hud;

import de.tum.cit.fop.maze.player.Player;

import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.utils.viewport.Viewport;

/**
 * Basic HUD: lives, key status, and an arrow that points to an exit direction.
 */
public class Hud {

    private final Stage stage;

    private final Label livesLabel;
    private final Label keyLabel;
    private final Image arrow;

    // Temporary exit target (until Member 1 provides real exits)
    private float exitX = 600;
    private float exitY = 400;

    public Hud(Viewport viewport, com.badlogic.gdx.graphics.g2d.SpriteBatch batch, Skin skin) {
        stage = new Stage(viewport, batch);

        livesLabel = new Label("Lives: 5", skin);
        keyLabel   = new Label("Key: NO", skin);

        // arrow texture: you need an arrow png in assets.
        // If you don't have one, use any small icon or create arrow.png quickly.
        Texture arrowTex = new Texture("things.png"); // placeholder: you WILL replace later
        arrow = new Image(arrowTex); // temporary: replace with a real arrow texture region later

        // Layout
        Table table = new Table();
        table.setFillParent(true);
        table.top().left().pad(10);

        table.add(livesLabel).left().row();
        table.add(keyLabel).left().row();

        stage.addActor(table);

        // Put arrow on top-right (temporary)
        arrow.setSize(32, 32);
        arrow.setPosition(viewport.getWorldWidth() - 48, viewport.getWorldHeight() - 48);
        arrow.setOrigin(16, 16);
        stage.addActor(arrow);
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
    }

    public void dispose() {
        stage.dispose();
    }

    public void setExitTarget(float x, float y) {
        this.exitX = x;
        this.exitY = y;
    }
}

