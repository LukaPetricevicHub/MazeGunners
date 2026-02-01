package de.tum.cit.fop.maze.items;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Animation;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import de.tum.cit.fop.maze.player.Player;

/**
 * A collectable heart item that restores one life when picked up.
 *
 * The heart is animated using a looping sprite animation.
 */
public class HeartItem extends Collectable {

    private static final int FRAME_WIDTH = 16;
    private static final int FRAME_HEIGHT = 16;
    private static final int START_X = 0;
    private static final int ROW_Y = 48;
    private static final int FRAME_COUNT = 4;

    private Animation<TextureRegion> animation;
    private float stateTime = 0f;

    /**
     * Creates a heart item at the given world position.
     *
     * @param x x-position in world coordinates
     * @param y y-position in world coordinates
     */
    public HeartItem(float x, float y) {
        super(x, y, "objects.png");
        createAnimation();
    }

    private void createAnimation() {
        TextureRegion[] frames = new TextureRegion[FRAME_COUNT];

        for (int col = 0; col < FRAME_COUNT; col++) {
            frames[col] = new TextureRegion(texture, START_X + col * FRAME_WIDTH, ROW_Y, FRAME_WIDTH, FRAME_HEIGHT);
        }

        animation = new Animation<>(0.15f, frames);
        animation.setPlayMode(Animation.PlayMode.LOOP);
    }

    /**
     * Renders the animated heart if it is still active.
     */
    @Override
    public void render(SpriteBatch batch) {
        if (!isActive()) return;

        stateTime += Gdx.graphics.getDeltaTime();
        TextureRegion frame = animation.getKeyFrame(stateTime);

        batch.draw(frame, bounds.x, bounds.y, 32, 32);
    }

    /**
     * Applies the heart effect to the player.
     *
     * Increases the player's life by one and removes the item.
     */
    @Override
    public void applyEffect(Player player) {
        player.addLife();
        collect();
    }
}

