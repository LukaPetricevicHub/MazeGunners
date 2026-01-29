package de.tum.cit.fop.maze.world;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;

import de.tum.cit.fop.maze.MazeRunnerGame;
import de.tum.cit.fop.maze.camera.GameCamera;
import de.tum.cit.fop.maze.enemies.*;
import de.tum.cit.fop.maze.map.Maze;
import de.tum.cit.fop.maze.obstacles.*;
import de.tum.cit.fop.maze.player.Bullet;
import de.tum.cit.fop.maze.player.Player;

import java.util.Iterator;
import java.util.List;

public class CombatSystem {

    private static final float ENEMY_WAKE_UP_DIST = 24 * 16f;
    private Texture droneLaserTexture;
    private List<Enemy> enemies;

    private long floorHazardSoundId = -1;
    private boolean floorHazardSoundPlaying = false;

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
        this.enemies = enemies;

        if (maze != null && enemies != null) {
            maze.updateEnemyOccupancy(enemies);
        }

        // 1) BULLETS

        if (player.getBullets() != null) {
            Iterator<Bullet> bIter = player.getBullets().iterator();
            while (bIter.hasNext()) {
                Bullet b = bIter.next();
                if (!b.isAlive()) {
                    bIter.remove();
                    continue;
                }

                if (isWall(b.getBounds(), maze)) {

                    float hitX = b.getBounds().x + b.getBounds().width * 0.5f;
                    float hitY = b.getBounds().y + b.getBounds().height * 0.5f;

                    for (Obstacle obs : obstacles) {
                        if (obs instanceof LightningTrap lightning) {
                            if (lightning.isHitByBullet(hitX, hitY)) {
                                lightning.disable();
                            }
                        }
                    }

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
                        if (!e.isDying()) {
                            e.takeDamage(5);
                        }
                        b.setDead();
                        break;
                    }
                }
            }
        }

        // 1b) DRONE LASERS

        // 1b) DRONE LASERS
        boolean playerHitByAnyLaser = false;

        for (Enemy enemy : enemies) {

            if (!(enemy instanceof FlyingDrone drone)) continue;

            boolean hitPlayerThisFrame = false;

            Iterator<DroneLaser> lIter = drone.getLasers().iterator();
            while (lIter.hasNext()) {
                DroneLaser laser = lIter.next();

                laser.update(deltaTime);

                if (!laser.isAlive()) {
                    lIter.remove();
                    continue;
                }

                if (isWall(laser.getBounds(), maze)) {
                    laser.kill();
                    continue;
                }

                Rectangle playerHitbox =
                        new Rectangle(player.getX(), player.getY(), 32f, 64f);

                if (laser.getBounds().overlaps(playerHitbox)) {
                    hitPlayerThisFrame = true;
                    playerHitByAnyLaser = true;

                    drone.increaseLaserLock(deltaTime);
                    player.onLaserHit(drone.getLaserLockTime(), deltaTime);
                }

            }

            // 🔥 THIS IS THE IMPORTANT PART 🔥
            if (!hitPlayerThisFrame) {
                drone.resetLaserLock();
            }
        }

        if (!playerHitByAnyLaser) {
            player.resetLaserHeat();
        }


        // 2) PLAYER MELEE → BOSS
        if (boss != null && !boss.isDead()
                && player.isAttacking()
                && player.getAttackHitbox() != null
                && player.getAttackHitbox().overlaps(boss.getHitbox())) {
            boss.damage(2);
        }


        // 3) BOSS AI
        if (boss != null && !boss.isDead() && bossTarget != null) {
            boss.update(deltaTime, bossTarget);
        }


        // 4) REPULSE SKILL
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

        // 5) ENEMIES (OLD LOGIC RESTORED)
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
                    && enemy.canBeHitByMelee()
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


        // 6) OBSTACLES (OLD LOGIC)
        float speedModifier = 1f;
        float visibilityFactor = 1f;

        float playerCenterX = player.getX() + 16f;
        float playerCenterY = player.getY() + 32f;

        boolean nearAnyFloorHazard = false;

        float playerX = player.getX() + 16f;
        float playerY = player.getY() + 32f;
        float tileSize = maze.getTileSize();


        for (Obstacle obs : obstacles) {

            if (obs instanceof FloorHazard hazard) {

                if (hazard.justEntered(playerFeetHitbox)) {
                    player.activateFloorHazard(
                            2.5f,
                            hazard.getSpeedBoost()
                    );
                }

                if (hazard.isPlayerInRange(playerX, playerY, tileSize)) {
                    nearAnyFloorHazard = true;
                }

                continue;
            }




            if (obs instanceof SpikeTrap spike) {
                boolean onTrap = spike.getCollisionBox().overlaps(playerFeetHitbox);
                spike.setPlayerOnTrap(onTrap);
                spike.update(deltaTime);

                if (spike.shouldDamagePlayerThisFrame()) {
                    player.takeDamage(1); // SPIKE_DAMAGE
                }
                continue;
            }


            if (obs instanceof LightningTrap lightning && lightning.disabled) {
                continue;
            }

            if (obs instanceof LightningTrap lightning) {

                if (lightning.disabled) {
                    continue;
                }

                boolean onTrap = lightning.containsPoint(playerCenterX, playerCenterY);
                lightning.setPlayerOnTrap(onTrap);
                lightning.update(deltaTime);

                if (lightning.didTriggerThisFrame()) {

                    if (!player.hasGhost()) {
                        player.takeDamage(lightning.getDamageToPlayer());
                    }

                    player.applyLightningDebuff(3f);
                }

                if (player.isLightningDebuffed()) {
                    speedModifier = lightning.getSlowMultiplier();
                    visibilityFactor = lightning.getVisionMultiplier();
                }


                continue;
            }


            obs.updateCooldown(deltaTime);


            if (obs.tryActivate(playerHitbox)) {
                boolean isTrap = (obs instanceof SpikeTrap || obs instanceof LightningTrap);

                if (!isTrap || !player.hasGhost()) {
                    obs.onActivate();
                    player.takeDamage(obs.getDamageToPlayer());
                }
            }
        }

        if (nearAnyFloorHazard && !floorHazardSoundPlaying) {
            floorHazardSoundId = ObstacleAudioManager.playFloorHazardLoop(0.6f);
            floorHazardSoundPlaying = true;
        }
        else if (!nearAnyFloorHazard && floorHazardSoundPlaying) {
            ObstacleAudioManager.stopFloorHazard(floorHazardSoundId);
            floorHazardSoundId = -1;
            floorHazardSoundPlaying = false;
        }

        player.setSpeedMultiplier(speedModifier);
        gameCamera.setZoomMultiplier(visibilityFactor);
    }

    // Drone render bullets
    public void renderProjectiles(SpriteBatch batch) {
        if (enemies == null) return;

        ensureDroneLaserTextureLoaded();

        for (Enemy enemy : enemies) {
            if (!(enemy instanceof FlyingDrone drone)) continue;
            if (!drone.isLaserActive()) continue;

            Vector2 start = drone.getLaserStart();
            Vector2 end   = drone.getLaserEnd();

            Vector2 dir = end.cpy().sub(start);
            float length = dir.len();
            if (length < 1f) continue;

            float angleDeg = dir.angleDeg();

            float charge = drone.getLaserCharge(); // 0 → 1

            float beamThickness =
                    MathUtils.lerp(12f, 30f, charge * charge);

            float glowAlpha = MathUtils.lerp(0.4f, 1f, charge);
            batch.setColor(1f, 0.3f, 0.3f, glowAlpha);

            batch.draw(droneLaserTexture, start.x, start.y - beamThickness * 0.5f, 0f, beamThickness * 0.5f, length, beamThickness, 1f, 1f, angleDeg, 0, 0, droneLaserTexture.getWidth(), droneLaserTexture.getHeight(), false, false);
            batch.setColor(1f, 1f, 1f, 1f);

        }
    }




    private boolean isWall(Rectangle rect, Maze maze) {
        int cx = (int) (rect.x / maze.getTileSize());
        int cy = (int) (rect.y / maze.getTileSize());
        return maze.isWallCell(cx, cy);
    }

    private void ensureDroneLaserTextureLoaded() {
        if (droneLaserTexture == null) {
            droneLaserTexture = new Texture("Enemies/FlyingDrone/Laser.png");
            droneLaserTexture.setFilter(
                    Texture.TextureFilter.Nearest,
                    Texture.TextureFilter.Nearest
            );
        }
    }


}
