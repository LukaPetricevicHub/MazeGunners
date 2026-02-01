package de.tum.cit.fop.maze.map;

import java.awt.Point;
import java.util.*;

/**
 * BSP-based dungeon generator.
 *
 * Order:
 * 1) Partition (BSP)
 * 2) Neighbor discovery (leaf adjacency)
 * 3) Shrink (leaf -> room)
 * 4) Hallways (ONLY straight between neighboring rooms if overlap is enough)
 * 5) Grid snapping (optional)
 * 6) Carve rooms + halls
 *
 * Extras in this version:
 * - Level size scales with level 1..5
 * - Target number of rooms scales with level 1..5
 * - Straight hallways only, exact width (even/odd supported)
 * - Doors placed at BOTH ends of each hall (aligned with wall opening)
 * - EXIT is carved on the MAZE BORDER and its width matches hall width
 * - Exit corridor is prevented from cutting through other rooms
 */
public class MazeGenerator {

    private static final int MAX_ATTEMPTS = 40;

    private static final int MIN_LEAF = 18;
    private static final int MIN_ROOM = 8;
    private static final int ROOM_MARGIN = 2;
    private static final float SHRINK_MIN = 0.50f;
    private static final float SHRINK_MAX = 0.58f;
    private static final float OVERLAP_THRESHOLD = 0.45f;
    private static final int GRID_SNAP = 1;
    private static final int HALL_WIDTH = 2;

    private static final int BOSS_ROOM_MARGIN = 1;
    private static final float BOSS_SHRINK_MIN = 0.82f;
    private static final float BOSS_SHRINK_MAX = 0.92f;
    private static final int BOSS_MIN_SIZE = 16;

    // Level sizes for level=1..5 (index 0 unused)
    private static final int[] LEVEL_W = {0, 80, 80, 90, 90, 100};
    private static final int[] LEVEL_H = {0, 80, 80, 90, 90, 100};

    // Target number of rooms per level (min, max)
    private static final int[][] LEVEL_ROOMS = {
            {},            // unused (index 0)
            {12, 14},      // level 1
            {14, 16},      // level 2
            {16, 18},      // level 3
            {18, 20},      // level 4
            {20, 22}       // level 5
    };

    /**
     * Generates a fully carved maze using BSP dungeon generation.
     *
     * @param spec level specification (dimensions and overrides)
     * @param tileSize size of a single tile in world units
     * @param seed random seed for deterministic generation
     * @param level current game level (affects scaling and boss logic)
     * @return generated maze instance
     * @throws RuntimeException if generation fails after multiple attempts
     */
    public Maze generate(MazeLoader.LevelSpec spec, int tileSize, long seed, int level) {
        Objects.requireNonNull(spec, "LevelSpec must not be null");
        spec = applyLevelSize(spec, level);

        Random rng = new Random(seed);

        if (spec.width < MIN_LEAF * 2 + 4 || spec.height < MIN_LEAF * 2 + 4) {
            throw new IllegalArgumentException(
                    "Map too small for BSP. width/height should be at least ~" + (MIN_LEAF * 2 + 4));
        }

        for (int attempt = 0; attempt < MAX_ATTEMPTS; attempt++) {
            Maze maze = new Maze(spec.width, spec.height, tileSize);
            fill(maze, Tiles.WALL);

            Leaf root = new Leaf(1, 1, spec.width - 2, spec.height - 2);
            List<Leaf> leaves = splitLeaves(root, rng, level, spec.width, spec.height);

            discoverNeighbors(leaves);

            List<Room> rooms = new ArrayList<>();
            for (Leaf l : leaves) {
                Room r = makeShrunkRoom(l, rng);
                if (r == null) continue;
                l.room = r;
                rooms.add(r);
            }
            if (rooms.size() < 3) continue;

            int lv = clampLevel(level);
            int minRooms = LEVEL_ROOMS[lv][0];
            int maxRooms = LEVEL_ROOMS[lv][1];

            if (rooms.size() < minRooms) continue;

            if (rooms.size() > maxRooms) {
                Collections.shuffle(rooms, rng);
                rooms = new ArrayList<>(rooms.subList(0, maxRooms));

                Set<Room> keep = new HashSet<>(rooms);
                for (Leaf l : leaves) {
                    if (l.room != null && !keep.contains(l.room)) l.room = null;
                }
            }

            boolean isFinalLevel = (clampLevel(level) == 5);
            if (isFinalLevel) {
                Leaf bossLeaf = pickBossLeaf(leaves);
                if (bossLeaf == null) continue;

                Room old = bossLeaf.room;
                Room bossRoom = makeBossRoom(bossLeaf, rng);
                if (bossRoom == null) continue;

                bossLeaf.room = bossRoom;
                bossRoom.setType(RoomType.BOSS);

                if (old != null) rooms.remove(old);
                rooms.add(bossRoom);
            }

            List<CandidateEdge> candidates = buildHallCandidates(leaves);
            if (candidates.isEmpty()) continue;

            List<HallPlan> hallPlans = buildMSTPlans(rooms, candidates, rng);
            if (hallPlans.size() < rooms.size() - 1) continue;

            if (GRID_SNAP > 1) {
                snapRooms(rooms, GRID_SNAP, maze);
                snapHallPlans(hallPlans, GRID_SNAP, maze);
            }

            carveRooms(maze, rooms);
            carveStraightHallsWide(maze, hallPlans, HALL_WIDTH);
            cleanupThinArtifacts(maze, rooms);

            clearSpecialTiles(maze);

            maze.setRooms(rooms);

            PlaceResult placed = assignSpecialRoomsAndPlaceTiles_BORDER_EXIT(maze, rooms, rng, HALL_WIDTH, level);
            if (!placed.ok) continue;

            if (MazeUtils.reachable(maze, placed.spawn, placed.exit)) {
                return maze;
            }
        }
        throw new RuntimeException("Failed to generate BSP dungeon after " + MAX_ATTEMPTS + " attempts");
    }

