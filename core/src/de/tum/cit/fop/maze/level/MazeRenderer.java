package de.tum.cit.fop.maze.level;

import de.tum.cit.fop.maze.level.Tiles;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;

public class MazeRenderer {
    private final TextureRegion[] tiles;

    public MazeRenderer(TextureRegion floor, TextureRegion wall, TextureRegion entry, TextureRegion exit){
        this.tiles = new TextureRegion[6];
        tiles[Tiles.Floor] = floor;
        tiles[Tiles.Wall] = wall;
        tiles[Tiles.Entry] = entry;
        tiles[Tiles.Exit] = exit;

    }

    public void render(Maze maze, SpriteBatch batch){
        int ts = maze.getTileSize();

        for(int y = 0; y < maze.getHeight(); y++){
            for(int x = 0; x < maze.getWidth(); x++){
                int id = maze.getTile(x, y);
                TextureRegion tile = tiles[id];
                batch.draw(tile, x * ts, y * ts, ts, ts);
            }
        }
    }
}
