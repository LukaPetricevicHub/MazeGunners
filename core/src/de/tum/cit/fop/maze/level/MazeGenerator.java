package de.tum.cit.fop.maze.level;

import java.awt.Point;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class MazeGenerator {

    private static final int MAX_ATTEMPTS = 200;

    private final int CELL;

    // BSP / room params (logical grid)
    private static final int MIN_LEAF = 12;      // was 14 (a bit too strict)
    private static final int MIN_ROOM = 6;       // was 7
    private static final int ROOM_MARGIN = 1;
    private static final int EXTRA_LOOPS = 4;

    public MazeGenerator(int cell) {
        this.CELL = Math.max(1, cell);
    }

    public Maze generate(int logicalW, int logicalH, int tileSize, long seed) {
        Random rng = new Random(seed);

        // basic sanity
        if (logicalW < MIN_LEAF * 2 + 2 || logicalH < MIN_LEAF * 2 + 2) {
            throw new IllegalArgumentException(
                    "logicalW/logicalH too small for MIN_LEAF=" + MIN_LEAF +
                            " (need about " + (MIN_LEAF * 2 + 2) + "+)"
            );
        }

        for (int attempt = 0; attempt < MAX_ATTEMPTS; attempt++) {

            // logical grid [x][y]
            Tiles[][] logical = new Tiles[logicalW][logicalH];
            for (int x = 0; x < logicalW; x++) {
                for (int y = 0; y < logicalH; y++) {
                    logical[x][y] = Tiles.WALL;
                }
            }

            Leaf root = new Leaf(1, 1, logicalW - 2, logicalH - 2);
            List<Leaf> leaves = splitLeaves(root, rng);

            List<Room> rooms = carveRooms(logical, leaves, rng);
            if (rooms.size() < 2) continue;

            connectTree(root, rng, logical);
            addLoops(logical, rooms, rng);

            Maze maze = buildFinalMaze(logical, tileSize);

            // robust placement
            if (!placeEntryExitKeyRobust(maze, rng)) continue;

            // final reachability check
            if (MazeUtils.reachable(maze, maze.getEntryCell(), maze.getExitCell())) {
                return maze;
            }
        }

        throw new RuntimeException("Failed to generate valid maze after " + MAX_ATTEMPTS + " attempts");
    }

    // ---------- BSP structures ----------

    private static class Leaf {
        final int x, y, w, h;
        Leaf left, right;
        Room room;

        Leaf(int x, int y, int w, int h) {
            this.x = x; this.y = y; this.w = w; this.h = h;
        }

        boolean canSplitVert() { return w >= MIN_LEAF * 2; }
        boolean canSplitHoriz() { return h >= MIN_LEAF * 2; }

        boolean split(Random rng) {
            boolean canH = canSplitHoriz();
            boolean canV = canSplitVert();
            if (!canH && !canV) return false;

            boolean horiz;
            if (canH && canV) {
                if (w > h * 1.25f) horiz = false;
                else if (h > w * 1.25f) horiz = true;
                else horiz = rng.nextBoolean();
            } else {
                horiz = canH;
            }

            int maxSpan = horiz ? h : w;
            int maxCut = maxSpan - MIN_LEAF;
            if (maxCut <= MIN_LEAF) return false;

            int cut = MIN_LEAF + rng.nextInt(maxCut - MIN_LEAF + 1);

            if (horiz) {
                left  = new Leaf(x, y, w, cut);
                right = new Leaf(x, y + cut, w, h - cut);
            } else {
                left  = new Leaf(x, y, cut, h);
                right = new Leaf(x + cut, y, w - cut, h);
            }
            return true;
        }
    }

    private static class Room {
        final int x, y, w, h;
        Room(int x, int y, int w, int h) { this.x=x; this.y=y; this.w=w; this.h=h; }
        int cx() { return x + w / 2; }
        int cy() { return y + h / 2; }
    }

    private List<Leaf> splitLeaves(Leaf root, Random rng) {
        List<Leaf> leaves = new ArrayList<>();
        leaves.add(root);

        boolean didSplit;
        do {
            didSplit = false;
            List<Leaf> next = new ArrayList<>();
            for (Leaf l : leaves) {
                if (l.split(rng)) {
                    next.add(l.left);
                    next.add(l.right);
                    didSplit = true;
                } else {
                    next.add(l);
                }
            }
            leaves = next;
        } while (didSplit);

        return leaves;
    }

    private List<Room> carveRooms(Tiles[][] g, List<Leaf> leaves, Random rng) {
        List<Room> rooms = new ArrayList<>();

        for (Leaf l : leaves) {
            int maxRw = l.w - 2 * ROOM_MARGIN;
            int maxRh = l.h - 2 * ROOM_MARGIN;
            if (maxRw < MIN_ROOM || maxRh < MIN_ROOM) continue;

            int rw = MIN_ROOM + rng.nextInt(maxRw - MIN_ROOM + 1);
            int rh = MIN_ROOM + rng.nextInt(maxRh - MIN_ROOM + 1);

            int rx = l.x + ROOM_MARGIN + rng.nextInt((maxRw - rw) + 1);
            int ry = l.y + ROOM_MARGIN + rng.nextInt((maxRh - rh) + 1);

            Room r = new Room(rx, ry, rw, rh);
            l.room = r;
            rooms.add(r);

            for (int x = rx; x < rx + rw; x++) {
                for (int y = ry; y < ry + rh; y++) {
                    g[x][y] = Tiles.FLOOR;
                }
            }
        }

        return rooms;
    }

    private void connectTree(Leaf node, Random rng, Tiles[][] g) {
        if (node == null) return;
        if (node.left == null || node.right == null) return;

        connectTree(node.left, rng, g);
        connectTree(node.right, rng, g);

        Room a = pickRoom(node.left, rng);
        Room b = pickRoom(node.right, rng);
        if (a == null || b == null) return;

        if (rng.nextBoolean()) {
            carveCorridor(g, a.cx(), a.cy(), b.cx(), a.cy());
            carveCorridor(g, b.cx(), a.cy(), b.cx(), b.cy());
        } else {
            carveCorridor(g, a.cx(), a.cy(), a.cx(), b.cy());
            carveCorridor(g, a.cx(), b.cy(), b.cx(), b.cy());
        }
    }

    private Room pickRoom(Leaf l, Random rng) {
        if (l == null) return null;
        if (l.room != null) return l.room;

        Room a = pickRoom(l.left, rng);
        Room b = pickRoom(l.right, rng);
        if (a == null) return b;
        if (b == null) return a;
        return rng.nextBoolean() ? a : b;
    }

    private void carveCorridor(Tiles[][] g, int x1, int y1, int x2, int y2) {
        int x = x1, y = y1;
        g[x][y] = Tiles.FLOOR;

        while (x != x2) { x += Integer.compare(x2, x); g[x][y] = Tiles.FLOOR; }
        while (y != y2) { y += Integer.compare(y2, y); g[x][y] = Tiles.FLOOR; }
    }

    private void addLoops(Tiles[][] g, List<Room> rooms, Random rng) {
        if (rooms.size() < 2) return;

        for (int i = 0; i < EXTRA_LOOPS; i++) {
            Room a = rooms.get(rng.nextInt(rooms.size()));
            Room b = rooms.get(rng.nextInt(rooms.size()));
            if (a == b) continue;
            carveCorridor(g, a.cx(), a.cy(), b.cx(), b.cy());
        }
    }

    // ---------- scaling ----------

    private Maze buildFinalMaze(Tiles[][] logical, int tileSize) {
        int lw = logical.length;
        int lh = logical[0].length;

        Maze maze = new Maze(lw * CELL, lh * CELL, tileSize);

        for (int x = 0; x < lw; x++) {
            for (int y = 0; y < lh; y++) {
                Tiles t = logical[x][y];
                int sx = x * CELL;
                int sy = y * CELL;

                for (int dx = 0; dx < CELL; dx++) {
                    for (int dy = 0; dy < CELL; dy++) {
                        maze.setTile(sx + dx, sy + dy, t);
                    }
                }
            }
        }

        forceBorderWalls(maze);
        return maze;
    }

    private void forceBorderWalls(Maze maze) {
        int w = maze.getWidth();
        int h = maze.getHeight();

        for (int x = 0; x < w; x++) {
            maze.setTile(x, 0, Tiles.WALL);
            maze.setTile(x, h - 1, Tiles.WALL);
        }
        for (int y = 0; y < h; y++) {
            maze.setTile(0, y, Tiles.WALL);
            maze.setTile(w - 1, y, Tiles.WALL);
        }
    }

    // ---------- robust entry/exit/key ----------

    private boolean placeEntryExitKeyRobust(Maze maze, Random rng) {
        List<Point> floors = new ArrayList<>();
        for (int x = 1; x < maze.getWidth() - 1; x++) {
            for (int y = 1; y < maze.getHeight() - 1; y++) {
                if (maze.getTile(x, y) == Tiles.FLOOR) floors.add(new Point(x, y));
            }
        }
        if (floors.isEmpty()) return false;

        // pick a random floor, then get connected component of FLOOR
        Point start = floors.get(rng.nextInt(floors.size()));
        List<Point> component = floodFillFloors(maze, start);
        if (component.size() < 20) return false; // small sanity, not crazy strict

        Point entry = component.get(rng.nextInt(component.size()));
        Point exit = farthestPoint(component, entry);

        // key somewhere not too close, but not necessarily farthest
        component.sort((a, b) -> Integer.compare(MazeUtils.dist2(b, entry), MazeUtils.dist2(a, entry)));
        int top = Math.min(component.size(), 200);
        Point key = component.get(rng.nextInt(top));

        maze.setTile(entry.x, entry.y, Tiles.ENTRY);
        maze.setTile(exit.x, exit.y, Tiles.EXIT);
        maze.setTile(key.x, key.y, Tiles.KEY);

        return true;
    }

    private List<Point> floodFillFloors(Maze maze, Point start) {
        boolean[][] visited = new boolean[maze.getWidth()][maze.getHeight()];
        ArrayDeque<Point> q = new ArrayDeque<>();
        List<Point> out = new ArrayList<>();

        q.add(start);
        visited[start.x][start.y] = true;

        int[][] dirs = {{1,0},{-1,0},{0,1},{0,-1}};

        while (!q.isEmpty()) {
            Point p = q.removeFirst();
            out.add(p);

            for (int[] d : dirs) {
                int nx = p.x + d[0];
                int ny = p.y + d[1];

                if (!maze.inBounds(nx, ny)) continue;
                if (visited[nx][ny]) continue;
                if (maze.getTile(nx, ny) != Tiles.FLOOR) continue;

                visited[nx][ny] = true;
                q.addLast(new Point(nx, ny));
            }
        }
        return out;
    }

    private Point farthestPoint(List<Point> points, Point from) {
        Point best = points.get(0);
        int bestD = -1;
        for (Point p : points) {
            int d = MazeUtils.dist2(p, from);
            if (d > bestD) { bestD = d; best = p; }
        }
        return best;
    }
}
