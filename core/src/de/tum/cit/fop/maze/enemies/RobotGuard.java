package de.tum.cit.fop.maze.enemies;

import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Animation;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.Array;
import de.tum.cit.fop.maze.behavior.BehaviorState;
import de.tum.cit.fop.maze.behavior.EvadeBehavior;
import de.tum.cit.fop.maze.behavior.PatrolBehavior;
import de.tum.cit.fop.maze.map.Maze;
import de.tum.cit.fop.maze.player.Player;

public class RobotGuard extends Enemy {

    // Attributes
    private final PatrolBehavior patrolBehavior;
    private final EvadeBehavior evadeBehavior = new EvadeBehavior();

    private static final float EVADE_DISTANCE = 80f;

    // Animation
    private Animation<TextureRegion> walkAnimation;
    private float animationTime = 0f;


    // Constructor
    public RobotGuard(Vector2 startWorldPosition, float movementSpeed, Array<Vector2> patrolWorldPositions, Player targetPlayer) {
        super(startWorldPosition, movementSpeed, new PatrolBehavior(patrolWorldPositions), targetPlayer);

        this.patrolBehavior = (PatrolBehavior) activeBehavior;

        // Disable chase entirely
        this.chaseRadius = 0f;

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


    @Override
    protected BehaviorState createChaseBehavior() {
        return patrolBehavior;
    }

    @Override
    protected BehaviorState createDefaultBehavior() {
        return patrolBehavior;
    }


    @Override
    protected void updateBehavior(float deltaTime, Maze maze) {
        animationTime += deltaTime;

        float dist = worldPosition.dst(
                targetPlayer.getX(),
                targetPlayer.getY()
        );

        if (dist < EVADE_DISTANCE) {
            evadeBehavior.execute(this, deltaTime, maze);
        } else {
            patrolBehavior.execute(this, deltaTime, maze);
        }
    }


    // Return current animation frame
    public TextureRegion getCurrentFrame() {
        return walkAnimation.getKeyFrame(animationTime, true);
    }
}


