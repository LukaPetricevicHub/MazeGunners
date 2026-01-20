package de.tum.cit.fop.maze.enemies;

import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Animation;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.Array;
import de.tum.cit.fop.maze.behavior.BehaviorState;
import de.tum.cit.fop.maze.behavior.BehaviorStateType;
import de.tum.cit.fop.maze.behavior.ChaseBehavior;
import de.tum.cit.fop.maze.behavior.PatrolBehavior;
import de.tum.cit.fop.maze.map.Maze;
import de.tum.cit.fop.maze.player.Player;
import de.tum.cit.fop.maze.behavior.ReturnToPatrolBehavior;


public class FlyingDrone extends Enemy {

    public static final float RENDER_WIDTH  = 32f;
    public static final float RENDER_HEIGHT = 32f;

    private static final int FLY_FRAMES = 5;
    private static final float FLY_FRAME_DURATION = 0.12f;


    private static final float[] HOVER_Y_OFFSETS = {0f, 4f, 8f, 4f, 0f};

    private enum Direction {
        DOWN, LEFT, RIGHT, UP
    }

    private Direction currentDirection = Direction.DOWN;
    private final Array<Vector2> patrolPoints;

    private static Animation<TextureRegion> flyDown;
    private static Animation<TextureRegion> flyLeft;
    private static Animation<TextureRegion> flyRight;
    private static Animation<TextureRegion> flyUp;

    private float animationTimer = 0f;


    private final Vector2 lastPosition = new Vector2();
    private boolean snapToCenterOnChase = false;
    private boolean snapToCenterOnReturn = false;



    public FlyingDrone(
            Vector2 startWorldPosition,
            float movementSpeed,
            Array<Vector2> patrolWorldPositions,
            Player targetPlayer
    ) {
        super(startWorldPosition, movementSpeed, new PatrolBehavior(patrolWorldPositions), targetPlayer);
        this.patrolPoints = new Array<>(patrolWorldPositions);
        this.chaseRadius = 160f;


        lastPosition.set(startWorldPosition);
        ensureAnimationsLoaded();
    }

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

    @Override
    protected void updateAI(float deltaTime) {
        float dist2 = worldPosition.dst2(getPlayerPosition());
        boolean playerInRange = dist2 <= chaseRadius * chaseRadius;

        if (playerInRange) {
            if (activeBehavior.getType() != BehaviorStateType.CHASE) {
                snapToCenterOnChase = true;
                setCurrentBehavior(createChaseBehavior());
            }
        } else {
            if (activeBehavior.getType() == BehaviorStateType.CHASE) {
                snapToCenterOnReturn = true;
                setCurrentBehavior(new ReturnToPatrolBehavior());
            }
        }
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

    @Override
    protected BehaviorState createChaseBehavior() {
        return new ChaseBehavior();
    }

    @Override
    protected BehaviorState createDefaultBehavior() {
        return new PatrolBehavior(patrolPoints);
    }


    @Override
    protected void updateBehavior(float deltaTime, Maze maze) {

        if (snapToCenterOnChase || snapToCenterOnReturn) {
            float ts = maze.getTileSize();
            Vector2 pos = getPosition();

            int tx = (int)(pos.x / ts);
            int ty = (int)(pos.y / ts);

            pos.set(
                    tx * ts + ts * 0.5f,
                    ty * ts + ts * 0.5f
            );

            snapToCenterOnChase = false;
            snapToCenterOnReturn = false;
        }

        animationTimer += deltaTime;
        super.updateBehavior(deltaTime, maze);
        updateDirectionFromMovement();
    }




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

    public TextureRegion getCurrentFrame() {
        return switch (currentDirection) {
            case LEFT  -> flyLeft.getKeyFrame(animationTimer, true);
            case RIGHT -> flyRight.getKeyFrame(animationTimer, true);
            case UP    -> flyUp.getKeyFrame(animationTimer, true);
            case DOWN  -> flyDown.getKeyFrame(animationTimer, true);
        };
    }

    public float getHoverYOffset() {
        int frameIndex = flyDown.getKeyFrameIndex(animationTimer);
        return HOVER_Y_OFFSETS[frameIndex];
    }

    public Vector2 getClosestPatrolPoint() {
        Vector2 pos = getPosition();
        Vector2 closest = patrolPoints.first();
        float bestDist = pos.dst2(closest);

        for (Vector2 p : patrolPoints) {
            float d = pos.dst2(p);
            if (d < bestDist) {
                bestDist = d;
                closest = p;
            }
        }
        return closest;
    }

    public BehaviorState createPatrolBehavior() {
        return new PatrolBehavior(patrolPoints);
    }


    @Override
    protected boolean shouldResetPatrolOnReturn() {
        return false;
    }

    @Override
    public int getPathfindingFootprint() {
        return 1;
    }


}

