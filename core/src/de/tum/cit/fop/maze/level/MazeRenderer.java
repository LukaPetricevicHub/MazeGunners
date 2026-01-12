package de.tum.cit.fop.maze.level;

import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;

public class MazeRenderer {

    private final TextureRegion floor;
    private final TextureRegion wall;
    private final TextureRegion entry;
    private final TextureRegion exit;

    public MazeRenderer(TextureRegion floor,
                        TextureRegion wall,
                        TextureRegion entry,
                        TextureRegion exit) {
        this.floor = floor;
        this.wall = wall;
        this.entry = entry;
        this.exit = exit;
    }

    public void render(Maze maze, SpriteBatch batch) {
        int ts = maze.getTileSize();

        for (int x = 0; x < maze.getWidth(); x++) {
            for (int y = 0; y < maze.getHeight(); y++) {

                TextureRegion tex = switch (maze.getTile(x, y)) {
                    case WALL -> wall;
                    case ENTRY -> entry;
                    case EXIT -> exit;
                    default -> floor;
                };

                batch.draw(tex, x * ts, y * ts, ts, ts);
            }
        }
    }
}
