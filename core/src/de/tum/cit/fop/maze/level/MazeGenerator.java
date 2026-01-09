package de.tum.cit.fop.maze.level;

import de.tum.cit.fop.maze.util.GridUtils;

import java.awt.Point;
import java.util.*;

public class MazeGenerator {

    private static final int MAX_ATTEMPTS = 60;

    public Maze generate(MazeLoader.LevelSpec spec, int tileSize, long seed) {
        Objects.requireNonNull(spec, "LevelSpec must not be null");
        Random rng = new Random(seed);

        for (int attempt = 0; attempt < MAX_ATTEMPTS; attempt++) {

            Maze maze = new Maze(spec.width, spec.height, tileSize);

            // 1) Fill walls
            fill(maze, Tiles.Wall);

            // 2) Carve paths
            carveDFS(maze, rng);

            // 3) Apply overrides (forced tiles from .properties)
            applyOverrides(maze, spec.overrides);

            // 4) Resolve Entry/Exit from overrides first, otherwise place procedurally
            Point forcedEntry = findFirst(maze, Tiles.Entry);
            Point forcedExit  = findFirst(maze, Tiles.Exit);

            if (forcedEntry != null) {
                maze.setEntryCell(forcedEntry);
            }
            if (forcedExit != null) {
                maze.setExitCell(forcedExit);
            }

            if (maze.getEntryCell() == null) {
                Point entry = pickEdgeWalkable(maze, rng);
                if (entry != null) {
                    maze.setTile(entry.x, entry.y, Tiles.Entry);
                    maze.setEntryCell(entry);
                }
            }

            if (maze.getExitCell() == null) {
                // Prefer far from entry if possible
                Point entry = maze.getEntryCell();
                Point exit = (entry == null)
                        ? pickEdgeWalkable(maze, rng)
                        : pickFarWalkable(maze, rng, entry);

                if (exit != null && entry != null && exit.equals(entry)) {
                    // avoid overlap
                    exit = pickFarWalkableDistinct(maze, rng, entry);
                }

                if (exit != null) {
                    maze.setTile(exit.x, exit.y, Tiles.Exit);
                    maze.setExitCell(exit);
                }
            }

            // 5) Validate
            Point entry = maze.getEntryCell();
            Point exit  = maze.getExitCell();

            if (entry != null && exit != null && MazeUtils.reachable(maze, entry, exit)) {
                return maze;
            }
        }

        // fallback: empty maze object (keeps game running)
        return new Maze(spec.width, spec.height, tileSize);
    }

    private void fill(Maze maze, int tileId) {
        for (int y = 0; y < maze.getHeight(); y++) {
            for (int x = 0; x < maze.getWidth(); x++) {
                maze.setTile(x, y, tileId);
            }
        }
    }

    private void applyOverrides(Maze maze, Map<Point, Integer> overrides) {
        if (overrides == null) return;
        for (Map.Entry<Point, Integer> e : overrides.entrySet()) {
            Point p = e.getKey();
            if (p == null) continue;
            if (!GridUtils.inBounds(p.x, p.y, maze.getWidth(), maze.getHeight())) continue;
            maze.setTile(p.x, p.y, e.getValue());
        }
    }

    // Standard DFS maze carving: steps by 2 cells and knocks down walls between
    private void carveDFS(Maze maze, Random rng) {
        int w = maze.getWidth();
        int h = maze.getHeight();

        // Need at least a 3x3 interior
        if (w < 3 || h < 3) return;

        int sx = randomOdd(rng, 1, w - 2);
        int sy = randomOdd(rng, 1, h - 2);

        ArrayDeque<Point> stack = new ArrayDeque<>();
        maze.setTile(sx, sy, Tiles.Floor);
        stack.push(new Point(sx, sy));

        int[][] dirs = {{2, 0}, {-2, 0}, {0, 2}, {0, -2}};

        while (!stack.isEmpty()) {
            Point cur = stack.peek();

            List<int[]> shuffled = new ArrayList<>(Arrays.asList(dirs));
            Collections.shuffle(shuffled, rng);

            boolean moved = false;

            for (int[] d : shuffled) {
                int nx = cur.x + d[0];
                int ny = cur.y + d[1];

                if (!GridUtils.inBounds(nx, ny, w, h)) continue;
                if (maze.getTile(nx, ny) != Tiles.Wall) continue;

                int wx = cur.x + d[0] / 2;
                int wy = cur.y + d[1] / 2;

                maze.setTile(wx, wy, Tiles.Floor);
                maze.setTile(nx, ny, Tiles.Floor);

                stack.push(new Point(nx, ny));
                moved = true;
                break;
            }

            if (!moved) {
                stack.pop();
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

    private Point pickEdgeWalkable(Maze maze, Random rng) {
        int w = maze.getWidth(), h = maze.getHeight();
        List<Point> candidates = new ArrayList<>();

        // top/bottom inner edges
        for (int x = 1; x < w - 1; x++) {
            if (Tiles.isWalkable(maze.getTile(x, 1))) candidates.add(new Point(x, 1));
            if (Tiles.isWalkable(maze.getTile(x, h - 2))) candidates.add(new Point(x, h - 2));
        }

        // left/right inner edges
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
                if (Tiles.isWalkable(maze.getTile(x, y))) {
                    floors.add(new Point(x, y));
                }
            }
        }

        if (floors.isEmpty()) return null;

        // Sort farthest first
        floors.sort((a, b) -> Integer.compare(MazeUtils.dist2(b, from), MazeUtils.dist2(a, from)));

        // Choose randomly among top N farthest cells to keep variety
        int top = Math.min(30, floors.size());
        return floors.get(rng.nextInt(top));
    }

    private Point pickFarWalkableDistinct(Maze maze, Random rng, Point from) {
        // Try a few times to avoid same cell
        for (int i = 0; i < 20; i++) {
            Point p = pickFarWalkable(maze, rng, from);
            if (p != null && !p.equals(from)) return p;
        }
        // fallback: brute scan for any other walkable cell
        for (int y = 1; y < maze.getHeight() - 1; y++) {
            for (int x = 1; x < maze.getWidth() - 1; x++) {
                if (Tiles.isWalkable(maze.getTile(x, y)) && !(x == from.x && y == from.y)) {
                    return new Point(x, y);
                }
            }
        }
        return null;
    }

    private Point findFirst(Maze maze, int tileId) {
        for (int y = 0; y < maze.getHeight(); y++) {
            for (int x = 0; x < maze.getWidth(); x++) {
                if (maze.getTile(x, y) == tileId) {
                    return new Point(x, y);
                }
            }
        }
        return null;
    }
}
