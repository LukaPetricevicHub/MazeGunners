package de.tum.cit.fop.maze.enemies;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.audio.Sound;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Animation;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.Array;
import de.tum.cit.fop.maze.behavior.BehaviorState;
import de.tum.cit.fop.maze.behavior.PatrolBehavior;
import de.tum.cit.fop.maze.map.Maze;
import de.tum.cit.fop.maze.player.Player;

/**
 * Enemy that patrols a fixed path and attacks the player
 * when they enter its directional attack range.
 *
 * Uses directional animations for walking, attacking, and dying.
 */
public class RobotGuard extends Enemy {

    /** Maximum HP for this enemy type. */
    private static final int MAX_HP = 15;

    /** Render width in world units. */
    public static final float RENDER_WIDTH  = 32f;
    /** Render height in world units. */
    public static final float RENDER_HEIGHT = 32f;

    /** Number of walk frames. */
    private static final int WALK_FRAMES = 3;
    /** Number of attack frames. */
    private static final int ATTACK_FRAMES = 5;

    /** Walk animation frame duration. */
    private static final float WALK_FRAME_DURATION = 0.15f;
    /** Attack animation frame duration. */
    private static final float ATTACK_FRAME_DURATION = 0.12f;

    /** Horizontal attack range. */
    private static final float ATTACK_RANGE_X = 24f;
    /** Vertical attack range. */
    private static final float ATTACK_RANGE_Y = 36f;

    /** Minimum safe horizontal range. */
    private static final float MIN_SAFE_RANGE_X = 12f;
    /** Minimum safe downward range. */
    private static final float MIN_SAFE_RANGE_DOWN = 18f;
    /** Minimum safe upward range. */
    private static final float MIN_SAFE_RANGE_UP   = 24f;

    /** Sprite sheet columns. */
    private static final int SHEET_COLS = 5;
    /** Sprite sheet rows. */
    private static final int SHEET_ROWS = 22;

    /** Row index for walk frames. */
    private static final int WALK_ROW = 4;
    /** Row index for attack frames. */
    private static final int ATTACK_ROW = 6;
    /** Row index for death frames. */
    private static final int DEATH_ROW = 7;
    /** Number of death frames. */
    private static final int DEATH_FRAMES = 5;
    /** Death animation frame duration. */
    private static final float DEATH_FRAME_DURATION = 0.18f;

    /** Attack sound effect. */
    private static Sound attackSound;
    /** Whether the attack sound has played this attack. */
    private boolean attackSoundPlayed = false;

    /** Cardinal directions for animation selection. */
    private enum Direction { DOWN, LEFT, RIGHT, UP }

    /** Current facing direction. */
    private Direction currentDirection = Direction.DOWN;

    /** Default patrol behavior. */
    private final PatrolBehavior defaultPatrol;

    /** Down-facing sprite sheet. */
    private static Texture sheetDown;
    /** Up-facing sprite sheet. */
    private static Texture sheetUp;
    /** Left-facing sprite sheet. */
    private static Texture sheetLeft;
    /** Right-facing sprite sheet. */
    private static Texture sheetRight;

    /** Walk animation facing down. */
    private static Animation<TextureRegion> walkDown;
    /** Walk animation facing up. */
    private static Animation<TextureRegion> walkUp;
    /** Walk animation facing left. */
    private static Animation<TextureRegion> walkLeft;
    /** Walk animation facing right. */
    private static Animation<TextureRegion> walkRight;

    /** Attack animation facing down. */
    private static Animation<TextureRegion> attackDown;
    /** Attack animation facing up. */
    private static Animation<TextureRegion> attackUp;
    /** Attack animation facing left. */
    private static Animation<TextureRegion> attackLeft;
    /** Attack animation facing right. */
    private static Animation<TextureRegion> attackRight;

    /** Death animation facing down. */
    private static Animation<TextureRegion> deathDown;
    /** Death animation facing up. */
    private static Animation<TextureRegion> deathUp;
    /** Death animation facing left. */
    private static Animation<TextureRegion> deathLeft;
    /** Death animation facing right. */
    private static Animation<TextureRegion> deathRight;


    /** Whether the guard is attacking. */
    private boolean attacking = false;
    /** Attack animation timer. */
    private float attackTimer = 0f;
    /** Movement animation timer. */
    private float animationTimer = 0f;
    /** Whether death animation is playing. */
    private boolean deathAnimating = false;
    /** Death animation timer. */
    private float deathTimer = 0f;


    /** Last position used to infer facing. */
    private final Vector2 lastPosition = new Vector2();

    /**
     * Creates a RobotGuard enemy.
     *
     * @param startWorldPosition initial world position
     * @param movementSpeed movement speed of the enemy
     * @param patrolWorldPositions patrol path points
     * @param targetPlayer player to attack
     */
    public RobotGuard(Vector2 startWorldPosition, float movementSpeed, Array<Vector2> patrolWorldPositions, Player targetPlayer) {
        super(startWorldPosition, movementSpeed, new PatrolBehavior(patrolWorldPositions), targetPlayer);
        initHp(MAX_HP);

        defaultPatrol = (PatrolBehavior) activeBehavior;
        chaseRadius = 0f;

        lastPosition.set(startWorldPosition);
        ensureAnimationsLoaded();
    }


