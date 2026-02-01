package de.tum.cit.fop.maze.behavior;

import com.badlogic.gdx.math.Vector2;
import de.tum.cit.fop.maze.ai.GridPathfinder;
import de.tum.cit.fop.maze.enemies.Enemy;
import de.tum.cit.fop.maze.map.Maze;
import de.tum.cit.fop.maze.util.GridUtils;
import java.awt.Point;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Behavior where an enemy runs away from the player.
 *
 * The enemy looks for nearby walkable tiles that are far from the player
 * and follows a path to one of those tiles.
 * The escape path is recalculated from time to time or when the player moves.
 */
public class EvadeBehavior implements BehaviorState {

    private static final float RECALC_COOLDOWN = 0.6f;
    private static final float ARRIVAL_RANGE = 4f;
    private static final int SCAN_RANGE = 10;

    private List<Point> escapeRoute = List.of();
    private int currentWaypoint = 0;
    private float timerSinceLastPath = 0f;
    private Point previousEnemyTile = null;
    private Point previousPlayerTile = null;

    /**
     * Returns the behavior type.
     *
     * @return EVADE behavior type
     */
    @Override
    public BehaviorStateType getType() {
        return BehaviorStateType.EVADE;
    }


    /**
     * Executes evade logic for the enemy.
     *
     * The enemy:
     * - Finds a safe tile away from the player
     * - Calculates an escape path
     * - Moves along that path using collision handling
     *
     * @param enemy the enemy using this behavior
     * @param deltaTime time since last frame
     * @param maze the maze used for movement and pathfinding
     */
    @Override
    public void execute(Enemy enemy, float deltaTime, Maze maze) {
        timerSinceLastPath += deltaTime;

        Point enemyPosTile = worldToTileCoords(enemy.getPosition(), maze);
        Point playerPosTile = worldToTileCoords(enemy.getPlayerPosition(), maze);

        boolean pathBroken = escapeRoute.isEmpty() || currentWaypoint >= escapeRoute.size();
        boolean needsNewEscape = pathBroken || previousEnemyTile == null || previousPlayerTile == null|| !playerPosTile.equals(previousPlayerTile) || timerSinceLastPath >= RECALC_COOLDOWN;

        if (needsNewEscape) {
            Point fleeTarget = findSafeSpot(enemyPosTile, playerPosTile, maze);

            if (fleeTarget != null && !fleeTarget.equals(enemyPosTile)) {
                escapeRoute = GridPathfinder.findPath(maze, enemyPosTile, fleeTarget);
                currentWaypoint = 0;
            } else
                escapeRoute = List.of();


            previousEnemyTile = enemyPosTile;
            previousPlayerTile = playerPosTile;
            timerSinceLastPath = 0f;
        }

        if (escapeRoute.isEmpty() || currentWaypoint >= escapeRoute.size()) {
            return;
        }

        Point targetPoint = escapeRoute.get(currentWaypoint);
        Vector2 worldTarget = getTileCenter(targetPoint, maze);

        Vector2 currentPos = enemy.getPosition();
        Vector2 offset = worldTarget.cpy().sub(currentPos);

        if(offset.len2() < ARRIVAL_RANGE * ARRIVAL_RANGE) {
            currentWaypoint++;
            return;
        }

        Vector2 runVelocity = offset.nor().scl(enemy.getSpeed());

        enemy.getCollisionHandler().move(currentPos, runVelocity, deltaTime, maze);
    }


    /**
     * Converts world coordinates to tile coordinates.
     *
     * @param worldPos World position
     * @param maze Maze reference
     * @return Tile coordinates
     */

    private static Point worldToTileCoords(Vector2 worldPos, Maze maze) {
        return new Point(
                GridUtils.worldToTile(worldPos.x, maze.getTileSize()),
                GridUtils.worldToTile(worldPos.y, maze.getTileSize())
        );
    }


    /**
     * Computes the world-space center of a tile.
     *
     * @param p Tile coordinates
     * @param maze Maze reference
     * @return Center position in world units
     */

    private static Vector2 getTileCenter(Point p, Maze maze) {
        float size = maze.getTileSize();

        return new Vector2(p.x * size + size * 0.5f, p.y * size + size * 0.5f);
    }


    /**
     * Finds a safe tile away from the player.
     *
     * @param enemyLoc Enemy tile position
     * @param playerLoc Player tile position
     * @param maze Maze reference
     * @return Safe tile to flee to, or null if none found
     */
    private Point findSafeSpot(Point enemyLoc, Point playerLoc, Maze maze) {
        List<Point> potentialTiles = new ArrayList<>();
        Random randomGen = new Random();

        for (int dy = -SCAN_RANGE; dy <= SCAN_RANGE; dy++) {
            for (int dx = -SCAN_RANGE; dx <= SCAN_RANGE; dx++) {
                int testX = enemyLoc.x + dx;
                int testY = enemyLoc.y + dy;

                if(!maze.inBounds(testX, testY))
                    continue;


                if (!maze.getTile(testX, testY).isWalkable()) {
                    continue;
                }

                int distFromThreat = Math.abs(testX - playerLoc.x) + Math.abs(testY - playerLoc.y);

                if (distFromThreat > SCAN_RANGE / 2)
                    potentialTiles.add(new Point(testX, testY));

            }
        }

        if(potentialTiles.isEmpty()) return null;

        return potentialTiles.get(randomGen.nextInt(potentialTiles.size()));
    }
}
