package de.tum.cit.fop.maze.map;

import com.badlogic.gdx.files.FileHandle;

import java.awt.Point;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

public class MazeLoader {

    public static final class LevelSpec {
        public final int width;
        public final int height;
        public final Map<Point, Integer> overrides;

        public LevelSpec(int width, int height, Map<Point, Integer> overrides) {
            this.width = width;
            this.height = height;
            this.overrides = Map.copyOf(overrides);
        }
    }

    public LevelSpec loadFromProperties(FileHandle file) {
        try {
            Properties props = new Properties();
            props.load(file.reader());

            int width = Integer.parseInt(props.getProperty("Width", "0"));
            int height = Integer.parseInt(props.getProperty("Height", "0"));

            Map<Point, Integer> overrides = new HashMap<>();
            int maxX = -1, maxY = -1;

            for (String key : props.stringPropertyNames()) {
                if (key.equalsIgnoreCase("Width") || key.equalsIgnoreCase("Height")) continue;

                String[] parts = key.split(",");
                if (parts.length != 2) continue;

                int x = Integer.parseInt(parts[0].trim());
                int y = Integer.parseInt(parts[1].trim());
                int tileId = Integer.parseInt(props.getProperty(key).trim());

                overrides.put(new Point(x, y), tileId);
                maxX = Math.max(maxX, x);
                maxY = Math.max(maxY, y);
            }

            if (width <= 0) width = maxX + 1;
            if (height <= 0) height = maxY + 1;

            return new LevelSpec(width, height, overrides);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public Maze buildMaze(LevelSpec spec, int tileSize) {
        Maze maze = new Maze(spec.width, spec.height, tileSize);

        for (var e : spec.overrides.entrySet()) {
            maze.setTile(e.getKey().x, e.getKey().y, fromFileId(e.getValue()));
        }
        return maze;
    }

    private Tiles fromFileId(int id) {
        return switch (id) {
            case 0 -> Tiles.WALL;
            case 1 -> Tiles.FLOOR;
            case 2 -> Tiles.FLOOR;
            case 3 -> Tiles.SPAWN;
            case 4 -> Tiles.EXIT;
            case 5 -> Tiles.KEY;
            default -> Tiles.FLOOR;
        };
    }
}
