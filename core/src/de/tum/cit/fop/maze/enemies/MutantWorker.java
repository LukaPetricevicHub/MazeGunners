package de.tum.cit.fop.maze.enemies;

import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Animation;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.Array;
import de.tum.cit.fop.maze.behavior.BehaviorState;
import de.tum.cit.fop.maze.behavior.ChaseBehavior;
import de.tum.cit.fop.maze.level.Maze;
import de.tum.cit.fop.maze.player.Player;

public class MutantWorker extends Enemy {

    // Animation
    private Animation<TextureRegion> walkAnimation;
    private float animationTime = 0f;


    // Constructor
    public MutantWorker(
            Vector2 startWorldPosition,
            float movementSpeed,
            Player targetPlayer
    ) {
        super(startWorldPosition, movementSpeed, new ChaseBehavior(), targetPlayer);
        this.chaseRadius = 250f;
        loadWalkAnimation();
    }


    // Load walking animation frames
    private void loadWalkAnimation() {
        Texture spriteSheet = new Texture("mobs.png");

        int startX = 147;
        int startY = 0;
        int frameWidth = 44 / 3;
        int frameHeight = 64 / 4;

        Array<TextureRegion> frames = new Array<>(TextureRegion.class);

        for (int col = 0; col < 3; col++) {
            frames.add(new TextureRegion(
                    spriteSheet,
                    startX + col * frameWidth,
                    startY,
                    frameWidth,
                    frameHeight
            ));
        }

        walkAnimation = new Animation<>(0.15f, frames);
        walkAnimation.setPlayMode(Animation.PlayMode.LOOP);
    }


    @Override
    protected BehaviorState createChaseBehavior() {
        return new ChaseBehavior();
    }


    // Never stop chasing your dreams, who's going to carry the boats
    @Override
    protected BehaviorState createDefaultBehavior() {
        return new ChaseBehavior();
    }


    @Override
    protected void updateBehavior(float deltaTime, Maze maze) {
        animationTime += deltaTime;
        super.updateBehavior(deltaTime, maze);
    }


    @Override
    protected boolean supportsAggroLock() {
        return true;
    }


    // Return current animation frame
    public TextureRegion getCurrentFrame() {
        return walkAnimation.getKeyFrame(animationTime, true);
    }
}


