package de.tum.cit.fop.maze.enemies;

import com.badlogic.gdx.graphics.g2d.Animation;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.audio.Sound;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;
import de.tum.cit.fop.maze.map.Maze;
import de.tum.cit.fop.maze.map.Room;

import java.util.Random;

/**
 * Represents the boss enemy of the game.
 *
 * Handles movement, attacks, phases, health and rendering.
 * The boss reacts to the player's position and changes its
 * behavior based on remaining health.
 */
public class Boss {

    private final float TILE;

    private final float DRAW_W; 
    private final float DRAW_H; 

    
    private final float HIT_W;  
    private final float HIT_H;  

    
    private static final int MAX_HP = 250;
    private static final float DAMAGE_TAKEN_MULT = 0.5f;
    private static final float RUN2_HP_PCT = 0.25f;


    
    private static final float WALK_SPEED_TPS = 2.8f;   
    private static final float RUN_SPEED_TPS  = 5.0f;
    private static final float RUN2_SPEED_TPS = 6.6f;

    private static final float COMBO_SPEED_TPS = 11.25f;

    private static final float ATTACK_START_DIST_T = 6.5f; 
    private static final float COMBO_START_DIST_T  = 9.0f; 

    private static final int ATTACK_DMG = 10;
    private static final int COMBO_DMG  = 14;

    private static final float ATTACK_WINDUP_T = 0.30f;
    private static final float ATTACK_ACTIVE_T = 0.25f;
    private static final float HIT_STUN_T      = 0.25f;

    private static final float ATTACK_CD = 0.9f;
    private static final float COMBO_CD  = 2.5f;

    
    private static final float COMBO_OVERSHOOT_T = 12f; 
    private static final float COMBO_MAX_TIME = 0.75f;  
    private static final float COMBO_STOP_DIST_T = 1.0f;

    
    private final Maze maze;
    private final Room bossRoom;
    private final BossAnimations anims;

    private Sound roarSound;
    private Sound crySound;
    private Sound attack1Sound;
    private Sound attack2Sound;
    private boolean roarPlayed = false;
    private boolean cryPlayed = false;

    private final Vector2 pos = new Vector2(); 
    private final Vector2 vel = new Vector2();
    private final Rectangle hitbox = new Rectangle();

    private BossState state = BossState.IDLE;
    private Facing facing = Facing.DOWN;
    private float stateTime = 0f;

    private int hp = MAX_HP;
    private float damageMult = 1f;

    private float attackCd = 0f;
    private float comboCd  = 0f;

    private boolean attackDamageApplied = false;
    private boolean comboHitApplied = false;

    private final Random rng = new Random();

    
    private final Vector2 comboTarget = new Vector2();

    
    private final Vector2 tmp1 = new Vector2();
    private final Vector2 tmp2 = new Vector2();
    private final Rectangle tmpRect1 = new Rectangle();
    private final Rectangle tmpRect2 = new Rectangle();

    /**
     * Initialize a new boss instance.
     *
     * @param maze the maze the boss is placed in
     * @param bossRoom the room in which the boss spawns
     * @param anims animations used for rendering the boss
     * @param spawnWorldX spawn x position in world coordinates
     * @param spawnWorldY spawn y position in world coordinates
     */
    public Boss(Maze maze, Room bossRoom, BossAnimations anims, float spawnWorldX, float spawnWorldY) {
        this.maze = maze;
        this.bossRoom = bossRoom;
        this.anims = anims;

        this.TILE = maze.getTileSize();

        this.DRAW_W = 10f * TILE;
        this.DRAW_H = 10f * TILE;

        this.HIT_W = 3f * TILE;
        this.HIT_H = 3f * TILE;

        this.pos.set(spawnWorldX, spawnWorldY);

        this.hitbox.set(
                pos.x - HIT_W / 2f,
                pos.y - HIT_H / 2f,
                HIT_W,
                HIT_H
        );

        if (Gdx.files.internal("bossroar.mp3").exists()) {
            roarSound = Gdx.audio.newSound(Gdx.files.internal("bossroar.mp3"));
        }
        if (Gdx.files.internal("bosscry.mp3").exists()) {
            crySound = Gdx.audio.newSound(Gdx.files.internal("bosscry.mp3"));
        }
        if (Gdx.files.internal("swordattack1.mp3").exists()) {
            attack1Sound = Gdx.audio.newSound(Gdx.files.internal("swordattack1.mp3"));
        }
        if (Gdx.files.internal("swordattack2.mp3").exists()) {
            attack2Sound = Gdx.audio.newSound(Gdx.files.internal("swordattack2.mp3"));
        }
    }

