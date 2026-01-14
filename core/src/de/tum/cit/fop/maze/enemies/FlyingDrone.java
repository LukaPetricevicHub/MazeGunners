package de.tum.cit.fop.maze.enemies;

import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Animation;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.Array;
import de.tum.cit.fop.maze.behavior.BehaviorState;
import de.tum.cit.fop.maze.behavior.ChaseBehavior;
import de.tum.cit.fop.maze.behavior.PatrolBehavior;
import de.tum.cit.fop.maze.map.Maze;
import de.tum.cit.fop.maze.player.Player;

public class FlyingDrone extends Enemy {
    // Attributes
    private final PatrolBehavior defaultPatrol;

    // Animation
    private Animation<TextureRegion> flyAnim;
    private float animationTimer = 0f;


    // Constructor
    public FlyingDrone(Vector2 startWorldPosition, float movementSpeed, Array<Vector2> patrolWorldPositions, Player targetPlayer) {
        super(startWorldPosition, movementSpeed, new PatrolBehavior(patrolWorldPositions), targetPlayer);
        this.defaultPatrol = (PatrolBehavior) activeBehavior;
        this.chaseRadius = 240f;
        initAnimations();
    }


    // Load flying drone animation frames
    private void initAnimations() {
        Texture droneSheet = new Texture("mobs.png");

        int initialX = 51;
        int initialY = 64;
        int frameW = 44 / 3;
        int frameH = 64 / 4;

        Array<TextureRegion> animationFrames = new Array<>(TextureRegion.class);

        for (int i = 0; i < 3; i++) {
            animationFrames.add(new TextureRegion(droneSheet, initialX + i * frameW, initialY, frameW, frameH));
        }

        flyAnim = new Animation<>(0.2f, animationFrames);
        flyAnim.setPlayMode(Animation.PlayMode.LOOP);
    }


    @Override
    protected BehaviorState createChaseBehavior() {
        return new ChaseBehavior();
    }

    @Override
    protected BehaviorState createDefaultBehavior() {
        return defaultPatrol;
    }


    @Override
    protected void updateBehavior(float deltaTime, Maze maze) {
        animationTimer += deltaTime;
        super.updateBehavior(deltaTime, maze);
    }


    // Return current animation frame
    public TextureRegion getCurrentFrame() {
        return flyAnim.getKeyFrame(animationTimer, true);
    }
}
