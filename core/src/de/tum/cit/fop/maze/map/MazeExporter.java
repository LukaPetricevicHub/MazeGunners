package de.tum.cit.fop.maze.map;

import com.badlogic.gdx.files.FileHandle;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.Writer;
import java.util.Map;
import java.util.TreeMap;

public class MazeExporter {

    /**
     * Writes a maze to a .properties-style file with ONLY numeric tile ids.
     *
     * Format:
     * Width=80
     * Height=80
     * 0,0=1
     */
    public static void saveToProperties(Maze maze, FileHandle file) {

        Map<String, String> out = new TreeMap<>();
        out.put("Width", Integer.toString(maze.getWidth()));
        out.put("Height", Integer.toString(maze.getHeight()));

        for (int y = 0; y < maze.getHeight(); y++) {
            for (int x = 0; x < maze.getWidth(); x++) {
                Tiles t = maze.getTile(x, y);
                out.put(x + "," + y, Integer.toString(t.id));
            }
        }

        try (Writer writer = file.writer(false);
             BufferedWriter w = new BufferedWriter(writer)) {
            w.write("GeneratedAt=" + System.currentTimeMillis());
            w.newLine();

            for (Map.Entry<String, String> e : out.entrySet()) {
                w.write(e.getKey());
                w.write('=');
                w.write(e.getValue());
                w.newLine();
            }

        } catch (IOException e) {
            throw new RuntimeException("Failed to write maze file: " + file.path(), e);
        }
    }
}
