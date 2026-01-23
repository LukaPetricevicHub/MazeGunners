package de.tum.cit.fop.maze.world;

import de.tum.cit.fop.maze.enemies.Boss;
import de.tum.cit.fop.maze.enemies.PlayerLike;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;
import de.tum.cit.fop.maze.MazeRunnerGame;
import de.tum.cit.fop.maze.camera.GameCamera;
import de.tum.cit.fop.maze.enemies.Enemy;
import de.tum.cit.fop.maze.map.Maze;
import de.tum.cit.fop.maze.obstacles.*;
import de.tum.cit.fop.maze.player.Player;
import de.tum.cit.fop.maze.player.Bullet;

import java.util.Iterator;
import java.util.List;

public class CombatSystem {

    private static final float ENEMY_WAKE_UP_DIST = 24 * 16f;

    public CombatSystem() {
    }

    public void update(float deltaTime,
                       Player player,
                       PlayerLike bossTarget,
                       Boss boss,
                       Maze maze,
                       List<Enemy> enemies,
                       List<Obstacle> obstacles,
                       GameCamera gameCamera) {

        // --- 1) BULLETS: walls, enemies, boss ---
        if (player.getBullets() != null) {
            Iterator<Bullet> bIter = player.getBullets().iterator();
            while (bIter.hasNext()) {
                Bullet b = bIter.next();
                if (!b.isAlive()) { bIter.remove(); continue; }

                // Wall
                if (isWall(b.getBounds(), maze)) {
                    b.setDead();
                    continue;
                }

                // Boss (separate entity)
                if (boss != null && !boss.isDead() && b.getBounds().overlaps(boss.getHitbox())) {
                    boss.damage(5);   // bullet dmg vs boss (tune)
                    b.setDead();      // if later you want piercing, gate this behind a flag
                    continue;
                }

                // Enemies
                for (Enemy e : enemies) {
                    if (b.getBounds().overlaps(e.getCollisionBox())) {
                        // If you later enable piercing, use b.hasAlreadyHit(e)
                        e.takeDamage(5);
                        b.setDead();
                        break;
                    }
                }
            }
        }

        // --- 1.5) Player Melee vs Boss ---
        if (boss != null && !boss.isDead() && player.isAttacking() && player.getAttackHitbox() != null) {
            if (player.getAttackHitbox().overlaps(boss.getHitbox())) {
                boss.damage(2); // melee dmg vs boss (tune)
            }
        }

        // --- 2) Boss AI vs Player ---
        if (boss != null && !boss.isDead() && bossTarget != null) {
            boss.update(deltaTime, bossTarget);
        }

        // --- 3) Repulse (enemies only, boss stays separate unless you want it affected) ---
        if (player.isRepulseActive()) {
            float repulseRange = 200f;
            float pushForce = 300f;

            for (Enemy e : enemies) {
                Vector2 enemyPos = e.getPosition();
                Vector2 playerPos = new Vector2(player.getX(), player.getY());

                if (playerPos.dst(enemyPos) < repulseRange) {
                    Vector2 push = enemyPos.cpy().sub(playerPos).nor().scl(pushForce);
                    e.getPosition().add(push.x * deltaTime * 5, push.y * deltaTime * 5);
                    e.takeDamage(2);
                }
            }
        }

        // --- 4) Enemy logic (UNCHANGED) ---
        Rectangle playerHitbox = new Rectangle(player.getX(), player.getY(), 32f, 32f);
        float centerX = player.getX() + 16f;
        float centerY = player.getY() + 16f;
        float detectionRangeSquared = ENEMY_WAKE_UP_DIST * ENEMY_WAKE_UP_DIST;

        Iterator<Enemy> iter = enemies.iterator();
        while (iter.hasNext()) {
            Enemy enemy = iter.next();

            float diffX = enemy.getPosition().x - centerX;
            float diffY = enemy.getPosition().y - centerY;
            float closenessSquared = diffX * diffX + diffY * diffY;
            boolean isTooFar = closenessSquared > detectionRangeSquared;

            if (isTooFar && !enemy.isAggroLocked())
                continue;

            enemy.update(deltaTime, maze);

            if (playerHitbox.overlaps(enemy.getCollisionBox())) {
                player.takeDamage(1);
            }

            if (player.isAttacking() && player.getAttackHitbox() != null) {
                if (player.getAttackHitbox().overlaps(enemy.getCollisionBox())) {
                    enemy.takeDamage(1);
                }
            }

            if (enemy.isDead()) {
                player.addExperience(50);
                player.addScore(100);

                MazeRunnerGame game = (MazeRunnerGame) Gdx.app.getApplicationListener();
                if (game.getAchievementManager() != null) {
                    game.getAchievementManager().unlock("first_blood");
                }

                iter.remove();
            }
        }

        // --- 5) Obstacles (UNCHANGED) ---
        float speedModifier = 1f;
        float visibilityFactor = 1f;

        for (Obstacle obs : obstacles) {
            obs.updateCooldown(deltaTime);

            if (obs.tryActivate(playerHitbox)) {
                boolean isTrap = (obs instanceof SpikeTrap || obs instanceof SlimeTrap);
                if (!isTrap || !player.hasGhost()) {
                    obs.onActivate();

                    if (obs instanceof SpikeTrap) player.takeDamage(1);
                    else player.takeDamage(obs.getDamageToPlayer());
                }
            }

            if (obs instanceof SpikeTrap spike) spike.update(deltaTime);

            if (obs instanceof SlimeTrap slime) {
                slime.updateEffect(deltaTime);
                if (slime.isEffectActive()) {
                    speedModifier = slime.getSlowMultiplier();
                    visibilityFactor = slime.getVisionMultiplier();
                }
            }

            if (obs instanceof FloorHazard hazard) {
                hazard.updateEffect(deltaTime);
                if (hazard.isBoostActive())
                    speedModifier = hazard.getSpeedBoost();
            }
        }

        player.setSpeedMultiplier(speedModifier);
        gameCamera.setZoomMultiplier(visibilityFactor);
    }


    public void renderProjectiles(SpriteBatch batch) {
        // No op (Player draws bullets now)
    }

    private boolean isWall(Rectangle rect, Maze maze) {
        int cx = (int) (rect.x / maze.getTileSize());
        int cy = (int) (rect.y / maze.getTileSize());
        return maze.isWallCell(cx, cy);
    }
}