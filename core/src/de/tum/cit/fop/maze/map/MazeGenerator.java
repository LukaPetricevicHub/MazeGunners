package de.tum.cit.fop.maze.map;

import java.awt.Point;
import java.util.*;

/**
 * BSP-based dungeon generator for YOUR codebase.
 *
 * Order:
 * 1) Partition (BSP)
 * 2) Neighbor discovery (leaf adjacency)
 * 3) Shrink (leaf -> room)
 * 4) Hallways (ONLY straight between neighboring rooms if overlap is enough)
 * 5) Grid snapping (optional)
 * 6) Carve rooms + halls
 *
 * Fixes:
 * - NO corridor bends (always straight)
 * - Doorways are widened to corridor width (no 1-tile entrances)
 * - All rooms connected using randomized Prim (MST over neighbor candidates)
 */
public class MazeGenerator {

    private static final int MAX_ATTEMPTS = 40;

    // ---------- tune these ----------
    private static final int MIN_LEAF = 18;
    private static final int MIN_ROOM = 8;
    private static final int ROOM_MARGIN = 2;

    private static final float SHRINK_MIN = 0.80f;
    private static final float SHRINK_MAX = 0.95f;

    private static final float OVERLAP_THRESHOLD = 0.45f; // lower -> more likely to connect neighbors
    private static final int GRID_SNAP = 1;               // tiles. 1 = no snap. (2 or 4 makes blockier)
    private static final int HALL_WIDTH = 5;              // odd recommended: 3/5/7...
    // -------------------------------

    public Maze generate(MazeLoader.LevelSpec spec, int tileSize, long seed, int level) {
        Objects.requireNonNull(spec, "LevelSpec must not be null");
        Random rng = new Random(seed);

        if (spec.width < MIN_LEAF * 2 + 4 || spec.height < MIN_LEAF * 2 + 4) {
            throw new IllegalArgumentException(
                    "Map too small for BSP. width/height should be at least ~" + (MIN_LEAF * 2 + 4));
        }

        for (int attempt = 0; attempt < MAX_ATTEMPTS; attempt++) {
            Maze maze = new Maze(spec.width, spec.height, tileSize);
            fill(maze, Tiles.WALL);

            // 1) BSP partition
            Leaf root = new Leaf(1, 1, spec.width - 2, spec.height - 2);
            List<Leaf> leaves = splitLeaves(root, rng);

            // 2) neighbor discovery on leaves
            discoverNeighbors(leaves);

            // 3) shrink leaves into rooms (rectangles inside leaves)
            List<Room> rooms = new ArrayList<>();
            for (Leaf l : leaves) {
                Room r = makeShrunkRoom(l, rng);
                if (r == null) continue;
                l.room = r;
                rooms.add(r);
            }
            if (rooms.size() < 3) continue;

            // 4) hall candidates from neighbors, with overlap check
            List<CandidateEdge> candidates = buildHallCandidates(leaves);
            if (candidates.isEmpty()) continue;

            // MST via randomized Prim to connect ALL rooms
            List<HallPlan> hallPlans = buildMSTPlans(rooms, candidates, rng);
            if (hallPlans.size() < rooms.size() - 1) continue; // not connected

            // 5) grid snap (optional)
            if (GRID_SNAP > 1) {
                snapRooms(rooms, GRID_SNAP, maze);
                snapHallPlans(hallPlans, GRID_SNAP, maze);
            }

            // carve rooms first
            carveRooms(maze, rooms);

            // carve halls (straight only) + widen doorways
            carveStraightHallsWide(maze, hallPlans, HALL_WIDTH);

            // remove 1-tile spikes outside rooms (optional light cleanup)
            cleanupThinArtifacts(maze, rooms);

            clearSpecialTiles(maze);

            PlaceResult placed = assignSpecialRoomsAndPlaceTiles(maze, rooms, rng);
            if (!placed.ok) continue;

            if (MazeUtils.reachable(maze, placed.spawn, placed.exit)) {
                return maze;
            }
        }

        throw new RuntimeException("Failed to generate BSP dungeon after " + MAX_ATTEMPTS + " attempts");
    }

    // ============================================================
    // BSP leaf
    // ============================================================

    private static class Leaf {
        final int x, y, w, h;
        Leaf left, right;
        Room room;
        final List<Leaf> neighbors = new ArrayList<>();

        Leaf(int x, int y, int w, int h) {
            this.x = x; this.y = y; this.w = w; this.h = h;
        }

