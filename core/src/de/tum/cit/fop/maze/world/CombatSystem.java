package de.tum.cit.fop.maze.world;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.Rectangle;
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

    public void update(float deltaTime, Player player, Maze maze, List<Enemy> enemies, List<Obstacle> obstacles, GameCamera gameCamera) {

        // --- 1. HANDLE BULLET COLLISIONS ---
        if (player.getBullets() != null) {
            Iterator<Bullet> bIter = player.getBullets().iterator();
            while (bIter.hasNext()) {
                Bullet b = bIter.next();

                // Check Wall Collision
                if (isWall(b.getBounds(), maze)) {
                    b.setDead();
                }

                // Check Enemy Collision
                for (Enemy e : enemies) {
                    if (b.getBounds().overlaps(e.getCollisionBox())) {
                        e.takeDamage(1); // Bullet damage
                        b.setDead();
                        break; // Bullet hits one enemy and vanishes
                    }
                }
            }
        }

        // --- 2. ENEMY LOGIC ---
        Rectangle playerHitbox = new Rectangle(player.getX(), player.getY(), 32f, 32f);
        float centerX = player.getX() + 16f;
        float centerY = player.getY() + 16f;
        float detectionRangeSquared = ENEMY_WAKE_UP_DIST * ENEMY_WAKE_UP_DIST;

        Iterator<Enemy> iter = enemies.iterator();
        while (iter.hasNext()) {
            Enemy enemy = iter.next();

            // Wake up check
            float diffX = enemy.getPosition().x - centerX;
            float diffY = enemy.getPosition().y - centerY;
            float closenessSquared = diffX * diffX + diffY * diffY;
            boolean isTooFar = closenessSquared > detectionRangeSquared;

            if (isTooFar && !enemy.isAggroLocked())
                continue;

            // Move Enemy
            enemy.update(deltaTime, maze);

            // Player takes damage from Enemy
            if (playerHitbox.overlaps(enemy.getCollisionBox())) {
                player.takeDamage(1);
            }

            // Enemy takes damage from Player Melee
            if (player.isAttacking() && player.getAttackHitbox() != null) {
                if (player.getAttackHitbox().overlaps(enemy.getCollisionBox())) {
                    enemy.takeDamage(1);
                }
            }

            // Death Check
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

        // --- 3. OBSTACLE LOGIC ---
        float speedModifier = 1f;
        float visibilityFactor = 1f;

        for (Obstacle obs : obstacles) {
            obs.updateCooldown(deltaTime);

            if (obs.tryActivate(playerHitbox)) {
                obs.onActivate();

                if (obs instanceof SpikeTrap spike) {
                    player.takeDamage(1);
                } else {
                    player.takeDamage(obs.getDamageToPlayer());
                }
            }

            if (obs instanceof SpikeTrap spike) {
                spike.update(deltaTime);
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
                if (hazard.isBoostActive())
                    speedModifier = hazard.getSpeedBoost();
            }
        }

        // Apply Status Effects
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