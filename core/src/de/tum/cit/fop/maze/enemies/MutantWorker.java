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
        loadMutantWorkerWalkAnimation();
    }

    private void loadMutantWorkerWalkAnimation() {
        Texture walkSheet = new Texture("mobs.png"); // Use the mutant_worker.png when done
        int mutantWorkerX = 148;
        int mutantWorkerY = 0;
        int mutantWorkerWidth = 44 / 3;
        int mutantWorkerHeight = 64 / 4;

        Array<TextureRegion> frames = new Array<>(TextureRegion.class);

        for (int col = 0; col < 3; col++) {
            frames.add(new TextureRegion(walkSheet, mutantWorkerX + col *  mutantWorkerWidth, 0, mutantWorkerWidth, mutantWorkerHeight));
        }

        walkAnimation = new Animation<>(0.15f, frames);
        walkAnimation.setPlayMode(Animation.PlayMode.LOOP);
    }

    public void updateAnimation(float delta) {
        stateTime += delta;
        updateEnemyPosition(delta);
    }

    public TextureRegion getCurrentFrame() {
        return walkAnimation.getKeyFrame(stateTime, true);
    }

    public float getStateTime() {
        return stateTime;
    }

    @Override
    public Vector2 getPlayerPosition() {
        return new Vector2(5, 5); // Placeholder target
    }
}