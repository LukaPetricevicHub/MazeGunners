// =======================================================
// Boss.java
//
// A complete boss AI with:
// - AOE circle
// - Line attack (ray forward until wall)
// - Normal melee
// - Dash to random point inside boss room
//
// You only need to call:
//   boss.update(dt, player);
//   boss.render(batch);
//
// And you need to spawn boss in boss room on level 5.
// =======================================================
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

    // ===== tuning =====
    private static final float MOVE_SPEED = 90f;
    private static final float DASH_SPEED = 320f;

    private static final float MELEE_RANGE = 42f;
    private static final int   MELEE_DMG = 10;

    private static final float LINE_RANGE = 520f;
    private static final float LINE_WIDTH = 36f;
    private static final int   LINE_DMG = 14;

    private static final float AOE_RADIUS = 96f;
    private static final int   AOE_DMG = 16;

    // windups/recovery (seconds)
    private static final float MELEE_WINDUP_T = 0.25f;
    private static final float LINE_WINDUP_T  = 0.55f;
    private static final float AOE_WINDUP_T   = 0.75f;
    private static final float DASH_WINDUP_T  = 0.25f;
    private static final float RECOVER_T      = 0.55f;

    // cooldowns (seconds)
    private static final float MELEE_CD = 0.35f;
    private static final float LINE_CD  = 2.0f;
    private static final float AOE_CD   = 3.0f;
    private static final float DASH_CD  = 2.5f;

    private final Maze maze;
    private final Room bossRoom;
    private final BossAnimations anims;

    private final Vector2 pos = new Vector2();      // world position (center-ish)
    private final Vector2 vel = new Vector2();
    private final Rectangle hitbox = new Rectangle();

    private BossState state = BossState.IDLE;
    private Facing facing = Facing.DOWN;
    private float stateTime = 0f;

    private float meleeCd = 0f;
    private float lineCd  = 0f;
    private float aoeCd   = 0f;
    private float dashCd  = 0f;

    private final Random rng = new Random();

    // For dash
    private final Vector2 dashTarget = new Vector2();

    // For line attack: we store a static rectangle hitbox
    private final Rectangle lineRect = new Rectangle();

    // For AOE: we just check radius during damage moment
    private boolean aoeDamageApplied = false;
    private boolean lineDamageApplied = false;
    private boolean meleeDamageApplied = false;

    private final int maxHp = 250;
    private int hp = maxHp;

    public Boss(Maze maze, Room bossRoom, BossAnimations anims, float spawnWorldX, float spawnWorldY) {
        this.maze = maze;
        this.bossRoom = bossRoom;
        this.anims = anims;

        this.pos.set(spawnWorldX, spawnWorldY);
        // hitbox: tweak to match your sprite scale
        this.hitbox.set(pos.x - 18, pos.y - 18, 36, 36);
    }

    public boolean isDead() { return state == BossState.DEAD; }

    public Rectangle getHitbox() { return new Rectangle(hitbox); }

    public void damage(int dmg) {
        if (state == BossState.DEAD) return;
        hp -= dmg;
        if (hp <= 0) {
            hp = 0;
            state = BossState.DEAD;
            stateTime = 0f;
            vel.setZero();
        }
    }

    public void update(float dt, PlayerLike player) {
        if (state == BossState.DEAD) return;

        // cooldowns tick
        meleeCd = Math.max(0, meleeCd - dt);
        lineCd  = Math.max(0, lineCd  - dt);
        aoeCd   = Math.max(0, aoeCd   - dt);
        dashCd  = Math.max(0, dashCd  - dt);

        stateTime += dt;

        // update facing toward player (most states)
        Vector2 toPlayer = tmp(player.getPosition()).sub(pos);
        updateFacing(toPlayer);

        switch (state) {
            case IDLE, CHASE -> updateChase(dt, player, toPlayer);
            case MELEE_WINDUP -> updateMeleeWindup(dt, player);
            case MELEE_ATTACK -> updateMeleeAttack(dt, player);

            case LINE_WINDUP -> updateLineWindup(dt, player);
            case LINE_ATTACK -> updateLineAttack(dt, player);

            case AOE_WINDUP -> updateAoeWindup(dt, player);
            case AOE_ATTACK -> updateAoeAttack(dt, player);

            case DASH_WINDUP -> updateDashWindup(dt);
            case DASHING -> updateDashing(dt, player);

            case RECOVER -> updateRecover(dt);

            default -> { /* ignore */ }
        }
    }

    // =======================================================
    // CHASE / DECISION LOGIC
    // =======================================================
    private void updateChase(float dt, PlayerLike player, Vector2 toPlayer) {
        float dist = toPlayer.len();

        // Decide an action when not moving/locked
        if (state == BossState.IDLE) {
            state = BossState.CHASE;
            stateTime = 0f;
        }

        // Priority:
        // close => AOE or melee
        // far => line
        // sometimes dash
        if (dist < AOE_RADIUS * 0.85f && aoeCd <= 0f) {
            startAOE();
            return;
        }

        if (dist < MELEE_RANGE && meleeCd <= 0f) {
            startMelee();
            return;
        }

        if (dist > MELEE_RANGE * 2.0f && lineCd <= 0f) {
            startLine();
            return;
        }

        if (dashCd <= 0f && rng.nextFloat() < 0.012f) { // small chance per frame
            startDash();
            return;
        }

        // Otherwise chase / reposition
        moveToward(dt, player.getPosition(), MOVE_SPEED);
    }

    // =======================================================
    // MELEE
    // =======================================================
    private void startMelee() {
        state = BossState.MELEE_WINDUP;
        stateTime = 0f;
        meleeDamageApplied = false;
        vel.setZero();
    }

    private void updateMeleeWindup(float dt, PlayerLike player) {
        if (stateTime >= MELEE_WINDUP_T) {
            state = BossState.MELEE_ATTACK;
            stateTime = 0f;
        }
    }

    private void updateMeleeAttack(float dt, PlayerLike player) {
        // apply damage once near mid animation
        if (!meleeDamageApplied && stateTime >= 0.10f) {
            meleeDamageApplied = true;

            Rectangle atk = meleeRect();
            if (atk.overlaps(player.getHitbox())) {
                player.damage(MELEE_DMG);
            }
        }

        // end
        if (stateTime >= 0.25f) {
            meleeCd = MELEE_CD;
            goRecover();
        }
    }

    private Rectangle meleeRect() {
        // small rectangle in front of boss
        float w = 44, h = 36;
        float cx = pos.x, cy = pos.y;

        return switch (facing) {
            case UP    -> new Rectangle(cx - w/2f, cy + 16, w, h);
            case DOWN  -> new Rectangle(cx - w/2f, cy - 16 - h, w, h);
            case LEFT  -> new Rectangle(cx - 16 - w, cy - h/2f, w, h);
            case RIGHT -> new Rectangle(cx + 16, cy - h/2f, w, h);
        };
    }

    // =======================================================
    // LINE ATTACK
    // =======================================================
    private void startLine() {
        state = BossState.LINE_WINDUP;
        stateTime = 0f;
        lineDamageApplied = false;
        vel.setZero();
    }

    private void updateLineWindup(float dt, PlayerLike player) {
        if (stateTime >= LINE_WINDUP_T) {
            state = BossState.LINE_ATTACK;
            stateTime = 0f;
        }
    }

    private void updateLineAttack(float dt, PlayerLike player) {
        // apply once shortly after attack begins
        if (!lineDamageApplied && stateTime >= 0.10f) {
            lineDamageApplied = true;
            performLineDamage(player);
        }

        if (stateTime >= 0.35f) {
            lineCd = LINE_CD;
            goRecover();
        }
    }

    private void performLineDamage(PlayerLike player) {
        // Create a thick rectangle that extends until a wall
        float range = computeLineRangeUntilWall();
        buildLineRect(range);

        if (lineRect.overlaps(player.getHitbox())) {
            player.damage(LINE_DMG);
        }
    }

    private float computeLineRangeUntilWall() {
        // step forward in small increments until wall
        float step = 8f;
        float max = LINE_RANGE;

        Vector2 dir = facingDir();
        float traveled = 0f;

        while (traveled < max) {
            traveled += step;

            float sx = pos.x + dir.x * traveled;
            float sy = pos.y + dir.y * traveled;

            // wall check using a small rect around sample point
            Rectangle probe = new Rectangle(sx - 6, sy - 6, 12, 12);
            if (maze.isWallRect(probe)) {
                traveled -= step;
                break;
            }
        }
        return Math.max(0, traveled);
    }

    private void buildLineRect(float range) {
        float cx = pos.x, cy = pos.y;
        float w = LINE_WIDTH;

        switch (facing) {
            case UP -> {
                lineRect.set(cx - w/2f, cy, w, range);
            }
            case DOWN -> {
                lineRect.set(cx - w/2f, cy - range, w, range);
            }
            case LEFT -> {
                lineRect.set(cx - range, cy - w/2f, range, w);
            }
            case RIGHT -> {
                lineRect.set(cx, cy - w/2f, range, w);
            }
        }
    }

    // =======================================================
    // AOE
    // =======================================================
    private void startAOE() {
        state = BossState.AOE_WINDUP;
        stateTime = 0f;
        aoeDamageApplied = false;
        vel.setZero();
    }

    private void updateAoeWindup(float dt, PlayerLike player) {
        if (stateTime >= AOE_WINDUP_T) {
            state = BossState.AOE_ATTACK;
            stateTime = 0f;
        }
    }

    private void updateAoeAttack(float dt, PlayerLike player) {
        if (!aoeDamageApplied && stateTime >= 0.05f) {
            aoeDamageApplied = true;

            float px = player.getPosition().x;
            float py = player.getPosition().y;

            float dx = px - pos.x;
            float dy = py - pos.y;

            if (dx*dx + dy*dy <= AOE_RADIUS*AOE_RADIUS) {
                player.damage(AOE_DMG);
            }
        }

        if (stateTime >= 0.25f) {
            aoeCd = AOE_CD;
            goRecover();
        }
    }

    // =======================================================
    // DASH
    // =======================================================
    private void startDash() {
        state = BossState.DASH_WINDUP;
        stateTime = 0f;
        vel.setZero();

        // pick random target inside boss room (world coords)
        float tx = roomWorldX(bossRoom.x() + 2 + rng.nextInt(Math.max(1, bossRoom.w() - 4)));
        float ty = roomWorldY(bossRoom.y() + 2 + rng.nextInt(Math.max(1, bossRoom.h() - 4)));
        dashTarget.set(tx, ty);
    }

    private void updateDashWindup(float dt) {
        if (stateTime >= DASH_WINDUP_T) {
            state = BossState.DASHING;
            stateTime = 0f;

            Vector2 dir = tmp(dashTarget).sub(pos);
            if (dir.len2() < 1f) dir.set(1, 0);
            dir.nor();

            vel.set(dir).scl(DASH_SPEED);
        }
    }

    private void updateDashing(float dt, PlayerLike player) {
        // move with collision
        moveWithCollision(dt, vel);

        // optional contact damage
        if (hitbox.overlaps(player.getHitbox())) {
            player.damage(8);
        }

        // stop if reached target or timed out
        if (pos.dst2(dashTarget) < 18*18 || stateTime >= 0.45f) {
            vel.setZero();
            dashCd = DASH_CD;
            goRecover();
        }
    }

    // =======================================================
    // RECOVER
    // =======================================================
    private void goRecover() {
        state = BossState.RECOVER;
        stateTime = 0f;
        vel.setZero();
    }

    private void updateRecover(float dt) {
        if (stateTime >= RECOVER_T) {
            state = BossState.CHASE;
            stateTime = 0f;
        }
    }

    // =======================================================
    // MOVEMENT + COLLISION
    // =======================================================
    private void moveToward(float dt, Vector2 target, float speed) {
        Vector2 d = tmp(target).sub(pos);
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

        // try x
        Rectangle nextX = new Rectangle(hitbox);
        nextX.x = (nx - hitbox.width/2f);
        if (!maze.isWallRect(nextX)) {
            pos.x = nx;
            hitbox.x = nextX.x;
        }

        // try y
        Rectangle nextY = new Rectangle(hitbox);
        nextY.y = (ny - hitbox.height/2f);
        if (!maze.isWallRect(nextY)) {
            pos.y = ny;
            hitbox.y = nextY.y;
        }
    }

    // =======================================================
    // RENDERING
    // =======================================================
    public void render(Batch batch) {
        Animation<TextureRegion> a = currentAnim();
        TextureRegion frame = a.getKeyFrame(stateTime, true);

        // Draw centered on pos
        float drawW = 64; // tweak to match your frame scale
        float drawH = 64;

        batch.draw(frame,
                pos.x - drawW/2f,
                pos.y - drawH/2f,
                drawW, drawH
        );
    }

    private Animation<TextureRegion> currentAnim() {
        return switch (state) {
            case MELEE_WINDUP, MELEE_ATTACK -> anims.melee;
            case LINE_WINDUP, LINE_ATTACK -> anims.line;
            case AOE_WINDUP, AOE_ATTACK -> anims.aoe;
            case DASH_WINDUP, DASHING -> anims.dash;
            case CHASE -> anims.run;
            case IDLE, RECOVER -> anims.idle;
            default -> anims.idle;
        };
    }

    // =======================================================
    // HELPERS
    // =======================================================
    private void updateFacing(Vector2 toPlayer) {
        if (Math.abs(toPlayer.x) > Math.abs(toPlayer.y)) {
            facing = (toPlayer.x >= 0) ? Facing.RIGHT : Facing.LEFT;
        } else {
            facing = (toPlayer.y >= 0) ? Facing.UP : Facing.DOWN;
        }
    }

    private Vector2 facingDir() {
        return switch (facing) {
            case UP -> new Vector2(0, 1);
            case DOWN -> new Vector2(0, -1);
            case LEFT -> new Vector2(-1, 0);
            case RIGHT -> new Vector2(1, 0);
        };
    }


    public int getHp() { return hp; }
    public int getMaxHp() { return maxHp; }
    public float getHpPct() { return (maxHp <= 0) ? 0f : (hp / (float)maxHp); }


    private float roomWorldX(int tileX) { return tileX * maze.getTileSize() + maze.getTileSize() / 2f; }
    private float roomWorldY(int tileY) { return tileY * maze.getTileSize() + maze.getTileSize() / 2f; }

    private static final Vector2 TMP = new Vector2();
    private static Vector2 tmp(Vector2 v) { return TMP.set(v); }
}
