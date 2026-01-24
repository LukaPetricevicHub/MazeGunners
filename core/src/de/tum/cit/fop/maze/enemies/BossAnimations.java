package de.tum.cit.fop.maze.enemies;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Animation;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.utils.Disposable;

public class BossAnimations implements Disposable {

    // ===== FRAME SIZE =====
    // Your sheet: 8 rows × 128px wide frames
    // Height MUST match the real sprite sheet
    private static final int FRAME_W = 128;
    private static final int FRAME_H = 64; // ← 8 rows in a 512px tall image

    // ===== ROW INDICES =====
    private static final int ROW_IDLE  = 0;
    private static final int ROW_RUN   = 1;
    private static final int ROW_MELEE = 2;
    private static final int ROW_LINE  = 3;
    private static final int ROW_AOE   = 4;
    private static final int ROW_DASH  = 5;
    private static final int ROW_HURT  = 6;
    private static final int ROW_DEAD  = 7;

    private final Texture texture;

    public final Animation<TextureRegion> idle;
    public final Animation<TextureRegion> run;
    public final Animation<TextureRegion> melee;
    public final Animation<TextureRegion> line;
    public final Animation<TextureRegion> aoe;
    public final Animation<TextureRegion> dash;
    public final Animation<TextureRegion> hurt;
    public final Animation<TextureRegion> dead;

    public BossAnimations(String path) {

        texture = new Texture(Gdx.files.internal(path));

        // ---- DEBUG INFO ----
        Gdx.app.log("BOSS_ANIMS",
                "Loaded " + path +
                        " size=" + texture.getWidth() + "x" + texture.getHeight() +
                        " frame=" + FRAME_W + "x" + FRAME_H);

        TextureRegion[][] grid = TextureRegion.split(texture, FRAME_W, FRAME_H);

        Gdx.app.log("BOSS_ANIMS",
                "Grid rows=" + grid.length +
                        " cols=" + grid[0].length);

        // ---- VALIDATION ----
        if (grid.length < 8) {
            throw new RuntimeException(
                    "Boss sprite sheet INVALID.\n" +
                            "Expected 8 rows but got " + grid.length + ".\n" +
                            "Check FRAME_H or wrong Sword.png loaded."
            );
        }

        idle  = anim(grid, ROW_IDLE,  0.12f, true);
        run   = anim(grid, ROW_RUN,   0.08f, true);

        melee = anim(grid, ROW_MELEE, 0.07f, false);
        line  = anim(grid, ROW_LINE,  0.09f, false);
        aoe   = anim(grid, ROW_AOE,   0.10f, false);
        dash  = anim(grid, ROW_DASH,  0.05f, false);

        hurt  = anim(grid, ROW_HURT,  0.10f, false);
        dead  = anim(grid, ROW_DEAD,  0.12f, false);
    }

    private Animation<TextureRegion> anim(
            TextureRegion[][] grid,
            int row,
            float frameTime,
            boolean loop) {

        if (row < 0 || row >= grid.length) {
            throw new RuntimeException(
                    "BossAnimations requested row " + row +
                            " but grid has only " + grid.length + " rows."
            );
        }

        TextureRegion[] frames = grid[row];

        Animation<TextureRegion> animation =
                new Animation<>(frameTime, frames);

        animation.setPlayMode(
                loop ? Animation.PlayMode.LOOP
                        : Animation.PlayMode.NORMAL
        );

        return animation;
    }

    @Override
    public void dispose() {
        texture.dispose();
    }
}
