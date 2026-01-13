package de.tum.cit.fop.maze.world;

import com.badlogic.gdx.math.Rectangle;
import de.tum.cit.fop.maze.enemies.Enemy;
import de.tum.cit.fop.maze.map.Maze;
import de.tum.cit.fop.maze.obstacles.Obstacle;
import de.tum.cit.fop.maze.obstacles.SpikeTrap;
import de.tum.cit.fop.maze.player.Player;

import java.util.List;

public class CombatSystem {

    private static final float ENEMY_ACTIVE_RADIUS = 12 * 16f; // 12 tiles

    public void update(float delta, Player player, Maze maze, List<Enemy> enemies, List<Obstacle> obstacles) {
        Rectangle playerBounds = new Rectangle(player.getX(), player.getY(), 32f, 64f);

        float px = player.getX() + 16f;
        float py = player.getY() + 32f;
        float activeRadius2 = ENEMY_ACTIVE_RADIUS * ENEMY_ACTIVE_RADIUS;

        for (Enemy e : enemies) {
            float dx = e.getPosition().x - px;
            float dy = e.getPosition().y - py;

            // Skip inactive enemies
            boolean outsideActiveRadius = dx * dx + dy * dy > activeRadius2;

            if (outsideActiveRadius && !e.isAggroLocked())
                continue;

            e.update(delta, maze);

            if (playerBounds.overlaps(e.getCollisionBox())) {
                player.takeDamage(1);
            }
        }


        for (Obstacle o : obstacles) {
            o.updateCooldown(delta);

            if (o.tryActivate(playerBounds)) {
                player.takeDamage(o.getDamageToPlayer());
                o.startCooldown();
            }

            if (o instanceof SpikeTrap s) {
                s.update(delta);
            }
        }
    }
}

