package de.tum.cit.fop.maze.map;

import com.badlogic.gdx.files.FileHandle;

import java.io.IOException;
import java.util.Properties;

public class MazeExporter {

    public static void saveToProperties(Maze maze, FileHandle file) {
        Properties props = new Properties();

        props.setProperty("Width", String.valueOf(maze.getWidth()));
        props.setProperty("Height", String.valueOf(maze.getHeight()));

        for (int x = 0; x < maze.getWidth(); x++) {
            for (int y = 0; y < maze.getHeight(); y++) {
                int id = toFileId(maze.getTile(x, y));
                if (id != 0) {
                    props.setProperty(x + "," + y, String.valueOf(id));
                }
            }
        }

        try {
            props.store(file.writer(false), "Generated maze");
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private static int toFileId(Tiles t) {
        return switch (t) {
            case WALL -> 0;
            case FLOOR -> 1;
            case SPAWN -> 3;
            case EXIT -> 4;
            case KEY -> 5;
        };
    }
}
