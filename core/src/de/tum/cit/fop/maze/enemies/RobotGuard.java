package de.tum.cit.fop.maze.enemies;

import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Animation;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.Array;
import de.tum.cit.fop.maze.behavior.PatrolBehavior;
import de.tum.cit.fop.maze.player.Player;

public class RobotGuard extends Enemy {

    private Animation<TextureRegion> walkAnimation;
    private float stateTime = 0f;

    public RobotGuard(Vector2 position, float speed, Player player) {
        super(position, speed, new PatrolBehavior(testPatrolPoints()), player);
        loadGuardAnimation();
    }

    private void loadGuardAnimation() {
        Texture guardSheet = new Texture("mobs.png"); // Adjust for later design png
        int startX = 99;
        int startY = 64;
        int frameWidth = 44 / 3;
        int frameHeight = 64 / 4;

        Array<TextureRegion> frames = new Array<>();
        for (int col = 0; col < 3; col++) { // 3 frames
            frames.add(new TextureRegion(
                    guardSheet,
                    startX + col * frameWidth,
                    startY,
                    frameWidth,
                    frameHeight
            ));
        }

        walkAnimation = new Animation<>(0.2f, frames);
        walkAnimation.setPlayMode(Animation.PlayMode.LOOP);
    }

    public TextureRegion getCurrentFrame() {
        return walkAnimation.getKeyFrame(stateTime, true);
    }

    @Override
    protected void updateEnemyPosition(float deltaTime) {
        stateTime += deltaTime; // advance animation
        if (currentBehavior != null)
            currentBehavior.execute(this, deltaTime);
    }

    private static Array<Vector2> testPatrolPoints() {
        Array<Vector2> patrolPoints = new Array<>();
        patrolPoints.add(new Vector2(2, 2));
        patrolPoints.add(new Vector2(2, 5));
        patrolPoints.add(new Vector2(5, 2));
        patrolPoints.add(new Vector2(5, 5));
        return patrolPoints;
    }
}
