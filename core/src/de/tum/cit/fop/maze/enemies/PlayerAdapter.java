package de.tum.cit.fop.maze.enemies;

import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;
import de.tum.cit.fop.maze.player.Player;

/**
 * Adapter class that allows a {@link Player} to be used as a {@link PlayerLike}.
 * <p>
 * Translates the {@code Player} interface into the methods required
 * by enemies, such as position, hitbox, and damage handling.
 */
public class PlayerAdapter implements PlayerLike {
    private final Player p;
    private final Vector2 tmp = new Vector2();
    private final Rectangle tmpRect = new Rectangle();

    /**
     * Creates a new adapter for the given player.
     *
     * @param p the player that should be adapted to the {@code PlayerLike} interface
     */
    public PlayerAdapter(Player p) { this.p = p; }

    /**
     * Returns the current position of the player.
     * <p>
     * The position corresponds to the center of the player's sprite, assuming
     * a logical sprite size of 32x32 units.
     *
     * @return a {@link Vector2} representing the player's center position
     */
    @Override
    public Vector2 getPosition() {
        
        return tmp.set(p.getX() + 16f, p.getY() + 16f);
    }

    /**
     * Returns the hitbox of the player.
     * <p>
     * The hitbox is represented as a rectangle with a width and height of 32 units,
     * positioned at the player's current coordinates.
     *
     * @return a {@link Rectangle} representing the player's hitbox
     */
    @Override
    public Rectangle getHitbox() {
        tmpRect.set(p.getX(), p.getY(), 32f, 32f);
        return tmpRect;
    }

    /**
     * Deals damage to the player.
     * <p>
     * This method forwards the damage value to the underlying {@link Player}.
     *
     * @param amount the amount of damage to deal
     */
    @Override
    public void damage(int amount) {
        p.takeDamage(amount);
    }
}