        boolean split(Random rng) {
            boolean canH = h >= MIN_LEAF * 2;
            boolean canV = w >= MIN_LEAF * 2;
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
            int cutMin = MIN_LEAF;
            int cutMax = maxSpan - MIN_LEAF;
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

    // ============================================================
    // Neighbor discovery on leaf rectangles
    // ============================================================

    private void discoverNeighbors(List<Leaf> leaves) {
        for (Leaf l : leaves) l.neighbors.clear();

        for (int i = 0; i < leaves.size(); i++) {
            Leaf a = leaves.get(i);
            for (int j = i + 1; j < leaves.size(); j++) {
                Leaf b = leaves.get(j);
                if (touchingAndOverlapping(a, b)) {
                    a.neighbors.add(b);
                    b.neighbors.add(a);
                }
            }
        }
    }

    private boolean touchingAndOverlapping(Leaf a, Leaf b) {
        // vertical touch: right border equals left border
        boolean touchV = (a.x + a.w == b.x) || (b.x + b.w == a.x);
        if (touchV) {
            int overlap = intervalOverlap(a.y, a.y + a.h, b.y, b.y + b.h);
            return overlap > 0;
        }

        // horizontal touch: bottom equals top
        boolean touchH = (a.y + a.h == b.y) || (b.y + b.h == a.y);
        if (touchH) {
            int overlap = intervalOverlap(a.x, a.x + a.w, b.x, b.x + b.w);
            return overlap > 0;
        }
        return false;
    }

    private int intervalOverlap(int a0, int a1, int b0, int b1) {
        int lo = Math.max(a0, b0);
        int hi = Math.min(a1, b1);
        return hi - lo;
    }

    // ============================================================
    // Shrink leaf -> room
    // ============================================================

    private Room makeShrunkRoom(Leaf l, Random rng) {
        int innerX = l.x + ROOM_MARGIN;
        int innerY = l.y + ROOM_MARGIN;
        int innerW = l.w - 2 * ROOM_MARGIN;
        int innerH = l.h - 2 * ROOM_MARGIN;

        if (innerW < MIN_ROOM || innerH < MIN_ROOM) return null;

        float s = SHRINK_MIN + rng.nextFloat() * (SHRINK_MAX - SHRINK_MIN);

        int rw = Math.max(MIN_ROOM, Math.round(innerW * s));
        int rh = Math.max(MIN_ROOM, Math.round(innerH * s));

        int maxOx = Math.max(0, innerW - rw);
        int maxOy = Math.max(0, innerH - rh);

        int rx = innerX + (maxOx == 0 ? 0 : rng.nextInt(maxOx + 1));
        int ry = innerY + (maxOy == 0 ? 0 : rng.nextInt(maxOy + 1));

        return new Room(rx, ry, rw, rh);
    }

    // ============================================================
    // Candidate halls (neighbor overlap on SHRUNK rooms)
    // ============================================================

    private enum Orientation { LR, UD }

    private static class CandidateEdge {
        final Room a, b;
        final Orientation orient;
        final int overlapLo;   // inclusive-ish
        final int overlapHi;   // exclusive-ish
        final int d2;

        CandidateEdge(Room a, Room b, Orientation orient, int overlapLo, int overlapHi, int d2) {
            this.a = a;
            this.b = b;
            this.orient = orient;
            this.overlapLo = overlapLo;
            this.overlapHi = overlapHi;
            this.d2 = d2;
        }
    }

    private List<CandidateEdge> buildHallCandidates(List<Leaf> leaves) {
        List<CandidateEdge> out = new ArrayList<>();
        Set<Long> seen = new HashSet<>();

        for (Leaf la : leaves) {
            if (la.room == null) continue;
            for (Leaf lb : la.neighbors) {
                if (lb.room == null) continue;

                Room a = la.room;
                Room b = lb.room;

                long key = pairKey(a, b);
                if (seen.contains(key)) continue;
                seen.add(key);

                CandidateEdge e = makeCandidateIfOverlapEnough(la, lb, a, b);
                if (e != null) out.add(e);
            }
        }

        out.sort(Comparator.comparingInt(e -> e.d2));
        return out;
    }

    /**
     * Overlap check explanation:
     * - If leaves touch left/right, corridor will be horizontal => need Y-overlap between rooms.
     * - If leaves touch up/down, corridor vertical => need X-overlap between rooms.
     * We require overlap >= OVERLAP_THRESHOLD * minSpan.
     */
    private CandidateEdge makeCandidateIfOverlapEnough(Leaf la, Leaf lb, Room a, Room b) {
        boolean touchV = (la.x + la.w == lb.x) || (lb.x + lb.w == la.x);
        boolean touchH = (la.y + la.h == lb.y) || (lb.y + lb.h == la.y);

        if (touchV) {
            int lo = Math.max(a.y(), b.y());
            int hi = Math.min(a.y() + a.h(), b.y() + b.h());
            int overlap = hi - lo;
            int minSpan = Math.min(a.h(), b.h());
            if (minSpan <= 0) return null;
            if (overlap < OVERLAP_THRESHOLD * minSpan) return null;
            return new CandidateEdge(a, b, Orientation.LR, lo, hi, dist2(a, b));
        }

        if (touchH) {
            int lo = Math.max(a.x(), b.x());
            int hi = Math.min(a.x() + a.w(), b.x() + b.w());
            int overlap = hi - lo;
            int minSpan = Math.min(a.w(), b.w());
            if (minSpan <= 0) return null;
            if (overlap < OVERLAP_THRESHOLD * minSpan) return null;
            return new CandidateEdge(a, b, Orientation.UD, lo, hi, dist2(a, b));
        }

        return null;
    }

    private long pairKey(Room a, Room b) {
        int ha = System.identityHashCode(a);
        int hb = System.identityHashCode(b);
        int lo = Math.min(ha, hb);
        int hi = Math.max(ha, hb);
        return (((long) lo) << 32) ^ (hi & 0xffffffffL);
    }

    private int dist2(Room a, Room b) {
        int dx = a.centerX() - b.centerX();
        int dy = a.centerY() - b.centerY();
        return dx * dx + dy * dy;
    }

    // ============================================================
    // Randomized Prim MST -> HallPlans (STRAIGHT)
    // ============================================================

    private static class HallPlan {
        final Orientation orient;
        final Room a, b;
        final int lineCoord; // if LR => y ; if UD => x
        HallPlan(Orientation orient, Room a, Room b, int lineCoord) {
            this.orient = orient;
            this.a = a;
            this.b = b;
            this.lineCoord = lineCoord;
        }
    }

    private List<HallPlan> buildMSTPlans(List<Room> rooms, List<CandidateEdge> candidates, Random rng) {
        // adjacency list
        Map<Room, List<CandidateEdge>> adj = new HashMap<>();
        for (Room r : rooms) adj.put(r, new ArrayList<>());
        for (CandidateEdge e : candidates) {
            adj.get(e.a).add(e);
            adj.get(e.b).add(e);
        }

        List<HallPlan> plans = new ArrayList<>();
        Set<Room> inTree = new HashSet<>();

        Room start = rooms.get(rng.nextInt(rooms.size()));
        inTree.add(start);

        List<CandidateEdge> frontier = new ArrayList<>(adj.get(start));

        while (!frontier.isEmpty() && inTree.size() < rooms.size()) {
            // pick among the shortest few for locality, but keep randomness
            frontier.sort(Comparator.comparingInt(e -> e.d2));
            int pickFrom = Math.min(frontier.size(), Math.max(3, rooms.size() / 3));
            CandidateEdge chosen = frontier.remove(rng.nextInt(pickFrom));

            Room u = chosen.a;
            Room v = chosen.b;

            Room next = null;
            if (inTree.contains(u) && !inTree.contains(v)) next = v;
            else if (inTree.contains(v) && !inTree.contains(u)) next = u;
            else continue;

            int coord = pickDoorLineCoord(chosen, rng, HALL_WIDTH / 2);
            plans.add(new HallPlan(chosen.orient, u, v, coord));

            inTree.add(next);

            for (CandidateEdge e : adj.get(next)) {
                Room other = (e.a == next) ? e.b : e.a;
                if (!inTree.contains(other)) frontier.add(e);
            }
        }

        return plans;
    }

    /**
     * Choose a corridor centerline coordinate inside the overlap range.
     * We pad by corridor radius so the widened corridor still fits within overlap.
     */
    private int pickDoorLineCoord(CandidateEdge e, Random rng, int radius) {
        int lo = e.overlapLo + radius;
        int hi = e.overlapHi - radius;
        if (hi <= lo) {
            // fallback: clamp to middle
            return (e.overlapLo + e.overlapHi) / 2;
        }
        return lo + rng.nextInt(hi - lo + 1);
    }

    // ============================================================
    // Carving (rooms + STRAIGHT halls)
    // ============================================================

    private void carveRooms(Maze maze, List<Room> rooms) {
        for (Room r : rooms) {
            for (int x = r.x(); x < r.x() + r.w(); x++) {
                for (int y = r.y(); y < r.y() + r.h(); y++) {
                    if (maze.inBounds(x, y)) maze.setTile(x, y, Tiles.FLOOR);
                }
            }
        }
    }

    /**
     * Straight-only carving:
     * - LR neighbors => horizontal corridor (constant y), widened vertically
     * - UD neighbors => vertical corridor (constant x), widened horizontally
     *
     * Also widens the "door mouth" at the room boundary so it is not 1-tile.
     */
    private void carveStraightHallsWide(Maze maze, List<HallPlan> plans, int hallWidth) {
        int r = Math.max(0, hallWidth / 2);

        for (HallPlan p : plans) {
            Room a = p.a;
            Room b = p.b;

            if (p.orient == Orientation.LR) {
                // determine left/right
                Room left = (a.centerX() <= b.centerX()) ? a : b;
                Room right = (left == a) ? b : a;

                int y = clamp(p.lineCoord, 1, maze.getHeight() - 2);

                // corridor should run in the GAP between rooms (from just outside left to just outside right)
                int xStart = (left.x() + left.w());   // tile just outside right wall of left room
                int xEnd   = (right.x() - 1);         // tile just outside left wall of right room

                // carve horizontal line widened vertically
                for (int x = Math.min(xStart, xEnd); x <= Math.max(xStart, xEnd); x++) {
                    carveVerticalBand(maze, x, y, r);
                }

                // widen doorway "mouth" into both rooms so it's not a single-tile entrance
                carveDoorMouthLR(maze, left, y, r, true);
                carveDoorMouthLR(maze, right, y, r, false);

            } else {
                // UD
                Room top = (a.centerY() <= b.centerY()) ? a : b;
                Room bottom = (top == a) ? b : a;

                int x = clamp(p.lineCoord, 1, maze.getWidth() - 2);

                int yStart = (top.y() + top.h());     // tile just outside bottom wall of top room
                int yEnd   = (bottom.y() - 1);        // tile just outside top wall of bottom room

                for (int y = Math.min(yStart, yEnd); y <= Math.max(yStart, yEnd); y++) {
                    carveHorizontalBand(maze, x, y, r);
                }

                carveDoorMouthUD(maze, top, x, r, true);
                carveDoorMouthUD(maze, bottom, x, r, false);
            }
        }
    }

    private void carveVerticalBand(Maze maze, int x, int y, int r) {
        for (int dy = -r; dy <= r; dy++) {
            int ny = y + dy;
            if (!maze.inBounds(x, ny)) continue;
            if (maze.getTile(x, ny) == Tiles.WALL) maze.setTile(x, ny, Tiles.FLOOR);
        }
    }

    private void carveHorizontalBand(Maze maze, int x, int y, int r) {
        for (int dx = -r; dx <= r; dx++) {
            int nx = x + dx;
            if (!maze.inBounds(nx, y)) continue;
            if (maze.getTile(nx, y) == Tiles.WALL) maze.setTile(nx, y, Tiles.FLOOR);
        }
    }

    /**
     * Widen the doorway at the room boundary for left/right corridor.
     * If isLeftRoom=true, open at room's right edge; else open at left edge.
     */
    private void carveDoorMouthLR(Maze maze, Room room, int y, int r, boolean isLeftRoom) {
        int doorXInside = isLeftRoom ? (room.x() + room.w() - 1) : room.x();
        int doorXOutside = isLeftRoom ? (doorXInside + 1) : (doorXInside - 1);

        // open outside tile + a short "throat" inside the room edge at full width
        for (int dy = -r; dy <= r; dy++) {
            int ny = y + dy;
            if (maze.inBounds(doorXOutside, ny)) maze.setTile(doorXOutside, ny, Tiles.FLOOR);
            if (maze.inBounds(doorXInside, ny))  maze.setTile(doorXInside, ny, Tiles.FLOOR);
        }
    }

    /**
     * Widen the doorway at the room boundary for up/down corridor.
     * If isTopRoom=true, open at room's bottom edge; else at top edge.
     */
    private void carveDoorMouthUD(Maze maze, Room room, int x, int r, boolean isTopRoom) {
        int doorYInside = isTopRoom ? (room.y() + room.h() - 1) : room.y();
        int doorYOutside = isTopRoom ? (doorYInside + 1) : (doorYInside - 1);

        for (int dx = -r; dx <= r; dx++) {
            int nx = x + dx;
            if (maze.inBounds(nx, doorYOutside)) maze.setTile(nx, doorYOutside, Tiles.FLOOR);
            if (maze.inBounds(nx, doorYInside))  maze.setTile(nx, doorYInside, Tiles.FLOOR);
        }
    }

    // ============================================================
    // Optional: snap (tile grid)
    // ============================================================

    private void snapRooms(List<Room> rooms, int snap, Maze maze) {
        for (int i = 0; i < rooms.size(); i++) {
            Room r = rooms.get(i);

            int x0 = snapDown(r.x(), snap);
            int y0 = snapDown(r.y(), snap);
            int x1 = snapDown(r.x() + r.w(), snap);
            int y1 = snapDown(r.y() + r.h(), snap);

            x0 = clamp(x0, 1, maze.getWidth() - 2);
            y0 = clamp(y0, 1, maze.getHeight() - 2);
            x1 = clamp(x1, x0 + MIN_ROOM, maze.getWidth() - 1);
            y1 = clamp(y1, y0 + MIN_ROOM, maze.getHeight() - 1);

            rooms.set(i, new Room(x0, y0, x1 - x0, y1 - y0));
        }
    }

    private void snapHallPlans(List<HallPlan> plans, int snap, Maze maze) {
        for (int i = 0; i < plans.size(); i++) {
            HallPlan p = plans.get(i);
            int coord = snapDown(p.lineCoord, snap);
            if (p.orient == Orientation.LR) coord = clamp(coord, 1, maze.getHeight() - 2);
            else coord = clamp(coord, 1, maze.getWidth() - 2);
            plans.set(i, new HallPlan(p.orient, p.a, p.b, coord));
        }
    }

    private int snapDown(int v, int snap) {
        return (v / snap) * snap;
    }

    private int clamp(int v, int lo, int hi) {
        return Math.max(lo, Math.min(hi, v));
    }

    // ============================================================
    // Cleanup: remove 1-tile spikes outside rooms
    // ============================================================

    private void cleanupThinArtifacts(Maze maze, List<Room> rooms) {
        boolean[][] inRoom = new boolean[maze.getWidth()][maze.getHeight()];
        for (Room r : rooms) {
            for (int x = r.x(); x < r.x() + r.w(); x++) {
                for (int y = r.y(); y < r.y() + r.h(); y++) {
                    if (maze.inBounds(x, y)) inRoom[x][y] = true;
                }
            }
        }

        for (int pass = 0; pass < 2; pass++) {
            List<Point> toWall = new ArrayList<>();
            for (int y = 1; y < maze.getHeight() - 1; y++) {
                for (int x = 1; x < maze.getWidth() - 1; x++) {
                    if (inRoom[x][y]) continue;
                    if (maze.getTile(x, y) != Tiles.FLOOR) continue;

                    int wn = walkableNeighbors4(maze, x, y);
                    if (wn <= 1) toWall.add(new Point(x, y));
                }
            }
            for (Point p : toWall) maze.setTile(p.x, p.y, Tiles.WALL);
        }
    }

    private int walkableNeighbors4(Maze maze, int x, int y) {
        int c = 0;
        if (maze.getTile(x + 1, y).isWalkable()) c++;
        if (maze.getTile(x - 1, y).isWalkable()) c++;
        if (maze.getTile(x, y + 1).isWalkable()) c++;
        if (maze.getTile(x, y - 1).isWalkable()) c++;
        return c;
    }

    // ============================================================
    // Special placement (your logic)
    // ============================================================

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
        for (Room r : rooms) if (r != spawn && r != exit) candidates.add(r);
        if (candidates.isEmpty()) return null;

        candidates.sort((a, b) -> Integer.compare(dist2(b, spawn), dist2(a, spawn)));
        int top = Math.min(Math.max(1, candidates.size() / 3), candidates.size());
        return candidates.get(rng.nextInt(top));
    }

    private void clearSpecialTiles(Maze maze) {
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

    // ============================================================
    // Helpers
    // ============================================================

    private void fill(Maze maze, Tiles tile) {
        for (int y = 0; y < maze.getHeight(); y++) {
            for (int x = 0; x < maze.getWidth(); x++) {
                maze.setTile(x, y, tile);
            }
        }
    }
}
