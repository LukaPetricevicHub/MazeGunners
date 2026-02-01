package de.tum.cit.fop.maze;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.Pixmap.Format;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.FrameBuffer;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.math.Vector3;
import de.tum.cit.fop.maze.player.Bullet;

import java.util.List;

public class FogOfWarRenderer {

    private float radiusWorld = 3f * 32f;   // e.g. 3 tiles * TILE_SIZE
    private float softEdgeWorld = 2f * 32f; // optional: also in world units
    private FrameBuffer fbo;
    private Texture black1x1;
    private Texture holeMask;

    private int screenW, screenH;
    private final Matrix4 screenProj = new Matrix4();
    private float fogAlpha = 0.75f;

    private final Vector3 tmp = new Vector3();

    public FogOfWarRenderer() {
        black1x1 = createSolidTexture(1, 1, 0x000000ff);
        holeMask = createRadialMask(256, 256); // alpha=1 center -> 0 edge
        resize(Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
    }

    public void resize(int width, int height) {
        screenW = Math.max(1, width);
        screenH = Math.max(1, height);

        if (fbo != null) fbo.dispose();
        fbo = new FrameBuffer(Format.RGBA8888, screenW, screenH, false);

        // y-up ortho for screen drawing
        screenProj.setToOrtho2D(0, 0, screenW, screenH);
    }
    public void setFogAlpha(float fogAlpha) { this.fogAlpha = clamp01(fogAlpha); }
    public void setRadiusWorld(float r) { radiusWorld = Math.max(1f, r); }
    public void setSoftEdgeWorld(float s) { softEdgeWorld = Math.max(0f, s); }

    /**
     * Draw fog overlay on top of the world.
     * Call AFTER world is rendered (after batch.end()).
     */
    public void render(SpriteBatch batch,
                       OrthographicCamera worldCam,
                       float playerWorldX,
                       float playerWorldY) {
        render(batch, worldCam, playerWorldX, playerWorldY, null, 0f);
    }

    public void render(SpriteBatch batch,
                       OrthographicCamera worldCam,
                       float playerWorldX,
                       float playerWorldY,
                       List<Bullet> bullets,
                       float bulletRadiusWorld) {
        if (fbo == null) return;

        // Convert player position from world -> screen coordinates (pixels)
        tmp.set(playerWorldX, playerWorldY, 0f);
        worldCam.project(tmp);

        float pxScreen = tmp.x;
        float pyScreen = screenH - tmp.y;

        // Convert WORLD radius -> SCREEN radius (so zoom doesn't change gameplay vision distance)
        float zoom = worldCam.zoom;                 // <1 zoom in, >1 zoom out
        float radiusScreen = radiusWorld / zoom;    // keep world-distance constant
        float softScreen   = softEdgeWorld / zoom;  // optional: keep softness constant in world too

        // 1) Render fog into FBO (screen-sized)
        fbo.begin();
        Gdx.gl.glClearColor(0, 0, 0, 0);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        batch.setProjectionMatrix(screenProj);
        batch.begin();

        Gdx.gl.glEnable(GL20.GL_BLEND);

        // draw full-screen dark fog
        batch.setBlendFunction(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);
        batch.setColor(0f, 0f, 0f, fogAlpha);
        batch.draw(black1x1, 0, 0, screenW, screenH);

        // 2) Punch hole in the fog layer (make fog alpha transparent near player)
        // alpha becomes: dstA = dstA * (1 - srcA)
        batch.setColor(1f, 1f, 1f, 1f);
        batch.setBlendFunctionSeparate(
                GL20.GL_ZERO, GL20.GL_ONE,                 // keep RGB
                GL20.GL_ZERO, GL20.GL_ONE_MINUS_SRC_ALPHA  // reduce alpha using mask
        );

        float size = (radiusScreen + softScreen) * 2f;
        batch.draw(holeMask,
                pxScreen - size / 2f,
                pyScreen - size / 2f,
                size, size);

        // 2b) Bullet light holes (temporary)
        if (bullets != null && bulletRadiusWorld > 0f) {
            float bulletRadiusScreen = bulletRadiusWorld / zoom;
            float bulletSoftScreen = Math.max(1f, softScreen * 0.5f);
            float bulletSize = (bulletRadiusScreen + bulletSoftScreen) * 2f;

            for (Bullet b : bullets) {
                if (b == null || !b.isAlive()) continue;

                tmp.set(b.getX(), b.getY(), 0f);
                worldCam.project(tmp);

                float bxScreen = tmp.x;
                float byScreen = screenH - tmp.y;

                batch.draw(holeMask,
                        bxScreen - bulletSize / 2f,
                        byScreen - bulletSize / 2f,
                        bulletSize, bulletSize);
            }
        }

        // restore defaults
        batch.setBlendFunction(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);
        batch.setColor(1f, 1f, 1f, 1f);

        batch.end();
        fbo.end();

        // 3) Draw fog texture to screen (on top of the already-rendered world)
        Texture fogTex = fbo.getColorBufferTexture();
        fogTex.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);

        batch.setProjectionMatrix(screenProj);
        batch.begin();

        batch.setBlendFunction(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);

        // libGDX FBO texture is typically flipped vertically
        batch.draw(fogTex,
                0, 0, screenW, screenH,
                0, 1, 1, 0);
        batch.end();
    }


    public void dispose() {
        if (fbo != null) fbo.dispose();
        if (black1x1 != null) black1x1.dispose();
        if (holeMask != null) holeMask.dispose();
        fbo = null;
        black1x1 = null;
        holeMask = null;
    }

    // -------- helpers --------

    private static Texture createSolidTexture(int w, int h, int rgba8888) {
        Pixmap pm = new Pixmap(w, h, Format.RGBA8888);
        pm.setColor(
                ((rgba8888 >> 24) & 0xff) / 255f,
                ((rgba8888 >> 16) & 0xff) / 255f,
                ((rgba8888 >> 8) & 0xff) / 255f,
                (rgba8888 & 0xff) / 255f
        );
        pm.fill();
        Texture t = new Texture(pm);
        pm.dispose();
        return t;
    }

    // alpha=1 center -> 0 edge (used as "eraser")
    private static Texture createRadialMask(int w, int h) {
        Pixmap pm = new Pixmap(w, h, Format.RGBA8888);

        float cx = (w - 1) / 2f;
        float cy = (h - 1) / 2f;
        float maxR = Math.min(w, h) * 0.5f;

        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                float dx = x - cx;
                float dy = y - cy;
                float d = (float) Math.sqrt(dx * dx + dy * dy);

                float a = 1f - (d / maxR); // center 1 -> edge 0
                a = clamp01(a);

                pm.setColor(1f, 1f, 1f, a);
                pm.drawPixel(x, y);
            }
        }

        Texture t = new Texture(pm);
        pm.dispose();
        return t;
    }

    private static float clamp01(float v) {
        return Math.max(0f, Math.min(1f, v));
    }
}
