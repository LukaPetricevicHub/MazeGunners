package de.tum.cit.fop.maze.level;

import com.badlogic.gdx.files.FileHandle;

import java.util.Properties;

public class MazeExporter {

    public static void saveToProperties(Maze maze, FileHandle file) {
        Properties props = new Properties();
        props.setProperty("Width", String.valueOf(maze.getWidth()));
        props.setProperty("Height", String.valueOf(maze.getHeight()));

        for (int x = 0; x < maze.getWidth(); x++) {
            for (int y = 0; y < maze.getHeight(); y++) {

                Tiles t = maze.getTile(x, y);

                // keep files small
                if (t == Tiles.FLOOR) continue;

                props.setProperty(x + "," + y, String.valueOf(toFileId(t)));
            }
        }

        try {
            props.store(file.writer(false), "Generated maze");
        } catch (Exception e) {
            throw new RuntimeException("Failed to write " + file.path(), e);
        }
    }

    private static int toFileId(Tiles t) {
        return switch (t) {
            case WALL  -> 0;
            case ENTRY -> 1;
            case EXIT  -> 2;
            case TRAP  -> 3;
            case ENEMY -> 4;
            case KEY   -> 5;
            default    -> 0;
        };
    }
}

