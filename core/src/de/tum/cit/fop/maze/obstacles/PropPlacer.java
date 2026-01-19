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

        // 1) Candidate tiles: FLOOR tiles that are in open areas and not corridors/chokepoints
        List<Point> candidates = new ArrayList<>();
        for (int y = 1; y < maze.getHeight() - 1; y++) {
            for (int x = 1; x < maze.getWidth() - 1; x++) {

                // Only place on FLOOR (not on DOOR/EXIT/KEY/SPAWN)
                if (maze.getTile(x, y) != Tiles.FLOOR) continue;

                // Avoid corridors / chokepoints (prevents blocking the map)
                if (walkable4(maze, x, y) <= 2) continue;

                // Require "room-like" openness: lots of walkable neighbors
                if (!isOpenArea8(maze, x, y, 6)) continue;

                // Avoid important tiles nearby
                if (nearTile(maze, x, y, Tiles.DOOR, 3)) continue;
                if (nearTile(maze, x, y, Tiles.SPAWN, 5)) continue;
                if (nearTile(maze, x, y, Tiles.EXIT, 5)) continue;
                if (nearTile(maze, x, y, Tiles.KEY, 5)) continue;

                candidates.add(new Point(x, y));
            }
        }

        // If your generator makes small rooms, strict filtering may leave few candidates.
        // Fall back to slightly relaxed openness if needed.
        if (candidates.isEmpty()) {
            for (int y = 1; y < maze.getHeight() - 1; y++) {
                for (int x = 1; x < maze.getWidth() - 1; x++) {
                    if (maze.getTile(x, y) != Tiles.FLOOR) continue;
                    if (walkable4(maze, x, y) <= 1) continue;
                    if (!isOpenArea8(maze, x, y, 4)) continue;
                    if (nearTile(maze, x, y, Tiles.DOOR, 3)) continue;
                    if (nearTile(maze, x, y, Tiles.SPAWN, 5)) continue;
                    if (nearTile(maze, x, y, Tiles.EXIT, 5)) continue;
                    if (nearTile(maze, x, y, Tiles.KEY, 5)) continue;
                    candidates.add(new Point(x, y));
                }
            }
        }

        Collections.shuffle(candidates, rng);

        List<Obstacle> out = new ArrayList<>();
        Set<Long> occupied = new HashSet<>();

        // Sort prop types small-to-large so small props place first
        PropType[] types = PropType.values();
        Arrays.sort(types, Comparator.comparingInt(t -> t.tilesW * t.tilesH));

        int placed = 0;
        int tries = 0;
        int maxTries = Math.max(200, count * 200);

        while (placed < count && tries++ < maxTries && !candidates.isEmpty()) {

            Point anchor = candidates.get(rng.nextInt(candidates.size()));

            boolean placedHere = false;

            // Try a few random types (biased toward small)
            for (int attempt = 0; attempt < types.length; attempt++) {
                PropType type = types[attempt];
                TextureRegion sprite = propSprites.get(type);
                if (sprite == null) continue;

                int wTiles = type.tilesW;
                int hTiles = type.tilesH;

                // Ensure the entire footprint is safe
                if (!footprintWalkableAndNotCorridor(maze, anchor.x, anchor.y, wTiles, hTiles)) continue;

                // Ensure we don't overlap already-placed props
                if (!footprintNotOccupied(anchor.x, anchor.y, wTiles, hTiles, occupied, maze)) continue;

                // Reserve occupancy
                reserve(anchor.x, anchor.y, wTiles, hTiles, occupied);

                // Create prop (world coords)
                float wx = anchor.x * maze.getTileSize();
                float wy = anchor.y * maze.getTileSize();

                out.add(new Prop(new Vector2(wx, wy), type, sprite, maze.getTileSize()));
                placed++;
                placedHere = true;
                break;
            }

            // If this anchor never works, you can optionally remove it after repeated failures
            if (!placedHere && candidates.size() > 20 && tries % 25 == 0) {
                candidates.remove(rng.nextInt(candidates.size()));
            }
        }

        return out;
    }

    // ---------------------------------------------------------
    // Corridor / room detection helpers
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

    // ---------------------------------------------------------
    // Footprint checks: walkable and not choking corridors
    // ---------------------------------------------------------

    private boolean footprintWalkableAndNotCorridor(Maze maze, int ax, int ay, int wTiles, int hTiles) {
        for (int dy = 0; dy < hTiles; dy++) {
            for (int dx = 0; dx < wTiles; dx++) {
                int x = ax + dx;
                int y = ay + dy;

                if (!maze.inBounds(x, y)) return false;

                // Only place on FLOOR footprint (never on doors/spawn/etc.)
                if (maze.getTile(x, y) != Tiles.FLOOR) return false;

                // Prevent placing on corridor-like cells
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

    // ---------------------------------------------------------
    // Nearby special tiles
    // ---------------------------------------------------------

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
}
