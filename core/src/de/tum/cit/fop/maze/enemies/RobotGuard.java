package de.tum.cit.fop.maze.enemies;

import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Animation;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.Array;
import de.tum.cit.fop.maze.player.Player;
import de.tum.cit.fop.maze.behavior.PatrolBehavior;

public class RobotGuard extends Enemy {

    private Animation<TextureRegion> walkAnimation;
    private float stateTime = 0f;

    private Array<Vector2> patrolPoints;
    private int currentPatrolIndex = 0;

    public RobotGuard(Vector2 position, float speed, Player player) {
        super(position, speed, null, player);
        patrolPoints = testPatrolPoints(); // define patrol points
        loadGuardAnimation();
    }

    private void loadGuardAnimation() {
        Texture guardSheet = new Texture("mobs.png"); // adjust for design
        int startX = 98;   // X start in sprite sheet
        int startY = 64;   // Y start in sprite sheet
        int frameWidth = 46 / 3;
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
        stateTime += deltaTime;

        if (patrolPoints == null || patrolPoints.size == 0) return;

        // get current target patrol point
        Vector2 target = patrolPoints.get(currentPatrolIndex);

        // compute direction to target
        Vector2 direction = target.cpy().sub(position);
        float distance = direction.len();

        if (distance < 1f) { // reached point → move to next
            currentPatrolIndex = (currentPatrolIndex + 1) % patrolPoints.size;
            target = patrolPoints.get(currentPatrolIndex);
            direction = target.cpy().sub(position);
        }

        direction.nor();
        position.mulAdd(direction, speed * deltaTime);
    }

    private static Array<Vector2> testPatrolPoints() {
        Array<Vector2> patrolPoints = new Array<>();
        patrolPoints.add(new Vector2(200, 300));
        patrolPoints.add(new Vector2(200, 500));
        patrolPoints.add(new Vector2(500, 500));
        patrolPoints.add(new Vector2(500, 300));
        return patrolPoints;
    }
}
