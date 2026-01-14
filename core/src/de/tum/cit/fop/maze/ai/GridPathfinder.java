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
    public static List<Point> findPath(Maze maze, Point start, Point goal) {
        if (!isValidPlacementForEnemy(start.x, start.y, maze)) {
            return List.of();
        }

        if (!isValidPlacementForEnemy(goal.x, goal.y, maze)) {
            return List.of();
        }

        if (start.equals(goal)) {
            return List.of(start);
        }

        PriorityQueue<Node> nodesToVisit = new PriorityQueue<>(Comparator.comparingInt(node -> node.estimatedTotalCost));
        Map<Point, Integer> cheapestPrice = new HashMap<>();

        int initialHeuristic = calculateManhattanDistance(start, goal);

        nodesToVisit.add(new Node(start, 0, initialHeuristic, null));
        cheapestPrice.put(start, 0);

        while (!nodesToVisit.isEmpty()) {
            Node currentNode = nodesToVisit.poll();

            if (currentNode.coordinates.equals(goal))
                return reconstructPath(currentNode);


            for(int[] directionOffset : cardDirections) {
                int neighborX = currentNode.coordinates.x + directionOffset[0];
                int neighborY = currentNode.coordinates.y + directionOffset[1];

                if (!maze.inBounds(neighborX, neighborY))
                    continue;


                if(!isValidPlacementForEnemy(neighborX, neighborY, maze))
                    continue;


                Point neighborPoint = new Point(neighborX, neighborY);
                int tentativeGScore = currentNode.costFromStart + 1;

                Integer existingBestCost = cheapestPrice.get(neighborPoint);

                if (existingBestCost != null && tentativeGScore >= existingBestCost)
                    continue;


                cheapestPrice.put(neighborPoint, tentativeGScore);

                int totalEstimatedCost = tentativeGScore + calculateManhattanDistance(neighborPoint, goal);
                nodesToVisit.add(new Node(neighborPoint, tentativeGScore, totalEstimatedCost, currentNode));
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
    private static boolean isValidPlacementForEnemy(int x, int y, Maze maze) {
        if (!maze.inBounds(x, y))
            return false;
        if (!maze.inBounds(x + 1, y))
            return false;
        if (!maze.inBounds(x, y + 1))
            return false;
        if (!maze.inBounds(x + 1, y + 1))
            return false;

        return maze.getTile(x, y).isWalkable() && maze.getTile(x + 1, y).isWalkable() && maze.getTile(x, y + 1).isWalkable() && maze.getTile(x + 1, y + 1).isWalkable();
    }


    // Find reachable goal near player
    public static Point findReachableGoalNearPlayer(Maze maze, Point enemyTile, Point playerTile, int searchRadius) {
        if (maze.getTile(playerTile.x, playerTile.y).isWalkable()) {
            if(!GridPathfinder.findPath(maze, enemyTile, playerTile).isEmpty()) {
                return playerTile;
            }
        }

        Point bestCandidate = null;
        int shortestDistanceToPlayer = Integer.MAX_VALUE;

        for (int offsetY = -searchRadius; offsetY <= searchRadius; offsetY++) {
            for (int offsetX = -searchRadius; offsetX <= searchRadius; offsetX++) {
                int candidateX = playerTile.x + offsetX;
                int candidateY = playerTile.y + offsetY;

                if (!maze.inBounds(candidateX, candidateY)) {
                    continue;
                }

                if (!maze.getTile(candidateX, candidateY).isWalkable())
                    continue;


                Point candidatePoint = new Point(candidateX, candidateY);
                List<Point> path = GridPathfinder.findPath(maze, enemyTile, candidatePoint);

                if (path.isEmpty()) {
                    continue;
                }

                int distanceToPlayer = Math.abs(offsetX) + Math.abs(offsetY);

                if(distanceToPlayer < shortestDistanceToPlayer) {
                    shortestDistanceToPlayer = distanceToPlayer;
                    bestCandidate = candidatePoint;
                }
            }
        }

        return bestCandidate;
    }
}
