package de.tum.cit.fop.maze.collision;

import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;
import de.tum.cit.fop.maze.map.Maze;

public class CollisionHandler {
    // Attributes
    private final Rectangle hitBox;
    private final float collisionPadding;
    private static final float MIN_MOVEMENT_THRESHOLD = 0.001f;


    // Constructor
    public CollisionHandler(Rectangle bounds, float inset) {
        this.hitBox = bounds;
        this.collisionPadding = inset;
    }


    // Handle entity movement and collisions
    public boolean move(Vector2 currentPos, Vector2 requestedVelocity, float deltaTime, Maze maze) {
        if (requestedVelocity.isZero())
            return false;


        float startX = currentPos.x;
        float startY = currentPos.y;

        Vector2 movementStep = requestedVelocity.cpy().scl(deltaTime);

        if (movementStep.x != 0f) {
            Vector2 newXPos = new Vector2(currentPos.x + movementStep.x, currentPos.y);

            if (!isHittingObstacle(newXPos, maze)) {
                currentPos.x += movementStep.x;
            }
        }

        if(movementStep.y != 0f) {
            Vector2 newYPos = new Vector2(currentPos.x, currentPos.y + movementStep.y);

            if(!isHittingObstacle(newYPos, maze))
                currentPos.y += movementStep.y;
        }

        hitBox.setPosition(currentPos.x, currentPos.y);

        float deltaX = currentPos.x - startX;
        float deltaY = currentPos.y - startY;
        boolean hasMoved = deltaX * deltaX + deltaY * deltaY > MIN_MOVEMENT_THRESHOLD;

        return hasMoved;
    }


    // Detect walls at current position
    private boolean isHittingObstacle(Vector2 checkPos, Maze maze) {
        float leftEdge = checkPos.x + collisionPadding;
        float rightEdge = checkPos.x + hitBox.width - collisionPadding;
        float bottomEdge = checkPos.y + collisionPadding;
        float topEdge = checkPos.y + hitBox.height - collisionPadding;

        return maze.isWallWorld(leftEdge, bottomEdge) || maze.isWallWorld(rightEdge, bottomEdge) || maze.isWallWorld(leftEdge, topEdge) || maze.isWallWorld(rightEdge, topEdge);
    }
}