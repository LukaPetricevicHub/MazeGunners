package de.tum.cit.fop.maze.map;

import com.badlogic.gdx.files.FileHandle;

import java.io.IOException;
import java.util.Properties;

public class MazeExporter {

    public static void saveToProperties(Maze maze, FileHandle file) {
        Properties props = new Properties();
        props.setProperty("Width", String.valueOf(maze.getWidth()));
        props.setProperty("Height", String.valueOf(maze.getHeight()));

        for (int y = 0; y < maze.getHeight(); y++) {
            for (int x = 0; x < maze.getWidth(); x++) {
                Tiles t = maze.getTile(x, y);
                props.setProperty(x + "," + y, t.name());
            }
        }

        try (var writer = file.writer(false)) {   //
            props.store(writer, "Generated Maze");
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
