package de.tum.cit.fop.maze.items;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Animation;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import de.tum.cit.fop.maze.player.Player;

public class Coin extends Collectable {

    private final int amount;

    // animation
    private final Texture sheet;
    private final Animation<TextureRegion> animation;
    private float stateTime = 0f;

    /**
     * Assumes coin.png is a spritesheet
     * with frames of equal size.
     */
    public Coin(float x, float y) {
        this(x, y, 1);
    }

    public Coin(float x, float y, int amount) {
        super(x, y, "coin.png");
        this.amount = amount;

        // We don't use Collectable.texture anymore
        this.texture.dispose();
        this.texture = null;

        sheet = new Texture("coin.png");

        //  IMPORTANT: CHANGE THESE VALUES
        int FRAME_WIDTH = 16;   // ← adjust to your sprite
        int FRAME_HEIGHT = 16;

        TextureRegion[][] split = TextureRegion.split(sheet, FRAME_WIDTH, FRAME_HEIGHT);

        // Flatten frames (supports multiple rows)
        int rows = split.length;
        int cols = split[0].length;

        TextureRegion[] frames = new TextureRegion[rows * cols];
        int index = 0;

        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < cols; c++) {
                frames[index++] = split[r][c];
            }
        }

        animation = new Animation<>(0.10f, frames);
        animation.setPlayMode(Animation.PlayMode.LOOP);
    }

    @Override
    public void applyEffect(Player player) {
        player.addCoins(amount);
        collect();
    }

    @Override
    public void render(SpriteBatch batch) {
        if (!isActive()) return;

        stateTime += Gdx.graphics.getDeltaTime();
        TextureRegion frame = animation.getKeyFrame(stateTime, true);

        batch.draw(frame, bounds.x, bounds.y, 32, 32);
    }

    @Override
    public void dispose() {
        if (sheet != null) sheet.dispose();
    }
}
