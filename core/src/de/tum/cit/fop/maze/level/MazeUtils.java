package de.tum.cit.fop.maze.level;

import java.awt.Point;
import java.util.ArrayDeque;

public class MazeUtils {

    public static boolean reachable(Maze maze, Point start, Point goal) {
        if (start == null || goal == null) return false;
        if (!maze.inBounds(start.x, start.y) || !maze.inBounds(goal.x, goal.y)) return false;
        if (!maze.getTile(start.x, start.y).isWalkable()) return false;
        if (!maze.getTile(goal.x, goal.y).isWalkable()) return false;

        boolean[][] visited = new boolean[maze.getWidth()][maze.getHeight()];
        ArrayDeque<Point> queue = new ArrayDeque<>();

        queue.add(start);
        visited[start.x][start.y] = true;

        int[][] dirs = {{1,0},{-1,0},{0,1},{0,-1}};

        while (!queue.isEmpty()) {
            Point p = queue.poll();
            if (p.equals(goal)) return true;

            for (int[] d : dirs) {
                int nx = p.x + d[0];
                int ny = p.y + d[1];

                if (!maze.inBounds(nx, ny)) continue;
                if (visited[nx][ny]) continue;
                if (!maze.getTile(nx, ny).isWalkable()) continue;

                visited[nx][ny] = true;
                queue.add(new Point(nx, ny));
            }
        }
        return false;
    }

    public static int dist2(Point a, Point b) {
        int dx = a.x - b.x;
        int dy = a.y - b.y;
        return dx * dx + dy * dy;
    }
}
