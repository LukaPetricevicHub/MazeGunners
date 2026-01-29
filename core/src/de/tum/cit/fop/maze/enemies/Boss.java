package de.tum.cit.fop.maze.enemies;

import com.badlogic.gdx.graphics.g2d.Animation;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;
import de.tum.cit.fop.maze.map.Maze;
import de.tum.cit.fop.maze.map.Room;

import java.util.Random;

public class Boss {

    // =========================
    // SIZE (10 x 10 tiles drawn)
    // =========================
    private final float TILE;

    private final float DRAW_W; // 10 tiles
    private final float DRAW_H; // 10 tiles

    // Hitbox smaller for fairness
    private final float HIT_W;  // 7 tiles
    private final float HIT_H;  // 7 tiles

    // =========================
    // HP / PHASE
    // =========================
    private static final int MAX_HP = 250;
    private static final float RUN2_HP_PCT = 0.25f;

    // =========================
    // TUNING
    // =========================
    private static final float WALK_SPEED_TPS = 2.8f;   // tiles per second
    private static final float RUN_SPEED_TPS  = 5.0f;
    private static final float RUN2_SPEED_TPS = 6.6f;

    private static final float COMBO_SPEED_TPS = 11.25f;

    private static final float ATTACK_START_DIST_T = 6.5f; // tiles
    private static final float COMBO_START_DIST_T  = 9.0f; // tiles

    private static final int ATTACK_DMG = 10;
    private static final int COMBO_DMG  = 14;

    private static final float ATTACK_WINDUP_T = 0.30f;
    private static final float ATTACK_ACTIVE_T = 0.25f;
    private static final float HIT_STUN_T      = 0.25f;

    private static final float ATTACK_CD = 0.9f;
    private static final float COMBO_CD  = 2.5f;

    // Combo dash control
    private static final float COMBO_OVERSHOOT_T = 12f; // tiles beyond player
    private static final float COMBO_MAX_TIME = 0.75f;  // seconds
    private static final float COMBO_STOP_DIST_T = 1.0f;

    // =========================
    // CORE FIELDS
    // =========================
    private final Maze maze;
    private final Room bossRoom;
    private final BossAnimations anims;

    private final Vector2 pos = new Vector2(); // world center
    private final Vector2 vel = new Vector2();
    private final Rectangle hitbox = new Rectangle();

    private BossState state = BossState.IDLE;
    private Facing facing = Facing.DOWN;
    private float stateTime = 0f;

    private int hp = MAX_HP;

    private float attackCd = 0f;
    private float comboCd  = 0f;

    private boolean attackDamageApplied = false;
    private boolean comboHitApplied = false;

    private final Random rng = new Random();

    // Combo dash target
    private final Vector2 comboTarget = new Vector2();

    // temps to avoid allocations
    private final Vector2 tmp1 = new Vector2();
    private final Vector2 tmp2 = new Vector2();
    private final Rectangle tmpRect1 = new Rectangle();
    private final Rectangle tmpRect2 = new Rectangle();

    public Boss(Maze maze, Room bossRoom, BossAnimations anims, float spawnWorldX, float spawnWorldY) {
        this.maze = maze;
        this.bossRoom = bossRoom;
        this.anims = anims;

        this.TILE = maze.getTileSize();

        this.DRAW_W = 10f * TILE;
        this.DRAW_H = 10f * TILE;

        this.HIT_W = 7f * TILE;
        this.HIT_H = 7f * TILE;

        this.pos.set(spawnWorldX, spawnWorldY);

        this.hitbox.set(
                pos.x - HIT_W / 2f,
                pos.y - HIT_H / 2f,
                HIT_W,
                HIT_H
        );
    }

    // =========================
    // PUBLIC API
    // =========================
    public boolean isDead() {
        return state == BossState.DEATH && anims.death.isAnimationFinished(stateTime);
    }


    public Rectangle getHitbox() {
        return new Rectangle(hitbox);
    }

