package de.tum.cit.fop.maze.ai;

import de.tum.cit.fop.maze.map.Maze;
import java.awt.Point;
import java.util.*;

/**
 * Grid-based A* pathfinding utility for enemies.
 *
 * Supports variable enemy footprints and avoids walls and occupied tiles.
 * All paths are computed on tile coordinates.
 */

public final class GridPathfinder {

    private static final int[][] cardDirections = {{1, 0}, {-1, 0}, {0, 1}, {0, -1}};

    private static class Node {
        final Point coordinates;
        int costFromStart;
        int estimatedTotalCost;
        Node parentNode;

        Node(Point coordinates, int costFromStart, int estimatedTotalCost, Node parent) {
            this.coordinates = coordinates;
            this.costFromStart = costFromStart;
            this.estimatedTotalCost = estimatedTotalCost;
            this.parentNode = parent;
        }
    }

    private GridPathfinder() {}

    /**
     * Finds a path from a start tile to a goal tile using A* pathfinding.
     *
     * The path respects maze bounds, walkable tiles, enemy footprints,
     * and existing enemy positions.
     *
     * @param maze the maze
     * @param start start tile
     * @param goal goal tile
     * @param footprint size of the enemy footprint in tiles
     * @return list of tiles from start to goal, or an empty list if no path exists
     */
    public static List<Point> findPath(Maze maze, Point start, Point goal, int footprint) {
        if (!isValidPlacement(start.x, start.y, maze, footprint, start))
            return List.of();

        if (!isValidPlacement(goal.x, goal.y, maze, footprint, start))
            return List.of();

        if (start.equals(goal))
            return List.of(start);

        PriorityQueue<Node> nodesToVisit =
                new PriorityQueue<>(Comparator.comparingInt(n -> n.estimatedTotalCost));

        Map<Point, Integer> cheapestPrice = new HashMap<>();

        nodesToVisit.add(new Node(
                start,
                0,
                calculateManhattanDistance(start, goal),
                null
        ));
        cheapestPrice.put(start, 0);

        while (!nodesToVisit.isEmpty()) {
            Node current = nodesToVisit.poll();

            if (current.coordinates.equals(goal))
                return reconstructPath(current);

            for (int[] d : cardDirections) {
                int nx = current.coordinates.x + d[0];
                int ny = current.coordinates.y + d[1];

                if (!maze.inBounds(nx, ny))
                    continue;

                if (!isValidPlacement(nx, ny, maze, footprint, start))
                    continue;

                Point next = new Point(nx, ny);
                int newCost = current.costFromStart + 1;

                Integer best = cheapestPrice.get(next);
                if (best != null && newCost >= best)
                    continue;

                cheapestPrice.put(next, newCost);
                nodesToVisit.add(new Node(
                        next,
                        newCost,
                        newCost + calculateManhattanDistance(next, goal),
                        current
                ));
            }
        }

        return List.of();
    }

    private static int calculateManhattanDistance(Point a, Point b) {
        return Math.abs(a.x - b.x) + Math.abs(a.y - b.y);
    }

    private static List<Point> reconstructPath(Node goalNode) {
        LinkedList<Point> path = new LinkedList<>();
        Node traversalNode = goalNode;

        while (traversalNode != null) {
            path.addFirst(traversalNode.coordinates);
            traversalNode = traversalNode.parentNode;
        }

        return path;
    }

    /**
     * Checks whether an enemy footprint can be placed at a given tile position.
     *
     * A placement is valid if:
     * - All tiles covered by the footprint are in bounds
     * - All tiles are walkable
     * - No other enemy occupies the footprint area
     *
     * The enemy's own footprint at the start position is allowed so that
     * pathfinding does not immediately invalidate the starting node.
     *
     * @param x top-left x tile of the footprint
     * @param y top-left y tile of the footprint
     * @param maze the maze
     * @param footprint size of the square footprint (in tiles)
     * @param start original start tile (used to allow self-overlap)
     * @return true if placement is valid
     */
    private static boolean isValidPlacement(int x, int y, Maze maze, int footprint, Point start) {
        for (int dx = 0; dx < footprint; dx++) {
            for (int dy = 0; dy < footprint; dy++) {
                int tx = x + dx;
                int ty = y + dy;

                if (!maze.inBounds(tx, ty))
                    return false;

                if (!maze.getTile(tx, ty).isWalkable())
                    return false;

                if (maze.isEnemyOccupied(tx, ty)) {
                    
                    if (start == null) return false;

                    boolean isOwnFootprint =
                            tx >= start.x && tx < start.x + footprint &&
                                    ty >= start.y && ty < start.y + footprint;

                    if (!isOwnFootprint)
                        return false;
                }


            }
        }
        return true;
    }

    /**
     * Finds a reachable tile near the player that the enemy can path to.
     *
     * If the player's tile is reachable, it is returned directly.
     * Otherwise, nearby tiles within the given radius are checked and
     * the closest reachable tile is selected.
     *
     * This prevents enemies from getting stuck when the player stands
     * on unreachable tiles.
     *
     * @param maze the maze
     * @param enemyTile enemy start tile
     * @param playerTile player tile
     * @param searchRadius radius (in tiles) to scan around the player
     * @param footprint enemy footprint size
     * @return reachable tile near the player, or null if none found
     */

    public static Point findReachableGoalNearPlayer(Maze maze, Point enemyTile, Point playerTile, int searchRadius, int footprint) {
        if (maze.getTile(playerTile.x, playerTile.y).isWalkable()) {
            if (!GridPathfinder.findPath(maze, enemyTile, playerTile, footprint).isEmpty()) {
                return playerTile;
            }
        }

        Point bestCandidate = null;
        int shortestDistanceToPlayer = Integer.MAX_VALUE;

        for (int offsetY = -searchRadius; offsetY <= searchRadius; offsetY++) {
            for (int offsetX = -searchRadius; offsetX <= searchRadius; offsetX++) {
                int candidateX = playerTile.x + offsetX;
                int candidateY = playerTile.y + offsetY;

                if (!maze.inBounds(candidateX, candidateY))
                    continue;

                if(!maze.getTile(candidateX, candidateY).isWalkable())
                    continue;

                Point candidatePoint = new Point(candidateX, candidateY);
                List<Point> path = GridPathfinder.findPath(maze, enemyTile, candidatePoint, footprint);

                if (path.isEmpty())
                    continue;

                int distanceToPlayer = Math.abs(offsetX) + Math.abs(offsetY);

                if (distanceToPlayer < shortestDistanceToPlayer) {
                    shortestDistanceToPlayer = distanceToPlayer;
                    bestCandidate = candidatePoint;
                }
            }
        }

        return bestCandidate;
    }

    public static List<Point> findPath(Maze maze, Point start, Point goal) {
        return findPath(maze, start, goal, 2);
    }
}
