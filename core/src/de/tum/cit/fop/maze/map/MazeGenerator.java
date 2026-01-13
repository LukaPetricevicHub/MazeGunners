package de.tum.cit.fop.maze.map;

import java.awt.Point;
import java.util.*;

public class MazeGenerator {

    private static final int MAX_ATTEMPTS = 60;

    /** Level-tuned parameters */
    private static final class Params {
        final int minLeaf;
        final int minRoom;
        final int roomMargin;
        final int hallWidth;   // recommended odd: 1,3,5...
        final int extraLoops;  // set 0 if you want "tree-like" structure

        Params(int minLeaf, int minRoom, int roomMargin, int hallWidth, int extraLoops) {
            this.minLeaf = minLeaf;
            this.minRoom = minRoom;
            this.roomMargin = roomMargin;
            this.hallWidth = hallWidth;
            this.extraLoops = extraLoops;
        }

        int hallRadius() { return hallWidth / 2; }
    }

    /** Hardcode settings per level (tune these freely) */
    private Params paramsForLevel(int level) {
        return switch (level) {
            case 1 -> new Params(14, 6, 1, 3, 0);
            case 2 -> new Params(16, 7, 1, 3, 0);
            case 3 -> new Params(18, 8, 1, 3, 0);
            case 4 -> new Params(18, 9, 1, 5, 1);
            case 5 -> new Params(20, 10, 1, 5, 2);
            default -> new Params(16, 7, 1, 3, 0);
        };
    }

    /**
     * Generate BSP dungeon for a given level.
     * Writes only in-memory Maze; GameScreen exports & reloads.
     */
    public Maze generate(MazeLoader.LevelSpec spec, int tileSize, long seed, int level) {
        Objects.requireNonNull(spec, "LevelSpec must not be null");
        Random rng = new Random(seed);

        Params p = paramsForLevel(level);

        // basic sanity so BSP can split
        if (spec.width < p.minLeaf * 2 + 4 || spec.height < p.minLeaf * 2 + 4) {
            throw new IllegalArgumentException(
                    "Map too small for BSP at level " + level +
                            ". width/height should be at least ~" + (p.minLeaf * 2 + 4) +
                            " (got " + spec.width + "x" + spec.height + ")");
        }

        for (int attempt = 0; attempt < MAX_ATTEMPTS; attempt++) {
            Maze maze = new Maze(spec.width, spec.height, tileSize);
            fill(maze, Tiles.WALL);

            Leaf root = new Leaf(1, 1, spec.width - 2, spec.height - 2);
            List<Leaf> leaves = splitLeaves(root, rng, p.minLeaf);

            List<Room> rooms = carveRooms(maze, leaves, rng, p.minRoom, p.roomMargin, p.hallRadius());
            if (rooms.size() < 3) continue; // need spawn+exit+key in distinct rooms

            List<Hall> halls = new ArrayList<>();
            connectTree(root, rng, halls); // BSP connectivity (tree)

            if (p.extraLoops > 0) {
                addLoops(rooms, rng, halls, p.extraLoops);
            }

            carveHalls(maze, halls, rng, p.hallRadius());

            // clear any old special tiles (paranoia; also reset tracked cells)
            clearSpecialTiles(maze);

            PlaceResult placed = assignSpecialRoomsAndPlaceTiles(maze, rooms, rng);
            if (!placed.ok) continue;

            if (MazeUtils.reachable(maze, placed.spawn, placed.exit)) {
                return maze;
            }
        }

        throw new RuntimeException("Failed to generate BSP dungeon after " + MAX_ATTEMPTS + " attempts");
    }

    // ---------------- BSP leaf ----------------

    private static class Leaf {
        final int x, y, w, h;
        Leaf left, right;
        Room room;

        Leaf(int x, int y, int w, int h) { this.x = x; this.y = y; this.w = w; this.h = h; }

