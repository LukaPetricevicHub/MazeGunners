package de.tum.cit.fop.maze.obstacles;

import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Vector2;
import de.tum.cit.fop.maze.map.Maze;
import de.tum.cit.fop.maze.map.Tiles;

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

        List<Point> candidates = new ArrayList<>();

        // --------------------------------------------------
        // Collect ROOM-ONLY candidate tiles
        // --------------------------------------------------
        for (int y = 1; y < maze.getHeight() - 1; y++) {
            for (int x = 1; x < maze.getWidth() - 1; x++) {

                if (maze.getTile(x, y) != Tiles.FLOOR) continue;

                // MUST be deep inside a room
                if (!isRoomInterior(maze, x, y)) continue;

                // must have open area around it (avoids hall mouths)
                if (!openArea(maze, x, y, 1)) continue;

                // avoid special tiles
                if (nearTile(maze, x, y, Tiles.DOOR, 2)) continue;
                if (nearTile(maze, x, y, Tiles.SPAWN, 4)) continue;
                if (nearTile(maze, x, y, Tiles.EXIT, 4)) continue;
                if (nearTile(maze, x, y, Tiles.KEY, 4)) continue;

                candidates.add(new Point(x, y));
            }
        }

        Collections.shuffle(candidates, rng);

        List<Obstacle> out = new ArrayList<>();
        Set<Point> occupied = new HashSet<>();

        int placed = 0;
        int tries = 0;
        int maxTries = count * 30;

        // --------------------------------------------------
        // Placement loop
        // --------------------------------------------------
        while (placed < count && tries++ < maxTries && !candidates.isEmpty()) {

            Point cell = candidates.get(rng.nextInt(candidates.size()));
            PropType type = randomType(rng);

            if (occupied.contains(cell)) continue;

            // Larger props need more free room space
            int radius = (type.tilesW >= 2 || type.tilesH >= 2) ? 2 : 1;

            if (!openArea(maze, cell.x, cell.y, radius)) continue;
            if (!footprintIsRoomInterior(maze, cell, type)) continue;

            if (!reserveFootprint(cell, maze, type, occupied)) continue;

            TextureRegion sprite = propSprites.get(type);
            if (sprite == null) {
                freeFootprint(cell, type, occupied);
                continue;
            }

            float wx = cell.x * maze.getTileSize();
            float wy = cell.y * maze.getTileSize();

            out.add(new Prop(
                    new Vector2(wx, wy),
                    type,
                    sprite,
                    maze.getTileSize()
            ));

            placed++;
        }

        return out;
    }

    // ======================================================
    // Helpers
    // ======================================================

    private PropType randomType(Random rng) {
        PropType[] v = PropType.values();
        return v[rng.nextInt(v.length)];
    }

    private boolean isRoomInterior(Maze maze, int x, int y) {
        if (maze.getTile(x, y) != Tiles.FLOOR) return false;

        // corridors always fail this
        return maze.getTile(x + 1, y).isWalkable()
                && maze.getTile(x - 1, y).isWalkable()
                && maze.getTile(x, y + 1).isWalkable()
                && maze.getTile(x, y - 1).isWalkable();
    }

    private boolean openArea(Maze maze, int x, int y, int radius) {
        for (int dy = -radius; dy <= radius; dy++) {
            for (int dx = -radius; dx <= radius; dx++) {
                int nx = x + dx;
                int ny = y + dy;

                if (!maze.inBounds(nx, ny)) return false;
                if (maze.getTile(nx, ny) != Tiles.FLOOR) return false;
            }
        }
        return true;
    }

    private boolean footprintIsRoomInterior(Maze maze, Point cell, PropType type) {
        int w = Math.max(1, type.tilesW);
        int h = Math.max(1, type.tilesH);

        for (int dy = 0; dy < h; dy++) {
            for (int dx = 0; dx < w; dx++) {
                int x = cell.x + dx;
                int y = cell.y + dy;

                if (!maze.inBounds(x, y)) return false;
                if (!isRoomInterior(maze, x, y)) return false;
            }
        }
        return true;
    }

    private boolean reserveFootprint(Point cell, Maze maze, PropType type, Set<Point> occupied) {
        int w = Math.max(1, type.tilesW);
        int h = Math.max(1, type.tilesH);

        for (int dy = 0; dy < h; dy++) {
            for (int dx = 0; dx < w; dx++) {
                int x = cell.x + dx;
                int y = cell.y + dy;

                if (!maze.inBounds(x, y)) return false;
                if (maze.getTile(x, y) != Tiles.FLOOR) return false;

                if (occupied.contains(new Point(x, y))) return false;
            }
        }

        for (int dy = 0; dy < h; dy++) {
            for (int dx = 0; dx < w; dx++) {
                occupied.add(new Point(cell.x + dx, cell.y + dy));
            }
        }

        return true;
    }

    private void freeFootprint(Point cell, PropType type, Set<Point> occupied) {
        int w = Math.max(1, type.tilesW);
        int h = Math.max(1, type.tilesH);

        for (int dy = 0; dy < h; dy++) {
            for (int dx = 0; dx < w; dx++) {
                occupied.remove(new Point(cell.x + dx, cell.y + dy));
            }
        }
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
}
