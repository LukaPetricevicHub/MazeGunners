package de.tum.cit.fop.maze.enemies;

import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;
import de.tum.cit.fop.maze.behavior.BehaviorState;
import de.tum.cit.fop.maze.player.Player;


public abstract class Enemy {

    // Attributes
    protected Vector2 position;
    protected float speed;
    protected Rectangle collisionBox;
    protected BehaviorState currentBehavior;

    // Reference to the Player
    protected Player player;

    // Constructor
    public Enemy(Vector2 position, float speed, BehaviorState currentBehavior, Player player) {
        this.position = position;
        this.speed = speed;
        this.collisionBox = new Rectangle(position.x, position.y, 1f, 1f);
        this.currentBehavior = currentBehavior;
        this.player = player;
    }

    // Update all enemies
    public final void update(float deltaTime) {
        updateEnemyPosition(deltaTime);
        collisionBox.setPosition(position.x, position.y);
    }

    // Update a specific enemies position
    protected void updateEnemyPosition(float deltaTime) {
        if (currentBehavior != null) {
            currentBehavior.execute(this, deltaTime);
        }
    }

    // Enemy movement
    public void moveEnemy(Vector2 direction, float deltaTime) {
        position.mulAdd(direction.nor(), speed * deltaTime);
    }

    // Find our player
    public Vector2 getPlayerPosition() {
        if (player != null)
            return new Vector2(player.getX(), player.getY());
        return new Vector2(0,0); // Fallback
    }

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

    public void setPlayer(Player player) {
        this.player = player;
    }

}
