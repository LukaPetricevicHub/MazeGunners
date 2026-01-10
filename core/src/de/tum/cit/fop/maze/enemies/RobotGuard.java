package de.tum.cit.fop.maze.enemies;

import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Animation;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.Array;
import de.tum.cit.fop.maze.behavior.PatrolBehavior;
import de.tum.cit.fop.maze.level.Maze;
import de.tum.cit.fop.maze.player.Player;

public class RobotGuard extends Enemy {

    // Attributes
    private Animation<TextureRegion> walkAnimation;
    private float animationTime = 0f;


    // Constructor
    public RobotGuard(Vector2 startWorldPosition, float movementSpeed, Array<Vector2> patrolWorldPositions, Player targetPlayer) {
        super(startWorldPosition, movementSpeed, new PatrolBehavior(patrolWorldPositions), targetPlayer);
        loadWalkAnimation();
    }


    // Load guard walking animation frames
    private void loadWalkAnimation() {
        Texture spriteSheet = new Texture("mobs.png");

        int startX = 98;
        int startY = 64;
        int frameWidth = 15;
        int frameHeight = 16;

        Array<TextureRegion> frames = new Array<>();

        for (int col = 0; col < 3; col++) {
            frames.add(new TextureRegion(
                    spriteSheet,
                    startX + col * (46 / 3),
                    startY,
                    frameWidth,
                    frameHeight
            ));
        }

        walkAnimation = new Animation<>(0.2f, frames);
        walkAnimation.setPlayMode(Animation.PlayMode.LOOP);
    }


    // Return current animation frame
    public TextureRegion getCurrentFrame() {
        return walkAnimation.getKeyFrame(animationTime, true);
    }


    // Update animation and patrol behavior
    @Override
    protected void updateBehavior(float deltaTime, Maze maze) {
        animationTime += deltaTime;

        if (activeBehavior != null) {
            activeBehavior.execute(this, deltaTime, maze);
        }
    }
}
