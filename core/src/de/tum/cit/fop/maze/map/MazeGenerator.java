package de.tum.cit.fop.maze.map;

import java.awt.Point;
import java.util.*;

public class MazeGenerator {

    private static final int MAX_ATTEMPTS = 120;

    /** Level-tuned parameters */
    private static final class Params {
        final int minLeaf;
        final int minRoom;
        final int roomMargin;
        final int hallWidth;   // recommended odd: 1,3,5...
        final int extraLoops;  // keep 0 if you don't want loops

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
            case 1 -> new Params(22, 10, 1, 3, 0);
            case 2 -> new Params(16, 7, 1, 3, 0);
            case 3 -> new Params(18, 8, 1, 3, 0);
            case 4 -> new Params(18, 9, 1, 3, 0);
            case 5 -> new Params(20, 10, 1, 3, 0);
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

            // Ensure every subtree has a usable anchor (prevents null anchors => no corridors)
            computeAnchors(root, rng);

            List<Hall> halls = new ArrayList<>();
            connectTree(root, rng, halls); // BSP connectivity (tree, no loops)

            // no loops on purpose (you said you don't want loops)
            // addLoops(rooms, rng, halls, p.extraLoops);

            carveHalls(maze, halls, rng, p.hallRadius());

            // clear any old special tiles
            clearSpecialTiles(maze);

            PlaceResult placed = assignSpecialRoomsAndPlaceTiles(maze, rooms, rng);
            if (!placed.ok) continue;

            // reachability test (spawn -> exit)
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

        // split metadata + anchor point for corridor connections
        boolean splitHorizontal;
        int splitPos;     // absolute coordinate of split line (x or y)
        Point anchor;     // connection point for this subtree

        Leaf(int x, int y, int w, int h) {
            this.x = x; this.y = y; this.w = w; this.h = h;
        }

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

            splitHorizontal = horiz;

            if (horiz) {
                left  = new Leaf(x, y, w, cut);
                right = new Leaf(x, y + cut, w, h - cut);
                splitPos = right.y; // absolute boundary y
            } else {
                left  = new Leaf(x, y, cut, h);
                right = new Leaf(x + cut, y, w - cut, h);
                splitPos = right.x; // absolute boundary x
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

        // Leave buffer so wide halls don't instantly merge rooms.
        // Slightly loosen vs +hallRadius to avoid starving room placement on small maps.
        int effectiveMargin = roomMargin + Math.max(0, hallRadius - 1);

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

            // IMPORTANT: room anchor = exactly one "doorway"/connection point inside the room
            l.anchor = room.randomInterior(rng);

            rooms.add(room);

            for (int x = rx; x < rx + rw; x++) {
                for (int y = ry; y < ry + rh; y++) {
                    maze.setTile(x, y, Tiles.FLOOR);
                }
            }
        }
        return rooms;
    }

    /**
     * Ensures every subtree has an anchor even if some leaves couldn't place rooms.
     * Without this, connectTree may see null anchors and create no corridors.
     */
    private void computeAnchors(Leaf node, Random rng) {
        if (node == null) return;

        // Leaf: if it has a room, ensure it has an anchor
        if (node.left == null && node.right == null) {
            if (node.anchor == null && node.room != null) {
                node.anchor = node.room.randomInterior(rng);
            }
            return;
        }

        computeAnchors(node.left, rng);
        computeAnchors(node.right, rng);

        Point la = (node.left != null) ? node.left.anchor : null;
        Point ra = (node.right != null) ? node.right.anchor : null;

        // If one side has no anchor, borrow from the other side (keeps connectivity)
        if (la == null && ra != null && node.left != null) node.left.anchor = ra;
        if (ra == null && la != null && node.right != null) node.right.anchor = la;
    }

    // ---------------- halls (no loops) ----------------

    /**
     * Junction-based connection:
     * - Leaves (rooms) have exactly ONE anchor inside the room.
     * - Internal nodes connect child anchors to a junction on the split line.
     * - Branching happens in corridors (junctions), not inside rooms.
     * This prevents rooms from having multiple corridors.
     */
    private void connectTree(Leaf node, Random rng, List<Hall> halls) {
        if (node == null) return;

        // leaf: anchor is inside room and should be used once
        if (node.left == null || node.right == null) return;

        connectTree(node.left, rng, halls);
        connectTree(node.right, rng, halls);

        Point a = node.left.anchor;
        Point b = node.right.anchor;
        if (a == null || b == null) return;

        Point j = pickJunctionOnSplit(node, rng);
        if (j == null) return;

        halls.add(new Hall(a, j));
        halls.add(new Hall(j, b));

        // parent will connect to this junction, not into a room
        node.anchor = j;
    }

    private Point pickJunctionOnSplit(Leaf node, Random rng) {
        int x0 = node.x + 1;
        int x1 = node.x + node.w - 2;
        int y0 = node.y + 1;
        int y1 = node.y + node.h - 2;

        if (x1 < x0 || y1 < y0) return null;

        if (node.splitHorizontal) {
            int y = node.splitPos;
            y = Math.max(y0, Math.min(y1, y));
            int x = x0 + rng.nextInt(x1 - x0 + 1);
            return new Point(x, y);
        } else {
            int x = node.splitPos;
            x = Math.max(x0, Math.min(x1, x));
            int y = y0 + rng.nextInt(y1 - y0 + 1);
            return new Point(x, y);
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
                while (x != p2.x) {
                    carveHorizontalStep(maze, x, y, p2.x, hallRadius);
                    x += Integer.compare(p2.x, x);
                }
                while (y != p2.y) {
                    carveVerticalStep(maze, x, y, p2.y, hallRadius);
                    y += Integer.compare(p2.y, y);
                }
            } else {
                while (y != p2.y) {
                    carveVerticalStep(maze, x, y, p2.y, hallRadius);
                    y += Integer.compare(p2.y, y);
                }
                while (x != p2.x) {
                    carveHorizontalStep(maze, x, y, p2.x, hallRadius);
                    x += Integer.compare(p2.x, x);
                }
            }

            // carve final cell with both orientations to avoid tiny gaps
            carveHorizontalBand(maze, x, y, hallRadius);
            carveVerticalBand(maze, x, y, hallRadius);
        }
    }

    private void carveHorizontalStep(Maze maze, int x, int y, int targetX, int r) {
        carveHorizontalBand(maze, x, y, r);
    }

    private void carveVerticalStep(Maze maze, int x, int y, int targetY, int r) {
        carveVerticalBand(maze, x, y, r);
    }

    /** Moving horizontally: widen vertically only */
    private void carveHorizontalBand(Maze maze, int x, int y, int r) {
        for (int dy = -r; dy <= r; dy++) {
            int ny = y + dy;
            if (!maze.inBounds(x, ny)) continue;
            if (maze.getTile(x, ny) == Tiles.WALL) {
                maze.setTile(x, ny, Tiles.FLOOR);
            }
        }
    }

    /** Moving vertically: widen horizontally only */
    private void carveVerticalBand(Maze maze, int x, int y, int r) {
        for (int dx = -r; dx <= r; dx++) {
            int nx = x + dx;
            if (!maze.inBounds(nx, y)) continue;
            if (maze.getTile(nx, y) == Tiles.WALL) {
                maze.setTile(nx, y, Tiles.FLOOR);
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

    /**
     * Clears SPAWN/EXIT/KEY tiles back to FLOOR.
     * (Doesn't rely on Maze.setEntryCell/setExitCell to keep compatibility with your Maze class.)
     */
    private void clearSpecialTiles(Maze maze) {
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
