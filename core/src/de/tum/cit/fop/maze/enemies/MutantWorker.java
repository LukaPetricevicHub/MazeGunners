package de.tum.cit.fop.maze.enemies;

import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Animation;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.Array;
import de.tum.cit.fop.maze.player.Player;

public class MutantWorker extends Enemy {

    private boolean isChasing = false;
    private float detectionRange = 150f; // pixels
    private Animation<TextureRegion> walkAnimation;
    private float stateTime = 0f;

    public MutantWorker(Vector2 position, float speed, Player player) {
        super(position, speed, null, player); // no behavior needed for straight-line chase
        loadMutantWorkerWalkAnimation();
    }

    private void loadMutantWorkerWalkAnimation() {
        Texture walkSheet = new Texture("mobs.png");
        int mutantWorkerX = 147;
        int mutantWorkerY = 0;
        int mutantWorkerWidth = 44 / 3;
        int mutantWorkerHeight = 64 / 4;

        Array<TextureRegion> frames = new Array<>(TextureRegion.class);
        for (int col = 0; col < 3; col++) {
            frames.add(new TextureRegion(
                    walkSheet,
                    mutantWorkerX + col * mutantWorkerWidth,
                    mutantWorkerY,
                    mutantWorkerWidth,
                    mutantWorkerHeight
            ));
        }

        walkAnimation = new Animation<>(0.15f, frames);
        walkAnimation.setPlayMode(Animation.PlayMode.LOOP);
    }

    public void update(float delta) {
        stateTime += delta;

        if (!isChasing && player != null) {
            float distance = position.dst(player.getX(), player.getY());
            if(distance <= detectionRange) {
                isChasing = true;
            }
        }
        if (isChasing) {
            Vector2 target = getPlayerPosition();
            Vector2 direction = target.cpy().sub(position);
            direction.nor(); // normalize direction
            position.mulAdd(direction, speed * delta); // move enemy
        }
    }

    public TextureRegion getCurrentFrame() {
        return walkAnimation.getKeyFrame(stateTime, true);
    }

    @Override
    public Vector2 getPlayerPosition() {
        return new Vector2(player.getX(), player.getY());
    }
}
