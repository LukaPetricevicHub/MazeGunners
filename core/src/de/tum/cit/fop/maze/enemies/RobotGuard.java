package de.tum.cit.fop.maze.enemies;

import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Animation;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.Array;
import de.tum.cit.fop.maze.behavior.EvadeBehavior;
import de.tum.cit.fop.maze.behavior.PatrolBehavior;
import de.tum.cit.fop.maze.level.Maze;
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
    public RobotGuard(
            Vector2 startWorldPosition,
            float movementSpeed,
            Array<Vector2> patrolWorldPositions,
            Player targetPlayer
    ) {
        this(
                startWorldPosition,
                movementSpeed,
                new PatrolBehavior(patrolWorldPositions),
                targetPlayer
        );
    }

    // Private chaining constructor
    private RobotGuard(
            Vector2 startWorldPosition,
            float movementSpeed,
            PatrolBehavior patrolBehavior,
            Player targetPlayer
    ) {
        super(
                startWorldPosition,
                movementSpeed,
                patrolBehavior,
                targetPlayer
        );

        this.patrolBehavior = patrolBehavior;

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


    // Update animation and behavior
    @Override
    protected void updateBehavior(float deltaTime, Maze maze) {
        animationTime += deltaTime;

        float dist = worldPosition.dst(
                targetPlayer.getX(),
                targetPlayer.getY()
        );

        if (dist < EVADE_DISTANCE) {
            activeBehavior = evadeBehavior;
        } else {
            activeBehavior = patrolBehavior;
        }

        activeBehavior.execute(this, deltaTime, maze);
    }


    // Return current animation frame
    public TextureRegion getCurrentFrame() {
        return walkAnimation.getKeyFrame(animationTime, true);
    }
}

