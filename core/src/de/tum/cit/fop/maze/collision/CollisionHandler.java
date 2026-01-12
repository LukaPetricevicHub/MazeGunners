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

        Vector2 nextX = new Vector2(position.x + step.x, position.y);
        if (!hitsWall(nextX, maze)) {
            position.x += step.x;
        }

        Vector2 nextY = new Vector2(position.x, position.y + step.y);
        if (!hitsWall(nextY, maze)) {
            position.y += step.y;
        }

        bounds.setPosition(position.x, position.y);
    }


    private boolean hitsWall(Vector2 position, Maze maze) {

        float centerX = position.x + bounds.width / 2f;
        float centerY = position.y + bounds.height / 2f;

        return maze.isWallWorld(centerX, centerY);
    }



    private boolean isWallAt(float x, float y, Maze maze) {
        return maze.isWallWorld(x, y);
    }

}

