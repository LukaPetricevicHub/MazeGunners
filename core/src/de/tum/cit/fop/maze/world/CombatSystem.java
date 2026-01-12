package de.tum.cit.fop.maze.world;

import com.badlogic.gdx.math.Rectangle;
import de.tum.cit.fop.maze.enemies.Enemy;
import de.tum.cit.fop.maze.level.Maze;
import de.tum.cit.fop.maze.obstacles.Obstacle;
import de.tum.cit.fop.maze.obstacles.SpikeTrap;
import de.tum.cit.fop.maze.player.Player;

import java.util.List;

public class CombatSystem {

    private static final float PLAYER_WIDTH = 32f;
    private static final float PLAYER_HEIGHT = 64f;


    // Cordinate combat logic for obstacles and enemies
    public void update(float delta, Player player, Maze maze, List<Enemy> enemies, List<Obstacle> obstacles) {
        Rectangle playerBounds = new Rectangle(
                player.getX(),
                player.getY(),
                PLAYER_WIDTH,
                PLAYER_HEIGHT
        );

        updateEnemies(delta, player, maze, enemies, playerBounds);
        updateObstacles(delta, player, obstacles, playerBounds);
    }


    // Moves enemies
    private void updateEnemies(float delta, Player player, Maze maze, List<Enemy> enemies, Rectangle playerBounds) {
        for (Enemy enemy : enemies) {
            enemy.update(delta, maze);

            if (playerBounds.overlaps(enemy.getCollisionBox())) {
                player.takeDamage(1);
            }
        }
    }


    // Update traps
    private void updateObstacles(float delta, Player player, List<Obstacle> obstacles, Rectangle playerBounds) {
        for (Obstacle obstacle : obstacles) {
            obstacle.updateCooldown(delta);

            if (obstacle.tryActivate(playerBounds)) {
                player.takeDamage(obstacle.getDamageToPlayer());
                obstacle.startCooldown();
            }

            if (obstacle instanceof SpikeTrap spikeTrap) {
                spikeTrap.update(delta);
            }
        }
    }
}

