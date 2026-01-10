package de.tum.cit.fop.maze.enemies;

import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Animation;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.Array;
import de.tum.cit.fop.maze.behavior.ChaseBehavior;
import de.tum.cit.fop.maze.level.Maze;
import de.tum.cit.fop.maze.player.Player;

public class MutantWorker extends Enemy {

    // Attributes
    private boolean hasDetectedPlayer = false;
    private final float detectionRange = 150f;
    private Animation<TextureRegion> walkAnimation;

    private float animationTime = 0f;


    // Constructor
    public MutantWorker(Vector2 startWorldPosition, float movementSpeed, Player targetPlayer) {
        super(startWorldPosition, movementSpeed, new ChaseBehavior(), targetPlayer);
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


    // Update detection, animation and behavior
    @Override
    protected void updateBehavior(float deltaTime, Maze maze) {
        animationTime += deltaTime;
        if (!hasDetectedPlayer && targetPlayer != null) {
            float distanceToPlayer =
                    worldPosition.dst(targetPlayer.getX(), targetPlayer.getY());

            if(distanceToPlayer <= detectionRange)
                hasDetectedPlayer = true;

        }

        if (hasDetectedPlayer && activeBehavior != null) {
            activeBehavior.execute(this, deltaTime, maze);
        }
    }


    // Return current animation frame
    public TextureRegion getCurrentFrame() {
        return walkAnimation.getKeyFrame(animationTime, true);
    }
}
