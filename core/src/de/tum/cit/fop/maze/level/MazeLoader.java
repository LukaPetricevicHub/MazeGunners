package de.tum.cit.fop.maze.level;

import com.badlogic.gdx.files.FileHandle;

import java.awt.Point;
import java.io.IOException;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.Properties;

public class MazeLoader {

    public static final class LevelSpec{
        public final int width;
        public final int height;
        public final Map<Point, Integer> overrides;

        public LevelSpec(int width, int height, Map<Point, Integer> overrides) {
            this.width = width;
            this.height = height;
            this.overrides = Map.copyOf(overrides);
        }
    }

    private static final Pattern Cell = Pattern.compile("^\\s*(\\d+)\\s*,\\s*(\\d+)\\s*=\\s*(-?\\d+)\\s*$");

    public LevelSpec loadFromText(String text){
        int width = 0;
        int height = 0;
        int maxX = -1;
        int maxY = -1;

        Map<Point, Integer> overrides = new HashMap<>();

        for (String raw : text.split("\\R")){
            String line = raw.trim();
            if(line.isEmpty()){
                continue;
            }

            if(line.startsWith("Width=")){
                width = Integer.parseInt(line.substring("Width=".length()).trim());
                continue;
            }

            if(line.startsWith("Height=")){
                height = Integer.parseInt(line.substring("Height=".length()).trim());
                continue;
            }

            Matcher m = Cell.matcher(line);
            if(!m.matches()){
                continue;
            }

            int x = Integer.parseInt(m.group(1));
            int y = Integer.parseInt(m.group(2));
            int z = Integer.parseInt(m.group(3));

            overrides.put(new Point(x,y), z);
            maxX= Math.max(maxX, x);
            maxY = Math.max(maxY, y);
        }
        if(width <= 0){
            width = maxX + 1;
        }

        if(height <= 0){
            height = maxY + 1;
        }

        return new LevelSpec(width, height, overrides);

    }

    public LevelSpec loadFromProperties(FileHandle file) {
        try {
            Properties props = new Properties();
            props.load(file.reader());

            int width = Integer.parseInt(props.getProperty("Width", "0"));
            int height = Integer.parseInt(props.getProperty("Height", "0"));
            Map<Point, Integer> overrides = new HashMap<>();
            int maxX = -1;
            int maxY = -1;

            for (String key : props.stringPropertyNames()) {
                if (key.equals("Width") || key.equalsIgnoreCase("Height")) {
                    continue;
                }

                String[] parts = key.split(",");
                if (parts.length != 2) {
                    continue;
                }

                int x = Integer.parseInt(parts[0].trim());
                int y = Integer.parseInt(parts[1].trim());
                int tileId = Integer.parseInt(props.getProperty(key).trim());

                overrides.put(new Point(x, y), tileId);
                maxX = Math.max(maxX, x);
                maxY = Math.max(maxY, y);

            }
            if(width <= 0){
                width = maxX + 1;
            }
            if(height <= 0){
                height = maxY + 1;
            }

            if(width <= 0 || height <= 0){
                throw new IllegalArgumentException("Maze width/height must be > 0. Check your properties file: " + file.path());
            }


            return new LevelSpec(width, height, overrides);

        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }

    }

}
