package de.tum.cit.fop.maze.items;

import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.Rectangle;
import de.tum.cit.fop.maze.player.Player;

public abstract class Collectable {
    protected Rectangle bounds;
    protected Texture texture;
    private boolean active = true;

    public Collectable(float x, float y, String texturePath) {
        this.bounds = new Rectangle(x, y, 32, 32);
        this.texture = new Texture(texturePath);
    }

    public abstract void applyEffect(Player player);

    public void render(SpriteBatch batch) {
        if (active) {
            batch.draw(texture, bounds.x, bounds.y, 32, 32);
        }
    }

    public Rectangle getBounds() { return bounds; }
    public boolean isActive() { return active; }
    public void collect() { active = false; }

    public void dispose() {
        if (texture != null) texture.dispose();
    }
}