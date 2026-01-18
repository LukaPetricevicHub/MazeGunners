package de.tum.cit.fop.maze.map;

import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;

public class MazeRenderer {

    private final TextureRegion floorRoom;
    private final TextureRegion floorHall;

    private final TextureRegion wallFill;     // single tile PNG (fill/outside)
    private final TextureRegion wallSetSheet; // spritesheet PNG (edges + corners)

    private TextureRegion wallLeft, wallRight, wallTop, wallBottom;
    private TextureRegion wallCornerTL, wallCornerTR, wallCornerBL, wallCornerBR;
    private boolean wallSetSliced = false;

    private final TextureRegion spawn;
    private final TextureRegion exit;
    private final TextureRegion key;
    private final TextureRegion door;

    public MazeRenderer(TextureRegion floorRoom,
                        TextureRegion floorHall,
                        TextureRegion wallFill,
                        TextureRegion wallSetSheet,
                        TextureRegion spawn,
                        TextureRegion exit,
                        TextureRegion key,
                        TextureRegion door) {

        this.floorRoom = floorRoom;
        this.floorHall = floorHall;

        this.wallFill = wallFill;
        this.wallSetSheet = wallSetSheet;

        this.spawn = spawn;
        this.exit = exit;
        this.key = key;
        this.door = door;
    }


    public void render(Maze maze, SpriteBatch batch) {
        int ts = maze.getTileSize();

        if (!wallSetSliced) {
            sliceWallSet(ts);
            wallSetSliced = true;
        }

        for (int y = 0; y < maze.getHeight(); y++) {
            for (int x = 0; x < maze.getWidth(); x++) {

                Tiles t = maze.getTile(x, y);

                TextureRegion tex = switch (t) {
                    case WALL  -> pickWallTexture(maze, x, y);
                    case SPAWN -> spawn;
                    case EXIT  -> exit;
                    case KEY   -> key;
                    case DOOR  -> door;
                    default    -> (isHallTile(maze, x, y) ? floorHall : floorRoom);
                };

                batch.draw(tex, x * ts, y * ts, ts, ts);
            }
        }
    }

    // -------- floor context (room vs hall) --------
    private boolean isHallTile(Maze maze, int x, int y) {
        if (!maze.getTile(x, y).isWalkable()) return false;

        boolean wallL = maze.getTile(x - 1, y).isWall();
        boolean wallR = maze.getTile(x + 1, y).isWall();
        boolean wallU = maze.getTile(x, y + 1).isWall();
        boolean wallD = maze.getTile(x, y - 1).isWall();

        // corridor-like: enclosed on opposite sides
        return (wallL && wallR && !wallU && !wallD)
                || (wallU && wallD && !wallL && !wallR);
    }

    // -------- wall slicing --------
    private void sliceWallSet(int ts) {
        Texture tex = wallSetSheet.getTexture();
        int baseX = wallSetSheet.getRegionX();
        int baseY = wallSetSheet.getRegionY();

        // Row 0
        wallLeft   = new TextureRegion(tex, baseX + 0 * ts, baseY + 0 * ts, ts, ts);
        wallRight  = new TextureRegion(tex, baseX + 1 * ts, baseY + 0 * ts, ts, ts);
        wallTop    = new TextureRegion(tex, baseX + 2 * ts, baseY + 0 * ts, ts, ts);
        wallBottom = new TextureRegion(tex, baseX + 3 * ts, baseY + 0 * ts, ts, ts);

        // Row 1
        wallCornerTL = new TextureRegion(tex, baseX + 0 * ts, baseY + 1 * ts, ts, ts);
        wallCornerTR = new TextureRegion(tex, baseX + 1 * ts, baseY + 1 * ts, ts, ts);
        wallCornerBL = new TextureRegion(tex, baseX + 2 * ts, baseY + 1 * ts, ts, ts);
        wallCornerBR = new TextureRegion(tex, baseX + 3 * ts, baseY + 1 * ts, ts, ts);
    }

    // -------- wall picking (edges/corners/fill) --------
    private TextureRegion pickWallTexture(Maze maze, int x, int y) {
        // Decide based on which neighbors are walkable (open space)
        boolean openL = maze.getTile(x - 1, y).isWalkable();
        boolean openR = maze.getTile(x + 1, y).isWalkable();
        boolean openU = maze.getTile(x, y + 1).isWalkable();
        boolean openD = maze.getTile(x, y - 1).isWalkable();

        // If no walkable around it -> it's just a fill wall (outside / solid block)
        if (!openL && !openR && !openU && !openD) return wallFill;

        // Corners: two adjacent open sides
        if (openL && openU) return wallCornerTL;
        if (openR && openU) return wallCornerTR;
        if (openL && openD) return wallCornerBL;
        if (openR && openD) return wallCornerBR;

        // Edges: show face towards open space
        if (openL) return wallLeft;
        if (openR) return wallRight;
        if (openU) return wallTop;
        if (openD) return wallBottom;

        // fallback
        return wallFill;
    }
}