    /**
     * Lazily loads animations and sounds for the robot guard.
     */
    private static void ensureAnimationsLoaded() {
        if (walkDown != null) return;

        sheetDown = new Texture("Enemies/RobotGuard/RobotGuardDown.png");
        sheetUp = new Texture("Enemies/RobotGuard/RobotGuardUp.png");
        sheetLeft = new Texture("Enemies/RobotGuard/RobotGuardLeft.png");
        sheetRight = new Texture("Enemies/RobotGuard/RobotGuardRight.png");

        setNearest(sheetDown);
        setNearest(sheetUp);
        setNearest(sheetLeft);
        setNearest(sheetRight);

        walkDown = buildRowAnim(sheetDown, WALK_ROW, 0, WALK_FRAMES, WALK_FRAME_DURATION, true);
        walkUp = buildRowAnim(sheetUp, WALK_ROW, 0, WALK_FRAMES, WALK_FRAME_DURATION, true);
        walkLeft = buildRowAnim(sheetLeft, WALK_ROW, 0, WALK_FRAMES, WALK_FRAME_DURATION, true);
        walkRight = buildRowAnim(sheetRight,WALK_ROW, 0, WALK_FRAMES, WALK_FRAME_DURATION, true);


        attackDown = buildRowAnim(sheetDown, ATTACK_ROW, 0, ATTACK_FRAMES, ATTACK_FRAME_DURATION, false);
        attackUp = buildRowAnim(sheetUp, ATTACK_ROW, 0, ATTACK_FRAMES, ATTACK_FRAME_DURATION, false);
        attackLeft = buildRowAnim(sheetLeft, ATTACK_ROW, 0, ATTACK_FRAMES, ATTACK_FRAME_DURATION, false);
        attackRight = buildRowAnim(sheetRight,ATTACK_ROW, 0, ATTACK_FRAMES, ATTACK_FRAME_DURATION, false);


        attackSound = Gdx.audio.newSound(Gdx.files.internal("Enemies/RobotGuard/RobotGuardAttack.wav"));

        deathDown = buildRowAnim(sheetDown, DEATH_ROW, 0, 1, DEATH_FRAME_DURATION, false);
        deathUp = buildRowAnim(sheetUp, DEATH_ROW, 0, 1, DEATH_FRAME_DURATION, false);
        deathLeft = buildRowAnim(sheetLeft, DEATH_ROW, 0, 1, DEATH_FRAME_DURATION, false);
        deathRight = buildRowAnim(sheetRight, DEATH_ROW, 0, 1, DEATH_FRAME_DURATION, false);



    }

