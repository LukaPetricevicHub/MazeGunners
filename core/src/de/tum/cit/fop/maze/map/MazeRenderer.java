package de.tum.cit.fop.maze.map;

import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;

import java.util.EnumMap;
import java.util.Map;

public class MazeRenderer {

    private enum WallShape {
        SOLID,
        EDGE_TOP, EDGE_BOTTOM, EDGE_LEFT, EDGE_RIGHT,
        OUTER_TL, OUTER_TR, OUTER_BL, OUTER_BR,
        INNER_TL, INNER_TR, INNER_BL, INNER_BR
    }

    private final Texture tileset;

    // size of ONE tile inside tileset image (e.g. 16x16)
    private final int tilePxW;
    private final int tilePxH;

    // computed grid size of the sheet
    @SuppressWarnings("unused")
    private final int cols;
    @SuppressWarnings("unused")
    private final int rows;

    private final Map<WallShape, TextureRegion> wall = new EnumMap<>(WallShape.class);

    // Floor variation pools
    private TextureRegion[] roomFloors;
    private int[] roomFloorWeights;

    private TextureRegion[] hallFloors;
    private int[] hallFloorWeights;

    // Specials (door intentionally ignored visually)
    private TextureRegion spawn;
    private TextureRegion exit;
    private TextureRegion key;

    public MazeRenderer(Texture tileset) {
        this(tileset, 16, 16);
    }

    public MazeRenderer(Texture tileset, int tilePxW, int tilePxH) {
        this.tileset = tileset;
        this.tilePxW = tilePxW;
        this.tilePxH = tilePxH;

        if (tileset.getWidth() % tilePxW != 0 || tileset.getHeight() % tilePxH != 0) {
            throw new IllegalArgumentException(
                    "Tileset size must be divisible by tilePxW/tilePxH. " +
                            "Got " + tileset.getWidth() + "x" + tileset.getHeight() +
                            " with tile " + tilePxW + "x" + tilePxH
            );
        }

        this.cols = tileset.getWidth() / tilePxW;
        this.rows = tileset.getHeight() / tilePxH;

        buildRegions();
        applyCornerFallbacks();
        validatePools();
    }