    /**
     * Returns whether the boss is fully dead.
     *
     * @return true if the death animation has finished
     */
    public boolean isDead() {
        return state == BossState.DEATH && anims.death.isAnimationFinished(stateTime);
    }


    /**
     * Retrieves a copy of the boss hitbox.
     *
     * @return hitbox rectangle
     */
    public Rectangle getHitbox() {
        return new Rectangle(hitbox);
    }

    /**
     * Retrieves a copy of the boss position.
     *
     * @return position vector
     */
    public Vector2 getPosition() {
        return new Vector2(pos);
    }

    /**
     * Retrieves current boss health.
     *
     * @return current HP value
     */
    public int getHp() {
        return hp;
    }

    /**
     * Retrieves maximum boss health.
     *
     * @return max HP value
     */
    public int getMaxHp() {
        return MAX_HP;
    }

    /**
     * Scales the boss health by a multiplier.
     *
     * @param mult health multiplier
     */
    public void scaleHp(float mult) {
        if (mult <= 1f) return;
        hp = Math.max(1, Math.round(hp * mult));
    }

    /**
     * Computes the boss health percentage.
     *
     * @return health ratio in [0, 1]
     */
    public float getHpPct() {
        return (MAX_HP <= 0) ? 0f : (hp / (float) MAX_HP);
    }

    /**
     * Applies damage to the boss.
     *
     * @param dmg incoming damage amount
     */
    public void damage(int dmg) {
        if (state == BossState.DEATH) return;

        int scaled = Math.max(1, Math.round(dmg * DAMAGE_TAKEN_MULT));
        hp -= scaled;
        if (hp <= 0) {
            hp = 0;
            if (!cryPlayed && crySound != null) {
                crySound.play(getSfxVolume());
                cryPlayed = true;
            }
            state = BossState.DEATH;
            stateTime = 0f;
            vel.setZero();
            return;
        }

        
        state = BossState.HIT;
        stateTime = 0f;
        vel.setZero();
    }

    /**
     * Updates the boss logic.
     *
     * @param dt time passed since last frame
     * @param player the player instance
     */
    public void update(float dt, PlayerLike player) {
        if (state == BossState.DEATH) {
            stateTime += dt;
            return;
        }

        boolean inRoom = isPlayerInBossRoom(player);
        if (inRoom && !roarPlayed && roarSound != null) {
            roarSound.play(getSfxVolume());
            roarPlayed = true;
        }
        if (!inRoom) {
            vel.setZero();
            state = BossState.IDLE;
            stateTime = 0f;
            return;
        }

        stateTime += dt;

        attackCd = Math.max(0f, attackCd - dt);
        comboCd  = Math.max(0f, comboCd - dt);

        
        tmp1.set(player.getPosition()).sub(pos);
        updateFacing(tmp1);

        ensureRunStateByHp();

        switch (state) {
            case IDLE -> updateIdle(player);
            case WALK -> updateWalk(dt, player);
            case RUN -> updateRun(dt, player, RUN_SPEED_TPS * TILE);
            case RUN_2 -> updateRun(dt, player, RUN2_SPEED_TPS * TILE);
            case ATTACK -> updateAttack(player);
            case COMBO -> updateCombo(dt, player);
            case HIT -> updateHit();
            case DEATH -> { }
        }
    }

    /**
     * Renders the boss.
     *
     * @param batch sprite batch used for drawing
     */
    public void render(Batch batch) {
        Animation<TextureRegion> a = currentAnim();

        boolean loop = switch (state) {
            case IDLE, WALK, RUN, RUN_2 -> true;
            case ATTACK, COMBO, HIT, DEATH -> false;
        };

        TextureRegion frame = a.getKeyFrame(stateTime, loop);

        batch.draw(frame, pos.x - DRAW_W / 2f, pos.y - DRAW_H / 2f, DRAW_W, DRAW_H
        );
    }


    /**
     * Updates boss behavior in the idle state.
     *
     * @param player target player reference
     */
    private void updateIdle(PlayerLike player) {
        
        state = (getHpPct() <= RUN2_HP_PCT) ? BossState.RUN_2 : BossState.RUN;
        stateTime = 0f;
    }

    /**
     * Updates boss behavior while walking.
     *
     * @param dt time step in seconds
     * @param player target player reference
     */
    private void updateWalk(float dt, PlayerLike player) {
        float dist = tmp1.set(player.getPosition()).sub(pos).len();

        if (dist <= ATTACK_START_DIST_T * TILE && attackCd <= 0f) {
            startAttack();
            return;
        }

        
        moveToward(dt, player.getPosition(), WALK_SPEED_TPS * TILE);

        
        if (dist > 7.5f * TILE) {
            state = (getHpPct() <= RUN2_HP_PCT) ? BossState.RUN_2 : BossState.RUN;
            stateTime = 0f;
        }
    }

