package de.tum.cit.fop.maze.world;

import com.badlogic.gdx.math.Rectangle;
import de.tum.cit.fop.maze.camera.GameCamera;
import de.tum.cit.fop.maze.enemies.Enemy;
import de.tum.cit.fop.maze.map.Maze;
import de.tum.cit.fop.maze.obstacles.*;
import de.tum.cit.fop.maze.player.Player;
import java.util.List;

public class CombatSystem {

    private static final float ENEMY_WAKE_UP_DIST = 24 * 16f;

    public void update(
            float deltaTime,
            Player player,
            Maze maze,
            List<Enemy> enemies,
            List<Obstacle> obstacles,
            GameCamera gameCamera
    ) {

        // make sure maze is aware of current enemy rectangles before any Enemy.update(pathfinding) runs
        if (maze != null && enemies != null) {
            maze.updateEnemyOccupancy(enemies);
        }

        Rectangle playerHitbox = new Rectangle(player.getX(), player.getY(), 32f, 64f);

        Rectangle playerFeetHitbox = new Rectangle(
                player.getX(),
                player.getY(),
                32f,
                8f
        );


        float centerX = player.getX() + 16f;
        float centerY = player.getY() + 32f;
        float detectionRangeSquared = ENEMY_WAKE_UP_DIST * ENEMY_WAKE_UP_DIST;

        for (Enemy enemy : enemies) {
            float diffX = enemy.getPosition().x - centerX;
            float diffY = enemy.getPosition().y - centerY;

            float closenessSquared = diffX * diffX + diffY * diffY;
            boolean isTooFar = closenessSquared > detectionRangeSquared;

            // FIRST: detect contact
            if (playerHitbox.overlaps(enemy.getCollisionBox())) {
                enemy.onPlayerContact();
            }

            enemy.update(deltaTime, maze);
            enemy.applyEnemySeparation(enemies, player, deltaTime, maze);

            if (playerHitbox.overlaps(enemy.getCollisionBox())) {
                player.takeDamage(1);
            }


        }

        float speedModifier = 1f;
        float visibilityFactor = 1f;

        for (Obstacle obs : obstacles) {

            if (obs instanceof SpikeTrap spike) {

                boolean onTrap = spike.getCollisionBox().overlaps(playerFeetHitbox);

                spike.setPlayerOnTrap(onTrap);
                spike.update(deltaTime);

                int damage = spike.getDamageToPlayer();
                if (damage > 0 && onTrap) {
                    player.takeDamage(damage);
                }

                continue;
            }

            obs.updateCooldown(deltaTime);

            if (obs.tryActivate(playerHitbox)) {
                obs.onActivate();
                player.takeDamage(obs.getDamageToPlayer());
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
}
