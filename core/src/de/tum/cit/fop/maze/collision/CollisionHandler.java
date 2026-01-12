package de.tum.cit.fop.maze.collision;

import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;
import de.tum.cit.fop.maze.level.Maze;

public class CollisionHandler {

    private final Rectangle bounds;
    private final float inset;


    public CollisionHandler(Rectangle bounds, float inset) {
        this.bounds = bounds;
        this.inset = inset;
    }


    public void move(Vector2 position, Vector2 velocity, float deltaTime, Maze maze) {

        if (velocity.isZero())
            return;

        Vector2 step = velocity.cpy().scl(deltaTime);


        if(step.x != 0f) {
            Vector2 nextX = new Vector2(position.x + step.x, position.y);
            if (!hitsWall(nextX, maze))
                position.x += step.x;

        }


        if (step.y != 0f) {
            Vector2 nextY = new Vector2(position.x, position.y + step.y);
            if (!hitsWall(nextY, maze)) {
                position.y += step.y;
            }
        }

        bounds.setPosition(position.x, position.y);
    }

    private boolean hitsWall(Vector2 pos, Maze maze) {

        float left   = pos.x + inset;
        float right  = pos.x + bounds.width  - inset;
        float bottom = pos.y + inset;
        float top    = pos.y + bounds.height - inset;

        return maze.isWallWorld(left, bottom)
                || maze.isWallWorld(right, bottom)
                || maze.isWallWorld(left, top)
                || maze.isWallWorld(right, top);
    }
}


