package de.tum.cit.fop.maze.obstacles;

import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Animation;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.Array;

public class SpikeTrap extends Obstacle {

    // Attributes
    private static final int damage = 1;
    private static final float cooldown = 1.5f;
    private static final float width = 16f;
    private static final float height = 16f;
    private Animation<TextureRegion> spikeAnimation;
    private float animationTime = 0f;


    // Creates spike trap at given position
    public SpikeTrap(Vector2 startWorldPosition) {
        super(startWorldPosition, damage, cooldown, width, height);
        loadSpikeAnimation();
    }


    // Loads spike animation frames
    private void loadSpikeAnimation() {
        Texture spriteSheet = new Texture("things.png");

        int startX = 0;
        int startY = 0;
        int frameWidth = 16;
        int frameHeight = 16;

        Array<TextureRegion> frames = new Array<>();

        for (int col = 0; col < 2; col++) {
            frames.add(new TextureRegion(
                    spriteSheet,
                    startX + col * frameWidth,
                    startY,
                    frameWidth,
                    frameHeight
            ));
        }

        spikeAnimation = new Animation<>(0.4f, frames);
        spikeAnimation.setPlayMode(Animation.PlayMode.LOOP);
    }


    // Update animation timing
    public void update(float deltaTime) {
        animationTime += deltaTime;
    }


    // Return current spike animation frame
    public TextureRegion getCurrentFrame() {
        return spikeAnimation.getKeyFrame(animationTime, true);
    }
}

