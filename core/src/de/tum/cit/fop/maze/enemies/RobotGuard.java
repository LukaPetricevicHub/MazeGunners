package de.tum.cit.fop.maze.enemies;

import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Animation;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.Array;
import de.tum.cit.fop.maze.behavior.BehaviorState;
import de.tum.cit.fop.maze.behavior.PatrolBehavior;
import de.tum.cit.fop.maze.map.Maze;
import de.tum.cit.fop.maze.player.Player;

public class RobotGuard extends Enemy {


    public static final float RENDER_WIDTH  = 32f;
    public static final float RENDER_HEIGHT = 32f;

    private static final int WALK_FRAMES = 5;
    private static final float WALK_FRAME_DURATION = 0.15f;

    private enum Direction {
        DOWN, LEFT, RIGHT, UP
    }

    private Direction currentDirection = Direction.DOWN;

    private final PatrolBehavior defaultPatrol;

    private static Animation<TextureRegion> walkDown;
    private static Animation<TextureRegion> walkLeft;
    private static Animation<TextureRegion> walkRight;
    private static Animation<TextureRegion> walkUp;

    private float animationTimer = 0f;

    private final Vector2 lastPosition = new Vector2();

    public RobotGuard(
            Vector2 startWorldPosition,
            float movementSpeed,
            Array<Vector2> patrolWorldPositions,
            Player targetPlayer
    ) {
        super(startWorldPosition, movementSpeed, new PatrolBehavior(patrolWorldPositions), targetPlayer);
        this.defaultPatrol = (PatrolBehavior) activeBehavior;

        // RobotGuard does not chase
        this.chaseRadius = 0f;

        lastPosition.set(startWorldPosition);
        ensureAnimationsLoaded();
    }

    private static void ensureAnimationsLoaded() {
        if (walkDown != null) return;

        Texture downTex  = new Texture("Enemies/RobotGuardDOWN.png");
        Texture leftTex  = new Texture("Enemies/RobotGuardLEFT.png");
        Texture rightTex = new Texture("Enemies/RobotGuardRIGHT.png");
        Texture upTex    = new Texture("Enemies/RobotGuardUP.png");

        setNearest(downTex);
        setNearest(leftTex);
        setNearest(rightTex);
        setNearest(upTex);

        walkDown  = buildStripAnim(downTex,  WALK_FRAMES, WALK_FRAME_DURATION);
        walkLeft  = buildStripAnim(leftTex,  WALK_FRAMES, WALK_FRAME_DURATION);
        walkRight = buildStripAnim(rightTex, WALK_FRAMES, WALK_FRAME_DURATION);
        walkUp    = buildStripAnim(upTex,    WALK_FRAMES, WALK_FRAME_DURATION);
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

        defaultPatrol.execute(this, deltaTime, maze);

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
            case LEFT  -> walkLeft.getKeyFrame(animationTimer, true);
            case RIGHT -> walkRight.getKeyFrame(animationTimer, true);
            case UP    -> walkUp.getKeyFrame(animationTimer, true);
            case DOWN  -> walkDown.getKeyFrame(animationTimer, true);
        };
    }
}
