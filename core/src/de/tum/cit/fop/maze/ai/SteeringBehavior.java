package de.tum.cit.fop.maze.ai;

import com.badlogic.gdx.math.Vector2;
import de.tum.cit.fop.maze.map.Maze;
import de.tum.cit.fop.maze.enemies.Enemy;
import java.awt.Point;
import java.util.List;

public final class SteeringBehavior {
    // Attributes
    private static final float ARRIVAL_THRESHOLD = 4f;


    // Constructor
    private SteeringBehavior() {}


    // Move enemy along calculated path
    public static int followPath(Enemy enemy, Maze maze, List<Point> path, int currentWaypointIndex, float deltaTime) {
        if (path == null || currentWaypointIndex >= path.size())
            return currentWaypointIndex;


        Vector2 currentEnemyPosition = enemy.getPosition();
        Point targetTile = path.get(currentWaypointIndex);
        float tileSize = maze.getTileSize();
        Vector2 targetWorldPos;

        if (currentWaypointIndex > 0) {
            Point previousTile = path.get(currentWaypointIndex - 1);
            targetWorldPos = getTransitionTarget(previousTile, targetTile, currentEnemyPosition, tileSize);
        } else {
            targetWorldPos = new Vector2(targetTile.x * tileSize + tileSize * 0.5f, targetTile.y * tileSize + tileSize * 0.5f);
        }

        Vector2 distToTarget = targetWorldPos.cpy().sub(currentEnemyPosition);

        if (distToTarget.len2() <= ARRIVAL_THRESHOLD * ARRIVAL_THRESHOLD)
            return currentWaypointIndex + 1;


        Vector2 desiredVelocity = distToTarget.nor().scl(enemy.getSpeed());
        boolean hasMoved = enemy.getCollisionHandler().move(currentEnemyPosition, desiredVelocity, deltaTime, maze);
        boolean axisSuccess = false;

        if(!hasMoved && Math.abs(desiredVelocity.x) > 0.001f) {
            axisSuccess = enemy.getCollisionHandler().move(currentEnemyPosition, new Vector2(desiredVelocity.x, 0), deltaTime, maze);
        }

        if (!hasMoved && !axisSuccess && Math.abs(desiredVelocity.y) > 0.001f)
            axisSuccess = enemy.getCollisionHandler().move(currentEnemyPosition, new Vector2(0, desiredVelocity.y), deltaTime, maze);


        if(!hasMoved && !axisSuccess)
            return currentWaypointIndex + 1;

        return currentWaypointIndex;
    }


    // Clamp target to reachable edges
    private static Vector2 clampToReachableEdge(Vector2 from, Vector2 target, float tileSize) {
        Vector2 moveDir = target.cpy().sub(from);

        if (Math.abs(moveDir.x) > Math.abs(moveDir.y))
            return new Vector2(target.x, from.y);
         else
            return new Vector2(from.x, target.y);

    }


    // Calculate target for tile transitions
    private static Vector2 getTransitionTarget(Point fromTile, Point toTile, Vector2 currentPos, float tileSize) {
        float centerX = fromTile.x * tileSize + tileSize * 0.5f;
        float centerY = fromTile.y * tileSize + tileSize * 0.5f;

        if (toTile.x > fromTile.x)
            return new Vector2(fromTile.x * tileSize + tileSize, centerY);


        if(toTile.x < fromTile.x)
            return new Vector2(fromTile.x * tileSize, centerY);

        if (toTile.y > fromTile.y) {
            return new Vector2(centerX, fromTile.y * tileSize + tileSize);
        }

        if (toTile.y < fromTile.y) {
            return new Vector2(centerX, fromTile.y * tileSize);
        }

        return currentPos;
    }
}