package de.tum.cit.fop.maze.enemies;

import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;

public interface PlayerLike {
    Vector2 getPosition();       // world position (x,y)
    Rectangle getHitbox();       // world hitbox
    void damage(int amount);
}
