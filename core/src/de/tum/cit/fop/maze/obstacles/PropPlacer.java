package de.tum.cit.fop.maze.obstacles;

import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Vector2;
import de.tum.cit.fop.maze.map.Maze;
import de.tum.cit.fop.maze.map.Tiles;

import java.awt.Point;
import java.util.*;

public class PropPlacer {

    public List<Obstacle> placeProps(Maze maze,
                                     int count,
                                     long seed,
                                     int level,
                                     Map<PropType, TextureRegion> propSprites) {

        Random rng = new Random(seed * 31L + level * 997L);

        List<Point> candidates = new ArrayList<>();
        for (int y = 1; y < maze.getHeight() - 1; y++) {
            for (int x = 1; x < maze.getWidth() - 1; x++) {
                if (maze.getTile(x, y) != Tiles.FLOOR) continue;

                // avoid corridor-ish tiles (narrow)
                if (!isRoomLike(maze, x, y)) continue;

                // avoid doors/special tiles
                if (nearTile(maze, x, y, Tiles.DOOR, 2)) continue;
                if (nearTile(maze, x, y, Tiles.SPAWN, 4)) continue;
                if (nearTile(maze, x, y, Tiles.EXIT, 4)) continue;
                if (nearTile(maze, x, y, Tiles.KEY, 4)) continue;

                if (!hasRoomSpaceForFootprint(maze, new Point(x, y), PropType.TABLE)) {
                    // This line is optional; see note below.
                }

                candidates.add(new Point(x, y));
            }
        }

        Collections.shuffle(candidates, rng);

        List<Obstacle> out = new ArrayList<>();
        Set<Point> occupied = new HashSet<>();

        int placed = 0;
        int tries = 0;
        int maxTries = count * 25;

        while (placed < count && tries++ < maxTries && !candidates.isEmpty()) {
            Point cell = candidates.get(rng.nextInt(candidates.size()));
            PropType type = randomType(rng);

            if (occupied.contains(cell)) continue;

            if (!hasRoomSpaceForFootprint(maze, cell, type)) continue;

            if (!reserveFootprint(cell, maze, type, occupied)) continue;

            TextureRegion sprite = propSprites.get(type);
            if (sprite == null) {
                freeFootprint(cell, type, occupied);
                continue;
            }

            float wx = cell.x * maze.getTileSize();
            float wy = cell.y * maze.getTileSize();

            out.add(new Prop(new Vector2(wx, wy), type, sprite, maze.getTileSize()));
            placed++;
        }

        return out;
    }

    private PropType randomType(Random rng) {
        PropType[] v = PropType.values();
        return v[rng.nextInt(v.length)];
    }

    private int walkableNeighbors4(Maze maze, int x, int y) {
        int c = 0;
        if (maze.getTile(x + 1, y).isWalkable()) c++;
        if (maze.getTile(x - 1, y).isWalkable()) c++;
        if (maze.getTile(x, y + 1).isWalkable()) c++;
        if (maze.getTile(x, y - 1).isWalkable()) c++;
        return c;
    }

    private boolean nearTile(Maze maze, int x, int y, Tiles t, int r) {
        for (int dy = -r; dy <= r; dy++) {
            for (int dx = -r; dx <= r; dx++) {
                int nx = x + dx, ny = y + dy;
                if (!maze.inBounds(nx, ny)) continue;
                if (maze.getTile(nx, ny) == t) return true;
            }
        }
        return false;
    }

    private boolean reserveFootprint(Point cell, Maze maze, PropType type, Set<Point> occupied) {

        int wTiles = Math.max(1, type.tilesW);
        int hTiles = Math.max(1, type.tilesH);

        // validate footprint area
        for (int dy = 0; dy < hTiles; dy++) {
            for (int dx = 0; dx < wTiles; dx++) {
                int x = cell.x + dx;
                int y = cell.y + dy;

                if (!maze.inBounds(x, y)) return false;
                if (maze.getTile(x, y) != Tiles.FLOOR) return false;

                Point p = new Point(x, y);
                if (occupied.contains(p)) return false;
            }
        }

        // reserve
        for (int dy = 0; dy < hTiles; dy++) {
            for (int dx = 0; dx < wTiles; dx++) {
                occupied.add(new Point(cell.x + dx, cell.y + dy));
            }
        }
        return true;
    }

    private void freeFootprint(Point cell, PropType type, Set<Point> occupied) {

        int wTiles = Math.max(1, type.tilesW);
        int hTiles = Math.max(1, type.tilesH);

        for (int dy = 0; dy < hTiles; dy++) {
            for (int dx = 0; dx < wTiles; dx++) {
                occupied.remove(new Point(cell.x + dx, cell.y + dy));
            }
        }
    }

    private boolean isRoomLike(Maze maze, int x, int y) {
        // A room tile should be "open": typically 3-4 walkable neighbors
        return walkableNeighbors4(maze, x, y) >= 3;
    }

    private boolean hasRoomSpaceForFootprint(Maze maze, Point cell, PropType type) {
        // Every tile under the prop footprint should also be room-like
        int wTiles = Math.max(1, type.tilesW);
        int hTiles = Math.max(1, type.tilesH);

        for (int dy = 0; dy < hTiles; dy++) {
            for (int dx = 0; dx < wTiles; dx++) {
                int x = cell.x + dx;
                int y = cell.y + dy;

                if (!maze.inBounds(x, y)) return false;
                if (maze.getTile(x, y) != Tiles.FLOOR) return false;

                if (!isRoomLike(maze, x, y)) return false;
            }
        }
        return true;
    }

}
