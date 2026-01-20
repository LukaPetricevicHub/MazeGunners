package de.tum.cit.fop.maze.ai;

import de.tum.cit.fop.maze.map.Maze;
import java.awt.Point;
import java.util.*;

public final class GridPathfinder {

    // Attributes
    private static final int[][] cardDirections = {{1, 0}, {-1, 0}, {0, 1}, {0, -1}};


    // Internal node for A* search
    private static class Node { final Point coordinates;int costFromStart;int estimatedTotalCost;Node parentNode;
        Node(Point coordinates, int costFromStart, int estimatedTotalCost, Node parent) {
            this.coordinates = coordinates;
            this.costFromStart = costFromStart;
            this.estimatedTotalCost = estimatedTotalCost;
            this.parentNode = parent;
        }
    }


    // Constructor
    private GridPathfinder() {}


    // Find path using A* algorithm
    public static List<Point> findPath(
            Maze maze,
            Point start,
            Point goal,
            int footprint
    ) {
        if (!isValidPlacement(start.x, start.y, maze, footprint))
            return List.of();

        if (!isValidPlacement(goal.x, goal.y, maze, footprint))
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

                if (!isValidPlacement(nx, ny, maze, footprint))
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



    // Calculate Manhattan distance
    private static int calculateManhattanDistance(Point a, Point b) {
        return Math.abs(a.x - b.x) + Math.abs(a.y - b.y);
    }


    // Reconstruct path from goal node
    private static List<Point> reconstructPath(Node goalNode) {
        LinkedList<Point> path = new LinkedList<>();
        Node traversalNode = goalNode;

        while (traversalNode != null) {
            path.addFirst(traversalNode.coordinates);
            traversalNode = traversalNode.parentNode;
        }

        return path;
    }


    // Check if space fits enemy
    private static boolean isValidPlacement(int x, int y, Maze maze, int footprint) {
        for (int dx = 0; dx < footprint; dx++) {
            for (int dy = 0; dy < footprint; dy++) {
                int tx = x + dx;
                int ty = y + dy;

                if (!maze.inBounds(tx, ty))
                    return false;

                if (!maze.getTile(tx, ty).isWalkable())
                    return false;
            }
        }
        return true;
    }



    // Find reachable goal near player
    public static Point findReachableGoalNearPlayer(
            Maze maze,
            Point enemyTile,
            Point playerTile,
            int searchRadius,
            int footprint
    ) {
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

                if (!maze.getTile(candidateX, candidateY).isWalkable())
                    continue;

                Point candidatePoint = new Point(candidateX, candidateY);
                List<Point> path = GridPathfinder.findPath(
                        maze,
                        enemyTile,
                        candidatePoint,
                        footprint
                );

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


    // Default pathfinding: 2x2 footprint (ground enemies)
    public static List<Point> findPath(Maze maze, Point start, Point goal) {
        return findPath(maze, start, goal, 2);
    }

}
