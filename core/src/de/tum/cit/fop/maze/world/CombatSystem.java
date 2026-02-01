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

/**
 * Handles all combat-related interactions in the game.
 *
 * This includes damage calculation, enemy and boss updates,
 * projectile handling, player attacks, and obstacle-based damage.
 * The combat system acts as the single authority for applying
 * combat rules and resolving damage.
 */
public class CombatSystem {

    private static final float ENEMY_WAKE_UP_DIST = 24 * 16f;

    private Texture droneLaserTexture;
    private List<Enemy> enemies;
    private float enemyDamageMult = 1f;

    // floor hazard loop sound removed
    private boolean wasPlayerAttacking = false;
    private final java.util.HashSet<Enemy> meleeHitThisSwing = new java.util.HashSet<>();

    public CombatSystem() {}

    public void setEnemyDamageMult(float mult) {
        enemyDamageMult = Math.max(0.1f, mult);
    }

    /**
     * Updates all combat interactions for the current frame.
     *
     * This method processes:
     * <ul>
     *   <li>Projectile collisions</li>
     *   <li>Melee attacks</li>
     *   <li>Enemy and boss behavior</li>
     *   <li>Player damage from enemies and obstacles</li>
     *   <li>Status effects and skill interactions</li>
     * </ul>
     *
     * @param deltaTime Time elapsed since the last frame
     * @param player Player instance
     * @param bossTarget Target adapter for boss AI
     * @param boss Active boss instance (may be null)
     * @param maze Maze reference
     * @param enemies List of active enemies
     * @param obstacles List of active obstacles
     * @param gameCamera Camera used for visual effects
     */
    public void update(float deltaTime, Player player, PlayerLike bossTarget, Boss boss, Maze maze, List<Enemy> enemies, List<Obstacle> obstacles, GameCamera gameCamera) {
        this.enemies = enemies;

        if (maze != null && enemies != null)
            maze.updateEnemyOccupancy(enemies);

        int strength = player.getStrengthStat();
        float dmgMult = player.getDamageMult();

        int base = Math.max(1, strength);
        int bulletDamage = Math.max(1, Math.round(base * dmgMult));
        int meleeDamage  = Math.max(1, Math.round(base * dmgMult * 0.6f));
        int repulseDamage = Math.max(1, Math.round(base * dmgMult * 0.6f));


        // 1) BULLETS => ENEMIES/BOSS
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

                if (boss != null && !boss.isDead() && b.getBounds().overlaps(boss.getHitbox())) {
                    boss.damage(bulletDamage);
                    b.setDead();
                    continue;
                }

                for (Enemy e : enemies) {
                    if (b.getBounds().overlaps(e.getCollisionBox())) {
                        if (!e.isDying()) {
                            e.takeDamage(bulletDamage);
                        }
                        b.setDead();
                        break;
                    }
                }
            }
        }


        // 1b) DRONE LASERS => PLAYER
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

                Rectangle playerHitbox = new Rectangle(player.getX(), player.getY(), 32f, 64f);

                if (laser.getBounds().overlaps(playerHitbox)) {
                    hitPlayerThisFrame = true;
                    playerHitByAnyLaser = true;

                    drone.increaseLaserLock(deltaTime);
                    player.onLaserHit(drone.getLaserLockTime(), deltaTime);
                }
            }

            if (!hitPlayerThisFrame) {
                drone.resetLaserLock();
            }
        }

        if (!playerHitByAnyLaser) {
            player.resetLaserHeat();
        }


        // 2) PLAYER MELEE => BOSS
        if (boss != null && !boss.isDead()
                && player.isAttacking()
                && player.getAttackHitbox() != null
                && player.getAttackHitbox().overlaps(boss.getHitbox())) {

            int bossMelee = Math.max(1, Math.round(Math.max(1, strength) * dmgMult * 0.3f));
            boss.damage(bossMelee);
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

                    e.takeDamage(repulseDamage);
                }
            }
        }

        // 5) ENEMIES (touch dmg + melee hit)
        if (!wasPlayerAttacking && player.isAttacking()) {
            meleeHitThisSwing.clear();
        }
        wasPlayerAttacking = player.isAttacking();

        int touchDamage = Math.max(1, Math.round(1f * enemyDamageMult));
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

            if (tooFar && !enemy.isAggroLocked()) continue;

            if (playerHitbox.overlaps(enemy.getCollisionBox())) {
                enemy.onPlayerContact();
            }

            enemy.update(deltaTime, maze);
            enemy.applyEnemySeparation(enemies, player, deltaTime, maze);

            // Touch damage
            if (playerHitbox.overlaps(enemy.getCollisionBox())) {
                player.takeDamage(touchDamage);
            }

            // Player melee hit
            if (player.isAttacking()
                    && enemy.canBeHitByMelee()
                    && player.getAttackHitbox() != null
                    && player.getAttackHitbox().overlaps(enemy.getCollisionBox())) {

                if (!meleeHitThisSwing.contains(enemy)) {
                    enemy.takeDamage(meleeDamage);
                    meleeHitThisSwing.add(enemy);
                }

                // Rewards: freeze + poison
                if (player.hasFreezeWeapon()) {
                    enemy.applyFreeze(1.25f);
                }
                if (player.hasPoisonKnuckles()) {
                    enemy.applyPoison(4.0f, 0.75f);
                }

                // Gloves knockback
                float kb = player.getGloveKnockbackStrength();
                if (kb > 0f) {
                    Vector2 push = enemy.getPosition().cpy().sub(new Vector2(player.getX() + 16f, player.getY() + 32f)).nor().scl(kb);
                    enemy.getPosition().add(push.x * deltaTime, push.y * deltaTime);
                }
            }

            if (enemy.isDead()) {

                player.addExperience(50);
                player.addScore(100);

                MazeRunnerGame game = (MazeRunnerGame) Gdx.app.getApplicationListener();
                if (game.getAchievementManager() != null) {
                    game.getAchievementManager().unlockAchievement("first_blood");
                }

                iter.remove();
            }
        }

        // 6) OBSTACLES
        float speedModifier = 1f;
        float visibilityFactor = 1f;

        float playerCenterX = player.getX() + 16f;
        float playerCenterY = player.getY() + 32f;

        float playerX = player.getX() + 16f;
        float playerY = player.getY() + 32f;
        float tileSize = maze.getTileSize();

        for (Obstacle obs : obstacles) {

            if (obs instanceof FloorHazard hazard) {

                if (hazard.justEntered(playerFeetHitbox)) {
                    player.activateFloorHazard(2.5f, hazard.getSpeedBoost());
                }
                // no floor hazard loop sound
                continue;
            }

            if (obs instanceof SpikeTrap spike) {

                boolean onTrap = spike.getCollisionBox().overlaps(playerFeetHitbox);
                spike.setPlayerOnTrap(onTrap);
                spike.update(deltaTime);

                if (spike.shouldDamagePlayerThisFrame()) {
                    if (!player.canIgnoreTrapDamage()) {
                        player.takeDamage(1);
                    }
                }

                continue;
            }

            if (obs instanceof LightningTrap lightning && lightning.disabled) {
                continue;
            }

            if (obs instanceof LightningTrap lightning) {

                boolean onTrap = lightning.containsPoint(playerCenterX, playerCenterY);
                lightning.setPlayerOnTrap(onTrap);
                lightning.update(deltaTime);

                if (lightning.didTriggerThisFrame()) {

                    boolean ignoreTrap = player.canIgnoreTrapDamage();

                    if (!ignoreTrap && !player.hasGhost()) {
                        player.takeDamage(lightning.getDamageToPlayer());
                        player.applyLightningDebuff(1.5f);
                    }
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

                if (isTrap && player.canIgnoreTrapDamage()) {
                    obs.onActivate();
                    continue;
                }

                if (!isTrap || !player.hasGhost()) {
                    obs.onActivate();
                    player.takeDamage(obs.getDamageToPlayer());
                }
            }
        }

        // floor hazard loop sound removed

        player.setSpeedMultiplier(speedModifier);
        gameCamera.setZoomMultiplier(visibilityFactor);
    }

    /**
     * Renders all active enemy projectiles.
     *
     * @param batch Sprite batch used for rendering
     */
    public void renderProjectiles(SpriteBatch batch) {

        if (enemies == null) return;

        ensureDroneLaserTextureLoaded();

        for (Enemy enemy : enemies) {

            if (!(enemy instanceof FlyingDrone drone)) continue;
            if (!drone.isLaserActive()) continue;

            Vector2 start = drone.getLaserStart();
            Vector2 end = drone.getLaserEnd();

            Vector2 dir = end.cpy().sub(start);
            float length = dir.len();
            if (length < 1f) continue;

            float angleDeg = dir.angleDeg();
            float charge = drone.getLaserCharge();
            float beamThickness = MathUtils.lerp(12f, 30f, charge * charge);
            float glowAlpha = MathUtils.lerp(0.4f, 1f, charge);

            batch.setColor(1f, 0.3f, 0.3f, glowAlpha);
            batch.draw(droneLaserTexture, start.x, start.y - beamThickness * 0.5f, 0f, beamThickness * 0.5f, length, beamThickness, 1f, 1f, angleDeg, 0, 0, droneLaserTexture.getWidth(), droneLaserTexture.getHeight(), false, false);
            batch.setColor(1f, 1f, 1f, 1f);
        }
    }

    /**
     * Stops any looping combat-related sound effects.
     */
    public void stopLoopingSfx() {
        // floor hazard loop sound removed
    }

    private boolean isWall(Rectangle rect, Maze maze) {
        int cx = (int) (rect.x / maze.getTileSize());
        int cy = (int) (rect.y / maze.getTileSize());
        return maze.isWallCell(cx, cy);
    }

    private void ensureDroneLaserTextureLoaded() {
        if (droneLaserTexture == null) {
            droneLaserTexture = new Texture("Enemies/FlyingDrone/Laser.png");
            droneLaserTexture.setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest);
        }
    }
}
