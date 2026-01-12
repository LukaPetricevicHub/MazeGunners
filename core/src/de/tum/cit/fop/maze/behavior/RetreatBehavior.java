package de.tum.cit.fop.maze.behavior;

import com.badlogic.gdx.math.Vector2;
import de.tum.cit.fop.maze.ai.GridPathfinder;
import de.tum.cit.fop.maze.enemies.Enemy;
import de.tum.cit.fop.maze.level.Maze;
import de.tum.cit.fop.maze.util.GridUtils;

import java.awt.Point;
import java.util.List;

public class RetreatBehavior implements BehaviorState {

    // Attributes
    private List<Point> path;
    private float timer = 0f;
    private static final float RECALC_INTERVAL = 0.7f;
    private static final float ARRIVAL_DISTANCE = 4f;


    @Override
    public BehaviorStateType getType() {
        return BehaviorStateType.RETREAT;
    }


    @Override
    public void execute(Enemy enemy, float deltaTime, Maze maze) {
        timer += deltaTime;

        if (path == null || path.isEmpty() || timer >= RECALC_INTERVAL) {

            Point enemyTile = new Point(
                    GridUtils.worldToTile(enemy.getPosition().x, maze.getTileSize()),
                    GridUtils.worldToTile(enemy.getPosition().y, maze.getTileSize())
            );

            Vector2 home = enemy.getHomePosition();
            Point homeTile = new Point(
                    GridUtils.worldToTile(home.x, maze.getTileSize()),
                    GridUtils.worldToTile(home.y, maze.getTileSize())
            );

            path = GridPathfinder.findTilePath(maze, enemyTile, homeTile);
            timer = 0f;
        }

        if (path == null || path.isEmpty())
            return;


        Point next = path.get(0);

        Vector2 target = new Vector2(
                (next.x + 0.5f) * maze.getTileSize(),
                (next.y + 0.5f) * maze.getTileSize()
        );

        Vector2 direction = target.sub(enemy.getPosition());

        if (direction.len() < ARRIVAL_DISTANCE) {
            path.remove(0);
            return;
        }

        Vector2 velocity =
                direction.nor().scl(enemy.getSpeed());

        enemy.getCollisionHandler().move(
                enemy.getPosition(),
                velocity,
                deltaTime,
                maze
        );
    }
}

