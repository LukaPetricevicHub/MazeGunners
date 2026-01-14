package de.tum.cit.fop.maze.world;

import com.badlogic.gdx.math.Rectangle;
import de.tum.cit.fop.maze.camera.GameCamera;
import de.tum.cit.fop.maze.enemies.Enemy;
import de.tum.cit.fop.maze.map.Maze;
import de.tum.cit.fop.maze.obstacles.*;
import de.tum.cit.fop.maze.player.Player;
import java.util.List;

public class CombatSystem {

    // Attributes
    private static final float ENEMY_WAKE_UP_DIST = 24 * 16f;


    // Update combat and trap logic
    public void update(float deltaTime, Player player, Maze maze, List<Enemy> enemies, List<Obstacle> obstacles, GameCamera gameCamera) {
        Rectangle playerHitbox = new Rectangle(player.getX(), player.getY(), 32f, 64f);

        float centerX = player.getX() + 16f;
        float centerY = player.getY() + 32f;
        float detectionRangeSquared = ENEMY_WAKE_UP_DIST * ENEMY_WAKE_UP_DIST;

        for (Enemy enemy : enemies) {
            float diffX = enemy.getPosition().x - centerX;
            float diffY = enemy.getPosition().y - centerY;

            float closenessSquared = diffX * diffX + diffY * diffY;
            boolean isTooFar = closenessSquared > detectionRangeSquared;

            if (isTooFar && !enemy.isAggroLocked())
                continue;


            enemy.update(deltaTime, maze);

            if(playerHitbox.overlaps(enemy.getCollisionBox())) {
                player.takeDamage(1);
            }
        }

        float speedModifier = 1f;
        float visibilityFactor = 1f;

        for (Obstacle obs : obstacles) {
            obs.updateCooldown(deltaTime);

            if (obs.tryActivate(playerHitbox)) {
                player.takeDamage(obs.getDamageToPlayer());
                obs.onActivate();
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

                if(hazard.isBoostActive())
                    speedModifier = hazard.getSpeedBoost();
            }
        }

        player.setSpeedMultiplier(speedModifier);
        gameCamera.setZoomMultiplier(visibilityFactor);
    }
}
