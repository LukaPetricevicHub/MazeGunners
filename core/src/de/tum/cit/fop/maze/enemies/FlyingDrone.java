package de.tum.cit.fop.maze.enemies;

import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Animation;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.Array;
import de.tum.cit.fop.maze.behavior.ChaseBehavior;
import de.tum.cit.fop.maze.behavior.PatrolBehavior;
import de.tum.cit.fop.maze.collision.CollisionHandler;
import de.tum.cit.fop.maze.level.Maze;
import de.tum.cit.fop.maze.player.Player;

public class FlyingDrone extends Enemy {

    // Attributes
    private final PatrolBehavior patrolBehavior;
    private final ChaseBehavior chaseBehavior;

    private static final float CHASE_DISTANCE = 180f;

    // Animation
    private Animation<TextureRegion> flyingAnimation;
    private float animationTime = 0f;


    // Constructor
    public FlyingDrone(
            Vector2 startWorldPosition,
            float movementSpeed,
            Array<Vector2> patrolWorldPositions,
            Player targetPlayer
    ) {
        super(startWorldPosition, movementSpeed, null, targetPlayer);

        this.patrolBehavior = new PatrolBehavior(patrolWorldPositions);
        this.chaseBehavior = new ChaseBehavior();
        this.activeBehavior = patrolBehavior;

        loadFlyingAnimation();
    }



    // Load flying drone animation frames
    private void loadFlyingAnimation() {
        Texture droneSpriteSheet = new Texture("mobs.png");

        int startX = 51;
        int startY = 64;
        int frameWidth = 44 / 3;
        int frameHeight = 64 / 4;

        Array<TextureRegion> frames = new Array<>(TextureRegion.class);

        for (int col = 0; col < 3; col++) {
            frames.add(new TextureRegion(
                    droneSpriteSheet,
                    startX + col * frameWidth,
                    startY,
                    frameWidth,
                    frameHeight
            ));
        }

        flyingAnimation = new Animation<>(0.2f, frames);
        flyingAnimation.setPlayMode(Animation.PlayMode.LOOP);
    }


    // Update behavior and animation
    @Override
    protected void updateBehavior(float deltaTime, Maze maze) {
        animationTime += deltaTime;

        float dist = worldPosition.dst(
                targetPlayer.getX(),
                targetPlayer.getY()
        );

        if (dist < CHASE_DISTANCE) {
            activeBehavior = chaseBehavior;
        } else {
            activeBehavior = patrolBehavior;
        }

        activeBehavior.execute(this, deltaTime, maze);
    }


    // Return current animation frame
    public TextureRegion getCurrentFrame() {
        return flyingAnimation.getKeyFrame(animationTime, true);
    }
}