    /**
     * Sets texture filtering to nearest.
     *
     * @param t Texture to update
     */
    private static void setNearest(Texture t) {
        t.setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest);
    }


    /**
     * Builds an animation from a sprite sheet row.
     *
     * @param sheet Sprite sheet texture
     * @param row Row index
     * @param startCol Starting column
     * @param frames Number of frames
     * @param frameDuration Frame duration
     * @param loop Whether the animation loops
     * @return Built animation
     */
    private static Animation<TextureRegion> buildRowAnim(Texture sheet, int row, int startCol, int frames, float frameDuration, boolean loop) {
        int frameWidth  = sheet.getWidth() / SHEET_COLS;
        int frameHeight = sheet.getHeight() / SHEET_ROWS;

        Array<TextureRegion> regions = new Array<>(frames);
        for (int i = 0; i < frames; i++) {
            int col = startCol + i;
            regions.add(new TextureRegion(sheet, col * frameWidth, row * frameHeight, frameWidth, frameHeight));
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

    /**
     * Updates movement, attacks, animations, and death logic.
     *
     * @param deltaTime time passed since last frame
     * @param maze current maze for collision handling
     */
    @Override
    protected void updateBehavior(float deltaTime, Maze maze) {

        if (dying) {
            deathTimer += deltaTime;

            if (getDeathAnimation().isAnimationFinished(deathTimer)) {
                finalizeDeath();
            }
            return;
        }


        if (isPlayerInAttackRangeDirectional()) {

            if (!attacking) {
                attacking = true;
                attackTimer = 0f;

                if(attackSound != null && !attackSoundPlayed) {
                    attackSound.play(0.8f * getSfxVolume());
                    attackSoundPlayed = true;
                }
            }


            attackTimer += deltaTime;
            if (getAttackAnimation().isAnimationFinished(attackTimer)) {
                attackTimer = 0f;
                attackSoundPlayed = false;
            }


            if (isPlayerTooCloseDirectional())
                stepBackFromPlayer(deltaTime, maze);
            return;
        }

        if (attacking) {
            attacking = false;
            attackTimer = 0f;
            animationTimer = 0f;
            attackSoundPlayed = false;
        }


        animationTimer += deltaTime;
        defaultPatrol.execute(this, deltaTime, maze);
        updateDirectionFromMovement();
    }

    // Update facing from movement
    private void updateDirectionFromMovement() {
        Vector2 pos = getPosition();
        float dx = pos.x - lastPosition.x;
        float dy = pos.y - lastPosition.y;

        if(Math.abs(dx) > Math.abs(dy)) {
            currentDirection = (dx > 0) ? Direction.RIGHT : Direction.LEFT;
        } else{
            currentDirection = (dy > 0) ? Direction.UP : Direction.DOWN;
        }

        lastPosition.set(pos);
    }

    /**
     * Returns the current animation frame based on state and direction.
     *
     * @return texture region to render
     */
    public TextureRegion getCurrentFrame() {

        if (dying){
            return getDeathAnimation().getKeyFrame(deathTimer, false);
        }

        if (attacking) {
            return getAttackAnimation().getKeyFrame(attackTimer, false);
        }

        switch (currentDirection) {
            case LEFT:  return walkLeft.getKeyFrame(animationTimer, true);
            case RIGHT: return walkRight.getKeyFrame(animationTimer, true);
            case UP:    return walkUp.getKeyFrame(animationTimer, true);
            default:    return walkDown.getKeyFrame(animationTimer, true);
        }
    }



    /**
     * Steps back from the player when too close.
     *
     * @param deltaTime Time since last frame
     * @param maze Maze for collision checks
     */
    private void stepBackFromPlayer(float deltaTime, Maze maze) {
        Vector2 pos = getPosition();
        Vector2 playerPos = getPlayerPosition();

        Vector2 dir = new Vector2();

        switch (currentDirection) {
            case LEFT:  dir.set(-1, 0); break;
            case RIGHT: dir.set(1, 0);  break;
            case UP:    dir.set(0, 1);  break;
            case DOWN:  dir.set(0, -1); break;
        }



        Vector2 away = pos.cpy().sub(playerPos);
        if (away.dot(dir) < 0) {
            dir.scl(-1);
        }

        dir.nor().scl(getSpeed());

        getCollisionHandler().move(pos, dir, deltaTime, maze);
    }


    /**
     * Returns the attack animation for the current attack direction.
     *
     * @return Attack animation
     */
    private Animation<TextureRegion> getAttackAnimation() {
        switch (getAttackDirection()) {
            case LEFT:  return attackLeft;
            case RIGHT: return attackRight;
            case UP:    return attackUp;
            default:    return attackDown;
        }
    }

    /**
     * Returns the death animation for the current facing direction.
     *
     * @return Death animation
     */
    private Animation<TextureRegion> getDeathAnimation() {
        switch (currentDirection) {
            case LEFT:  return deathLeft;
            case RIGHT: return deathRight;
            case UP:    return deathUp;
            default:    return deathDown;
        }
    }


    private boolean isPlayerInAttackRangeDirectional() {
        Vector2 pos = getPosition();
        Vector2 p   = getPlayerPosition();

        float dx = Math.abs(p.x - pos.x);
        float dy = Math.abs(p.y - pos.y);

        switch (getAttackDirection()) {
            case LEFT:
            case RIGHT: return dx <= ATTACK_RANGE_X && dy <= ATTACK_RANGE_Y * 0.6f;
            case UP:
            case DOWN: return dy <= ATTACK_RANGE_Y && dx <= ATTACK_RANGE_X * 0.6f;
        }
        return false;
    }

    /**
     * Checks if the player is too close based on facing direction.
     *
     * @return True if too close
     */
    private boolean isPlayerTooCloseDirectional() {
        Vector2 pos = getPosition();
        Vector2 p   = getPlayerPosition();

        float dx = Math.abs(p.x - pos.x);
        float dy = Math.abs(p.y - pos.y);

        switch (getAttackDirection()) {
            case LEFT:
            case RIGHT: return dx <= MIN_SAFE_RANGE_X;
            case DOWN: return dy <= MIN_SAFE_RANGE_DOWN;
            case UP: return dy <= MIN_SAFE_RANGE_UP;
        }
        return false;
    }


    /**
     * Determines attack direction based on player position.
     *
     * @return Direction to attack
     */
    private Direction getAttackDirection() {
        Vector2 pos = getPosition();
        Vector2 p = getPlayerPosition();

        float dx = p.x - pos.x;
        float dy = p.y - pos.y;

        if (Math.abs(dx) > Math.abs(dy)) {
            return (dx > 0) ? Direction.RIGHT : Direction.LEFT;
        } else {
            return (dy > 0) ? Direction.UP : Direction.DOWN;
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

    /**
     * Puts the enemy into the dying state.
     *
     * Starts the death animation, stops attacking,
     * and locks the facing direction for rendering.
     */
    @Override
    protected void die() {
        if (dying) return;

        dying = true;
        deathTimer = 0f;
        currentDirection = getAttackDirection();

        attacking = false;
        attackSoundPlayed = false;
    }

    /**
     * Retrieves SFX volume from preferences.
     *
     * @return SFX volume
     */
    private float getSfxVolume() {
        if (Gdx.app == null) return 1f;
        return Gdx.app.getPreferences("maze_runner_prefs").getFloat("sound_vol", 0.5f);
    }

}
