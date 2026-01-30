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

    private static final int WALK_FRAMES = 3;
    private static final int ATTACK_FRAMES = 5;

    private static final float WALK_FRAME_DURATION = 0.15f;
    private static final float ATTACK_FRAME_DURATION = 0.12f;

    private static final float ATTACK_RANGE_X = 24f;
    private static final float ATTACK_RANGE_Y = 36f;

    private static final float MIN_SAFE_RANGE_X = 12f;
    private static final float MIN_SAFE_RANGE_DOWN = 18f;
    private static final float MIN_SAFE_RANGE_UP   = 24f;

    private static final int SHEET_COLS = 5;
    private static final int SHEET_ROWS = 22;

    private static final int WALK_ROW = 4;
    private static final int ATTACK_ROW = 6;

    private enum Direction { DOWN, LEFT, RIGHT, UP }

    private Direction currentDirection = Direction.DOWN;

    private final PatrolBehavior defaultPatrol;

    private static Texture sheetDown;
    private static Texture sheetUp;
    private static Texture sheetLeft;
    private static Texture sheetRight;

    private static Animation<TextureRegion> walkDown;
    private static Animation<TextureRegion> walkUp;
    private static Animation<TextureRegion> walkLeft;
    private static Animation<TextureRegion> walkRight;

    private static Animation<TextureRegion> attackDown;
    private static Animation<TextureRegion> attackUp;
    private static Animation<TextureRegion> attackLeft;
    private static Animation<TextureRegion> attackRight;

    private boolean attacking = false;
    private float attackTimer = 0f;
    private float animationTimer = 0f;

    private final Vector2 lastPosition = new Vector2();

    // Initialize robot guard
    public RobotGuard(
            Vector2 startWorldPosition,
            float movementSpeed,
            Array<Vector2> patrolWorldPositions,
            Player targetPlayer
    ) {
        super(startWorldPosition, movementSpeed, new PatrolBehavior(patrolWorldPositions), targetPlayer);
        defaultPatrol = (PatrolBehavior) activeBehavior;
        chaseRadius = 0f;

        lastPosition.set(startWorldPosition);
        ensureAnimationsLoaded();
    }

    // Load robot animations once
    private static void ensureAnimationsLoaded() {
        if(walkDown != null) return;

        sheetDown  = new Texture("Enemies/RobotGuard/RobotGuardDown.png");
        sheetUp    = new Texture("Enemies/RobotGuard/RobotGuardUp.png");
        sheetLeft  = new Texture("Enemies/RobotGuard/RobotGuardLeft.png");
        sheetRight = new Texture("Enemies/RobotGuard/RobotGuardRight.png");

        setNearest(sheetDown);
        setNearest(sheetUp);
        setNearest(sheetLeft);
        setNearest(sheetRight);

        walkDown  = buildRowAnim(sheetDown,WALK_ROW,WALK_FRAMES,WALK_FRAME_DURATION,true);
        walkUp    = buildRowAnim(sheetUp,WALK_ROW,WALK_FRAMES,WALK_FRAME_DURATION,true);
        walkLeft  = buildRowAnim(sheetLeft,WALK_ROW,WALK_FRAMES,WALK_FRAME_DURATION,true);
        walkRight = buildRowAnim(sheetRight,WALK_ROW,WALK_FRAMES,WALK_FRAME_DURATION,true);

        attackDown  = buildRowAnim(sheetDown,ATTACK_ROW,ATTACK_FRAMES,ATTACK_FRAME_DURATION,false);
        attackUp    = buildRowAnim(sheetUp,ATTACK_ROW,ATTACK_FRAMES,ATTACK_FRAME_DURATION,false);
        attackLeft  = buildRowAnim(sheetLeft,ATTACK_ROW,ATTACK_FRAMES,ATTACK_FRAME_DURATION,false);
        attackRight = buildRowAnim(sheetRight,ATTACK_ROW,ATTACK_FRAMES,ATTACK_FRAME_DURATION,false);
    }

    // Nearest filtering
    private static void setNearest(Texture t) {
        t.setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest);
    }

    // Build animation from sheet row
    private static Animation<TextureRegion> buildRowAnim(Texture sheet, int row, int frames, float frameDuration, boolean loop) {
        int frameWidth  = sheet.getWidth() / SHEET_COLS;
        int frameHeight = sheet.getHeight() / SHEET_ROWS;

        Array<TextureRegion> regions = new Array<>(frames);
        for(int col = 0; col < frames; col++) {
            regions.add(new TextureRegion(
                    sheet,
                    col * frameWidth,
                    row * frameHeight,
                    frameWidth,
                    frameHeight
            ));
        }

        Animation<TextureRegion> anim = new Animation<>(frameDuration, regions);
        anim.setPlayMode(loop ? Animation.PlayMode.LOOP : Animation.PlayMode.NORMAL);
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

    // Main behavior update
    @Override
    protected void updateBehavior(float deltaTime, Maze maze) {

        if(isPlayerInAttackRangeDirectional()) {

            if(!attacking) {
                attacking = true;
                attackTimer = 0f;
            }

            attackTimer += deltaTime;
            if(getAttackAnimation().isAnimationFinished(attackTimer)) {
                attackTimer = 0f;
            }

            if(isPlayerTooCloseDirectional()) {
                stepBackFromPlayer(deltaTime,maze);
            }

            return;
        }

        if(attacking) {
            attacking = false;
            attackTimer = 0f;
            animationTimer = 0f;
        }

        animationTimer += deltaTime;
        defaultPatrol.execute(this,deltaTime,maze);
        updateDirectionFromMovement();
    }

    // Update facing from movement
    private void updateDirectionFromMovement() {
        Vector2 pos = getPosition();
        float dx = pos.x - lastPosition.x;
        float dy = pos.y - lastPosition.y;

        if(Math.abs(dx) > Math.abs(dy)) {
            if(dx > 0) currentDirection = Direction.RIGHT;
            else currentDirection = Direction.LEFT;
        } else {
            if(dy > 0) currentDirection = Direction.UP;
            else currentDirection = Direction.DOWN;
        }

        lastPosition.set(pos);
    }

    // Get current sprite frame
    public TextureRegion getCurrentFrame() {
        if(attacking) {
            return getAttackAnimation().getKeyFrame(attackTimer,false);
        }

        switch(currentDirection) {
            case LEFT:  return walkLeft.getKeyFrame(animationTimer,true);
            case RIGHT: return walkRight.getKeyFrame(animationTimer,true);
            case UP:    return walkUp.getKeyFrame(animationTimer,true);
            default:    return walkDown.getKeyFrame(animationTimer,true);
        }
    }

    // Step back when too close
    private void stepBackFromPlayer(float deltaTime,Maze maze) {
        Vector2 pos = getPosition();
        Vector2 playerPos = getPlayerPosition();

        Vector2 dir = new Vector2();

        switch(currentDirection) {
            case LEFT:  dir.set(-1,0); break;
            case RIGHT: dir.set(1,0);  break;
            case UP:    dir.set(0,1);  break;
            case DOWN:  dir.set(0,-1); break;
        }

        Vector2 away = pos.cpy().sub(playerPos);
        if(away.dot(dir) < 0) {
            dir.scl(-1);
        }

        dir.nor().scl(getSpeed());

        getCollisionHandler().move(
                pos,
                dir,
                deltaTime,
                maze
        );
    }

    // Get attack animation
    private Animation<TextureRegion> getAttackAnimation() {
        switch(getAttackDirection()) {
            case LEFT:  return attackLeft;
            case RIGHT: return attackRight;
            case UP:    return attackUp;
            default:    return attackDown;
        }
    }

    private boolean isPlayerInAttackRangeDirectional() {
        Vector2 pos = getPosition();
        Vector2 p   = getPlayerPosition();

        float dx = Math.abs(p.x - pos.x);
        float dy = Math.abs(p.y - pos.y);

        switch(getAttackDirection()) {
            case LEFT:
            case RIGHT:
                return dx <= ATTACK_RANGE_X && dy <= ATTACK_RANGE_Y * 0.6f;
            case UP:
            case DOWN:
                return dy <= ATTACK_RANGE_Y && dx <= ATTACK_RANGE_X * 0.6f;
        }
        return false;
    }

    private boolean isPlayerTooCloseDirectional() {
        Vector2 pos = getPosition();
        Vector2 p   = getPlayerPosition();

        float dx = Math.abs(p.x - pos.x);
        float dy = Math.abs(p.y - pos.y);

        switch(getAttackDirection()) {
            case LEFT:
            case RIGHT:
                return dx <= MIN_SAFE_RANGE_X;
            case DOWN:
                return dy <= MIN_SAFE_RANGE_DOWN;
            case UP:
                return dy <= MIN_SAFE_RANGE_UP;
        }
        return false;
    }

    // Decide attack direction
    private Direction getAttackDirection() {
        Vector2 pos = getPosition();
        Vector2 p = getPlayerPosition();

        float dx = p.x - pos.x;
        float dy = p.y - pos.y;

        if(Math.abs(dx) > Math.abs(dy)) {
            if(dx > 0) return Direction.RIGHT;
            return Direction.LEFT;
        } else {
            if(dy > 0) return Direction.UP;
            return Direction.DOWN;
        }
    }

    @Override
    public float getRenderWidth() {
        return 48f;
    }

    @Override
    public float getRenderHeight() {
        return 48f;
    }

    @Override
    public float getAttackRadius() {
        return Math.max(ATTACK_RANGE_X, ATTACK_RANGE_Y);
    }

    @Override
    public float getSeparationRadius() {
        return 20f;
    }
}
