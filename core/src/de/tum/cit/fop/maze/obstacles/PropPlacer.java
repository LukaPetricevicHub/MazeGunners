package de.tum.cit.fop.maze.obstacles;

import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Vector2;
import de.tum.cit.fop.maze.map.Maze;
import de.tum.cit.fop.maze.map.Tiles;
import de.tum.cit.fop.maze.util.GridUtils;

import java.awt.Point;
import java.util.*;

public class PropPlacer {

    public List<Obstacle> placeProps(
            Maze maze,
            int count,
            long seed,
            int level,
            Map<PropType, TextureRegion> propSprites
    ) {
        Random rng = new Random(seed * 31L + level * 997L);

        List<Point> candidates = buildCandidates(maze, true);

        // relax if needed
        if (candidates.isEmpty()) {
            candidates = buildCandidates(maze, false);
        }

        Collections.shuffle(candidates, rng);

        List<Obstacle> out = new ArrayList<>();
        Set<Long> occupied = new HashSet<>();

        int placed = 0;
        int tries = 0;
        int maxTries = Math.max(300, count * 300);

        while (placed < count && tries++ < maxTries && !candidates.isEmpty()) {

            Point anchor = candidates.get(rng.nextInt(candidates.size()));

            boolean placedHere = false;

            // Try up to N random types, then fall back to small ones
            for (int attempt = 0; attempt < 10; attempt++) {
                PropType type = randomWeightedType(rng);
                TextureRegion sprite = propSprites.get(type);
                if (sprite == null) continue;

                int wTiles = type.tilesW;
                int hTiles = type.tilesH;

                if (!footprintSafe(maze, anchor.x, anchor.y, wTiles, hTiles)) continue;
                if (!footprintNotOccupied(anchor.x, anchor.y, wTiles, hTiles, occupied, maze)) continue;

                reserve(anchor.x, anchor.y, wTiles, hTiles, occupied);

                float wx = anchor.x * maze.getTileSize();
                float wy = anchor.y * maze.getTileSize();

                out.add(new Prop(new Vector2(wx, wy), type, sprite, maze.getTileSize()));
                placed++;
                placedHere = true;
                break;
            }

            // Fallback: try small props explicitly so we still place something
            if (!placedHere) {
                placedHere = tryPlaceSpecific(maze, anchor, PropType.BARREL, propSprites, occupied, out)
                        || tryPlaceSpecific(maze, anchor, PropType.CRATE, propSprites, occupied, out)
                        || tryPlaceSpecific(maze, anchor, PropType.SKELETON, propSprites, occupied, out)
                        || tryPlaceSpecific(maze, anchor, PropType.ROBOT, propSprites, occupied, out)
                        || tryPlaceSpecific(maze, anchor, PropType.FLESH, propSprites, occupied, out)
                        || tryPlaceSpecific(maze, anchor, PropType.FLASKS, propSprites, occupied, out)
                        || tryPlaceSpecific(maze, anchor, PropType.SKELETON, propSprites, occupied, out);

                if (placedHere) placed++;
            }

            // Optional: occasionally drop random candidates to reduce repeated failures
            if (!placedHere && candidates.size() > 80 && tries % 50 == 0) {
                candidates.remove(rng.nextInt(candidates.size()));
            }
        }

        return out;
    }

    // ---------------------------------------------------------
    // Candidate building
    // ---------------------------------------------------------

    private List<Point> buildCandidates(Maze maze, boolean strict) {
        List<Point> candidates = new ArrayList<>();

        for (int y = 1; y < maze.getHeight() - 1; y++) {
            for (int x = 1; x < maze.getWidth() - 1; x++) {

                if (maze.getTile(x, y) != Tiles.FLOOR) continue;

                int deg = walkable4(maze, x, y);

                // strict: avoid anything corridor-like
                if (strict) {
                    if (deg <= 2) continue;
                    if (!isOpenArea8(maze, x, y, 6)) continue;
                } else {
                    // relaxed: still avoid dead ends, allow more room-ish areas
                    if (deg <= 1) continue;
                    if (!isOpenArea8(maze, x, y, 4)) continue;
                }

                // avoid important tiles nearby
                if (nearTile(maze, x, y, Tiles.DOOR, 3)) continue;
                if (nearTile(maze, x, y, Tiles.SPAWN, 5)) continue;
                if (nearTile(maze, x, y, Tiles.EXIT, 5)) continue;
                if (nearTile(maze, x, y, Tiles.KEY, 5)) continue;

                candidates.add(new Point(x, y));
            }
        }

        return candidates;
    }