    /**
     * Updates boss behavior while running.
     *
     * @param dt time step in seconds
     * @param player target player reference
     * @param runSpeed speed in world units per second
     */
    private void updateRun(float dt, PlayerLike player, float runSpeed) {
        float dist = tmp1.set(player.getPosition()).sub(pos).len();

        if (dist <= ATTACK_START_DIST_T * TILE && attackCd <= 0f) {
            startAttack();
            return;
        }

        
        if (dist >= COMBO_START_DIST_T * TILE && comboCd <= 0f && rng.nextFloat() < 0.015f) {
            startComboDashTowardPlayer(player);
            return;
        }

        moveToward(dt, player.getPosition(), runSpeed);

        
        if (dist < 6.8f * TILE) {
            state = BossState.WALK;
            stateTime = 0f;
        }
    }

    /**
     * Transitions the boss into the attack state.
     */
    private void startAttack() {
        state = BossState.ATTACK;
        stateTime = 0f;
        vel.setZero();
        attackDamageApplied = false;
        if (attack1Sound != null) attack1Sound.play(getSfxVolume());
    }

    /**
     * Updates the attack state and applies damage during the active window.
     *
     * @param player target player reference
     */
    private void updateAttack(PlayerLike player) {
        
        if (stateTime < ATTACK_WINDUP_T) return;

        
        if (!attackDamageApplied && stateTime >= ATTACK_WINDUP_T + 0.05f) {
            attackDamageApplied = true;

            Rectangle atk = attackRect();
            if (atk.overlaps(player.getHitbox())) {
                player.damage(Math.max(1, Math.round(ATTACK_DMG * damageMult)));
            }
        }

        
        if (stateTime >= ATTACK_WINDUP_T + ATTACK_ACTIVE_T) {
            attackCd = ATTACK_CD;
            
            state = (getHpPct() <= RUN2_HP_PCT) ? BossState.RUN_2 : BossState.RUN;
            stateTime = 0f;
        }
    }

    /**
     * Starts a combo dash aimed past the player.
     *
     * @param player target player reference
     */
    private void startComboDashTowardPlayer(PlayerLike player) {
        state = BossState.COMBO;
        stateTime = 0f;
        comboHitApplied = false;
        if (attack2Sound != null) attack2Sound.play(getSfxVolume());

        Vector2 playerPos = player.getPosition();
        Vector2 dir = tmp1.set(playerPos).sub(pos);
        if (dir.len2() < 1f) dir.set(1, 0);
        dir.nor();

        
        comboTarget.set(playerPos).mulAdd(dir, COMBO_OVERSHOOT_T * TILE);
        clampToBossRoom(comboTarget);

        vel.set(tmp2.set(comboTarget).sub(pos).nor().scl(COMBO_SPEED_TPS * TILE));
    }

    /**
     * Updates the combo dash movement and hit detection.
     *
     * @param dt time step in seconds
     * @param player target player reference
     */
    private void updateCombo(float dt, PlayerLike player) {
        moveWithCollision(dt, vel);

        if (!comboHitApplied && hitbox.overlaps(player.getHitbox())) {
            comboHitApplied = true;
            player.damage(Math.max(1, Math.round(COMBO_DMG * damageMult)));
        }

        boolean reached = pos.dst2(comboTarget) < (COMBO_STOP_DIST_T * TILE) * (COMBO_STOP_DIST_T * TILE);
        boolean timedOut = stateTime >= COMBO_MAX_TIME;

        if (reached || timedOut) {
            vel.setZero();
            comboCd = COMBO_CD;
            state = (getHpPct() <= RUN2_HP_PCT) ? BossState.RUN_2 : BossState.RUN;
            stateTime = 0f;
        }
    }

    /**
     * Updates the hit-stun state.
     */
    private void updateHit() {
        if (stateTime >= HIT_STUN_T) {
            state = (getHpPct() <= RUN2_HP_PCT) ? BossState.RUN_2 : BossState.RUN;
            stateTime = 0f;
        }
    }

    /**
     * Ensures the run state matches the current health phase.
     */
    private void ensureRunStateByHp() {
        boolean low = getHpPct() <= RUN2_HP_PCT;
        if (state == BossState.WALK || state == BossState.RUN || state == BossState.RUN_2) {
            BossState desired = low ? BossState.RUN_2 : BossState.RUN;
            if (state != desired) {
                state = desired;
                stateTime = 0f;
            }
        }
    }

