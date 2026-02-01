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

/**
 * Renders a fog-of-war overlay on top of the game world.
 *
 * The fog hides unexplored areas and creates a visible radius
 * around the player. The visibility adapts to the camera zoom
 * so the vision distance stays consistent in world units.
 */

public class FogOfWarRenderer {

    private float radiusWorld = 3f * 32f;   
    private float softEdgeWorld = 2f * 32f; 
    private FrameBuffer fbo;
    private Texture black1x1;
    private Texture holeMask;

    private int screenW, screenH;
    private final Matrix4 screenProj = new Matrix4();
    private float fogAlpha = 0.75f;

    private final Vector3 tmp = new Vector3();

    /**
     * Creates a new fog-of-war renderer.
     *
     * Initializes textures and frame buffers based on the
     * current screen size.
     */
    public FogOfWarRenderer() {
        black1x1 = createSolidTexture(1, 1, 0x000000ff);
        holeMask = createRadialMask(256, 256); 
        resize(Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
    }

    /**
     * Updates internal buffers when the screen size changes.
     *
     * @param width  new screen width
     * @param height new screen height
     */
    public void resize(int width, int height) {
        screenW = Math.max(1, width);
        screenH = Math.max(1, height);

        if (fbo != null) fbo.dispose();
        fbo = new FrameBuffer(Format.RGBA8888, screenW, screenH, false);

        
        screenProj.setToOrtho2D(0, 0, screenW, screenH);
    }
    public void setFogAlpha(float fogAlpha) { this.fogAlpha = clamp01(fogAlpha); }
    public void setRadiusWorld(float r) { radiusWorld = Math.max(1f, r); }
    public void setSoftEdgeWorld(float s) { softEdgeWorld = Math.max(0f, s); }

    /**
     * Draw fog overlay on top of the world.
     * Call AFTER world is rendered (after batch.end()).
     */
    public void render(SpriteBatch batch, OrthographicCamera worldCam, float playerWorldX, float playerWorldY) {
        render(batch, worldCam, playerWorldX, playerWorldY, null, 0f);
    }

    public void render(SpriteBatch batch,
                       OrthographicCamera worldCam,
                       float playerWorldX,
                       float playerWorldY,
                       List<Bullet> bullets,
                       float bulletRadiusWorld) {
        if (fbo == null) return;

        
        tmp.set(playerWorldX, playerWorldY, 0f);
        worldCam.project(tmp);

        float pxScreen = tmp.x;
        float pyScreen = screenH - tmp.y;


        float zoom = worldCam.zoom;
        float radiusScreen = radiusWorld / zoom;
        float softScreen   = softEdgeWorld / zoom;

        
        fbo.begin();
        Gdx.gl.glClearColor(0, 0, 0, 0);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        batch.setProjectionMatrix(screenProj);
        batch.begin();

        Gdx.gl.glEnable(GL20.GL_BLEND);

        
        batch.setBlendFunction(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);
        batch.setColor(0f, 0f, 0f, fogAlpha);
        batch.draw(black1x1, 0, 0, screenW, screenH);

        
        batch.setColor(1f, 1f, 1f, 1f);
        batch.setBlendFunctionSeparate(
                GL20.GL_ZERO, GL20.GL_ONE,
                GL20.GL_ZERO, GL20.GL_ONE_MINUS_SRC_ALPHA
        );

        float size = (radiusScreen + softScreen) * 2f;
        batch.draw(holeMask, pxScreen - size / 2f, pyScreen - size / 2f, size, size);

        
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

        
        batch.setBlendFunction(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);
        batch.setColor(1f, 1f, 1f, 1f);

        batch.end();
        fbo.end();

        
        Texture fogTex = fbo.getColorBufferTexture();
        fogTex.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);

        batch.setProjectionMatrix(screenProj);
        batch.begin();

        batch.setBlendFunction(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);


        batch.draw(fogTex, 0, 0, screenW, screenH, 0, 1, 1, 0);
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

                float a = 1f - (d / maxR); 
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
