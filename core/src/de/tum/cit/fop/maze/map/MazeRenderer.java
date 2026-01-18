package de.tum.cit.fop.maze.map;

import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;

public class MazeRenderer {

    private final TextureRegion floorRoom;
    private final TextureRegion floorHall;

    private final TextureRegion wallFill;     // single tile PNG
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

                if (t == Tiles.WALL && tex == floorHall && tex == floorRoom) {
                    System.out.println("Renderer mismatch at " + x + "," + y);
                }

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

        return (wallL && wallR && !wallU && !wallD)
                || (wallU && wallD && !wallL && !wallR);
    }

    // -------- wall slicing --------

    private void sliceWallSet(int ts) {
        // wallSet.png layout (2 rows x 4 cols):
        // row0: LEFT, RIGHT, TOP, BOTTOM
        // row1: TL, TR, BL, BR

        int baseX = wallSetSheet.getRegionX();
        int baseY = wallSetSheet.getRegionY();
        Texture tex = wallSetSheet.getTexture();

        wallLeft   = new TextureRegion(tex, baseX + 0 * ts, baseY + 1 * ts, ts, ts);
        wallRight  = new TextureRegion(tex, baseX + 2 * ts, baseY + 1 * ts, ts, ts);
        wallTop    = new TextureRegion(tex, baseX + 1 * ts, baseY + 0 * ts, ts, ts);
        wallBottom = new TextureRegion(tex, baseX + 1 * ts, baseY + 2 * ts, ts, ts);

        wallCornerTL = new TextureRegion(tex, baseX + 0 * ts, baseY + 0 * ts, ts, ts);
        wallCornerTR = new TextureRegion(tex, baseX + 1 * ts, baseY + 1 * ts, ts, ts);
        wallCornerBL = new TextureRegion(tex, baseX + 0 * ts, baseY + 2 * ts, ts, ts);
        wallCornerBR = new TextureRegion(tex, baseX + 2 * ts, baseY + 2 * ts, ts, ts);
    }

    // -------- wall picking (edges/corners/fill) --------

    private TextureRegion pickWallTexture(Maze maze, int x, int y) {
        boolean openL = maze.getTile(x - 1, y).isWalkable();
        boolean openR = maze.getTile(x + 1, y).isWalkable();
        boolean openU = maze.getTile(x, y + 1).isWalkable();
        boolean openD = maze.getTile(x, y - 1).isWalkable();

        // Corner rules: two adjacent open sides
        if (openL && openU && !openR && !openD) return wallCornerTL;
        if (openR && openU && !openL && !openD) return wallCornerTR;
        if (openL && openD && !openR && !openU) return wallCornerBL;
        if (openR && openD && !openL && !openU) return wallCornerBR;

        // Edge rules (prefer showing the face that borders walkable space)
        if (openL) return wallLeft;
        if (openR) return wallRight;
        if (openU) return wallTop;
        if (openD) return wallBottom;

        // Solid block wall
        return wallFill;
    }
}