        boolean split(Random rng, int minLeaf) {
            boolean canH = h >= minLeaf * 2;
            boolean canV = w >= minLeaf * 2;
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
            int cutMin = minLeaf;
            int cutMax = maxSpan - minLeaf;
            if (cutMax <= cutMin) return false;

            int cut = cutMin + rng.nextInt(cutMax - cutMin + 1);

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

    private List<Leaf> splitLeaves(Leaf root, Random rng, int minLeaf) {
        List<Leaf> leaves = new ArrayList<>();
        leaves.add(root);

        boolean didSplit;
        do {
            didSplit = false;
            List<Leaf> next = new ArrayList<>();
            for (Leaf l : leaves) {
                if (l.split(rng, minLeaf)) {
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

    // ---------------- rooms ----------------

    private List<Room> carveRooms(Maze maze,
                                  List<Leaf> leaves,
                                  Random rng,
                                  int minRoom,
                                  int roomMargin,
                                  int hallRadius) {

        List<Room> rooms = new ArrayList<>();

        // Leave extra buffer so wide halls don't instantly merge rooms
        int effectiveMargin = roomMargin + hallRadius;

        for (Leaf l : leaves) {
            int maxRw = l.w - 2 * effectiveMargin;
            int maxRh = l.h - 2 * effectiveMargin;
            if (maxRw < minRoom || maxRh < minRoom) continue;

            int rw = minRoom + rng.nextInt(maxRw - minRoom + 1);
            int rh = minRoom + rng.nextInt(maxRh - minRoom + 1);

            int rx = l.x + effectiveMargin + rng.nextInt((maxRw - rw) + 1);
            int ry = l.y + effectiveMargin + rng.nextInt((maxRh - rh) + 1);

            Room room = new Room(rx, ry, rw, rh);
            l.room = room;
            rooms.add(room);

            for (int x = rx; x < rx + rw; x++) {
                for (int y = ry; y < ry + rh; y++) {
                    maze.setTile(x, y, Tiles.FLOOR);
                }
            }
        }
        return rooms;
    }

    // ---------------- halls ----------------

    private void connectTree(Leaf node, Random rng, List<Hall> halls) {
        if (node == null || node.left == null || node.right == null) return;

        connectTree(node.left, rng, halls);
        connectTree(node.right, rng, halls);

        Room a = pickRoom(node.left, rng);
        Room b = pickRoom(node.right, rng);
        if (a == null || b == null || a == b) return;

        // Use interior points (less corner/edge weirdness)
        halls.add(new Hall(a.randomInterior(rng), b.randomInterior(rng)));
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

    private void addLoops(List<Room> rooms, Random rng, List<Hall> halls, int extraLoops) {
        if (rooms.size() < 3) return;
        for (int i = 0; i < extraLoops; i++) {
            Room a = rooms.get(rng.nextInt(rooms.size()));
            Room b = rooms.get(rng.nextInt(rooms.size()));
            if (a == b) continue;
            halls.add(new Hall(a.randomInterior(rng), b.randomInterior(rng)));
        }
    }

    private void carveHalls(Maze maze, List<Hall> halls, Random rng, int hallRadius) {
        for (Hall h : halls) {
            Point p1 = h.a();
            Point p2 = h.b();

            int x = p1.x;
            int y = p1.y;

            boolean xFirst = rng.nextBoolean();

            if (xFirst) {
                while (x != p2.x) { setHallTileFat(maze, x, y, hallRadius); x += Integer.compare(p2.x, x); }
                while (y != p2.y) { setHallTileFat(maze, x, y, hallRadius); y += Integer.compare(p2.y, y); }
            } else {
                while (y != p2.y) { setHallTileFat(maze, x, y, hallRadius); y += Integer.compare(p2.y, y); }
                while (x != p2.x) { setHallTileFat(maze, x, y, hallRadius); x += Integer.compare(p2.x, x); }
            }
            setHallTileFat(maze, p2.x, p2.y, hallRadius);
        }
    }

    /** Carve a thick corridor centered at (x,y). Uses a square brush. */
    private void setHallTileFat(Maze maze, int cx, int cy, int r) {
        for (int dx = -r; dx <= r; dx++) {
            for (int dy = -r; dy <= r; dy++) {
                int nx = cx + dx;
                int ny = cy + dy;
                if (!maze.inBounds(nx, ny)) continue;
                if (maze.getTile(nx, ny) == Tiles.WALL) {
                    maze.setTile(nx, ny, Tiles.FLOOR);
                }
            }
        }
    }

    // ---------------- special placement ----------------

    private static class PlaceResult {
        final boolean ok;
        final Point spawn;
        final Point exit;

        PlaceResult(boolean ok, Point spawn, Point exit) {
            this.ok = ok;
            this.spawn = spawn;
            this.exit = exit;
        }
    }

    /** Ensures exactly ONE SPAWN, EXIT, KEY tile, each in a different room. */
    private PlaceResult assignSpecialRoomsAndPlaceTiles(Maze maze, List<Room> rooms, Random rng) {
        for (Room r : rooms) r.setType(RoomType.NORMAL);

        Room spawnRoom = rooms.get(rng.nextInt(rooms.size()));
        Room exitRoom = farthestRoom(rooms, spawnRoom);
        if (exitRoom == spawnRoom) return new PlaceResult(false, null, null);

        Room keyRoom = pickKeyRoom(rooms, spawnRoom, exitRoom, rng);
        if (keyRoom == null) return new PlaceResult(false, null, null);

        spawnRoom.setType(RoomType.SPAWN);
        exitRoom.setType(RoomType.EXIT);
        keyRoom.setType(RoomType.KEY);

        Point spawn = spawnRoom.randomInterior(rng);
        Point exit  = exitRoom.randomInterior(rng);
        Point key   = keyRoom.randomInterior(rng);

        if (maze.getTile(spawn.x, spawn.y) != Tiles.FLOOR) return new PlaceResult(false, null, null);
        if (maze.getTile(exit.x, exit.y)  != Tiles.FLOOR) return new PlaceResult(false, null, null);
        if (maze.getTile(key.x, key.y)   != Tiles.FLOOR) return new PlaceResult(false, null, null);

        maze.setTile(spawn.x, spawn.y, Tiles.SPAWN);
        maze.setTile(exit.x,  exit.y,  Tiles.EXIT);
        maze.setTile(key.x,   key.y,   Tiles.KEY);

        return new PlaceResult(true, spawn, exit);
    }

    private Room farthestRoom(List<Room> rooms, Room from) {
        Room best = from;
        int bestD = -1;

        int fx = from.centerX();
        int fy = from.centerY();

        for (Room r : rooms) {
            int dx = r.centerX() - fx;
            int dy = r.centerY() - fy;
            int d = dx * dx + dy * dy;
            if (d > bestD) {
                bestD = d;
                best = r;
            }
        }
        return best;
    }

    private Room pickKeyRoom(List<Room> rooms, Room spawn, Room exit, Random rng) {
        List<Room> candidates = new ArrayList<>();
        for (Room r : rooms) {
            if (r != spawn && r != exit) candidates.add(r);
        }
        if (candidates.isEmpty()) return null;

        candidates.sort((a, b) -> Integer.compare(dist2(b, spawn), dist2(a, spawn)));
        int top = Math.min(Math.max(1, candidates.size() / 3), candidates.size());
        return candidates.get(rng.nextInt(top));
    }

    private int dist2(Room a, Room b) {
        int dx = a.centerX() - b.centerX();
        int dy = a.centerY() - b.centerY();
        return dx * dx + dy * dy;
    }

    private void clearSpecialTiles(Maze maze) {
        // clear tracked spawn/exit (avoids stale references)
        maze.setEntryCell(null);
        maze.setExitCell(null);

        for (int x = 0; x < maze.getWidth(); x++) {
            for (int y = 0; y < maze.getHeight(); y++) {
                Tiles t = maze.getTile(x, y);
                if (t == Tiles.SPAWN || t == Tiles.EXIT || t == Tiles.KEY) {
                    maze.setTile(x, y, Tiles.FLOOR);
                }
            }
        }
    }

    // ---------------- helpers ----------------

    private void fill(Maze maze, Tiles tile) {
        for (int y = 0; y < maze.getHeight(); y++) {
            for (int x = 0; x < maze.getWidth(); x++) {
                maze.setTile(x, y, tile);
            }
        }
    }
}