    public Vector2 getPosition() {
        return new Vector2(pos);
    }

    public int getHp() {
        return hp;
    }

    public int getMaxHp() {
        return MAX_HP;
    }

    public float getHpPct() {
        return (MAX_HP <= 0) ? 0f : (hp / (float) MAX_HP);
    }

    public void damage(int dmg) {
        if (state == BossState.DEATH) return;

        hp -= dmg;
        if (hp <= 0) {
            hp = 0;
            state = BossState.DEATH;
            stateTime = 0f;
            vel.setZero();
            return;
        }

        // hit reaction
        state = BossState.HIT;
        stateTime = 0f;
        vel.setZero();
    }

    public void update(float dt, PlayerLike player) {
        if (state == BossState.DEATH) {
            stateTime += dt;
            return;
        }

        stateTime += dt;

        attackCd = Math.max(0f, attackCd - dt);
        comboCd  = Math.max(0f, comboCd - dt);

        // face player
        tmp1.set(player.getPosition()).sub(pos);
        updateFacing(tmp1);

        // phase swap (only if in movement states)
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

    public void render(Batch batch) {
        Animation<TextureRegion> a = currentAnim();

        boolean loop = switch (state) {
            case IDLE, WALK, RUN, RUN_2 -> true;
            case ATTACK, COMBO, HIT, DEATH -> false;
        };

        TextureRegion frame = a.getKeyFrame(stateTime, loop);

        batch.draw(frame,
                pos.x - DRAW_W / 2f,
                pos.y - DRAW_H / 2f,
                DRAW_W,
                DRAW_H
        );
    }


    // =========================
    // STATE LOGIC
    // =========================
    private void updateIdle(PlayerLike player) {
        // immediately start pursuing
        state = (getHpPct() <= RUN2_HP_PCT) ? BossState.RUN_2 : BossState.RUN;
        stateTime = 0f;
    }

    private void updateWalk(float dt, PlayerLike player) {
        float dist = tmp1.set(player.getPosition()).sub(pos).len();

        if (dist <= ATTACK_START_DIST_T * TILE && attackCd <= 0f) {
            startAttack();
            return;
        }

        // slow stalking
        moveToward(dt, player.getPosition(), WALK_SPEED_TPS * TILE);

        // if player is far, go to running chase
        if (dist > 7.5f * TILE) {
            state = (getHpPct() <= RUN2_HP_PCT) ? BossState.RUN_2 : BossState.RUN;
            stateTime = 0f;
        }
    }

    private void updateRun(float dt, PlayerLike player, float runSpeed) {
        float dist = tmp1.set(player.getPosition()).sub(pos).len();

        if (dist <= ATTACK_START_DIST_T * TILE && attackCd <= 0f) {
            startAttack();
            return;
        }

        // combo sometimes when far
        if (dist >= COMBO_START_DIST_T * TILE && comboCd <= 0f && rng.nextFloat() < 0.015f) {
            startComboDashTowardPlayer(player);
            return;
        }

        moveToward(dt, player.getPosition(), runSpeed);

        // if close, switch to walk for nicer motion (unless attacking)
        if (dist < 6.8f * TILE) {
            state = BossState.WALK;
            stateTime = 0f;
        }
    }

    private void startAttack() {
        state = BossState.ATTACK;
        stateTime = 0f;
        vel.setZero();
        attackDamageApplied = false;
    }

    private void updateAttack(PlayerLike player) {
        // windup
        if (stateTime < ATTACK_WINDUP_T) return;

        // damage once
        if (!attackDamageApplied && stateTime >= ATTACK_WINDUP_T + 0.05f) {
            attackDamageApplied = true;

            Rectangle atk = attackRect();
            if (atk.overlaps(player.getHitbox())) {
                player.damage(ATTACK_DMG);
            }
        }

        // end
        if (stateTime >= ATTACK_WINDUP_T + ATTACK_ACTIVE_T) {
            attackCd = ATTACK_CD;
            // return to chase (phase-aware)
            state = (getHpPct() <= RUN2_HP_PCT) ? BossState.RUN_2 : BossState.RUN;
            stateTime = 0f;
        }
    }

    private void startComboDashTowardPlayer(PlayerLike player) {
        state = BossState.COMBO;
        stateTime = 0f;
        comboHitApplied = false;

        Vector2 playerPos = player.getPosition();
        Vector2 dir = tmp1.set(playerPos).sub(pos);
        if (dir.len2() < 1f) dir.set(1, 0);
        dir.nor();

        // aim past player
        comboTarget.set(playerPos).mulAdd(dir, COMBO_OVERSHOOT_T * TILE);
        clampToBossRoom(comboTarget);

        vel.set(tmp2.set(comboTarget).sub(pos).nor().scl(COMBO_SPEED_TPS * TILE));
    }

    private void updateCombo(float dt, PlayerLike player) {
        moveWithCollision(dt, vel);

        if (!comboHitApplied && hitbox.overlaps(player.getHitbox())) {
            comboHitApplied = true;
            player.damage(COMBO_DMG);
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

    private void updateHit() {
        if (stateTime >= HIT_STUN_T) {
            state = (getHpPct() <= RUN2_HP_PCT) ? BossState.RUN_2 : BossState.RUN;
            stateTime = 0f;
        }
    }

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

    // =========================
    // ATTACK RECT
    // =========================
    private Rectangle attackRect() {
        float w = 6f * TILE;
        float h = 4f * TILE;
        float cx = pos.x, cy = pos.y;

        return switch (facing) {
            case UP -> new Rectangle(cx - w / 2f, cy + HIT_H / 2f, w, h);
            case DOWN -> new Rectangle(cx - w / 2f, cy - HIT_H / 2f - h, w, h);
            case LEFT -> new Rectangle(cx - HIT_W / 2f - w, cy - h / 2f, w, h);
            case RIGHT -> new Rectangle(cx + HIT_W / 2f, cy - h / 2f, w, h);
        };
    }

    // =========================
    // MOVEMENT + COLLISION
    // =========================
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

    private void moveWithCollision(float dt, Vector2 velocity) {
        float nx = pos.x + velocity.x * dt;
        float ny = pos.y + velocity.y * dt;

        // try X
        tmpRect1.set(hitbox);
        tmpRect1.x = (nx - HIT_W / 2f);
        if (!maze.isWallRect(tmpRect1)) {
            pos.x = nx;
            hitbox.x = tmpRect1.x;
        }

        // try Y
        tmpRect2.set(hitbox);
        tmpRect2.y = (ny - HIT_H / 2f);
        if (!maze.isWallRect(tmpRect2)) {
            pos.y = ny;
            hitbox.y = tmpRect2.y;
        }
    }

    // =========================
    // ANIM SELECTION
    // =========================
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

    // =========================
    // HELPERS
    // =========================
    private void updateFacing(Vector2 toPlayer) {
        if (Math.abs(toPlayer.x) > Math.abs(toPlayer.y)) {
            facing = (toPlayer.x >= 0) ? Facing.RIGHT : Facing.LEFT;
        } else {
            facing = (toPlayer.y >= 0) ? Facing.UP : Facing.DOWN;
        }
    }

    private void clampToBossRoom(Vector2 worldPos) {
        int padTiles = 3;

        float minX = (bossRoom.x() + padTiles) * TILE;
        float minY = (bossRoom.y() + padTiles) * TILE;
        float maxX = (bossRoom.x() + bossRoom.w() - padTiles) * TILE;
        float maxY = (bossRoom.y() + bossRoom.h() - padTiles) * TILE;

        worldPos.x = Math.max(minX, Math.min(maxX, worldPos.x));
        worldPos.y = Math.max(minY, Math.min(maxY, worldPos.y));
    }
}
