package de.tum.cit.fop.maze.level;

import java.awt.Point;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class MazeGenerator {

    private final int CELL; // scaling factor for final maze (1 = normal corridors)

    public MazeGenerator(int cell) {
        this.CELL = Math.max(1, cell);
    }

    public Maze generate(int logicalW, int logicalH, int tileSize, long seed) {
        Random rng = new Random(seed);

        // --- 1) Ensure odd dimensions for a proper maze grid ---
        int baseW = makeOdd(Math.max(5, logicalW));
        int baseH = makeOdd(Math.max(5, logicalH));

        // Base maze tiles (WALL everywhere initially)
        Tiles[][] base = new Tiles[baseW][baseH];
        for (int x = 0; x < baseW; x++) {
            for (int y = 0; y < baseH; y++) {
                base[x][y] = Tiles.WALL;
            }
        }

        // Cells exist at odd coordinates: (1,1), (3,1), ...
        int cellW = (baseW - 1) / 2;
        int cellH = (baseH - 1) / 2;

        boolean[][] visited = new boolean[cellW][cellH];

        // --- 2) Hunt & Kill ---
        int cx = rng.nextInt(cellW);
        int cy = rng.nextInt(cellH);
        carveCell(base, cx, cy);
        visited[cx][cy] = true;

        while (true) {
            // KILL phase: random walk until dead end
            boolean moved = true;
            while (moved) {
                moved = false;

                int[][] dirs = shuffledDirs(rng);
                for (int[] d : dirs) {
                    int nx = cx + d[0];
                    int ny = cy + d[1];

                    if (nx < 0 || ny < 0 || nx >= cellW || ny >= cellH) continue;
                    if (visited[nx][ny]) continue;

                    // carve passage between (cx,cy) and (nx,ny)
                    carvePassage(base, cx, cy, nx, ny);
                    visited[nx][ny] = true;

                    cx = nx;
                    cy = ny;
                    moved = true;
                    break;
                }
            }

            // HUNT phase: find an unvisited cell that has a visited neighbor
            Point hunt = huntNext(visited, rng);
            if (hunt == null) break; // done

            cx = hunt.x;
            cy = hunt.y;

            // connect hunted cell to a visited neighbor
            List<Point> visitedNeighbors = new ArrayList<>();
            if (cx > 0 && visited[cx - 1][cy]) visitedNeighbors.add(new Point(cx - 1, cy));
            if (cx < cellW - 1 && visited[cx + 1][cy]) visitedNeighbors.add(new Point(cx + 1, cy));
            if (cy > 0 && visited[cx][cy - 1]) visitedNeighbors.add(new Point(cx, cy - 1));
            if (cy < cellH - 1 && visited[cx][cy + 1]) visitedNeighbors.add(new Point(cx, cy + 1));

            if (visitedNeighbors.isEmpty()) continue;

            Point nb = visitedNeighbors.get(rng.nextInt(visitedNeighbors.size()));
            carvePassage(base, cx, cy, nb.x, nb.y);
            visited[cx][cy] = true;
        }

        // --- 3) Build final Maze (optionally scaled by CELL) ---
        Maze maze = buildScaledMaze(base, tileSize);

        // --- 4) Place ENTRY/EXIT/KEY robustly ---
        placeEntryExitKey(maze, rng);

        return maze;
    }

    // -------------------- internals --------------------

    private int makeOdd(int v) {
        return (v % 2 == 0) ? v - 1 : v;
    }

    private void carveCell(Tiles[][] base, int cx, int cy) {
        int gx = 2 * cx + 1;
        int gy = 2 * cy + 1;
        base[gx][gy] = Tiles.FLOOR;
    }

    private void carvePassage(Tiles[][] base, int cx, int cy, int nx, int ny) {
        int gx1 = 2 * cx + 1;
        int gy1 = 2 * cy + 1;
        int gx2 = 2 * nx + 1;
        int gy2 = 2 * ny + 1;

        base[gx1][gy1] = Tiles.FLOOR;
        base[gx2][gy2] = Tiles.FLOOR;

        // wall between them
        int wx = (gx1 + gx2) / 2;
        int wy = (gy1 + gy2) / 2;
        base[wx][wy] = Tiles.FLOOR;
    }

    private int[][] shuffledDirs(Random rng) {
        int[][] dirs = {{1,0},{-1,0},{0,1},{0,-1}};
        for (int i = dirs.length - 1; i > 0; i--) {
            int j = rng.nextInt(i + 1);
            int[] tmp = dirs[i];
            dirs[i] = dirs[j];
            dirs[j] = tmp;
        }
        return dirs;
    }

    private Point huntNext(boolean[][] visited, Random rng) {
        int w = visited.length;
        int h = visited[0].length;

        // random scan start so it doesn't look patterned
        int startX = rng.nextInt(w);
        int startY = rng.nextInt(h);

        for (int dy = 0; dy < h; dy++) {
            for (int dx = 0; dx < w; dx++) {
                int x = (startX + dx) % w;
                int y = (startY + dy) % h;

                if (visited[x][y]) continue;

                boolean hasVisitedNeighbor =
                        (x > 0 && visited[x - 1][y]) ||
                                (x < w - 1 && visited[x + 1][y]) ||
                                (y > 0 && visited[x][y - 1]) ||
                                (y < h - 1 && visited[x][y + 1]);

                if (hasVisitedNeighbor) return new Point(x, y);
            }
        }
        return null;
    }

    private Maze buildScaledMaze(Tiles[][] base, int tileSize) {
        int bw = base.length;
        int bh = base[0].length;

        Maze maze = new Maze(bw * CELL, bh * CELL, tileSize);

        for (int x = 0; x < bw; x++) {
            for (int y = 0; y < bh; y++) {
                Tiles t = base[x][y];
                int sx = x * CELL;
                int sy = y * CELL;

                for (int dx = 0; dx < CELL; dx++) {
                    for (int dy = 0; dy < CELL; dy++) {
                        maze.setTile(sx + dx, sy + dy, t);
                    }
                }
            }
        }
        return maze;
    }

    private void placeEntryExitKey(Maze maze, Random rng) {
        List<Point> floors = new ArrayList<>();

        for (int x = 1; x < maze.getWidth() - 1; x++) {
            for (int y = 1; y < maze.getHeight() - 1; y++) {
                if (maze.getTile(x, y) == Tiles.FLOOR) floors.add(new Point(x, y));
            }
        }
        if (floors.size() < 10) {
            // extremely unlikely with hunt & kill, but just in case
            maze.setTile(1, 1, Tiles.ENTRY);
            maze.setTile(maze.getWidth() - 2, maze.getHeight() - 2, Tiles.EXIT);
            maze.setTile(2, 1, Tiles.KEY);
            return;
        }

        Point entry = floors.get(rng.nextInt(floors.size()));
        List<Point> component = floodFillFloors(maze, entry);

        Point exit = farthestPoint(component, entry);

        // key somewhere “not too close” to entry
        component.sort((a, b) -> Integer.compare(MazeUtils.dist2(b, entry), MazeUtils.dist2(a, entry)));
        Point key = component.get(rng.nextInt(Math.min(component.size(), 200)));

        maze.setTile(entry.x, entry.y, Tiles.ENTRY);
        maze.setTile(exit.x, exit.y, Tiles.EXIT);
        maze.setTile(key.x, key.y, Tiles.KEY);
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
            if (d > bestD) {
                bestD = d;
                best = p;
            }
        }
        return best;
    }
}

