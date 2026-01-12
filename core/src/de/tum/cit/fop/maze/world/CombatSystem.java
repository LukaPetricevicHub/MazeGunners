package de.tum.cit.fop.maze.world;

import com.badlogic.gdx.math.Rectangle;
import de.tum.cit.fop.maze.enemies.Enemy;
import de.tum.cit.fop.maze.level.Maze;
import de.tum.cit.fop.maze.obstacles.Obstacle;
import de.tum.cit.fop.maze.obstacles.SpikeTrap;
import de.tum.cit.fop.maze.player.Player;

import java.util.List;

public class CombatSystem {

    public void update(
            float delta,
            Player player,
            Maze maze,
            List<Enemy> enemies,
            List<Obstacle> obstacles
    ) {
        Rectangle playerBounds =
                new Rectangle(player.getX(), player.getY(), 32f, 64f);

        for (Enemy e : enemies) {
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

