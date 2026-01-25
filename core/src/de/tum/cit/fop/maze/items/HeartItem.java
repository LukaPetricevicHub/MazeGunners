package de.tum.cit.fop.maze.items;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Animation;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import de.tum.cit.fop.maze.player.Player;

public class HeartItem extends Collectable {

    private static final int FRAME_WIDTH = 16;
    private static final int FRAME_HEIGHT = 16;
    private static final int START_X = 0;
    private static final int ROW_Y = 48;
    private static final int FRAME_COUNT = 4;

    private Animation<TextureRegion> animation;
    private float stateTime = 0f;

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

    @Override
    public void render(SpriteBatch batch) {
        if (!isActive()) return;

        stateTime += Gdx.graphics.getDeltaTime();
        TextureRegion frame = animation.getKeyFrame(stateTime);

        batch.draw(frame, bounds.x, bounds.y, 32, 32);
    }

    @Override
    public void applyEffect(Player player) {
        player.addLife();
        collect();
    }
}

