package de.tum.cit.fop.maze.level;

import de.tum.cit.fop.maze.util.GridUtils;

import java.awt.Point;
import java.util.*;

public class MazeGenerator {

    private static final int MAX_ATTEMPTS = 80;

    // Corridor thickness in CELLS (2 or 3 looks good)
    private static final int CORRIDOR_THICKNESS = 2;

    public Maze generate(MazeLoader.LevelSpec spec, int tileSize, long seed) {
        Objects.requireNonNull(spec, "LevelSpec must not be null");
        Random rng = new Random(seed);

        for (int attempt = 0; attempt < MAX_ATTEMPTS; attempt++) {

            Maze maze = new Maze(spec.width, spec.height, tileSize);

            // 1) Start as all walls
            fill(maze, Tiles.Wall);

            // 2) Carve with Prim's algorithm
            carvePrims(maze, rng, CORRIDOR_THICKNESS);

            // 3) Force a solid border of walls
            forceBorderWalls(maze);

            // 4) Place entry and exit
            Point entry = pickEdgeWalkable(maze, rng);
            if (entry != null) {
                maze.setTile(entry.x, entry.y, Tiles.Entry);
                maze.setEntryCell(entry);
            }

            Point exit = (entry == null) ? pickEdgeWalkable(maze, rng) : pickFarWalkable(maze, rng, entry);
            if (exit != null && entry != null && exit.equals(entry)) {
                exit = pickFarWalkableDistinct(maze, rng, entry);
            }
            if (exit != null) {
                maze.setTile(exit.x, exit.y, Tiles.Exit);
                maze.setExitCell(exit);
            }

            // 5) Validate solvable
            if (maze.getEntryCell() != null && maze.getExitCell() != null &&
                    MazeUtils.reachable(maze, maze.getEntryCell(), maze.getExitCell())) {
                return maze;
            }
        }

        // fallback
        return new Maze(spec.width, spec.height, tileSize);
    }

    private void fill(Maze maze, int tileId) {
        for (int y = 0; y < maze.getHeight(); y++) {
            for (int x = 0; x < maze.getWidth(); x++) {
                maze.setTile(x, y, tileId);
            }
        }
    }

    private void forceBorderWalls(Maze maze) {
        int w = maze.getWidth();
        int h = maze.getHeight();

        for (int x = 0; x < w; x++) {
            maze.setTile(x, 0, Tiles.Wall);
            maze.setTile(x, h - 1, Tiles.Wall);
        }
        for (int y = 0; y < h; y++) {
            maze.setTile(0, y, Tiles.Wall);
            maze.setTile(w - 1, y, Tiles.Wall);
        }
    }

    // ----------------------------
    // Randomized Prim's algorithm
    // ----------------------------
    private void carvePrims(Maze maze, Random rng, int thickness) {
        int w = maze.getWidth();
        int h = maze.getHeight();
        if (w < 5 || h < 5) return;

        // Work on an odd grid to keep clean walls between corridors.
        // We treat "cells" as odd coordinates (1..w-2, 1..h-2).
        int startX = randomOdd(rng, 1, w - 2);
        int startY = randomOdd(rng, 1, h - 2);

        paintFloorBlockCentered(maze, startX, startY, thickness);

        // Frontier edges from carved region to walls (step 2)
        ArrayList<Edge> frontier = new ArrayList<>();
        addFrontier(frontier, startX, startY, w, h);

        while (!frontier.isEmpty()) {
            Edge e = frontier.remove(rng.nextInt(frontier.size()));

            // If target already carved, skip
            if (maze.getTile(e.tx, e.ty) != Tiles.Wall) continue;

            // Carve the wall between + the target cell
            paintFloorBlockCentered(maze, e.wx, e.wy, thickness);
            paintFloorBlockCentered(maze, e.tx, e.ty, thickness);

            addFrontier(frontier, e.tx, e.ty, w, h);
        }
    }

    private static class Edge {
        final int wx, wy; // wall between
        final int tx, ty; // target cell
        Edge(int wx, int wy, int tx, int ty) {
            this.wx = wx; this.wy = wy;
            this.tx = tx; this.ty = ty;
        }
    }

    private void addFrontier(List<Edge> frontier, int cx, int cy, int w, int h) {
        int[][] dirs = {{2,0}, {-2,0}, {0,2}, {0,-2}};
        for (int[] d : dirs) {
            int tx = cx + d[0];
            int ty = cy + d[1];
            int wx = cx + d[0] / 2;
            int wy = cy + d[1] / 2;

            if (!GridUtils.inBounds(tx, ty, w, h)) continue;
            // frontier only makes sense if target is still wall
            frontier.add(new Edge(wx, wy, tx, ty));
        }
    }

    // Thickness is "diameter" in cells; we paint a square centered on (x,y)
    private void paintFloorBlockCentered(Maze maze, int x, int y, int thickness) {
        int half = thickness / 2;
        for (int oy = -half; oy <= half; oy++) {
            for (int ox = -half; ox <= half; ox++) {
                int px = x + ox;
                int py = y + oy;
                if (GridUtils.inBounds(px, py, maze.getWidth(), maze.getHeight())) {
                    maze.setTile(px, py, Tiles.Floor);
                }
            }
        }
    }

    private int randomOdd(Random rng, int min, int max) {
        if (max <= min) return min;
        int v = min + rng.nextInt(max - min + 1);
        if (v % 2 == 0) v = Math.max(min, v - 1);
        if (v % 2 == 0) v = Math.min(max, v + 1);
        return v;
    }

    // ----------------------------
    // Entry/Exit placement helpers
    // ----------------------------
    private Point pickEdgeWalkable(Maze maze, Random rng) {
        int w = maze.getWidth(), h = maze.getHeight();
        List<Point> candidates = new ArrayList<>();

        for (int x = 1; x < w - 1; x++) {
            if (Tiles.isWalkable(maze.getTile(x, 1))) candidates.add(new Point(x, 1));
            if (Tiles.isWalkable(maze.getTile(x, h - 2))) candidates.add(new Point(x, h - 2));
        }
        for (int y = 1; y < h - 1; y++) {
            if (Tiles.isWalkable(maze.getTile(1, y))) candidates.add(new Point(1, y));
            if (Tiles.isWalkable(maze.getTile(w - 2, y))) candidates.add(new Point(w - 2, y));
        }

        return candidates.isEmpty() ? null : candidates.get(rng.nextInt(candidates.size()));
    }

    private Point pickFarWalkable(Maze maze, Random rng, Point from) {
        if (from == null) return pickEdgeWalkable(maze, rng);

        List<Point> floors = new ArrayList<>();
        for (int y = 1; y < maze.getHeight() - 1; y++) {
            for (int x = 1; x < maze.getWidth() - 1; x++) {
                if (Tiles.isWalkable(maze.getTile(x, y))) floors.add(new Point(x, y));
            }
        }
        if (floors.isEmpty()) return null;

        floors.sort((a, b) -> Integer.compare(MazeUtils.dist2(b, from), MazeUtils.dist2(a, from)));
        int top = Math.min(40, floors.size());
        return floors.get(rng.nextInt(top));
    }

    private Point pickFarWalkableDistinct(Maze maze, Random rng, Point from) {
        for (int i = 0; i < 30; i++) {
            Point p = pickFarWalkable(maze, rng, from);
            if (p != null && !p.equals(from)) return p;
        }
        return null;
    }
}
