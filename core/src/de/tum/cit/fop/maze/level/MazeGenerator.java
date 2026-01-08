package de.tum.cit.fop.maze.level;

import de.tum.cit.fop.maze.util.GridUtils;

import java.awt.Point;
import java.util.*;


public class MazeGenerator {
    public Maze generate(MazeLoader.LevelSpec spec, int tileSize, long seed){
        Objects.requireNonNull(spec, "LevelSpec must not be null");
        Random rng = new Random(seed);

        for(int attempt = 0; attempt < 60; attempt++){
            Maze maze = new Maze(spec.width, spec.height, tileSize);
            for(int y = 0; y < maze.getHeight(); y++){
                for(int x = 0; x < maze.getWidth(); x++){
                    maze.setTile(x, y, Tiles.Wall);
                }

            }

            carveDFS(maze, rng);

            Point entry = pickEdgeWalkable(maze, rng);
            Point exit = pickFarWalkable(maze, rng, entry);
            if(entry != null){
                maze.setTile(entry.x, entry.y, Tiles.Entry);
                maze.setEntryCell(entry);
            }

            if(exit != null){
                maze.setTile(exit.x, exit.y, Tiles.Exit);
                maze.setExitCell(exit);
            }

            applyOverrides(maze, spec.overrides);

            Point forcedEntry = findFirst(maze, Tiles.Entry);
            Point forcedExit = findFirst(maze, Tiles.Exit);
            if(forcedEntry != null){
                maze.setEntryCell(forcedEntry);
            }

            if(forcedExit != null){
                maze.setExitCell(forcedExit);
            }

            if(maze.getEntryCell() == null){
                Point e = pickEdgeWalkable(maze, rng);
                if (e != null) {
                    maze.setTile(e.x, e.y, Tiles.Entry);
                    maze.setEntryCell(e);
                }
            }

            if(maze.getExitCell() == null){
                Point e = pickEdgeWalkable(maze, rng);
                if (e != null) {
                    maze.setTile(e.x, e.y, Tiles.Exit);
                    maze.setEntryCell(e);
                }
            }

            if(MazeUtils.reachable(maze, maze.getEntryCell(), maze.getExitCell())){
                return maze;
            }
        }
        return new Maze(spec.width, spec.height, tileSize);
    }

    private void applyOverrides(Maze maze, Map<Point, Integer> overrides){
        for(Map.Entry<Point, Integer> e : overrides.entrySet()){
            Point p = e.getKey();
            maze.setTile(p.x, p.y, e.getValue());
        }
    }

    private void carveDFS(Maze maze, Random rng){
        int w = maze.getWidth();
        int h = maze.getHeight();

        int sx = randomOdd(rng, 1, w - 2);
        int sy = randomOdd(rng, 1, h - 2);

        ArrayDeque<Point> stack = new ArrayDeque<>();
        maze.setTile(sx, sy, Tiles.Floor);
        stack.push(new Point(sx, sy));

        int[][] dirs = {{2,0}, {-2,0}, {0,2},{0,-2}};

        while(!stack.isEmpty()){

            Point cur = stack.peek();
            List<int[]> shuffled = new ArrayList<>(Arrays.asList(dirs));
            Collections.shuffle(shuffled,rng);

            boolean moved = false;

            for(int[] d : shuffled){
                int nx = cur.x + d[0];
                int ny = cur.y +d[1];

                if(!GridUtils.inBounds(nx, ny, w, h)){
                    continue;
                }
                if(maze.getTile(nx, ny) != Tiles.Wall){
                    continue;
                }

                int wx = cur.x + d[0] / 2;
                int wy = cur.y + d[1] / 2;

                maze.setTile(wx, wy, Tiles.Floor);
                maze.setTile(nx, ny, Tiles.Floor);

                stack.push(new Point(nx, ny));
                moved = true;
                break;
            }

            if(!moved){
                stack.pop();
            }
        }
    }

    private int randomOdd(Random rng, int min, int max){
        if(max <= min){
            return min;
        }
        int v = min + rng.nextInt(max - min + 1);
        if(v % 2 == 0){
            v = Math.max(min, v - 1);
        }
        if(v % 2 == 0){
            v = Math.min(max, v + 1);
        }
        return v;
    }

    private Point pickEdgeWalkable(Maze maze, Random rng){
        int w = maze.getWidth(), h = maze.getHeight();
        List<Point> candidates = new ArrayList<>();

        for (int x = 1; x < w - 1; x++){
            if(Tiles.isWalkable(maze.getTile(x, 1))){
                candidates.add(new Point(x, 1));
            }

            if(Tiles.isWalkable(maze.getTile(x, h -2))){
                candidates.add(new Point(x, h -2));
            }
        }

        for(int y = 1; y < h - 1; y++){
            if(Tiles.isWalkable(maze.getTile(1, y))){
                candidates.add(new Point(1, y));
            }

            if(Tiles.isWalkable(maze.getTile(w -2, y))){
                candidates.add(new Point(w - 2, y));
            }
        }
        return candidates.isEmpty() ? null : candidates.get(rng.nextInt(candidates.size()));
    }

    private Point pickFarWalkable(Maze maze, Random rng, Point from){
        if(from == null){
            return pickEdgeWalkable(maze, rng);
        }
        List<Point> floors = new ArrayList<>();
        for(int y = 1; y < maze.getHeight() -1; y++){
            for(int x = 1; x < maze.getWidth() - 1; x++){
                if(Tiles.isWalkable(maze.getTile(x, y))){
                    floors.add(new Point(x, y));
                }
            }
        }
        if(floors.isEmpty()){
            return null;
        }

        floors.sort((a, b) -> Integer.compare(MazeUtils.dist2(b, from), MazeUtils.dist2(a, from)));
        int top = Math.min(30, floors.size());
        return floors.get(rng.nextInt(top));
    }

    private Point findFirst(Maze maze, int tileId){
        for(int y = 0; y < maze.getHeight(); y++){
            for(int x = 0; x < maze.getWidth(); x++){
                if(maze.getTile(x, y) == tileId){
                    return new Point(x, y);
                }
            }

        }
        return null;
    }
}
