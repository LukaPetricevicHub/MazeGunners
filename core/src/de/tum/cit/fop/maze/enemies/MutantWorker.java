package de.tum.cit.fop.maze.enemies;

import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Animation;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.Array;
import de.tum.cit.fop.maze.behavior.BehaviorState;
import de.tum.cit.fop.maze.behavior.ChaseBehavior;
import de.tum.cit.fop.maze.map.Maze;
import de.tum.cit.fop.maze.player.Player;

public class MutantWorker extends Enemy {

    // Animation
    private Animation<TextureRegion> walkingAnim;
    private float animationTimer = 0f;


    // Constructor
    public MutantWorker(Vector2 startWorldPosition, float movementSpeed, Player targetPlayer) {
        super(startWorldPosition, movementSpeed, new ChaseBehavior(), targetPlayer);
        this.chaseRadius = 250f;
        initWalkAnimation();
    }


    // Load walking animation frames
    private void initWalkAnimation() {
        Texture mutantSheet = new Texture("mobs.png");

        int initialX = 147;
        int initialY = 0;
        int frameW = 44 / 3;
        int frameH = 64 / 4;

        Array<TextureRegion> animationFrames = new Array<>(TextureRegion.class);

        for (int i = 0; i < 3; i++) {
            animationFrames.add(new TextureRegion(mutantSheet, initialX + i * frameW, initialY, frameW, frameH));
        }

        walkingAnim = new Animation<>(0.15f, animationFrames);
        walkingAnim.setPlayMode(Animation.PlayMode.LOOP);
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
        animationTimer += deltaTime;
        super.updateBehavior(deltaTime, maze);
    }


    @Override
    protected boolean supportsAggroLock() {
        return true;
    }


    // Return current animation frame
    public TextureRegion getCurrentFrame() {
        return walkingAnim.getKeyFrame(animationTimer, true);
    }
}
