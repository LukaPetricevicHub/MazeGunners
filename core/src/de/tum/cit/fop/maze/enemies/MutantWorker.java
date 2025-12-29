package de.tum.cit.fop.maze.enemies;

import com.badlogic.gdx.graphics.g2d.Animation;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.Array;
import de.tum.cit.fop.maze.behavior.BehaviorState;

public class MutantWorker extends Enemy {

    private Animation<TextureRegion> walkAnimation;
    private float stateTime = 0f;

    public MutantWorker(Vector2 position, float speed, BehaviorState currentBehavior) {
        super(position, speed, currentBehavior);
        loadWalkAnimation();
    }

    private void loadWalkAnimation() {
        Texture walkSheet = new Texture("character.png"); // Use the mutant_worker.png when done
        int frameWidth = 16;
        int frameHeight = 32;
        int animationFrames = 4;

        Array<TextureRegion> frames = new Array<>(TextureRegion.class);
        for (int i = 0; i < animationFrames; i++) {
            frames.add(new TextureRegion(walkSheet, i * frameWidth, 0, frameWidth, frameHeight));
        }

        walkAnimation = new Animation<>(0.1f, frames);
    }

    public void updateAnimation(float delta) {
        stateTime += delta;
    }

    public Animation<TextureRegion> getWalkAnimation() {
        return walkAnimation;
    }

    public float getStateTime() {
        return stateTime;
    }

    @Override
    public Vector2 getPlayerPosition() {
        return new Vector2(5, 5); // Placeholder target
    }
}