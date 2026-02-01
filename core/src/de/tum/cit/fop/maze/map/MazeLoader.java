package de.tum.cit.fop.maze.map;

import com.badlogic.gdx.files.FileHandle;

import java.awt.Point;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

/**
 * Loads mazes from .properties files and builds Maze objects.
 *
 * The file can define the maze size and individual tiles.
 */
public class MazeLoader {

    /**
     * Simple data holder for maze size and tile overrides.
     */
    public static final class LevelSpec {
        public final int width;
        public final int height;
        public final Map<Point, Tiles> overrides;


        public LevelSpec(int width, int height, Map<Point, Tiles> overrides) {
            this.width = width;
            this.height = height;
            this.overrides = Map.copyOf(overrides);
        }
    }

    /**
     * Reads a maze description from a properties file.
     *
     * Width and height are read if present.
     * Tile entries are expected in the form "x,y=value".
     *
     * @param file the properties file
     * @return the loaded level specification
     */
    public LevelSpec loadFromProperties(FileHandle file) {
        try {
            Properties props = new Properties();
            props.load(file.reader());

            int width = Integer.parseInt(props.getProperty("Width", "0"));
            int height = Integer.parseInt(props.getProperty("Height", "0"));

            Map<Point, Tiles> overrides = new HashMap<>();
            int maxX = -1, maxY = -1;

            for (String key : props.stringPropertyNames()) {
                if (key.equalsIgnoreCase("Width") || key.equalsIgnoreCase("Height")) continue;

                String[] parts = key.split(",");
                if (parts.length != 2) continue;

                int x = Integer.parseInt(parts[0].trim());
                int y = Integer.parseInt(parts[1].trim());

                String raw = props.getProperty(key);
                if (raw == null) continue;

                Tiles tile = parseTileNumericOnly(raw);

                overrides.put(new Point(x, y), tile);
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

    /**
     * Creates a Maze from a level specification.
     *
     * The maze is created with the given size and then all tile overrides
     * are applied.
     *
     * @param spec the level specification
     * @param tileSize size of one tile
     * @return the created maze
     */
    public Maze buildMaze(LevelSpec spec, int tileSize) {
        Maze maze = new Maze(spec.width, spec.height, tileSize);

        for (var e : spec.overrides.entrySet()) {
            int x = e.getKey().x;
            int y = e.getKey().y;
            maze.setTile(x, y, e.getValue());

        }

        return maze;
    }

    /**
     * Converts a tile value from the properties file into a Tiles enum.
     *
     * Numeric tile IDs are preferred.
     * KEY tiles are converted to FLOOR.
     *
     * @param raw raw tile value
     * @return parsed tile type
     */
    private static Tiles parseTileNumericOnly(String raw) {
        try {
            int id = Integer.parseInt(raw.trim());
            Tiles t = Tiles.fromId(id);
            return t == Tiles.KEY ? Tiles.FLOOR : t;
        } catch (NumberFormatException e) {
            Tiles t = Tiles.valueOf(raw.trim().toUpperCase());
            return t == Tiles.KEY ? Tiles.FLOOR : t;
        }
    }

    /**
     * Loads a maze from a properties file and builds it directly.
     *
     * @param file the properties file
     * @param tileSize size of one tile
     * @return the loaded maze
     */
    public Maze loadMazeFromProperties(FileHandle file, int tileSize) {
        LevelSpec spec = loadFromProperties(file);
        return buildMaze(spec, tileSize);
    }
}
