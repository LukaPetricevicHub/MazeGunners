package de.tum.cit.fop.maze.enemies;

import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;
import de.tum.cit.fop.maze.player.Player;

public class PlayerAdapter implements PlayerLike {
    private final Player p;
    private final Vector2 tmp = new Vector2();
    private final Rectangle tmpRect = new Rectangle();

    public PlayerAdapter(Player p) { this.p = p; }

    @Override
    public Vector2 getPosition() {
        // use player center; player sprite is 32x32-ish logically
        return tmp.set(p.getX() + 16f, p.getY() + 16f);
    }

    @Override
    public Rectangle getHitbox() {
        tmpRect.set(p.getX(), p.getY(), 32f, 32f);
        return tmpRect;
    }

    @Override
    public void damage(int amount) {
        p.takeDamage(amount);
    }
}
