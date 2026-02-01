package de.tum.cit.fop.maze.enemies;

import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;

/**
 * Minimal interface representing a player-like target for enemies.
 */
public interface PlayerLike {
    /**
     * Retrieves the center position of the target.
     *
     * @return Center position
     */
    Vector2 getPosition();      // center position
    /**
     * Retrieves the hitbox of the target.
     *
     * @return Hitbox rectangle
     */
    Rectangle getHitbox();
    /**
     * Applies damage to the target.
     *
     * @param amount Damage amount
     */
    void damage(int amount);
}
