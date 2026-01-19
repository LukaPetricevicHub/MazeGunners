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

        // Store parsed tiles directly (not ints)
        public final Map<Point, Tiles> overrides;

        public LevelSpec(int width, int height, Map<Point, Tiles> overrides) {
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

                Tiles tile = parseTile(raw);

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
     * Robust parser that supports:
     *  - enum names (e.g., "WALL", "FLOOR")
     *  - exported variant names that don't exist in Tiles (e.g., "FLOOR_ROOM")
     *  - numeric fallback (if older maps store IDs)
     */
    private static Tiles parseTile(String raw) {
        if (raw == null) throw new IllegalArgumentException("Tile value is null");

        String s = raw.trim().toUpperCase();

        // ---- Aliases: map tokens found in properties to actual enum constants ----
        // Adjust these mappings to match your Tiles enum constants.
        s = switch (s) {
            case "FLOOR_ROOM" -> "FLOOR"; // or "ROOM_FLOOR" etc., depending on your enum
            case "FLOOR_HALL" -> "FLOOR";
            case "WALL_FILL"  -> "WALL";
            case "WALL_SET"   -> "WALL";
            default -> s;
        };

        // 1) Try enum parsing
        try {
            return Tiles.valueOf(s);
        } catch (IllegalArgumentException ignored) {
            // 2) Fallback: try numeric IDs
            try {
                int id = Integer.parseInt(s);
                return Tiles.fromId(id);
            } catch (NumberFormatException nfe) {
                throw new IllegalArgumentException("Unknown tile token: " + raw, nfe);
            }
        }
    }

    public Maze loadMazeFromProperties(FileHandle file, int tileSize) {
        LevelSpec spec = loadFromProperties(file);
        return buildMaze(spec, tileSize);
    }

}