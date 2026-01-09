package de.tum.cit.fop.maze.enemies;

import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Animation;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.Array;
import de.tum.cit.fop.maze.behavior.BehaviorState;
import de.tum.cit.fop.maze.behavior.ChaseBehavior;
import de.tum.cit.fop.maze.behavior.PatrolBehavior;
import de.tum.cit.fop.maze.player.Player;

public class FlyingDrone extends Enemy {

    private BehaviorState patrolBehavior;
    private BehaviorState chaseBehavior;
    private float chaseDistance = 3f; // distance to player to switch to chase

    private Animation<TextureRegion> flyAnimation;
    private float stateTime = 0f;

    public FlyingDrone(Vector2 position, float speed, Array<Vector2> patrolPoints, Player player) {
        super(position, speed, null, player);
        patrolBehavior = new PatrolBehavior(patrolPoints);
        chaseBehavior = new ChaseBehavior();
        this.currentBehavior = patrolBehavior;
        loadFlyingDroneAnimation();
    }

    private void loadFlyingDroneAnimation() {
        Texture droneSheet = new Texture("mobs.png"); // reuse the same sheet for now
        int startX = 51; // adjust X position of flying drone in sheet
        int startY = 64; // adjust Y position
        int frameWidth = 44 / 3;
        int frameHeight = 64 / 4;

        Array<TextureRegion> frames = new Array<>(TextureRegion.class);
        for (int col = 0; col < 3; col++) { // 3 frames for flying
            frames.add(new TextureRegion(
                    droneSheet,
                    startX + col * frameWidth,
                    startY,
                    frameWidth,
                    frameHeight
            ));
        }

        flyAnimation = new Animation<>(0.2f, frames);
        flyAnimation.setPlayMode(Animation.PlayMode.LOOP);
    }

    public TextureRegion getCurrentFrame() {
        return flyAnimation.getKeyFrame(stateTime, true);
    }

    @Override
    protected void updateEnemyPosition(float deltaTime) {
        stateTime += deltaTime; // only increment here

        // Switch behavior based on distance to player
        if (position.dst(player.getX(), player.getY()) < chaseDistance)
            currentBehavior = chaseBehavior;
        else
            currentBehavior = patrolBehavior;

        // Execute current behavior
        if (currentBehavior != null)
            currentBehavior.execute(this, deltaTime);
    }

}