    public void render(Maze maze, SpriteBatch batch) {
        int w = maze.getWidth();
        int h = maze.getHeight();
        int dst = maze.getTileSize(); // world tile size (32px) - we scale tileset tiles up

        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {

                Tiles t = maze.getTile(x, y);
                TextureRegion r;

                switch (t) {
                    case WALL -> r = pickWall(maze, x, y);

                    case FLOOR -> r = pickFloor(maze, x, y);

                    // DOOR: render like floor (door tile “irrelevant”)
                    case DOOR -> r = pickFloor(maze, x, y);

                    case SPAWN -> r = spawn;
                    case EXIT -> r = exit;
                    case KEY -> r = pickFloor(maze, x, y);

                    default -> r = pickFloor(maze, x, y);
                }

                batch.draw(r, x * dst, y * dst, dst, dst);
            }
        }
    }

    public void dispose() {
        // GameScreen owns the tileset Texture; do not dispose here.
    }

    // ---------------- FLOOR PICKING (WEIGHTED) ----------------

    private TextureRegion pickFloor(Maze maze, int x, int y) {
        boolean isRoom = maze.isRoomCell(x, y);

        if (isRoom) {
            return pickWeighted(roomFloors, roomFloorWeights, x, y, 1337);
        } else {
            return pickWeighted(hallFloors, hallFloorWeights, x, y, 7331);
        }
    }

    private TextureRegion pickWeighted(TextureRegion[] pool, int[] weights, int x, int y, int salt) {
        int total = 0;
        for (int w : weights) total += Math.max(0, w);
        if (total <= 0) return pool[0];

        int r = stableRand0(total, x, y, salt);

        int acc = 0;
        for (int i = 0; i < pool.length; i++) {
            acc += Math.max(0, weights[i]);
            if (r < acc) return pool[i];
        }
        return pool[pool.length - 1];
    }

    /**
     * Deterministic "random" in [0, bound).
     */
    private int stableRand0(int bound, int x, int y, int salt) {
        int h = x * 73856093 ^ y * 19349663 ^ salt * 83492791;
        h ^= (h >>> 16);
        h *= 0x7feb352d;
        h ^= (h >>> 15);
        h *= 0x846ca68b;
        h ^= (h >>> 16);

        // make non-negative
        if (h < 0) h = -h;

        return h % bound;
    }

    // ---------------- WALL AUTOTILING ----------------

    private TextureRegion pickWall(Maze maze, int x, int y) {
        boolean N = isWalkable(maze, x, y + 1);
        boolean S = isWalkable(maze, x, y - 1);
        boolean E = isWalkable(maze, x + 1, y);
        boolean W = isWalkable(maze, x - 1, y);

        boolean NE = isWalkable(maze, x + 1, y + 1);
        boolean NW = isWalkable(maze, x - 1, y + 1);
        boolean SE = isWalkable(maze, x + 1, y - 1);
        boolean SW = isWalkable(maze, x - 1, y - 1);

        if (!N && !S && !E && !W) return wall.get(WallShape.SOLID);

        // inner corners (concave)
        if (S && E && !SE) return wall.get(WallShape.INNER_TL);
        if (S && W && !SW) return wall.get(WallShape.INNER_TR);
        if (N && E && !NE) return wall.get(WallShape.INNER_BL);
        if (N && W && !NW) return wall.get(WallShape.INNER_BR);

        // outer corners (convex)
        if (S && E && !N && !W) return wall.get(WallShape.OUTER_TL);
        if (S && W && !N && !E) return wall.get(WallShape.OUTER_TR);
        if (N && E && !S && !W) return wall.get(WallShape.OUTER_BL);
        if (N && W && !S && !E) return wall.get(WallShape.OUTER_BR);

        // edges
        if (S) return wall.get(WallShape.EDGE_TOP);
        if (N) return wall.get(WallShape.EDGE_BOTTOM);
        if (E) return wall.get(WallShape.EDGE_LEFT);
        if (W) return wall.get(WallShape.EDGE_RIGHT);

        return wall.get(WallShape.SOLID);
    }

    private boolean isWalkable(Maze maze, int x, int y) {
        return maze.inBounds(x, y) && maze.getTile(x, y).isWalkable();
    }

    // ---------------- TILESET MAPPING ----------------
    // IMPORTANT: This uses BOTTOM->TOP coordinates:
    // row=0 is the BOTTOM row (matches your latest labeled image).

    private void buildRegions() {
        TilesetMapping m = new TilesetMapping();

        // Room floor pool + weights (weights can be different per tile)
        roomFloors = new TextureRegion[] {
                tile(m.ROOM_F0_C, m.ROOM_F0_R),
                tile(m.ROOM_F1_C, m.ROOM_F1_R),
                tile(m.ROOM_F2_C, m.ROOM_F2_R),
                tile(m.ROOM_F3_C, m.ROOM_F3_R)
        };
        roomFloorWeights = new int[] {
                70, 20, 8, 2   // example: mostly F0, rare F3
        };

        // Hall floor pool + weights
        hallFloors = new TextureRegion[] {
                tile(m.HALL_F0_C, m.HALL_F0_R),
                tile(m.HALL_F1_C, m.HALL_F1_R),
                tile(m.HALL_F2_C, m.HALL_F2_R),
                tile(m.HALL_F3_C, m.HALL_F3_R)
        };
        hallFloorWeights = new int[] {
                70, 25, 15, 5  // example: more variety in halls
        };

        // Specials
        spawn = tile(m.SPAWN_C, m.SPAWN_R);
        exit  = tile(m.EXIT_C,  m.EXIT_R);
        key   = tile(m.KEY_C,   m.KEY_R);

        // Walls
        wall.put(WallShape.SOLID,       tile(m.WALL_SOLID_C,       m.WALL_SOLID_R));
        wall.put(WallShape.EDGE_TOP,    tile(m.WALL_EDGE_TOP_C,    m.WALL_EDGE_TOP_R));
        wall.put(WallShape.EDGE_BOTTOM, tile(m.WALL_EDGE_BOTTOM_C, m.WALL_EDGE_BOTTOM_R));
        wall.put(WallShape.EDGE_LEFT,   tile(m.WALL_EDGE_LEFT_C,   m.WALL_EDGE_LEFT_R));
        wall.put(WallShape.EDGE_RIGHT,  tile(m.WALL_EDGE_RIGHT_C,  m.WALL_EDGE_RIGHT_R));

        // corners optional — if your tileset doesn't have some of them, fallbacks handle it
        wall.put(WallShape.OUTER_TL, tile(m.WALL_OUTER_TL_C, m.WALL_OUTER_TL_R));
        wall.put(WallShape.OUTER_TR, tile(m.WALL_OUTER_TR_C, m.WALL_OUTER_TR_R));
        wall.put(WallShape.OUTER_BL, tile(m.WALL_OUTER_BL_C, m.WALL_OUTER_BL_R));
        wall.put(WallShape.OUTER_BR, tile(m.WALL_OUTER_BR_C, m.WALL_OUTER_BR_R));

        wall.put(WallShape.INNER_TL, tile(m.WALL_INNER_TL_C, m.WALL_INNER_TL_R));
        wall.put(WallShape.INNER_TR, tile(m.WALL_INNER_TR_C, m.WALL_INNER_TR_R));
        wall.put(WallShape.INNER_BL, tile(m.WALL_INNER_BL_C, m.WALL_INNER_BL_R));
        wall.put(WallShape.INNER_BR, tile(m.WALL_INNER_BR_C, m.WALL_INNER_BR_R));
    }

    private void validatePools() {
        if (roomFloors == null || roomFloors.length == 0) throw new IllegalStateException("roomFloors not set");
        if (hallFloors == null || hallFloors.length == 0) throw new IllegalStateException("hallFloors not set");
        if (roomFloorWeights == null || roomFloorWeights.length != roomFloors.length)
            throw new IllegalStateException("roomFloorWeights must match roomFloors length");
        if (hallFloorWeights == null || hallFloorWeights.length != hallFloors.length)
            throw new IllegalStateException("hallFloorWeights must match hallFloors length");
    }

    private void applyCornerFallbacks() {
        TextureRegion SOL = wall.get(WallShape.SOLID);
        TextureRegion TOP = wall.get(WallShape.EDGE_TOP);
        TextureRegion LFT = wall.get(WallShape.EDGE_LEFT);

        wall.put(WallShape.OUTER_TL, safe(wall.get(WallShape.OUTER_TL), TOP));
        wall.put(WallShape.OUTER_TR, safe(wall.get(WallShape.OUTER_TR), TOP));
        wall.put(WallShape.OUTER_BL, safe(wall.get(WallShape.OUTER_BL), SOL));
        wall.put(WallShape.OUTER_BR, safe(wall.get(WallShape.OUTER_BR), SOL));

        wall.put(WallShape.INNER_TL, safe(wall.get(WallShape.INNER_TL), LFT));
        wall.put(WallShape.INNER_TR, safe(wall.get(WallShape.INNER_TR), LFT));
        wall.put(WallShape.INNER_BL, safe(wall.get(WallShape.INNER_BL), SOL));
        wall.put(WallShape.INNER_BR, safe(wall.get(WallShape.INNER_BR), SOL));
    }

    private TextureRegion safe(TextureRegion r, TextureRegion fallback) {
        return (r != null) ? r : fallback;
    }

    /**
     * Bottom-to-top tileset coordinates:
     * row=0 is bottom row.
     */
    private TextureRegion tile(int col, int rowTop) {
        int px = col * tilePxW;
        int py = tileset.getHeight() - (rowTop + 1) * tilePxH;
        return new TextureRegion(tileset, px, py, tilePxW, tilePxH);
    }

    /**
     * EDIT THESE TILE COORDS using your bottom-to-top labeled image.
     * Valid:
     *   col: 0..18
     *   row: 0..12   (row 0 = BOTTOM)
     */
    private static final class TilesetMapping {
        // Room floors (pick 4 nice ones)
        int ROOM_F0_C = 0, ROOM_F0_R = 1;
        int ROOM_F1_C = 4, ROOM_F1_R = 2;
        int ROOM_F2_C = 7, ROOM_F2_R = 1;
        int ROOM_F3_C = 2, ROOM_F3_R = 1;

        // Hall floors (pick 4 nice ones)
        int HALL_F0_C = 0, HALL_F0_R = 1;
        int HALL_F1_C = 6, HALL_F1_R = 1;
        int HALL_F2_C = 3, HALL_F2_R = 1;
        int HALL_F3_C = 7, HALL_F3_R = 2;

        // Specials (set to whatever you want)
        int SPAWN_C = 3, SPAWN_R = 3;
        int EXIT_C  = 4, EXIT_R  = 3;
        int KEY_C   = 4, KEY_R   = 4;

        // Walls
        int WALL_SOLID_C = 0, WALL_SOLID_R = 6;

        int WALL_EDGE_TOP_C    = 2, WALL_EDGE_TOP_R    = 7;
        int WALL_EDGE_BOTTOM_C = 2, WALL_EDGE_BOTTOM_R = 9;
        int WALL_EDGE_LEFT_C   = 3, WALL_EDGE_LEFT_R   = 8;
        int WALL_EDGE_RIGHT_C  = 1, WALL_EDGE_RIGHT_R  = 8;

        int WALL_OUTER_TL_C = 3, WALL_OUTER_TL_R = 7;
        int WALL_OUTER_TR_C = 1, WALL_OUTER_TR_R = 7;
        int WALL_OUTER_BL_C = 3, WALL_OUTER_BL_R = 9;
        int WALL_OUTER_BR_C = 1, WALL_OUTER_BR_R = 9;

        // If you don’t have inner corners, point them anywhere; fallbacks will cover them.
        int WALL_INNER_TL_C = 0, WALL_INNER_TL_R = 6;
        int WALL_INNER_TR_C = 0, WALL_INNER_TR_R = 6;
        int WALL_INNER_BL_C = 0, WALL_INNER_BL_R = 6;
        int WALL_INNER_BR_C = 0, WALL_INNER_BR_R = 6;
    }
}
