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
    private final int cols;
    private final int rows;

    private final int srcW;
    private final int srcH;

    private final Map<WallShape, TextureRegion> wall = new EnumMap<>(WallShape.class);

    private TextureRegion roomFloor;
    private TextureRegion hallFloor;

    private TextureRegion door;
    private TextureRegion spawn;
    private TextureRegion exit;
    private TextureRegion key;

    /**
     * Convenience ctor to match your GameScreen:
     * new MazeRenderer(new Texture("TILESET2.png"));
     *
     * Your labeled sheet is 9x10 => cols=9 rows=10
     */
    public MazeRenderer(Texture tileset) {
        this(tileset, 9, 10);
    }

    public MazeRenderer(Texture tileset, int tilesetCols, int tilesetRows) {
        this.tileset = tileset;
        this.cols = tilesetCols;
        this.rows = tilesetRows;

        this.srcW = tileset.getWidth() / cols;
        this.srcH = tileset.getHeight() / rows;

        // (Optional but recommended)
        // tileset.setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest);

        buildRegions();
    }

    /**
     * Convenience render to match your GameScreen:
     * mazeRenderer.render(maze, batch);
     *
     * Draws maze at world origin (0,0).
     */
    public void render(Maze maze, SpriteBatch batch) {
        render(batch, maze, 0f, 0f);
    }

    /**
     * Full render with offset.
     */
    public void render(SpriteBatch batch, Maze maze, float originX, float originY) {
        int w = maze.getWidth();
        int h = maze.getHeight();
        int dst = maze.getTileSize();

        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {

                Tiles t = maze.getTile(x, y);
                TextureRegion r;

                switch (t) {
                    case WALL -> r = pickWall(maze, x, y);
                    case FLOOR -> r = maze.isRoomCell(x, y) ? roomFloor : hallFloor;
                    case DOOR -> r = door;
                    case SPAWN -> r = spawn;
                    case EXIT -> r = exit;
                    case KEY -> r = key;
                    default -> r = hallFloor;
                }

                float drawX = originX + x * dst;
                float drawY = originY + y * dst;
                batch.draw(r, drawX, drawY, dst, dst);
            }
        }
    }

    public void dispose() {
        // Do NOT dispose tileset here because GameScreen created it.
        // If you want MazeRenderer to own it, tell me and I'll add an ownership flag.
    }

    // --------------------------------------------------------------------------------------------
    // WALL AUTOTILING
    // --------------------------------------------------------------------------------------------

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

        // Inner (concave) corners
        if (S && E && !SE) return wall.get(WallShape.INNER_TL);
        if (S && W && !SW) return wall.get(WallShape.INNER_TR);
        if (N && E && !NE) return wall.get(WallShape.INNER_BL);
        if (N && W && !NW) return wall.get(WallShape.INNER_BR);

        // Outer (convex) corners
        if (S && E && !N && !W) return wall.get(WallShape.OUTER_TL);
        if (S && W && !N && !E) return wall.get(WallShape.OUTER_TR);
        if (N && E && !S && !W) return wall.get(WallShape.OUTER_BL);
        if (N && W && !S && !E) return wall.get(WallShape.OUTER_BR);

        // Edges
        if (S) return wall.get(WallShape.EDGE_TOP);
        if (N) return wall.get(WallShape.EDGE_BOTTOM);
        if (E) return wall.get(WallShape.EDGE_LEFT);
        if (W) return wall.get(WallShape.EDGE_RIGHT);

        return wall.get(WallShape.SOLID);
    }

    private boolean isWalkable(Maze maze, int x, int y) {
        if (!maze.inBounds(x, y)) return false;
        return maze.getTile(x, y).isWalkable();
    }

    // --------------------------------------------------------------------------------------------
    // TILESET MAPPING (EDIT THESE COORDS TO MATCH YOUR LABELLED IMAGE)
    // --------------------------------------------------------------------------------------------

    private void buildRegions() {
        TilesetMapping m = new TilesetMapping();

        roomFloor = region(m.ROOM_FLOOR_C, m.ROOM_FLOOR_R);
        hallFloor = region(m.HALL_FLOOR_C, m.HALL_FLOOR_R);

        door  = region(m.DOOR_C,  m.DOOR_R);
        spawn = region(m.SPAWN_C, m.SPAWN_R);
        exit  = region(m.EXIT_C,  m.EXIT_R);
        key   = new TextureRegion(new Texture("key.png"));

        wall.put(WallShape.SOLID,       region(m.WALL_SOLID_C,       m.WALL_SOLID_R));

        wall.put(WallShape.EDGE_TOP,    region(m.WALL_EDGE_TOP_C,    m.WALL_EDGE_TOP_R));
        wall.put(WallShape.EDGE_BOTTOM, region(m.WALL_EDGE_BOTTOM_C, m.WALL_EDGE_BOTTOM_R));
        wall.put(WallShape.EDGE_LEFT,   region(m.WALL_EDGE_LEFT_C,   m.WALL_EDGE_LEFT_R));
        wall.put(WallShape.EDGE_RIGHT,  region(m.WALL_EDGE_RIGHT_C,  m.WALL_EDGE_RIGHT_R));

        wall.put(WallShape.OUTER_TL, region(m.WALL_OUTER_TL_C, m.WALL_OUTER_TL_R));
        wall.put(WallShape.OUTER_TR, region(m.WALL_OUTER_TR_C, m.WALL_OUTER_TR_R));
        wall.put(WallShape.OUTER_BL, region(m.WALL_OUTER_BL_C, m.WALL_OUTER_BL_R));
        wall.put(WallShape.OUTER_BR, region(m.WALL_OUTER_BR_C, m.WALL_OUTER_BR_R));

        wall.put(WallShape.INNER_TL, region(m.WALL_INNER_TL_C, m.WALL_INNER_TL_R));
        wall.put(WallShape.INNER_TR, region(m.WALL_INNER_TR_C, m.WALL_INNER_TR_R));
        wall.put(WallShape.INNER_BL, region(m.WALL_INNER_BL_C, m.WALL_INNER_BL_R));
        wall.put(WallShape.INNER_BR, region(m.WALL_INNER_BR_C, m.WALL_INNER_BR_R));
    }

    private static final class TilesetMapping {
        // Put your real coords here (col,row), row=0 is TOP row of your sheet.

        int ROOM_FLOOR_C = 4, ROOM_FLOOR_R = 1;
        int HALL_FLOOR_C = 0, HALL_FLOOR_R = 8;

        int DOOR_C  = 3, DOOR_R  = 6;
        int SPAWN_C = 0, SPAWN_R = 2;
        int EXIT_C  = 2, EXIT_R  = 2;

        int WALL_SOLID_C =0, WALL_SOLID_R = 0;

        int WALL_EDGE_TOP_C    = 0, WALL_EDGE_TOP_R    = 2;
        int WALL_EDGE_BOTTOM_C = 1, WALL_EDGE_BOTTOM_R = 2;
        int WALL_EDGE_LEFT_C   = 0, WALL_EDGE_LEFT_R   = 1;
        int WALL_EDGE_RIGHT_C  = 1, WALL_EDGE_RIGHT_R  = 1;

        int WALL_OUTER_TL_C = 2, WALL_OUTER_TL_R = 1;
        int WALL_OUTER_TR_C = 3, WALL_OUTER_TR_R = 1;
        int WALL_OUTER_BL_C = 2, WALL_OUTER_BL_R = 2;
        int WALL_OUTER_BR_C = 3, WALL_OUTER_BR_R = 2;

        int WALL_INNER_TL_C = 4, WALL_INNER_TL_R = 1;
        int WALL_INNER_TR_C = 5, WALL_INNER_TR_R = 1;
        int WALL_INNER_BL_C = 4, WALL_INNER_BL_R = 2;
        int WALL_INNER_BR_C = 5, WALL_INNER_BR_R = 2;
    }

    private TextureRegion region(int col, int rowTop) {
        int x = col * srcW;
        int y = tileset.getHeight() - (rowTop + 1) * srcH; // top-based -> bottom-based
        return new TextureRegion(tileset, x, y, srcW, srcH);
    }
}
