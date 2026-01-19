package de.tum.cit.fop.maze.map;

import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;

public class MazeRenderer {

    private static final int SRC = 32;
    private final Texture tileset;

    // =============================
    // FLOORS
    // =============================
    private final TextureRegion hallFloor;
    private final TextureRegion[] roomFloorVariants;

    // =============================
    // SPECIALS
    // =============================
    private final TextureRegion spawn, exit, door, key;

    // =============================
    // WALLS (AUTOTILE)
    // =============================
    private final TextureRegion[] wallFillTiles;
    private final TextureRegion[] wallNTiles;
    private final TextureRegion[] wallETiles;
    private final TextureRegion[] wallSTiles;
    private final TextureRegion[] wallWTiles;

    // corners (NOT randomized)
    private final TextureRegion wallNE, wallNW, wallSE, wallSW;

    // optional inner corners (can be null)
    private final TextureRegion wallInnerNE, wallInnerNW, wallInnerSE, wallInnerSW;

    public MazeRenderer(Texture tileset) {
        this.tileset = tileset;
        this.tileset.setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest);

        // =============================
        // SET TILE COORDINATES HERE
        // =============================

        // Hall floor
        hallFloor = region(4, 8);

        // Room interior random pool
        roomFloorVariants = new TextureRegion[] {
                region(4, 8),
                region(3, 6),
        };

        // Specials
        spawn = region(5, 6);
        exit  = region(6, 6);
        door  = region(3, 6);
        key   = new TextureRegion(new Texture("key.png"));

        // =============================
        // WALL TILE POOLS
        // =============================

        wallFillTiles = new TextureRegion[] {
                region(0, 9),

        };

        wallNTiles = new TextureRegion[] {
                region(4, 7),
                region(7, 9),
                region(8,9),
                region(7,7)
        };

        wallSTiles = new TextureRegion[] {
                region(4, 9)
        };

        wallETiles = new TextureRegion[] {
                region(5, 8)
        };

        wallWTiles = new TextureRegion[] {
                region(3, 8),

        };

        // corners
        wallNW = region(3, 7);
        wallNE = region(5, 7);
        wallSW = region(3, 9);
        wallSE = region(5, 9);

        // inner corners (if you have them)
        wallInnerNW = null;
        wallInnerNE = null;
        wallInnerSW = null;
        wallInnerSE = null;
    }

    private TextureRegion region(int col, int row) {
        return new TextureRegion(tileset, col * SRC, row * SRC, SRC, SRC);
    }

    // ============================================================
    // RENDER
    // ============================================================

    public void render(Maze maze, SpriteBatch batch) {
        int ts = maze.getTileSize();

        for (int y = 0; y < maze.getHeight(); y++) {
            for (int x = 0; x < maze.getWidth(); x++) {

                Tiles t = maze.getTile(x, y);

                // ----- FLOOR -----
                if (!t.isWall()) {
                    TextureRegion floor = maze.isRoomCell(x, y)
                            ? pickRandom(roomFloorVariants, x, y, 11)
                            : hallFloor;

                    batch.draw(floor, x * ts, y * ts, ts, ts);
                }

                // ----- WALL -----
                if (t.isWall()) {
                    TextureRegion wall = pickWallTile(maze, x, y);
                    batch.draw(wall, x * ts, y * ts, ts, ts);
                    continue;
                }

                // ----- SPECIALS -----
                switch (t) {
                    case SPAWN -> batch.draw(spawn, x * ts, y * ts, ts, ts);
                    case EXIT  -> batch.draw(exit,  x * ts, y * ts, ts, ts);
                    case DOOR  -> batch.draw(door,  x * ts, y * ts, ts, ts);
                    case KEY   -> batch.draw(key,   x * ts, y * ts, ts, ts);
                    default -> {}
                }
            }
        }
    }

    // ============================================================
    // WALL AUTOTILING
    // ============================================================

    private TextureRegion pickWallTile(Maze maze, int x, int y) {

        boolean fN = isFloorAt(maze, x, y + 1);
        boolean fE = isFloorAt(maze, x + 1, y);
        boolean fS = isFloorAt(maze, x, y - 1);
        boolean fW = isFloorAt(maze, x - 1, y);

        // interior wall
        if (!fN && !fE && !fS && !fW)
            return pickRandom(wallFillTiles, x, y, 101);

        boolean fNE = isFloorAt(maze, x + 1, y + 1);
        boolean fNW = isFloorAt(maze, x - 1, y + 1);
        boolean fSE = isFloorAt(maze, x + 1, y - 1);
        boolean fSW = isFloorAt(maze, x - 1, y - 1);

        // outer corners
        if (fS && fE) return wallNE;
        if (fS && fW) return wallNW;
        if (fN && fE) return wallSE;
        if (fN && fW) return wallSW;

        // inner corners (optional)
        if (wallInnerNE != null && fN && fW && !fNW) return wallInnerNE;
        if (wallInnerNW != null && fN && fE && !fNE) return wallInnerNW;
        if (wallInnerSE != null && fS && fW && !fSW) return wallInnerSE;
        if (wallInnerSW != null && fS && fE && !fSE) return wallInnerSW;

        // edges (random pools)
        if (fS) return pickRandom(wallNTiles, x, y, 201);
        if (fN) return pickRandom(wallSTiles, x, y, 202);
        if (fW) return pickRandom(wallETiles, x, y, 203);
        if (fE) return pickRandom(wallWTiles, x, y, 204);

        return pickRandom(wallFillTiles, x, y, 105);
    }

    // ============================================================
    // HELPERS
    // ============================================================

    private boolean isFloorAt(Maze maze, int x, int y) {
        if (x < 0 || y < 0 || x >= maze.getWidth() || y >= maze.getHeight())
            return false;
        return maze.getTile(x, y).isWalkable();
    }

    /**
     * Deterministic random picker
     */
    private TextureRegion pickRandom(TextureRegion[] arr, int x, int y, int salt) {
        int h = x * 73856093 ^ y * 19349663 ^ salt;
        h = Math.abs(h);
        return arr[h % arr.length];
    }

    public void dispose() {
        tileset.dispose();
    }
}