    /**
     * Builds the attack hit rectangle based on facing.
     *
     * @return attack rectangle in world coordinates
     */
    private Rectangle attackRect() {
        float w = 4f * TILE;
        float h = 3f * TILE;
        float cx = pos.x, cy = pos.y;

        return switch (facing) {
            case UP -> new Rectangle(cx - w / 2f, cy + HIT_H / 2f, w, h);
            case DOWN -> new Rectangle(cx - w / 2f, cy - HIT_H / 2f - h, w, h);
            case LEFT -> new Rectangle(cx - HIT_W / 2f - w, cy - h / 2f, w, h);
            case RIGHT -> new Rectangle(cx + HIT_W / 2f, cy - h / 2f, w, h);
        };
    }

    /**
     * Moves the boss toward a target position with collision.
     *
     * @param dt time step in seconds
     * @param target target world position
     * @param speed movement speed in world units per second
     */
    private void moveToward(float dt, Vector2 target, float speed) {
        Vector2 d = tmp1.set(target).sub(pos);
        if (d.len2() < 1f) {
            vel.setZero();
            return;
        }
        d.nor().scl(speed);
        vel.set(d);
        moveWithCollision(dt, vel);
    }

    /**
     * Applies velocity with collision checks against walls.
     *
     * @param dt time step in seconds
     * @param velocity velocity vector in world units per second
     */
    private void moveWithCollision(float dt, Vector2 velocity) {
        float nx = pos.x + velocity.x * dt;
        float ny = pos.y + velocity.y * dt;

        
        tmpRect1.set(hitbox);
        tmpRect1.x = (nx - HIT_W / 2f);
        if (!maze.isWallRect(tmpRect1)) {
            pos.x = nx;
            hitbox.x = tmpRect1.x;
        }

        
        tmpRect2.set(hitbox);
        tmpRect2.y = (ny - HIT_H / 2f);
        if (!maze.isWallRect(tmpRect2)) {
            pos.y = ny;
            hitbox.y = tmpRect2.y;
        }
    }

    /**
     * Selects the animation corresponding to the current state.
     *
     * @return animation to render
     */
    private Animation<TextureRegion> currentAnim() {
        return switch (state) {
            case IDLE -> anims.idle;
            case WALK -> anims.walk;
            case RUN -> anims.run;
            case RUN_2 -> anims.run2;
            case ATTACK -> anims.attack;
            case COMBO -> anims.combo;
            case HIT -> anims.hit;
            case DEATH -> anims.death;
        };
    }


    /**
     * Updates boss facing based on a direction vector.
     *
     * @param toPlayer vector from boss to player
     */
    private void updateFacing(Vector2 toPlayer) {
        if (Math.abs(toPlayer.x) > Math.abs(toPlayer.y)) {
            facing = (toPlayer.x >= 0) ? Facing.RIGHT : Facing.LEFT;
        } else {
            facing = (toPlayer.y >= 0) ? Facing.UP : Facing.DOWN;
        }
    }

    /**
     * Clamps a position inside the boss room bounds with padding.
     *
     * @param worldPos world position to clamp
     */
    private void clampToBossRoom(Vector2 worldPos) {
        int padTiles = 5;

        float minX = (bossRoom.x() + padTiles) * TILE;
        float minY = (bossRoom.y() + padTiles) * TILE;
        float maxX = (bossRoom.x() + bossRoom.w() - padTiles) * TILE;
        float maxY = (bossRoom.y() + bossRoom.h() - padTiles) * TILE;

        worldPos.x = Math.max(minX, Math.min(maxX, worldPos.x));
        worldPos.y = Math.max(minY, Math.min(maxY, worldPos.y));
    }

    /**
     * Checks whether the player is currently inside the boss room.
     *
     * @param player player reference
     * @return true if the player is within the boss room bounds
     */
    private boolean isPlayerInBossRoom(PlayerLike player) {
        if (bossRoom == null || player == null) return true;
        Vector2 p = player.getPosition();
        float minX = bossRoom.x() * TILE;
        float minY = bossRoom.y() * TILE;
        float maxX = (bossRoom.x() + bossRoom.w()) * TILE;
        float maxY = (bossRoom.y() + bossRoom.h()) * TILE;
        return p.x >= minX && p.x <= maxX && p.y >= minY && p.y <= maxY;
    }

    private float getSfxVolume() {
        if (Gdx.app == null) return 1f;
        return Gdx.app.getPreferences("maze_runner_prefs").getFloat("sound_vol", 0.5f);
    }

    public void dispose() {
        if (roarSound != null) roarSound.dispose();
        if (crySound != null) crySound.dispose();
        if (attack1Sound != null) attack1Sound.dispose();
        if (attack2Sound != null) attack2Sound.dispose();
    }

    public void setDamageMult(float mult) {
        damageMult = Math.max(0.1f, mult);
    }
}
