package de.tum.cit.fop.maze.items;

import de.tum.cit.fop.maze.player.Player;

/**
 * Special collectable dropped by a boss.
 *
 * Grants a large score bonus, experience, and one extra life
 * when collected by the player.
 */
public class BossRelicItem extends Collectable {

    /**
     * Creates a boss relic centered on the given position.
     *
     * @param x world x-position
     * @param y world y-position
     */
    public BossRelicItem(float x, float y) {
        super(x - 16f,
                y - 16f,
                "skull.png"  // texture path
        );
    }

    /**
     * Applies the boss reward effects to the player.
     */
    @Override
    public void applyEffect(Player player) {
        player.addScore(1500);
        player.addExperience(200);
        player.addLife();
    }
}
