package de.tum.cit.fop.maze.enemies;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Animation;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.utils.Disposable;

 public class BossAnimations implements Disposable {

    private static final int FRAME_W = 128;
    private static final int FRAME_H = 128;

    private static final int ROW_IDLE  = 0;
    private static final int ROW_RUN   = 1;
    private static final int ROW_MELEE = 2;
    private static final int ROW_LINE  = 3;
    private static final int ROW_AOE   = 4;
    private static final int ROW_DASH  = 5;

    private final Texture tex;

    public final Animation<TextureRegion> idle;
    public final Animation<TextureRegion> run;
    public final Animation<TextureRegion> melee;
    public final Animation<TextureRegion> line;
    public final Animation<TextureRegion> aoe;
    public final Animation<TextureRegion> dash;

    public BossAnimations(String path) {
        tex = new Texture(Gdx.files.internal(path));
        TextureRegion[][] grid = TextureRegion.split(tex, FRAME_W, FRAME_H);

        idle  = anim(grid, ROW_IDLE, 0.10f, true);
        run   = anim(grid, ROW_RUN,  0.08f, true);

        melee = anim(grid, ROW_MELEE, 0.08f, false);
        line  = anim(grid, ROW_LINE,  0.09f, false);
        aoe   = anim(grid, ROW_AOE,   0.10f, false);
        dash  = anim(grid, ROW_DASH,  0.06f, false);
    }

    private Animation<TextureRegion> anim(TextureRegion[][] grid, int row, float frameTime, boolean loop) {
        TextureRegion[] frames = grid[row];
        Animation<TextureRegion> a = new Animation<>(frameTime, frames);
        a.setPlayMode(loop ? Animation.PlayMode.LOOP : Animation.PlayMode.NORMAL);
        return a;
    }

    @Override
    public void dispose() {
        tex.dispose();
    }
}
