package de.tum.cit.fop.maze.map;

import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;

public class MazeRenderer {

    private final TextureRegion floor;
    private final TextureRegion wall;
    private final TextureRegion spawn;
    private final TextureRegion exit;
    private final TextureRegion key;

    public MazeRenderer(TextureRegion floor,
                        TextureRegion wall,
                        TextureRegion spawn,
                        TextureRegion exit,
                        TextureRegion key) {
        this.floor = floor;
        this.wall = wall;
        this.spawn = spawn;
        this.exit = exit;
        this.key = key;
    }

    public void render(Maze maze, SpriteBatch batch) {
        int ts = maze.getTileSize();

        for (int y = 0; y < maze.getHeight(); y++) {
            for (int x = 0; x < maze.getWidth(); x++) {

                TextureRegion tex = switch (maze.getTile(x, y)) {
                    case WALL  -> wall;
                    case SPAWN -> spawn;
                    case EXIT  -> exit;
                    case KEY   -> key;
                    default    -> floor; // FLOOR, TRAP, ENEMY, etc.
                };

                batch.draw(tex, x * ts, y * ts, ts, ts);
            }
        }
    }
}
