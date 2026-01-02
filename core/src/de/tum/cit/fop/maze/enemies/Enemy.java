package de.tum.cit.fop.maze.enemies;

import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;
import de.tum.cit.fop.maze.behavior.BehaviorState;

public abstract class Enemy {

    // Attributes
    protected Vector2 position;
    protected float speed;
    protected Rectangle collisionBox;
    protected BehaviorState currentBehavior;

    // Constructor
    public Enemy(Vector2 position, float speed, BehaviorState currentBehavior) {
        this.position = position;
        this.speed = speed;
        this.collisionBox = new Rectangle(position.x, position.y, 1f, 1f);
        this.currentBehavior = currentBehavior;
    }

    // Update the enemies Position
    public void updateEnemyPosition(float deltaTime) {
        if (currentBehavior != null) {
            currentBehavior.execute(this, deltaTime);
        }
        collisionBox.setPosition(position.x, position.y);
    }

    // Enemy movement
    public void moveEnemy(Vector2 direction, float deltaTime) {
        position.mulAdd(direction.nor(), speed * deltaTime);
    }

    // Find our player
    public abstract Vector2 getPlayerPosition();

    // Getters & Setters
    public Vector2 getPosition() {
        return position;
    }

    public void setPosition(Vector2 position) {
        this.position = position;
    }

    public float getSpeed() {
        return speed;
    }

    public void setSpeed(float speed) {
        this.speed = speed;
    }

    public Rectangle getCollisionBox() {
        return collisionBox;
    }

    public void setCollisionBox(Rectangle collisionBox) {
        this.collisionBox = collisionBox;
    }

    public BehaviorState getCurrentBehavior() {
        return currentBehavior;
    }

    public void setCurrentBehavior(BehaviorState currentBehavior) {
        this.currentBehavior = currentBehavior;
    }

}
