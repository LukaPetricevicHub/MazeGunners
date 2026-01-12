package de.tum.cit.fop.maze.ai;

import de.tum.cit.fop.maze.level.Maze;

import java.awt.Point;
import java.util.*;

public class GridPathfinder {


    // Movement offsets
    private static final int[][] cardinalDirections= {{0, 1}, {0, -1}, {1, 0}, {-1, 0}};


    // Find walkable path between two tile positions
    public static List<Point> findTilePath(Maze maze, Point startTile, Point goalTile) {

        if(startTile.equals(goalTile))
            return Collections.emptyList();


        PriorityQueue<PathNode> openNodes = new PriorityQueue<>(Comparator.comparingInt(n -> n.estimatedTotalCost));

        Map<Point, Point> parentByNode = new HashMap<>();
        Map<Point, Integer> costFromStart = new HashMap<>();

        costFromStart.put(startTile, 0);
        openNodes.add(new PathNode(startTile, estimateCostToGoal(startTile, goalTile)));

        while (!openNodes.isEmpty()) {
            PathNode currentNode = openNodes.poll();

            if (currentNode.tilePosition.equals(goalTile)) {
                return buildPathFromParents(parentByNode, goalTile);
            }

            for (int[] direction : cardinalDirections) {
                Point neighborTile = new Point(
                        currentNode.tilePosition.x + direction[0],
                        currentNode.tilePosition.y + direction[1]
                );

                if (neighborTile.x < 0 || neighborTile.x >= maze.getWidth() || neighborTile.y < 0 || neighborTile.y >= maze.getHeight() || maze.getTile(neighborTile.x, neighborTile.y).isWalkable()) {
                    continue;
                }


                int newCostFromStart = costFromStart.getOrDefault(currentNode.tilePosition, Integer.MAX_VALUE) + 1;

                if (newCostFromStart < costFromStart.getOrDefault(neighborTile, Integer.MAX_VALUE)) {
                    parentByNode.put(neighborTile, currentNode.tilePosition);
                    costFromStart.put(neighborTile, newCostFromStart);

                    int estimatedTotalCost = newCostFromStart + estimateCostToGoal(neighborTile, goalTile);
                    openNodes.add(new PathNode(neighborTile, estimatedTotalCost));
                }
            }
        }

        return null;
    }


    // Estimate remaining distance
    private static int estimateCostToGoal(Point from, Point to) {
        return Math.abs(from.x - to.x) + Math.abs(from.y - to.y);
    }


    // Reconstruct path
    private static List<Point> buildPathFromParents(
            Map<Point, Point> parentByNode, Point goal) {

        List<Point> path = new ArrayList<>();
        Point curr = goal;

        while (parentByNode.containsKey(curr)) {
            path.add(0, curr);
            curr = parentByNode.get(curr);
        }

        return path;
    }


    // Internal A* node storing
    private static class PathNode {
        final Point tilePosition;
        final int estimatedTotalCost;

        PathNode(Point tilePosition, int estimatedTotalCost) {
            this.tilePosition = tilePosition;
            this.estimatedTotalCost = estimatedTotalCost;
        }
    }
}
