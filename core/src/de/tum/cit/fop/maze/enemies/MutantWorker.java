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

    private static final float WALK_FRAME_DURATION   = 0.12f;
    private static final float ATTACK_FRAME_DURATION = 0.10f;

    private static final float ATTACK_RADIUS = 24f;
    private static final float MIN_SAFE_RADIUS = 20f;

    private static final float ATTACK_COOLDOWN = 0.6f; // tune per enemy
    private float attackCooldownTimer = 0f;
    private float attackTimer = 0f;


    private enum Direction { DOWN, LEFT, RIGHT, UP }
    private Direction currentDirection = Direction.DOWN;

    private boolean attacking = false;
    private float animTimer = 0f;

    private static Animation<TextureRegion> walkDown, walkLeft, walkRight, walkUp;
    private static Animation<TextureRegion> atkDown, atkLeft, atkRight, atkUp;

    private final Vector2 lastPosition = new Vector2();

    public MutantWorker(Vector2 startWorldPosition, float movementSpeed, Player targetPlayer) {
        super(startWorldPosition, movementSpeed, new ChaseBehavior(), targetPlayer);
        this.chaseRadius = 250f;
        lastPosition.set(startWorldPosition);
        ensureAnimationsLoaded();
    }

    private static void ensureAnimationsLoaded() {
        if (walkDown != null) return;

        walkDown  = loadStrip("Enemies/MutantWorker/MutantWorkerDown.png", 8, WALK_FRAME_DURATION, true);
        walkLeft  = loadStrip("Enemies/MutantWorker/MutantWorkerLeft.png", 8, WALK_FRAME_DURATION, true);
        walkRight = loadStrip("Enemies/MutantWorker/MutantWorkerRight.png",8, WALK_FRAME_DURATION, true);
        walkUp    = loadStrip("Enemies/MutantWorker/MutantWorkerUp.png",   8, WALK_FRAME_DURATION, true);

        atkDown  = loadStrip("Enemies/MutantWorker/MutantWorkerDownAttack.png", 7, ATTACK_FRAME_DURATION, false);
        atkLeft  = loadStrip("Enemies/MutantWorker/MutantWorkerLeftAttack.png", 7, ATTACK_FRAME_DURATION, false);
        atkRight = loadStrip("Enemies/MutantWorker/MutantWorkerRightAttack.png",7, ATTACK_FRAME_DURATION, false);
        atkUp    = loadStrip("Enemies/MutantWorker/MutantWorkerUpAttack.png",   7, ATTACK_FRAME_DURATION, false);
    }

    private static Animation<TextureRegion> loadStrip(String path, int frames, float duration, boolean loop) {
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

    @Override
    protected void updateBehavior(float deltaTime, Maze maze) {
        animTimer += deltaTime;
        attackCooldownTimer -= deltaTime;

        // ✅ PURE distance-based logic (same as FlyingDrone)
        if (isPlayerInAttackRange()) {

            updateDirectionFromPlayer();

            // Start attack if cooldown allows
            if (!attacking && attackCooldownTimer <= 0f) {
                attacking = true;
                attackTimer = 0f;
                attackCooldownTimer = ATTACK_COOLDOWN;
            }

            // Play attack animation
            if (attacking) {
                attackTimer += deltaTime;
                if (getAttackAnim().isAnimationFinished(attackTimer)) {
                    attacking = false;
                    attackTimer = 0f;
                }
            }

            // ✅ distance-based kiting (THIS fixes "enemy above player")
            float dist2 = getPosition().dst2(getPlayerPosition());
            if (dist2 < MIN_SAFE_RADIUS * MIN_SAFE_RADIUS) {
                moveAwayFromPlayer(deltaTime, maze);
            }

            // ⛔ STOP ChaseBehavior from pulling forward
            return;
        }

        // Outside attack range → full chase resumes
        attacking = false;
        attackTimer = 0f;

        super.updateBehavior(deltaTime, maze);
        updateDirectionFromMovement();
    }



    private void updateDirectionFromPlayer() {
        Vector2 pos = getPosition();
        Vector2 p = getPlayerPosition();

        float dx = p.x - pos.x;
        float dy = p.y - pos.y;

        if (Math.abs(dx) > Math.abs(dy))
            currentDirection = dx > 0 ? Direction.RIGHT : Direction.LEFT;
        else
            currentDirection = dy > 0 ? Direction.UP : Direction.DOWN;
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

    private Direction getAttackDirection() {
        Vector2 pos = getPosition();
        Vector2 p = getPlayerPosition();

        float dx = p.x - pos.x;
        float dy = p.y - pos.y;

        return Math.abs(dx) > Math.abs(dy)
                ? (dx > 0 ? Direction.RIGHT : Direction.LEFT)
                : (dy > 0 ? Direction.UP : Direction.DOWN);
    }

    public TextureRegion getCurrentFrame() {
        return attacking
                ? getAttackAnim().getKeyFrame(attackTimer, false)
                : getWalkAnim().getKeyFrame(animTimer, true);
    }

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

    @Override
    public float getRenderWidth() {
        return 24f;
    }

    @Override
    public float getRenderHeight() {
        return 24f;
    }

    private boolean isPlayerInAttackRange() {
        float dist2 = getPosition().dst2(getPlayerPosition());
        return dist2 <= ATTACK_RADIUS * ATTACK_RADIUS;
    }

    private void moveAwayFromPlayer(float deltaTime, Maze maze) {
        Vector2 pos = getPosition();
        Vector2 playerPos = getPlayerPosition();

        Vector2 away = pos.cpy().sub(playerPos);
        if (away.len2() < 0.001f) return;

        away.nor().scl(getSpeed());
        getCollisionHandler().move(pos, away, deltaTime, maze);
    }


    @Override
    public float getAttackRadius() {
        return ATTACK_RADIUS;
    }

}

