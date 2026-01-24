package de.tum.cit.fop.maze.world;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;

import de.tum.cit.fop.maze.MazeRunnerGame;
import de.tum.cit.fop.maze.camera.GameCamera;
import de.tum.cit.fop.maze.enemies.Boss;
import de.tum.cit.fop.maze.enemies.Enemy;
import de.tum.cit.fop.maze.enemies.PlayerLike;
import de.tum.cit.fop.maze.map.Maze;
import de.tum.cit.fop.maze.obstacles.*;
import de.tum.cit.fop.maze.player.Bullet;
import de.tum.cit.fop.maze.player.Player;

import java.util.Iterator;
import java.util.List;

public class CombatSystem {

    private static final float ENEMY_WAKE_UP_DIST = 24 * 16f;

    public CombatSystem() {}

    public void update(
            float deltaTime,
            Player player,
            PlayerLike bossTarget,
            Boss boss,
            Maze maze,
            List<Enemy> enemies,
            List<Obstacle> obstacles,
            GameCamera gameCamera
    ) {

        // =========================================================
        // IMPORTANT: update maze enemy occupancy BEFORE enemy update
        // =========================================================
        if (maze != null && enemies != null) {
            maze.updateEnemyOccupancy(enemies);
        }

        // =========================
        // 1) BULLETS
        // =========================
        if (player.getBullets() != null) {
            Iterator<Bullet> bIter = player.getBullets().iterator();
            while (bIter.hasNext()) {
                Bullet b = bIter.next();
                if (!b.isAlive()) {
                    bIter.remove();
                    continue;
                }

                if (isWall(b.getBounds(), maze)) {
                    b.setDead();
                    continue;
                }

                if (boss != null && !boss.isDead()
                        && b.getBounds().overlaps(boss.getHitbox())) {
                    boss.damage(5);
                    b.setDead();
                    continue;
                }

                for (Enemy e : enemies) {
                    if (b.getBounds().overlaps(e.getCollisionBox())) {
                        e.takeDamage(5);
                        b.setDead();
                        break;
                    }
                }
            }
        }

        // =========================
        // 2) PLAYER MELEE → BOSS
        // =========================
        if (boss != null && !boss.isDead()
                && player.isAttacking()
                && player.getAttackHitbox() != null
                && player.getAttackHitbox().overlaps(boss.getHitbox())) {
            boss.damage(2);
        }

        // =========================
        // 3) BOSS AI
        // =========================
        if (boss != null && !boss.isDead() && bossTarget != null) {
            boss.update(deltaTime, bossTarget);
        }

        // =========================
        // 4) REPULSE SKILL
        // =========================
        if (player.isRepulseActive()) {
            float repulseRange = 200f;
            float pushForce = 300f;
            Vector2 playerPos = new Vector2(player.getX(), player.getY());

            for (Enemy e : enemies) {
                Vector2 enemyPos = e.getPosition();
                if (playerPos.dst(enemyPos) < repulseRange) {
                    Vector2 push = enemyPos.cpy().sub(playerPos).nor().scl(pushForce);
                    e.getPosition().add(push.x * deltaTime * 5, push.y * deltaTime * 5);
                    e.takeDamage(2);
                }
            }
        }

        // =========================
        // 5) ENEMIES (OLD LOGIC RESTORED)
        // =========================
        Rectangle playerHitbox = new Rectangle(player.getX(), player.getY(), 32f, 64f);
        Rectangle playerFeetHitbox = new Rectangle(player.getX(), player.getY(), 32f, 8f);

        float centerX = player.getX() + 16f;
        float centerY = player.getY() + 32f;
        float detectionRangeSq = ENEMY_WAKE_UP_DIST * ENEMY_WAKE_UP_DIST;

        Iterator<Enemy> iter = enemies.iterator();
        while (iter.hasNext()) {
            Enemy enemy = iter.next();

            float dx = enemy.getPosition().x - centerX;
            float dy = enemy.getPosition().y - centerY;
            boolean tooFar = (dx * dx + dy * dy) > detectionRangeSq;

            if (tooFar && !enemy.isAggroLocked())
                continue;

            // FIRST: detect contact
            if (playerHitbox.overlaps(enemy.getCollisionBox())) {
                enemy.onPlayerContact();
            }

            enemy.update(deltaTime, maze);
            enemy.applyEnemySeparation(enemies, player, deltaTime, maze);

            if (playerHitbox.overlaps(enemy.getCollisionBox())) {
                player.takeDamage(1);
            }

            if (player.isAttacking()
                    && player.getAttackHitbox() != null
                    && player.getAttackHitbox().overlaps(enemy.getCollisionBox())) {
                enemy.takeDamage(1);
            }

            if (enemy.isDead()) {
                player.addExperience(50);
                player.addScore(100);

                MazeRunnerGame game =
                        (MazeRunnerGame) Gdx.app.getApplicationListener();
                if (game.getAchievementManager() != null) {
                    game.getAchievementManager().unlock("first_blood");
                }

                iter.remove();
            }
        }

        // =========================
        // 6) OBSTACLES (OLD LOGIC)
        // =========================
        float speedModifier = 1f;
        float visibilityFactor = 1f;

        for (Obstacle obs : obstacles) {

            if (obs instanceof SpikeTrap spike) {
                boolean onTrap = spike.getCollisionBox().overlaps(playerFeetHitbox);
                spike.setPlayerOnTrap(onTrap);
                spike.update(deltaTime);

                if (onTrap && spike.getDamageToPlayer() > 0) {
                    player.takeDamage(spike.getDamageToPlayer());
                }
                continue;
            }

            obs.updateCooldown(deltaTime);

            if (obs.tryActivate(playerHitbox)) {
                boolean isTrap =
                        (obs instanceof SpikeTrap || obs instanceof SlimeTrap);

                if (!isTrap || !player.hasGhost()) {
                    obs.onActivate();
                    player.takeDamage(obs.getDamageToPlayer());
                }
            }

            if (obs instanceof SlimeTrap slime) {
                slime.updateEffect(deltaTime);
                if (slime.isEffectActive()) {
                    speedModifier = slime.getSlowMultiplier();
                    visibilityFactor = slime.getVisionMultiplier();
                }
            }

            if (obs instanceof FloorHazard hazard) {
                hazard.updateEffect(deltaTime);
                if (hazard.isBoostActive()) {
                    speedModifier = hazard.getSpeedBoost();
                }
            }
        }

        player.setSpeedMultiplier(speedModifier);
        gameCamera.setZoomMultiplier(visibilityFactor);
    }

    // Player renders bullets
    public void renderProjectiles(SpriteBatch batch) {}

    private boolean isWall(Rectangle rect, Maze maze) {
        int cx = (int) (rect.x / maze.getTileSize());
        int cy = (int) (rect.y / maze.getTileSize());
        return maze.isWallCell(cx, cy);
    }
}
