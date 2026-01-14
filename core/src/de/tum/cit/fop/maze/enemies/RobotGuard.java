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
    private final PatrolBehavior defaultPatrol;
    private final EvadeBehavior fleeingLogic = new EvadeBehavior();

    private static final float ESCAPE_THRESHOLD = 80f;

    // Animation
    private Animation<TextureRegion> walkingAnim;
    private float animationTimer = 0f;


    // Constructor
    public RobotGuard(Vector2 startWorldPosition, float movementSpeed, Array<Vector2> patrolWorldPositions, Player targetPlayer) {
        super(startWorldPosition, movementSpeed, new PatrolBehavior(patrolWorldPositions), targetPlayer);
        this.defaultPatrol = (PatrolBehavior) activeBehavior;
        this.chaseRadius = 0f; // don't cheese me xD

        initWalkAnimation();
    }


    // Load guard walking animation frames
    private void initWalkAnimation() {
        Texture guardSheet = new Texture("mobs.png");

        int initialX = 98;
        int initialY = 64;
        int frameW = 15;
        int frameH = 16;

        Array<TextureRegion> animFrames = new Array<>();

        for (int i = 0; i < 3; i++) {
            animFrames.add(new TextureRegion(guardSheet, initialX + i * (46 / 3), initialY, frameW, frameH));
        }

        walkingAnim = new Animation<>(0.2f, animFrames);
        walkingAnim.setPlayMode(Animation.PlayMode.LOOP);
    }


    @Override
    protected BehaviorState createChaseBehavior() {
        return defaultPatrol;
    }

    @Override
    protected BehaviorState createDefaultBehavior() {
        return defaultPatrol;
    }


    @Override
    protected void updateBehavior(float deltaTime, Maze maze) {
        animationTimer += deltaTime;
        float distToPlayer = worldPosition.dst(targetPlayer.getX(), targetPlayer.getY());

        if (distToPlayer < ESCAPE_THRESHOLD)
            fleeingLogic.execute(this, deltaTime, maze);
        else
            defaultPatrol.execute(this, deltaTime, maze);

    }


    // Return current animation frame
    public TextureRegion getCurrentFrame() {
        return walkingAnim.getKeyFrame(animationTimer, true);
    }
}
