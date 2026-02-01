package de.tum.cit.fop.maze.enemies;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Animation;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.utils.Disposable;

/**
 * Holds and manages all animations used by the boss.
 *
 * Loads a sprite sheet and splits it into animations
 * for different boss states like idle, running and attacking.
 */
public class BossAnimations implements Disposable {

    private static final int FRAME_W = 128;
    private static final int FRAME_H = 64;

    // Rows 0-7
    private static final int ROW_IDLE   = 0;
    private static final int ROW_WALK   = 1;
    private static final int ROW_RUN_1  = 2;
    private static final int ROW_RUN_2  = 3;
    private static final int ROW_ATTACK = 4; // "normal"
    private static final int ROW_COMBO  = 5;
    private static final int ROW_HIT    = 6;
    private static final int ROW_DEATH  = 7;

    private final Texture texture;

    public final Animation<TextureRegion> idle;
    public final Animation<TextureRegion> walk;
    public final Animation<TextureRegion> run;
    public final Animation<TextureRegion> run2;
    public final Animation<TextureRegion> attack;
    public final Animation<TextureRegion> combo;
    public final Animation<TextureRegion> hit;
    public final Animation<TextureRegion> death;

    /**
     * Loads the boss sprite sheet and creates all animations.
     *
     * @param path internal path to the sprite sheet texture
     */
    public BossAnimations(String path) {
        texture = new Texture(Gdx.files.internal(path));

        Gdx.app.log("BOSS_ANIMS",
                "Loaded " + path +
                        " size=" + texture.getWidth() + "x" + texture.getHeight() +
                        " frame=" + FRAME_W + "x" + FRAME_H);

        TextureRegion[][] grid = TextureRegion.split(texture, FRAME_W, FRAME_H);

        Gdx.app.log("BOSS_ANIMS",
                "Grid rows=" + grid.length +
                        " cols=" + grid[0].length);

        if (grid.length < 8) {
            throw new RuntimeException(
                    "Boss sprite sheet INVALID.\n" +
                            "Expected 8 rows but got " + grid.length + ".\n" +
                            "Check FRAME_H or wrong png loaded."
            );
        }

        idle = anim(grid, ROW_IDLE,   0.12f, true);
        walk = anim(grid, ROW_WALK,   0.10f, true);

        run = anim(grid, ROW_RUN_1,  0.08f, true);
        run2 = anim(grid, ROW_RUN_2,  0.07f, true);

        attack = anim(grid, ROW_ATTACK, 0.07f, false);
        combo = anim(grid, ROW_COMBO,  0.06f, false);

        hit = anim(grid, ROW_HIT,    0.10f, false);
        death = anim(grid, ROW_DEATH,  0.12f, false);
    }

    private Animation<TextureRegion> anim(TextureRegion[][] grid, int row, float frameTime, boolean loop) {
        if (row < 0 || row >= grid.length) {
            throw new RuntimeException(
                    "BossAnimations requested row " + row +
                            " but grid has only " + grid.length + " rows."
            );
        }

        TextureRegion[] frames = grid[row];
        Animation<TextureRegion> a = new Animation<>(frameTime, frames);
        a.setPlayMode(loop ? Animation.PlayMode.LOOP : Animation.PlayMode.NORMAL);
        return a;
    }

    /**
     * Disposes the underlying texture.
     *
     * Called when the animations
     * are no longer needed.
     */
    @Override
    public void dispose() {
        texture.dispose();
    }
}
