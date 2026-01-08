package de.tum.cit.fop.maze.level;

import de.tum.cit.fop.maze.util.GridUtils;

import java.awt.Point;
import java.util.*;

public class MazeUtils {

    public static boolean inBounds(int x, int y, int w, int h){
        return x >= 0 && y >= 0 && x < w && y <h;
    }

    public static int dist2(Point a, Point b){
        int dx = a.x - b.x, dy = a.y - b.y;
        return dx * dx + dy * dy;
    }

    public static boolean reachable (Maze maze, Point start, Point goal){
        if(start == null || goal == null){
            return false;
        }

        int h = maze.getHeight();
        int w = maze.getWidth();
        boolean[][] vis = new boolean [h][w];
        ArrayDeque<Point> q = new ArrayDeque<>();

        q.add(start);
        vis[start.y][start.x] = true;

        int[][] dirs = {{1,0},{-1,0},{0,1},{0,-1}};

        while(!q.isEmpty()){
            Point p = q.removeFirst();
            if(p.equals(goal)){
                return true;
            }
            for(int[] d : dirs){
                int nx = p.x +d[0], ny = p.y + d[1];

                if(!inBounds(nx, ny, w, h)){
                    continue;
                }
                if(vis[ny][nx]){
                    continue;
                }

                if (!Tiles.isWalkable(maze.getTile(nx, ny))) {
                    continue;
                }

                vis[ny][nx] = true;
                q.addLast(new Point(nx, ny));

            }
        }
        return false;
    }

}