    // ---------------------------------------------------------
    // Placement checks
    // ---------------------------------------------------------

    private boolean footprintSafe(Maze maze, int ax, int ay, int wTiles, int hTiles) {
        for (int dy = 0; dy < hTiles; dy++) {
            for (int dx = 0; dx < wTiles; dx++) {
                int x = ax + dx;
                int y = ay + dy;

                if (!maze.inBounds(x, y)) return false;
                if (maze.getTile(x, y) != Tiles.FLOOR) return false;

                // don't allow props sitting on corridor-like cells
                if (walkable4(maze, x, y) <= 2) return false;
            }
        }
        return true;
    }

    private boolean footprintNotOccupied(int ax, int ay, int wTiles, int hTiles, Set<Long> occupied, Maze maze) {
        for (int dy = 0; dy < hTiles; dy++) {
            for (int dx = 0; dx < wTiles; dx++) {
                int x = ax + dx;
                int y = ay + dy;
                if (!maze.inBounds(x, y)) return false;
                if (occupied.contains(pack(x, y))) return false;
            }
        }
        return true;
    }

    private void reserve(int ax, int ay, int wTiles, int hTiles, Set<Long> occupied) {
        for (int dy = 0; dy < hTiles; dy++) {
            for (int dx = 0; dx < wTiles; dx++) {
                occupied.add(pack(ax + dx, ay + dy));
            }
        }
    }

    private boolean tryPlaceSpecific(
            Maze maze,
            Point anchor,
            PropType type,
            Map<PropType, TextureRegion> propSprites,
            Set<Long> occupied,
            List<Obstacle> out
    ) {
        TextureRegion sprite = propSprites.get(type);
        if (sprite == null) return false;

        int wTiles = type.tilesW;
        int hTiles = type.tilesH;

        if (!footprintSafe(maze, anchor.x, anchor.y, wTiles, hTiles)) return false;
        if (!footprintNotOccupied(anchor.x, anchor.y, wTiles, hTiles, occupied, maze)) return false;

        reserve(anchor.x, anchor.y, wTiles, hTiles, occupied);

        float wx = anchor.x * maze.getTileSize();
        float wy = anchor.y * maze.getTileSize();

        out.add(new Prop(new Vector2(wx, wy), type, sprite, maze.getTileSize()));
        return true;
    }

    // ---------------------------------------------------------
    // Geometry helpers
    // ---------------------------------------------------------

    private int walkable4(Maze maze, int x, int y) {
        int n = 0;
        if (maze.getTile(x + 1, y).isWalkable()) n++;
        if (maze.getTile(x - 1, y).isWalkable()) n++;
        if (maze.getTile(x, y + 1).isWalkable()) n++;
        if (maze.getTile(x, y - 1).isWalkable()) n++;
        return n;
    }

    private boolean isOpenArea8(Maze maze, int x, int y, int minWalkable) {
        int walkable = 0;
        for (int dy = -1; dy <= 1; dy++) {
            for (int dx = -1; dx <= 1; dx++) {
                if (dx == 0 && dy == 0) continue;
                int nx = x + dx;
                int ny = y + dy;
                if (!GridUtils.inBounds(nx, ny, maze.getWidth(), maze.getHeight())) continue;
                if (maze.getTile(nx, ny).isWalkable()) walkable++;
            }
        }
        return walkable >= minWalkable;
    }

    private boolean nearTile(Maze maze, int x, int y, Tiles t, int r) {
        for (int dy = -r; dy <= r; dy++) {
            for (int dx = -r; dx <= r; dx++) {
                int nx = x + dx;
                int ny = y + dy;
                if (!maze.inBounds(nx, ny)) continue;
                if (maze.getTile(nx, ny) == t) return true;
            }
        }
        return false;
    }

    private long pack(int x, int y) {
        return (((long) x) << 32) ^ (y & 0xffffffffL);
    }

    // ---------------------------------------------------------
    // Weighted random type
    // ---------------------------------------------------------

    private PropType randomWeightedType(Random rng) {
        PropType[] vals = PropType.values();
        int total = 0;
        for (PropType t : vals) total += t.weight;

        int roll = rng.nextInt(total);
        int acc = 0;
        for (PropType t : vals) {
            acc += t.weight;
            if (roll < acc) return t;
        }
        return vals[0];
    }
}
