package de.tum.cit.fop.maze.items;

import de.tum.cit.fop.maze.player.Player;

public class BossRelicItem extends AbstractCollectable {

    public BossRelicItem(float x, float y) {
        super(
                x - 16f,                // center it on boss
                y - 16f,
                "skull.png"  // texture path
        );
    }

    @Override
    public void applyEffect(Player player) {
        player.addScore(1500);
        player.addExperience(200);
        player.addLife();

        // Optional future reward:
        // player.addSkillPoint();
    }
}
