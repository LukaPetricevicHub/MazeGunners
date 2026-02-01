package de.tum.cit.fop.maze.collision;

import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;
import de.tum.cit.fop.maze.map.Maze;

/**
 * Handles movement and collision detection for an entity inside a maze.
 * Prevents the entity from moving through walls.
 */
public class CollisionHandler {
    // Attributes
    /** Hitbox used for collision checks. */
    private final Rectangle hitBox;
    /** Padding inset used to shrink collision bounds. */
    private final float collisionPadding;
    /** Minimum movement threshold to consider as movement. */
    private static final float MIN_MOVEMENT_THRESHOLD = 0.001f;


    /**
     * Creates a new CollisionHandler with a given hitbox and collision inset.
     *
     * @param bounds the rectangle representing the entity's hitbox
     * @param inset padding applied to the hitbox for collision checks
     */
    public CollisionHandler(Rectangle bounds, float inset) {
        this.hitBox = bounds;
        this.collisionPadding = inset;
    }


    /**
     * Moves the entity according to its velocity while checking for wall collisions.
     *
     * @param currentPos the current position of the entity
     * @param requestedVelocity the desired movement direction and speed
     * @param deltaTime time passed since the last update
     * @param maze the maze used for collision detection
     * @return true if the entity has moved, false otherwise
     */
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
    /**
     * Checks whether the hitbox at a position collides with walls.
     *
     * @param checkPos Candidate position
     * @param maze Maze reference
     * @return True if colliding with a wall
     */
    private boolean isHittingObstacle(Vector2 checkPos, Maze maze) {
        float leftEdge = checkPos.x + collisionPadding;
        float rightEdge = checkPos.x + hitBox.width - collisionPadding;
        float bottomEdge = checkPos.y + collisionPadding;
        float topEdge = checkPos.y + hitBox.height - collisionPadding;

        return maze.isWallWorld(leftEdge, bottomEdge) || maze.isWallWorld(rightEdge, bottomEdge) || maze.isWallWorld(leftEdge, topEdge) || maze.isWallWorld(rightEdge, topEdge);
    }
}
