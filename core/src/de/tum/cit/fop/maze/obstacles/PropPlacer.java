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
                if (walkableNeighbors4(maze, x, y) <= 2) continue;

                // avoid doors/special tiles
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
        int maxTries = count * 25;

        while (placed < count && tries++ < maxTries && !candidates.isEmpty()) {
            Point cell = candidates.get(rng.nextInt(candidates.size()));
            if (occupied.contains(cell)) continue;

            PropType type = randomType(rng);

            if (!reserveFootprint(cell, maze, type, occupied)) continue;

            TextureRegion sprite = propSprites.get(type);
            if (sprite == null) {
                freeFootprint(cell, maze, type, occupied);
                continue;
            }

            float wx = cell.x * maze.getTileSize();
            float wy = cell.y * maze.getTileSize();

            out.add(new Prop(new Vector2(wx, wy), type, sprite));
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
        int ts = maze.getTileSize();
        int wTiles = Math.max(1, (int) Math.ceil(type.wPx / (double) ts));
        int hTiles = Math.max(1, (int) Math.ceil(type.hPx / (double) ts));

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

        for (int dy = 0; dy < hTiles; dy++) {
            for (int dx = 0; dx < wTiles; dx++) {
                occupied.add(new Point(cell.x + dx, cell.y + dy));
            }
        }
        return true;
    }

    private void freeFootprint(Point cell, Maze maze, PropType type, Set<Point> occupied) {
        int ts = maze.getTileSize();
        int wTiles = Math.max(1, (int) Math.ceil(type.wPx / (double) ts));
        int hTiles = Math.max(1, (int) Math.ceil(type.hPx / (double) ts));

        for (int dy = 0; dy < hTiles; dy++) {
            for (int dx = 0; dx < wTiles; dx++) {
                occupied.remove(new Point(cell.x + dx, cell.y + dy));
            }
        }
    }
}
