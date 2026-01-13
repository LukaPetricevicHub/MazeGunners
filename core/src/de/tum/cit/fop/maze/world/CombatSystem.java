package de.tum.cit.fop.maze.world;

import com.badlogic.gdx.math.Rectangle;
import de.tum.cit.fop.maze.camera.GameCamera;
import de.tum.cit.fop.maze.enemies.Enemy;
import de.tum.cit.fop.maze.map.Maze;
import de.tum.cit.fop.maze.obstacles.*;
import de.tum.cit.fop.maze.player.Player;

import java.util.List;

public class CombatSystem {

    private static final float ENEMY_ACTIVE_RADIUS = 12 * 16f;

    public void update(
            float delta,
            Player player,
            Maze maze,
            List<Enemy> enemies,
            List<Obstacle> obstacles,
            GameCamera gameCamera
    ) {

        Rectangle playerBounds = new Rectangle(
                player.getX(),
                player.getY(),
                32f,
                64f
        );


        // Enemies
        float px = player.getX() + 16f;
        float py = player.getY() + 32f;
        float activeRadius2 = ENEMY_ACTIVE_RADIUS * ENEMY_ACTIVE_RADIUS;

        for (Enemy e : enemies) {
            float dx = e.getPosition().x - px;
            float dy = e.getPosition().y - py;

            boolean outside = dx * dx + dy * dy > activeRadius2;
            if (outside && !e.isAggroLocked()) continue;

            e.update(delta, maze);

            if (playerBounds.overlaps(e.getCollisionBox())) {
                player.takeDamage(1);
            }
        }

        // Obstacles
        float speedMultiplier = 1f;
        float visionMultiplier = 1f;

        for (Obstacle o : obstacles) {

            o.updateCooldown(delta);

            if (o.tryActivate(playerBounds)) {
                player.takeDamage(o.getDamageToPlayer());
                o.onActivate();
            }

            if (o instanceof SpikeTrap spike) {
                spike.update(delta);
            }

            if (o instanceof SlimeTrap slime) {
                slime.updateEffect(delta);

                if (slime.isEffectActive()) {
                    speedMultiplier = slime.getSlowMultiplier();
                    visionMultiplier = slime.getVisionMultiplier();
                }
            }

            if (o instanceof FloorHazard hazard) {
                hazard.updateEffect(delta);

                if (hazard.isBoostActive()) {
                    speedMultiplier = hazard.getSpeedBoost();
                }
            }
        }

        // Apply effects ONCE
        player.setSpeedMultiplier(speedMultiplier);
        gameCamera.setZoomMultiplier(visionMultiplier);
    }
}


