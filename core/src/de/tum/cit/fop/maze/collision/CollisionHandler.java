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
        if (velocity.isZero()) return;

        Vector2 step = velocity.cpy().scl(deltaTime);


        float dx = step.x;
        float dy = step.y;

        if (!hitsWall(position.x + dx, position.y, maze)) {
            position.x += dx;
        }


        if (!hitsWall(position.x, position.y + dy, maze)) {
            position.y += dy;
        }

        bounds.setPosition(position.x, position.y);
    }

    private boolean hitsWall(float x, float y, Maze maze) {
        float left   = x + inset;
        float right  = x + bounds.width - inset;
        float bottom = y + inset;
        float top    = y + bounds.height - inset;

        return maze.isWallWorld(left, bottom)
                || maze.isWallWorld(right, bottom)
                || maze.isWallWorld(left, top)
                || maze.isWallWorld(right, top);
    }
}

