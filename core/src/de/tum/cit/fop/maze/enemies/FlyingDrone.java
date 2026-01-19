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

    // ---------------- Render size ----------------
    public static final float RENDER_WIDTH  = 32f;
    public static final float RENDER_HEIGHT = 32f;

    // ---------------- Animation config ----------------
    private static final int FLY_FRAMES = 5;
    private static final float FLY_FRAME_DURATION = 0.12f;

    // ---------------- Hover animation ----------------
    // Per-frame vertical hover offsets (pixels)
    private static final float[] HOVER_Y_OFFSETS = {
            0f, // frame 1
            4f, // frame 2
            8f, // frame 3
            4f, // frame 4
            0f  // frame 5
    };

    // ---------------- Direction ----------------
    private enum Direction {
        DOWN, LEFT, RIGHT, UP
    }

    private Direction currentDirection = Direction.DOWN;

    // ---------------- Behavior ----------------
    private final PatrolBehavior defaultPatrol;

    // ---------------- Animations ----------------
    private static Animation<TextureRegion> flyDown;
    private static Animation<TextureRegion> flyLeft;
    private static Animation<TextureRegion> flyRight;
    private static Animation<TextureRegion> flyUp;

    private float animationTimer = 0f;

    // Used to infer movement direction
    private final Vector2 lastPosition = new Vector2();

    // ---------------- Constructor ----------------
    public FlyingDrone(
            Vector2 startWorldPosition,
            float movementSpeed,
            Array<Vector2> patrolWorldPositions,
            Player targetPlayer
    ) {
        super(startWorldPosition, movementSpeed, new PatrolBehavior(patrolWorldPositions), targetPlayer);
        this.defaultPatrol = (PatrolBehavior) activeBehavior;
        this.chaseRadius = 240f;

        lastPosition.set(startWorldPosition);
        ensureAnimationsLoaded();
    }

    // ---------------- Animation loading ----------------
    private static void ensureAnimationsLoaded() {
        if (flyDown != null) return;

        Texture downTex  = new Texture("Enemies/FlyingDroneDOWN.png");
        Texture leftTex  = new Texture("Enemies/FlyingDroneLEFT.png");
        Texture rightTex = new Texture("Enemies/FlyingDroneRIGHT.png");
        Texture upTex    = new Texture("Enemies/FlyingDroneUP.png");

        setNearest(downTex);
        setNearest(leftTex);
        setNearest(rightTex);
        setNearest(upTex);

        flyDown  = buildStripAnim(downTex,  FLY_FRAMES, FLY_FRAME_DURATION);
        flyLeft  = buildStripAnim(leftTex,  FLY_FRAMES, FLY_FRAME_DURATION);
        flyRight = buildStripAnim(rightTex, FLY_FRAMES, FLY_FRAME_DURATION);
        flyUp    = buildStripAnim(upTex,    FLY_FRAMES, FLY_FRAME_DURATION);
    }

    private static void setNearest(Texture t) {
        t.setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest);
    }

    private static Animation<TextureRegion> buildStripAnim(Texture tex, int frames, float frameDuration) {
        int frameWidth  = tex.getWidth() / frames;
        int frameHeight = tex.getHeight();

        Array<TextureRegion> regions = new Array<>(frames);
        for (int i = 0; i < frames; i++) {
            regions.add(new TextureRegion(
                    tex,
                    i * frameWidth,
                    0,
                    frameWidth,
                    frameHeight
            ));
        }

        Animation<TextureRegion> anim = new Animation<>(frameDuration, regions);
        anim.setPlayMode(Animation.PlayMode.LOOP);
        return anim;
    }

    // ---------------- Behavior hooks ----------------
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
        updateDirectionFromMovement();
        super.updateBehavior(deltaTime, maze);
    }

    // ---------------- Direction logic ----------------
    private void updateDirectionFromMovement() {
        Vector2 pos = getPosition();

        float dx = pos.x - lastPosition.x;
        float dy = pos.y - lastPosition.y;

        if (Math.abs(dx) > Math.abs(dy)) {
            if (dx > 0.01f) currentDirection = Direction.RIGHT;
            else if (dx < -0.01f) currentDirection = Direction.LEFT;
        } else {
            if (dy > 0.01f) currentDirection = Direction.UP;
            else if (dy < -0.01f) currentDirection = Direction.DOWN;
        }

        lastPosition.set(pos);
    }

    // ---------------- Frame access ----------------
    public TextureRegion getCurrentFrame() {
        return switch (currentDirection) {
            case LEFT  -> flyLeft.getKeyFrame(animationTimer, true);
            case RIGHT -> flyRight.getKeyFrame(animationTimer, true);
            case UP    -> flyUp.getKeyFrame(animationTimer, true);
            case DOWN  -> flyDown.getKeyFrame(animationTimer, true);
        };
    }

    // ---------------- Hover offset access ----------------
    public float getHoverYOffset() {
        int frameIndex = flyDown.getKeyFrameIndex(animationTimer);
        return HOVER_Y_OFFSETS[frameIndex];
    }
}

