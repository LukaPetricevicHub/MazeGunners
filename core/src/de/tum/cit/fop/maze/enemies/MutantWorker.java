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

    // ---------------- Render size ----------------
    public static final float RENDER_WIDTH  = 32f;
    public static final float RENDER_HEIGHT = 32f;

    // ---------------- Animation config ----------------
    private static final int WALK_FRAMES   = 5;
    private static final int ATTACK_FRAMES = 5;

    private static final float WALK_FRAME_DURATION   = 0.12f;
    private static final float ATTACK_FRAME_DURATION = 0.10f;

    // ---------------- Direction ----------------
    private enum Direction { DOWN, LEFT, RIGHT, UP }
    private Direction currentDirection = Direction.DOWN;

    // ---------------- State ----------------
    private boolean attacking = false;
    private float attackCooldown = 0f;
    private static final float ATTACK_COOLDOWN_TIME = 0.4f;

    // ---------------- Animations ----------------
    private static Animation<TextureRegion> walkDown, walkLeft, walkRight, walkUp;
    private static Animation<TextureRegion> atkDown,  atkLeft,  atkRight,  atkUp;

    private float animTimer = 0f;

    // Movement tracking
    private final Vector2 lastPosition = new Vector2();

    // ---------------- Constructor ----------------
    public MutantWorker(Vector2 startWorldPosition, float movementSpeed, Player targetPlayer) {
        super(startWorldPosition, movementSpeed, new ChaseBehavior(), targetPlayer);
        this.chaseRadius = 250f;

        lastPosition.set(startWorldPosition);
        ensureAnimationsLoaded();
    }

    // ---------------- Animation loading ----------------
    private static void ensureAnimationsLoaded() {
        if (walkDown != null) return;

        // WALK
        walkDown  = loadStrip("Enemies/MutantWorkerDOWN.png",  WALK_FRAMES, WALK_FRAME_DURATION, true);
        walkLeft  = loadStrip("Enemies/MutantWorkerLEFT.png",  WALK_FRAMES, WALK_FRAME_DURATION, true);
        walkRight = loadStrip("Enemies/MutantWorkerRIGHT.png", WALK_FRAMES, WALK_FRAME_DURATION, true);
        walkUp    = loadStrip("Enemies/MutantWorkerUP.png",    WALK_FRAMES, WALK_FRAME_DURATION, true);

        // ATTACK
        atkDown  = loadStrip("Enemies/MutantWorkerDOWNAttack.png",  ATTACK_FRAMES, ATTACK_FRAME_DURATION, false);
        atkLeft  = loadStrip("Enemies/MutantWorkerLEFTAttack.png",  ATTACK_FRAMES, ATTACK_FRAME_DURATION, false);
        atkRight = loadStrip("Enemies/MutantWorkerRIGHTAttack.png", ATTACK_FRAMES, ATTACK_FRAME_DURATION, false);
        atkUp    = loadStrip("Enemies/MutantWorkerUPAttack.png",    ATTACK_FRAMES, ATTACK_FRAME_DURATION, false);
    }

    private static Animation<TextureRegion> loadStrip(
            String path, int frames, float duration, boolean loop
    ) {
        Texture tex = new Texture(path);
        tex.setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest);

        int frameW = tex.getWidth() / frames;
        int frameH = tex.getHeight();

        Array<TextureRegion> regions = new Array<>(frames);
        for (int i = 0; i < frames; i++) {
            regions.add(new TextureRegion(tex, i * frameW, 0, frameW, frameH));
        }

        Animation<TextureRegion> anim = new Animation<>(duration, regions);
        anim.setPlayMode(loop ? Animation.PlayMode.LOOP : Animation.PlayMode.NORMAL);
        return anim;
    }

    // ---------------- Behavior hooks ----------------
    @Override
    protected BehaviorState createChaseBehavior() {
        return new ChaseBehavior();
    }

    @Override
    protected BehaviorState createDefaultBehavior() {
        return new ChaseBehavior();
    }

    @Override
    protected boolean supportsAggroLock() {
        return true;
    }

    // ---------------- Update ----------------
    @Override
    protected void updateBehavior(float deltaTime, Maze maze) {
        animTimer += deltaTime;
        attackCooldown = Math.max(0f, attackCooldown - deltaTime);

        if (!attacking) {
            updateDirectionFromMovement();
        }

        // Check collision with player → trigger attack
        if (!attacking && attackCooldown <= 0f &&
                getCollisionBox().overlaps(
                        targetPlayer != null
                                ? new com.badlogic.gdx.math.Rectangle(
                                targetPlayer.getX(),
                                targetPlayer.getY(),
                                32f, 64f)
                                : null
                )) {
            startAttack();
        }

        // End attack when animation finished
        if (attacking && getAttackAnim().isAnimationFinished(animTimer)) {
            attacking = false;
            animTimer = 0f;
            attackCooldown = ATTACK_COOLDOWN_TIME;
        }

        super.updateBehavior(deltaTime, maze);
    }

    private void startAttack() {
        attacking = true;
        animTimer = 0f;
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

    // ---------------- Animation selection ----------------
    private Animation<TextureRegion> getAttackAnim() {
        return switch (currentDirection) {
            case UP    -> atkUp;
            case LEFT  -> atkLeft;
            case RIGHT -> atkRight;
            case DOWN  -> atkDown;
        };
    }

    private Animation<TextureRegion> getWalkAnim() {
        return switch (currentDirection) {
            case UP    -> walkUp;
            case LEFT  -> walkLeft;
            case RIGHT -> walkRight;
            case DOWN  -> walkDown;
        };
    }

    // ---------------- Rendering hook ----------------
    public TextureRegion getCurrentFrame() {
        return attacking
                ? getAttackAnim().getKeyFrame(animTimer)
                : getWalkAnim().getKeyFrame(animTimer, true);
    }
}
