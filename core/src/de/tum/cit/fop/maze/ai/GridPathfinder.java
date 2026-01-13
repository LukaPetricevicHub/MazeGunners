package de.tum.cit.fop.maze.ai;

import de.tum.cit.fop.maze.map.Maze;

import java.awt.Point;
import java.util.*;

public class GridPathfinder {

    private static final int[][] CARDINAL_DIRS = {
            {0, 1}, {0, -1}, {1, 0}, {-1, 0}
    };


    public static List<Point> findTilePath(Maze maze, Point startTile, Point goalTile) {
        if(startTile == null || goalTile == null)
            return Collections.emptyList();

        if (!maze.inBounds(startTile.x, startTile.y) || !maze.inBounds(goalTile.x, goalTile.y))
            return Collections.emptyList();

        if (!maze.getTile(startTile.x, startTile.y).isWalkable() || !maze.getTile(goalTile.x, goalTile.y).isWalkable())
            return Collections.emptyList();

        if (startTile.equals(goalTile))
            return Collections.emptyList();

        PriorityQueue<PathNode> open =
                new PriorityQueue<>(Comparator.comparingInt(n -> n.fCost));

        Map<Point, Point> cameFrom = new HashMap<>();
        Map<Point, Integer> gCost = new HashMap<>();
        Set<Point> closed = new HashSet<>();

        gCost.put(startTile, 0);
        open.add(new PathNode(startTile, heuristic(startTile, goalTile)));

        while (!open.isEmpty()) {
            PathNode current = open.poll();

            if(closed.contains(current.pos))
                continue;

            if (current.pos.equals(goalTile)) {
                return reconstructPath(cameFrom, goalTile);
            }

            closed.add(current.pos);

            int baseCost = gCost.get(current.pos);

            for (int[] d : CARDINAL_DIRS) {
                int nx = current.pos.x + d[0];
                int ny = current.pos.y + d[1];

                if (!maze.inBounds(nx, ny))
                    continue;

                if (!isTileWalkableForEnemy(maze, nx, ny))
                    continue;

                Point neighbor = new Point(nx, ny);
                if (closed.contains(neighbor))
                    continue;

                int newCost = baseCost + 1;
                int oldCost = gCost.getOrDefault(neighbor, Integer.MAX_VALUE);

                if (newCost < oldCost) {
                    cameFrom.put(neighbor, current.pos);
                    gCost.put(neighbor, newCost);
                    open.add(new PathNode(
                            neighbor,
                            newCost + heuristic(neighbor, goalTile)
                    ));
                }
            }

        }

        return Collections.emptyList();
    }

    // Manhattan distance
    private static int heuristic(Point a, Point b) {
        return Math.abs(a.x - b.x) + Math.abs(a.y - b.y);
    }

    private static List<Point> reconstructPath(
            Map<Point, Point> parents,
            Point goal
    ) {
        List<Point> path = new ArrayList<>();
        Point current = goal;

        while (parents.containsKey(current)) {
            path.add(0, current);
            current = parents.get(current);
        }
        return path;
    }

    // Internal node
    private static final class PathNode {
        final Point pos;
        final int fCost;

        PathNode(Point pos, int fCost) {
            this.pos = pos;
            this.fCost = fCost;
        }
    }

    private static boolean isTileWalkableForEnemy(Maze maze, int x, int y) {
        if (!maze.getTile(x, y).isWalkable())
            return false;

        for (int dy = -1; dy <= 1; dy++) {
            for (int dx = -1; dx <= 1; dx++) {
                int nx = x + dx;
                int ny = y + dy;

                if (!maze.inBounds(nx, ny))
                    return false;

                if (maze.getTile(nx, ny).isWall())
                    return false;
            }
        }
        return true;
    }

}