    private static class Leaf {
        final int x, y, w, h;
        Leaf left, right;
        Room room;
        final List<Leaf> neighbors = new ArrayList<>();

        Leaf(int x, int y, int w, int h) {
            this.x = x;
            this.y = y;
            this.w = w;
            this.h = h;
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

    private List<Leaf> splitLeaves(Leaf root, Random rng, int level, int mapW, int mapH) {
        int lv = clampLevel(level);

        int minRooms = LEVEL_ROOMS[lv][0];
        int maxRooms = LEVEL_ROOMS[lv][1];
        int targetRooms = rndBetween(rng, minRooms, maxRooms);

        int targetLeaves = targetRooms * 2;
        int hardCapLeaves = targetRooms * 3;


        int innerW = mapW - 2;
        int innerH = mapH - 2;
        double avgLeafArea = (innerW * (double) innerH) / Math.max(1, targetLeaves);
        int dynamicMinLeaf = (int) (Math.sqrt(avgLeafArea) * 0.9);
        int minLeafFloor = MIN_ROOM + 2 * ROOM_MARGIN; // keep your MIN_LEAF as a floor
        int minLeaf = clamp(dynamicMinLeaf, minLeafFloor, 22);

        List<Leaf> leaves = new ArrayList<>();
        leaves.add(root);

        boolean didSplit;
        do {
            didSplit = false;

            if (leaves.size() >= targetLeaves) break;
            if (leaves.size() >= hardCapLeaves) break;

            List<Leaf> next = new ArrayList<>();
            for (Leaf l : leaves) {

                if (next.size() >= hardCapLeaves) {
                    next.add(l);
                    continue;
                }

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
        boolean touchV = (a.x + a.w == b.x) || (b.x + b.w == a.x);
        if (touchV) {
            int overlap = intervalOverlap(a.y, a.y + a.h, b.y, b.y + b.h);
            return overlap > 0;
        }

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

    private enum Orientation {LR, UD}

    private static class CandidateEdge {
        final Room a, b;
        final Orientation orient;
        final int overlapLo;
        final int overlapHi;
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


    private static class HallPlan {
        final Orientation orient;
        final Room a, b;
        final int lineCoord;

        HallPlan(Orientation orient, Room a, Room b, int lineCoord) {
            this.orient = orient;
            this.a = a;
            this.b = b;
            this.lineCoord = lineCoord;
        }
    }

    private List<HallPlan> buildMSTPlans(List<Room> rooms, List<CandidateEdge> candidates, Random rng) {
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
            frontier.sort(Comparator.comparingInt(e -> e.d2));
            int pickFrom = Math.min(frontier.size(), Math.max(3, rooms.size() / 3));
            CandidateEdge chosen = frontier.remove(rng.nextInt(pickFrom));

            Room u = chosen.a;
            Room v = chosen.b;

            Room next = null;
            if (inTree.contains(u) && !inTree.contains(v)) next = v;
            else if (inTree.contains(v) && !inTree.contains(u)) next = u;
            else continue;

            int pad = hallPad(HALL_WIDTH);
            int coord = pickDoorLineCoord(chosen, rng, pad);
            plans.add(new HallPlan(chosen.orient, u, v, coord));

            inTree.add(next);

            for (CandidateEdge e : adj.get(next)) {
                Room other = (e.a == next) ? e.b : e.a;
                if (!inTree.contains(other)) frontier.add(e);
            }
        }

        return plans;
    }

    private int hallPad(int hallWidth) {
        int lo = -(hallWidth / 2);
        int hi = (hallWidth - 1) / 2;
        return Math.max(Math.abs(lo), Math.abs(hi));
    }

    private int pickDoorLineCoord(CandidateEdge e, Random rng, int pad) {
        int lo = e.overlapLo + pad;
        int hi = e.overlapHi - pad;
        if (hi <= lo) return (e.overlapLo + e.overlapHi) / 2;
        return lo + rng.nextInt(hi - lo + 1);
    }


    private void carveRooms(Maze maze, List<Room> rooms) {
        maze.clearRoomMask();

        for (Room r : rooms) {
            for (int x = r.x(); x < r.x() + r.w(); x++) {
                for (int y = r.y(); y < r.y() + r.h(); y++) {
                    if (!maze.inBounds(x, y)) continue;
                    maze.setTile(x, y, Tiles.FLOOR);
                    maze.setRoomCell(x, y, true); // room
                }
            }
        }
    }

    private void carveStraightHallsWide(Maze maze, List<HallPlan> plans, int hallWidth) {
        for (HallPlan p : plans) {
            Room a = p.a;
            Room b = p.b;

            if (p.orient == Orientation.LR) {
                Room left = (a.centerX() <= b.centerX()) ? a : b;
                Room right = (left == a) ? b : a;

                int y = clamp(p.lineCoord, 1, maze.getHeight() - 2);

                int xStart = left.x() + left.w();
                int xEnd = right.x() - 1;

                for (int x = Math.min(xStart, xEnd); x <= Math.max(xStart, xEnd); x++) {
                    carveVerticalBandExact(maze, x, y, hallWidth);
                }

                carveDoorMouthLR(maze, left, y, hallWidth, true);
                carveDoorMouthLR(maze, right, y, hallWidth, false);

                // doors aligned with wall opening (outside edge)
                placeDoorBandLRAligned(maze, left, y, hallWidth, true);
                placeDoorBandLRAligned(maze, right, y, hallWidth, false);

            } else {
                Room top = (a.centerY() <= b.centerY()) ? a : b;
                Room bottom = (top == a) ? b : a;

                int x = clamp(p.lineCoord, 1, maze.getWidth() - 2);

                int yStart = top.y() + top.h();
                int yEnd = bottom.y() - 1;

                for (int y = Math.min(yStart, yEnd); y <= Math.max(yStart, yEnd); y++) {
                    carveHorizontalBandExact(maze, x, y, hallWidth);
                }

                carveDoorMouthUD(maze, top, x, hallWidth, true);
                carveDoorMouthUD(maze, bottom, x, hallWidth, false);

                placeDoorBandUDAligned(maze, top, x, hallWidth, true);
                placeDoorBandUDAligned(maze, bottom, x, hallWidth, false);
            }
        }
    }

    private void carveVerticalBandExact(Maze maze, int x, int y, int hallWidth) {
        int lo = -(hallWidth / 2);
        int hi = (hallWidth - 1) / 2;

        for (int dy = lo; dy <= hi; dy++) {
            int ny = y + dy;
            if (!maze.inBounds(x, ny)) continue;
            if (maze.getTile(x, ny) == Tiles.WALL) maze.setTile(x, ny, Tiles.FLOOR);
            if (!maze.isRoomCell(x, ny)) {
                maze.setRoomCell(x, ny, false);
            }

        }

    }

    private void carveHorizontalBandExact(Maze maze, int x, int y, int hallWidth) {
        int lo = -(hallWidth / 2);
        int hi = (hallWidth - 1) / 2;

        for (int dx = lo; dx <= hi; dx++) {
            int nx = x + dx;
            if (!maze.inBounds(nx, y)) continue;
            if (maze.getTile(nx, y) == Tiles.WALL) maze.setTile(nx, y, Tiles.FLOOR);
            if (!maze.isRoomCell(nx, y)) {
                maze.setRoomCell(nx, y, false);
            }
        }
    }

    private void carveDoorMouthLR(Maze maze, Room room, int y, int hallWidth, boolean isLeftRoom) {
        int lo = -(hallWidth / 2);
        int hi = (hallWidth - 1) / 2;

        int doorXInside = isLeftRoom ? (room.x() + room.w() - 1) : room.x();
        int doorXOutside = isLeftRoom ? (doorXInside + 1) : (doorXInside - 1);

        for (int dy = lo; dy <= hi; dy++) {
            int ny = y + dy;
            if (maze.inBounds(doorXOutside, ny)) maze.setTile(doorXOutside, ny, Tiles.FLOOR);
            if (maze.inBounds(doorXInside, ny)) maze.setTile(doorXInside, ny, Tiles.FLOOR);
        }
    }

    private void carveDoorMouthUD(Maze maze, Room room, int x, int hallWidth, boolean isTopRoom) {
        int lo = -(hallWidth / 2);
        int hi = (hallWidth - 1) / 2;

        int doorYInside = isTopRoom ? (room.y() + room.h() - 1) : room.y();
        int doorYOutside = isTopRoom ? (doorYInside + 1) : (doorYInside - 1);

        for (int dx = lo; dx <= hi; dx++) {
            int nx = x + dx;
            if (maze.inBounds(nx, doorYOutside)) maze.setTile(nx, doorYOutside, Tiles.FLOOR);
            if (maze.inBounds(nx, doorYInside)) maze.setTile(nx, doorYInside, Tiles.FLOOR);
        }
    }

    private void placeDoorBandLRAligned(Maze maze, Room room, int y, int hallWidth, boolean isLeftRoom) {
        int lo = -(hallWidth / 2);
        int hi = (hallWidth - 1) / 2;

        int doorX = isLeftRoom ? (room.x() + room.w()) : (room.x() - 1);

    }

    private void placeDoorBandUDAligned(Maze maze, Room room, int x, int hallWidth, boolean isTopRoom) {
        int lo = -(hallWidth / 2);
        int hi = (hallWidth - 1) / 2;

        int doorY = isTopRoom ? (room.y() + room.h()) : (room.y() - 1);

    }




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

    private enum BorderSide {LEFT, RIGHT, TOP, BOTTOM}

    private static class BorderEntryPlan {
        final BorderSide side;
        final int coord;
        BorderEntryPlan(BorderSide side, int coord) { this.side = side; this.coord = coord; }
    }

    private BorderEntryPlan pickBorderEntryPlan(Room spawnRoom, Maze maze, Random rng, int hallWidth, List<Room> rooms) {
        int pad = hallPad(hallWidth);
        int W = maze.getWidth();
        int H = maze.getHeight();

        int dLeft = spawnRoom.x();
        int dRight = (W - 1) - (spawnRoom.x() + spawnRoom.w() - 1);
        int dTop = spawnRoom.y();
        int dBottom = (H - 1) - (spawnRoom.y() + spawnRoom.h() - 1);

        int wLeft = 1 + Math.max(0, 80 - dLeft);
        int wRight = 1 + Math.max(0, 80 - dRight);
        int wTop = 1 + Math.max(0, 80 - dTop);
        int wBottom = 1 + Math.max(0, 80 - dBottom);

        for (int attempt = 0; attempt < 120; attempt++) {
            BorderSide side = weightedPick(rng,
                    new BorderSide[]{BorderSide.LEFT, BorderSide.RIGHT, BorderSide.TOP, BorderSide.BOTTOM},
                    new int[]{wLeft, wRight, wTop, wBottom});

            BorderEntryPlan be;

            if (side == BorderSide.LEFT || side == BorderSide.RIGHT) {
                int minY = spawnRoom.y() + 1 + pad;
                int maxY = spawnRoom.y() + spawnRoom.h() - 2 - pad;

                if (maxY < minY) {
                    minY = spawnRoom.y() + pad;
                    maxY = spawnRoom.y() + spawnRoom.h() - 1 - pad;
                }

                minY = clamp(minY, 1 + pad, H - 2 - pad);
                maxY = clamp(maxY, 1 + pad, H - 2 - pad);
                if (maxY < minY) continue;

                int y = minY + rng.nextInt(maxY - minY + 1);
                be = new BorderEntryPlan(side, y);
            } else {
                int minX = spawnRoom.x() + 1 + pad;
                int maxX = spawnRoom.x() + spawnRoom.w() - 2 - pad;

                if (maxX < minX) {
                    minX = spawnRoom.x() + pad;
                    maxX = spawnRoom.x() + spawnRoom.w() - 1 - pad;
                }

                minX = clamp(minX, 1 + pad, W - 2 - pad);
                maxX = clamp(maxX, 1 + pad, W - 2 - pad);
                if (maxX < minX) continue;

                int x = minX + rng.nextInt(maxX - minX + 1);
                be = new BorderEntryPlan(side, x);
            }

            // reuse the same intersection test you already use for exit
            BorderExitPlan asExit = new BorderExitPlan(be.side, be.coord);
            if (!corridorIntersectsOtherRoom(asExit, spawnRoom, rooms, maze, hallWidth)) {
                return be;
            }
        }

        return null;
    }

    private Point carveStraightToBorderAndPlaceSpawn(Maze maze, Room spawnRoom, BorderEntryPlan be, int hallWidth) {
        int W = maze.getWidth();
        int H = maze.getHeight();

        int lo = -(hallWidth / 2);
        int hi = (hallWidth - 1) / 2;

        if (be.side == BorderSide.LEFT) {
            int y = clamp(be.coord, 1 + hallPad(hallWidth), H - 2 - hallPad(hallWidth));
            for (int x = spawnRoom.x(); x >= 0; x--) carveVerticalBandExact(maze, x, y, hallWidth);
            for (int dy = lo; dy <= hi; dy++) maze.setTile(0, y + dy, Tiles.SPAWN);
            return new Point(0, y);
        }

        if (be.side == BorderSide.RIGHT) {
            int y = clamp(be.coord, 1 + hallPad(hallWidth), H - 2 - hallPad(hallWidth));
            for (int x = spawnRoom.x() + spawnRoom.w() - 1; x <= W - 1; x++) carveVerticalBandExact(maze, x, y, hallWidth);
            for (int dy = lo; dy <= hi; dy++) maze.setTile(W - 1, y + dy, Tiles.SPAWN);
            return new Point(W - 1, y);
        }

        if (be.side == BorderSide.TOP) {
            int x = clamp(be.coord, 1 + hallPad(hallWidth), W - 2 - hallPad(hallWidth));
            for (int y = spawnRoom.y(); y >= 0; y--) carveHorizontalBandExact(maze, x, y, hallWidth);
            for (int dx = lo; dx <= hi; dx++) maze.setTile(x + dx, 0, Tiles.SPAWN);
            return new Point(x, 0);
        }

        // BOTTOM
        int x = clamp(be.coord, 1 + hallPad(hallWidth), W - 2 - hallPad(hallWidth));
        for (int y = spawnRoom.y() + spawnRoom.h() - 1; y <= H - 1; y++) carveHorizontalBandExact(maze, x, y, hallWidth);
        for (int dx = lo; dx <= hi; dx++) maze.setTile(x + dx, H - 1, Tiles.SPAWN);
        return new Point(x, H - 1);
    }


    private static class BorderExitPlan {
        final BorderSide side;
        final int coord;

        BorderExitPlan(BorderSide side, int coord) {
            this.side = side;
            this.coord = coord;
        }
    }

    private PlaceResult assignSpecialRoomsAndPlaceTiles_BORDER_EXIT(Maze maze, List<Room> rooms, Random rng, int hallWidth, int level) {for (Room r : rooms) {
        if (r.getType() != RoomType.BOSS) {
            r.setType(RoomType.NORMAL);
        }
    }

        Room spawnRoom = rooms.get(rng.nextInt(rooms.size()));
        Room exitRoom = farthestRoom(rooms, spawnRoom);
        if (exitRoom == spawnRoom) return new PlaceResult(false, null, null);

        Room keyRoom = pickKeyRoom(rooms, spawnRoom, exitRoom, rng);
        if (keyRoom == null) return new PlaceResult(false, null, null);

        spawnRoom.setType(RoomType.SPAWN);
        exitRoom.setType(RoomType.EXIT);
        keyRoom.setType(RoomType.KEY);

        boolean isFinalLevel = (clampLevel(level) == 5);
        if (isFinalLevel) {
            boolean hasBossAlready = false;
            for (Room r : rooms) {
                if (r.getType() == RoomType.BOSS) {
                    hasBossAlready = true;
                    break;
                }
            }

            if (!hasBossAlready) {
                Room bossRoom = pickBossRoomNearExitFarFromSpawn(rooms, spawnRoom, exitRoom, rng);
                if (bossRoom != null) bossRoom.setType(RoomType.BOSS);
            }
        }

        BorderEntryPlan entryPlan = pickBorderEntryPlan(spawnRoom, maze, rng, hallWidth, rooms);
        if (entryPlan == null) return new PlaceResult(false, null, null);

        Point spawn = carveStraightToBorderAndPlaceSpawn(maze, spawnRoom, entryPlan, hallWidth);
        if (spawn == null) return new PlaceResult(false, null, null);


        Point key = keyRoom.randomInterior(rng);
        if (maze.getTile(key.x, key.y) != Tiles.FLOOR) return new PlaceResult(false, null, null);

        maze.setTile(spawn.x, spawn.y, Tiles.SPAWN);
        maze.setTile(key.x, key.y, Tiles.FLOOR);

        BorderExitPlan be = pickBorderExitPlan(exitRoom, maze, rng, hallWidth, rooms);
        if (be == null) return new PlaceResult(false, null, null);

        Point exitPoint = carveStraightToBorderAndPlaceExit(maze, exitRoom, be, hallWidth);
        if (exitPoint == null) return new PlaceResult(false, null, null);

        return new PlaceResult(true, spawn, exitPoint);
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
            if (r == spawn || r == exit) continue;
            candidates.add(r);
        }
        if (candidates.isEmpty()) return null;

        candidates.sort((a, b) -> {
            int sa = Math.min(dist2(a, spawn), dist2(a, exit));
            int sb = Math.min(dist2(b, spawn), dist2(b, exit));
            return Integer.compare(sb, sa); // descending
        });

        // pick from top slice for variety
        int top = Math.min(Math.max(1, candidates.size() / 4), candidates.size());
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

    private BorderExitPlan pickBorderExitPlan(Room exitRoom, Maze maze, Random rng, int hallWidth, List<Room> rooms) {
        int pad = hallPad(hallWidth);
        int W = maze.getWidth();
        int H = maze.getHeight();

        int dLeft = exitRoom.x();
        int dRight = (W - 1) - (exitRoom.x() + exitRoom.w() - 1);
        int dTop = exitRoom.y();
        int dBottom = (H - 1) - (exitRoom.y() + exitRoom.h() - 1);

        int wLeft = 1 + Math.max(0, 80 - dLeft);
        int wRight = 1 + Math.max(0, 80 - dRight);
        int wTop = 1 + Math.max(0, 80 - dTop);
        int wBottom = 1 + Math.max(0, 80 - dBottom);

        for (int attempt = 0; attempt < 120; attempt++) {
            BorderSide side = weightedPick(rng,
                    new BorderSide[]{BorderSide.LEFT, BorderSide.RIGHT, BorderSide.TOP, BorderSide.BOTTOM},
                    new int[]{wLeft, wRight, wTop, wBottom});

            BorderExitPlan be;

            if (side == BorderSide.LEFT || side == BorderSide.RIGHT) {
                int minY = exitRoom.y() + 1 + pad;
                int maxY = exitRoom.y() + exitRoom.h() - 2 - pad;

                if (maxY < minY) {
                    minY = exitRoom.y() + pad;
                    maxY = exitRoom.y() + exitRoom.h() - 1 - pad;
                }

                minY = clamp(minY, 1 + pad, H - 2 - pad);
                maxY = clamp(maxY, 1 + pad, H - 2 - pad);
                if (maxY < minY) continue;

                int y = minY + rng.nextInt(maxY - minY + 1);
                be = new BorderExitPlan(side, y);
            } else {
                int minX = exitRoom.x() + 1 + pad;
                int maxX = exitRoom.x() + exitRoom.w() - 2 - pad;

                if (maxX < minX) {
                    minX = exitRoom.x() + pad;
                    maxX = exitRoom.x() + exitRoom.w() - 1 - pad;
                }

                minX = clamp(minX, 1 + pad, W - 2 - pad);
                maxX = clamp(maxX, 1 + pad, W - 2 - pad);
                if (maxX < minX) continue;

                int x = minX + rng.nextInt(maxX - minX + 1);
                be = new BorderExitPlan(side, x);
            }

            if (!corridorIntersectsOtherRoom(be, exitRoom, rooms, maze, hallWidth)) {
                return be;
            }
        }

        return null;
    }

    private BorderSide weightedPick(Random rng, BorderSide[] sides, int[] weights) {
        int total = 0;
        for (int w : weights) total += Math.max(0, w);
        int roll = rng.nextInt(Math.max(1, total));
        int acc = 0;
        for (int i = 0; i < sides.length; i++) {
            acc += Math.max(0, weights[i]);
            if (roll < acc) return sides[i];
        }
        return sides[0];
    }

    private boolean rectsOverlap(int ax0, int ay0, int ax1, int ay1,
                                 int bx0, int by0, int bx1, int by1) {
        return ax0 < bx1 && ax1 > bx0 && ay0 < by1 && ay1 > by0;
    }

    private boolean corridorIntersectsOtherRoom(BorderExitPlan be, Room exitRoom, List<Room> rooms, Maze maze, int hallWidth) {
        int W = maze.getWidth();
        int H = maze.getHeight();

        int lo = -(hallWidth / 2);
        int hi = (hallWidth - 1) / 2;

        int cx0, cy0, cx1, cy1;

        if (be.side == BorderSide.LEFT) {
            int y = be.coord;
            cx0 = 0;
            cx1 = exitRoom.x() + 1;
            cy0 = y + lo;
            cy1 = y + hi + 1;
        } else if (be.side == BorderSide.RIGHT) {
            int y = be.coord;
            cx0 = exitRoom.x() + exitRoom.w() - 1;
            cx1 = W;
            cy0 = y + lo;
            cy1 = y + hi + 1;
        } else if (be.side == BorderSide.TOP) {
            int x = be.coord;
            cx0 = x + lo;
            cx1 = x + hi + 1;
            cy0 = 0;
            cy1 = exitRoom.y() + 1;
        } else { // BOTTOM
            int x = be.coord;
            cx0 = x + lo;
            cx1 = x + hi + 1;
            cy0 = exitRoom.y() + exitRoom.h() - 1;
            cy1 = H;
        }

        cx0 = clamp(cx0, 0, W);
        cx1 = clamp(cx1, 0, W);
        cy0 = clamp(cy0, 0, H);
        cy1 = clamp(cy1, 0, H);

        if (cx1 <= cx0 || cy1 <= cy0) return true;

        for (Room r : rooms) {
            if (r == exitRoom) continue;

            int rx0 = r.x();
            int ry0 = r.y();
            int rx1 = r.x() + r.w();
            int ry1 = r.y() + r.h();

            if (rectsOverlap(cx0, cy0, cx1, cy1, rx0, ry0, rx1, ry1)) {
                return true;
            }
        }

        return false;
    }

    private Point carveStraightToBorderAndPlaceExit(Maze maze, Room exitRoom, BorderExitPlan be, int hallWidth) {
        int W = maze.getWidth();
        int H = maze.getHeight();

        int lo = -(hallWidth / 2);
        int hi = (hallWidth - 1) / 2;

        if (be.side == BorderSide.LEFT) {
            int y = clamp(be.coord, 1 + hallPad(hallWidth), H - 2 - hallPad(hallWidth));
            for (int x = exitRoom.x(); x >= 0; x--) carveVerticalBandExact(maze, x, y, hallWidth);
            for (int dy = lo; dy <= hi; dy++) maze.setTile(0, y + dy, Tiles.EXIT);
            return new Point(0, y);
        }

        if (be.side == BorderSide.RIGHT) {
            int y = clamp(be.coord, 1 + hallPad(hallWidth), H - 2 - hallPad(hallWidth));
            for (int x = exitRoom.x() + exitRoom.w() - 1; x <= W - 1; x++) carveVerticalBandExact(maze, x, y, hallWidth);
            for (int dy = lo; dy <= hi; dy++) maze.setTile(W - 1, y + dy, Tiles.EXIT);
            return new Point(W - 1, y);
        }

        if (be.side == BorderSide.TOP) {
            int x = clamp(be.coord, 1 + hallPad(hallWidth), W - 2 - hallPad(hallWidth));
            for (int y = exitRoom.y(); y >= 0; y--) carveHorizontalBandExact(maze, x, y, hallWidth);
            for (int dx = lo; dx <= hi; dx++) maze.setTile(x + dx, 0, Tiles.EXIT);
            return new Point(x, 0);
        }

        // BOTTOM
        int x = clamp(be.coord, 1 + hallPad(hallWidth), W - 2 - hallPad(hallWidth));
        for (int y = exitRoom.y() + exitRoom.h() - 1; y <= H - 1; y++) carveHorizontalBandExact(maze, x, y, hallWidth);
        for (int dx = lo; dx <= hi; dx++) maze.setTile(x + dx, H - 1, Tiles.EXIT);
        return new Point(x, H - 1);
    }

    private static class PlaceResult {
        final boolean ok;
        final Point spawn;
        final Point exit;

        PlaceResult(boolean ok, Point spawn, Point exit) {
            this.ok = ok; this.spawn = spawn; this.exit = exit;
        }
    }

    private Room biggestRoom(List<Room> rooms) {
        Room best = null;
        int bestA = -1;
        for (Room r : rooms) {
            int a = r.w() * r.h();
            if (a > bestA) { bestA = a; best = r; }
        }
        return best;
    }


    private MazeLoader.LevelSpec applyLevelSize(MazeLoader.LevelSpec spec, int level) {
        // keep your original sizes for 1..5
        if (level <= 5) {
            int lv = clamp(level, 1, 5);
            return new MazeLoader.LevelSpec(LEVEL_W[lv], LEVEL_H[lv], spec.overrides);
        }

        // Endless scaling after 5:
        int extra = (level - 5) * 8;
        int targetW = 100 + extra;
        int targetH = 100 + extra;

        targetW = clamp(targetW, 80, 220);
        targetH = clamp(targetH, 80, 220);

        return new MazeLoader.LevelSpec(targetW, targetH, spec.overrides);
    }


    private void fill(Maze maze, Tiles tile) {
        for (int y = 0; y < maze.getHeight(); y++) {
            for (int x = 0; x < maze.getWidth(); x++) {
                maze.setTile(x, y, tile);
            }
        }
    }

    private Leaf pickBossLeaf(List<Leaf> leaves) {
        Leaf best = null;
        int bestArea = -1;

        for (Leaf l : leaves) {
            int innerW = l.w - 2 * BOSS_ROOM_MARGIN;
            int innerH = l.h - 2 * BOSS_ROOM_MARGIN;
            if (innerW < BOSS_MIN_SIZE || innerH < BOSS_MIN_SIZE) continue;

            int area = innerW * innerH;
            if (area > bestArea) {
                bestArea = area;
                best = l;
            }
        }
        return best;
    }

    private boolean isInsideRoomTile(Room r, int tx, int ty) {
        return tx >= r.x() && tx < r.x() + r.w()
                && ty >= r.y() && ty < r.y() + r.h();
    }

    private Room makeBossRoom(Leaf l, Random rng) {
        int innerX = l.x + BOSS_ROOM_MARGIN;
        int innerY = l.y + BOSS_ROOM_MARGIN;
        int innerW = l.w - 2 * BOSS_ROOM_MARGIN;
        int innerH = l.h - 2 * BOSS_ROOM_MARGIN;

        if (innerW < BOSS_MIN_SIZE || innerH < BOSS_MIN_SIZE) return null;

        float s = BOSS_SHRINK_MIN + rng.nextFloat() * (BOSS_SHRINK_MAX - BOSS_SHRINK_MIN);

        int rw = Math.max(BOSS_MIN_SIZE, Math.round(innerW * s));
        int rh = Math.max(BOSS_MIN_SIZE, Math.round(innerH * s));

        int maxOx = Math.max(0, innerW - rw);
        int maxOy = Math.max(0, innerH - rh);

        int rx = innerX + (maxOx == 0 ? 0 : rng.nextInt(maxOx + 1));
        int ry = innerY + (maxOy == 0 ? 0 : rng.nextInt(maxOy + 1));

        return new Room(rx, ry, rw, rh);
    }


    private int clamp(int v, int lo, int hi) {
        return Math.max(lo, Math.min(hi, v));
    }

    private static int clampLevel(int level) {
        return Math.max(1, Math.min(5, level));
    }

    private static int rndBetween(Random rng, int a, int b) {
        if (b < a) return a;
        return a + rng.nextInt(b - a + 1);
    }

    /**
     * Generates a maze and additionally returns the list of generated rooms.
     *
     * <p>This method behaves like {@link #generate(MazeLoader.LevelSpec, int, long, int)}
     * but exposes the internal room layout for gameplay systems such as enemy
     * spawning, boss placement, or debugging.</p>
     *
     * @param spec level specification containing base dimensions and overrides
     * @param tileSize size of a single tile in world units
     * @param seed random seed used for deterministic generation
     * @param level current game level (affects scaling and boss placement)
     * @return a {@link GeneratedMaze} containing the maze and its rooms
     */
    public GeneratedMaze generateWithRooms(MazeLoader.LevelSpec spec, int tileSize, long seed, int level) {
        Objects.requireNonNull(spec, "LevelSpec must not be null");

        // We reuse the existing generate logic, but intercept the rooms before returning.
        GenerationCapture capture = new GenerationCapture();

        Maze maze = generateInternal(spec, tileSize, seed, level, capture);

        return new GeneratedMaze(maze, capture.rooms);
    }

    private static final class GenerationCapture {
        List<Room> rooms;
    }

    private Maze generateInternal(MazeLoader.LevelSpec spec, int tileSize, long seed, int level, GenerationCapture capture) {
        Objects.requireNonNull(spec, "LevelSpec must not be null");
        spec = applyLevelSize(spec, level);

        Random rng = new Random(seed);

        if (spec.width < MIN_LEAF * 2 + 4 || spec.height < MIN_LEAF * 2 + 4) {
            throw new IllegalArgumentException(
                    "Map too small for BSP. width/height should be at least ~" + (MIN_LEAF * 2 + 4));
        }

        for (int attempt = 0; attempt < MAX_ATTEMPTS; attempt++) {
            Maze maze = new Maze(spec.width, spec.height, tileSize);
            fill(maze, Tiles.WALL);

            Leaf root = new Leaf(1, 1, spec.width - 2, spec.height - 2);
            List<Leaf> leaves = splitLeaves(root, rng, level, spec.width, spec.height);

            discoverNeighbors(leaves);

            List<Room> rooms = new ArrayList<>();
            for (Leaf l : leaves) {
                Room r = makeShrunkRoom(l, rng);
                if (r == null) continue;
                l.room = r;
                rooms.add(r);
            }
            if (rooms.size() < 3) continue;

            int lv = clampLevel(level);
            int minRooms = LEVEL_ROOMS[lv][0];
            int maxRooms = LEVEL_ROOMS[lv][1];

            if (rooms.size() < minRooms) continue;

            if (rooms.size() > maxRooms) {
                Collections.shuffle(rooms, rng);
                rooms = new ArrayList<>(rooms.subList(0, maxRooms));

                Set<Room> keep = new HashSet<>(rooms);
                for (Leaf l : leaves) {
                    if (l.room != null && !keep.contains(l.room)) l.room = null;
                }
            }

            boolean isFinalLevel = (clampLevel(level) == 5);
            if (isFinalLevel) {
                Leaf bossLeaf = pickBossLeaf(leaves);
                if (bossLeaf == null) continue;

                Room old = bossLeaf.room;
                Room bossRoom = makeBossRoom(bossLeaf, rng);
                if (bossRoom == null) continue;

                bossLeaf.room = bossRoom;
                bossRoom.setType(RoomType.BOSS);

                if (old != null) rooms.remove(old);
                rooms.add(bossRoom);
            }

            List<CandidateEdge> candidates = buildHallCandidates(leaves);
            if (candidates.isEmpty()) continue;

            List<HallPlan> hallPlans = buildMSTPlans(rooms, candidates, rng);
            if (hallPlans.size() < rooms.size() - 1) continue;

            carveRooms(maze, rooms);
            carveStraightHallsWide(maze, hallPlans, HALL_WIDTH);
            cleanupThinArtifacts(maze, rooms);
            clearSpecialTiles(maze);

            maze.setRooms(rooms);

            PlaceResult placed =
                    assignSpecialRoomsAndPlaceTiles_BORDER_EXIT(maze, rooms, rng, HALL_WIDTH, level);

            if (!placed.ok) continue;

            if (MazeUtils.reachable(maze, placed.spawn, placed.exit)) {
                capture.rooms = rooms;
                return maze;
            }
        }

        throw new RuntimeException("Failed to generate BSP dungeon after " + MAX_ATTEMPTS + " attempts");
    }

    private Room pickBossRoomNearExitFarFromSpawn(List<Room> rooms, Room spawn, Room exit, Random rng) {
        // Prefer decent-sized rooms so boss has space
        final int minW = 14;
        final int minH = 14;

        Room best = null;
        float bestScore = -Float.MAX_VALUE;

        for (Room r : rooms) {
            if (r == spawn || r == exit) continue;
            if (r.w() < minW || r.h() < minH) continue;

            int dSpawn = dist2(r, spawn);
            int dExit  = dist2(r, exit);

            // Bigger is slightly better; far from spawn and close to exit is the main objective
            float sizeBonus = (r.w() * r.h()) * 0.01f;
            float score = (float)dSpawn - 0.85f * (float)dExit + sizeBonus;

            if (score > bestScore) {
                bestScore = score;
                best = r;
            }
        }

        // fallback: just biggest non-spawn/non-exit
        if (best == null) {
            best = biggestRoom(rooms);
            if (best == spawn || best == exit) {
                // find next biggest
                Room second = null;
                int bestA = -1;
                for (Room r : rooms) {
                    if (r == spawn || r == exit) continue;
                    int a = r.w() * r.h();
                    if (a > bestA) { bestA = a; second = r; }
                }
                best = second;
            }
        }

        return best;
    }

}
