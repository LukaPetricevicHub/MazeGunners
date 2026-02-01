package de.tum.cit.fop.maze.world;

import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;
import de.tum.cit.fop.maze.map.HallOrientation;
import de.tum.cit.fop.maze.map.Maze;
import de.tum.cit.fop.maze.map.Room;
import de.tum.cit.fop.maze.map.Tiles;
import de.tum.cit.fop.maze.obstacles.*;

import java.awt.Point;
import java.util.*;

/**
 * Handles procedural placement of obstacles inside a maze.
 *
 * Obstacles are placed only on valid floor tiles while respecting
 * gameplay constraints such as spawn room safety, minimum distance
 * between identical trap types, and tile validity.
 *
 * Placement is deterministic when using the same random seed.
 */
public class ObstaclePlacer {

    private static final int SPAWN_ROOM_BUFFER_TILES = 3;
    private static final int MIN_SAME_TRAP_DISTANCE_TILES = 12;

    /**
     * Places obstacles throughout the maze.
     *
     * @param maze Maze in which obstacles are placed
     * @param trapCount Maximum number of obstacles
     * @param seed Random seed for deterministic placement
     * @param excludedRoom Room where obstacles are not allowed
     * @param allowLightningTraps Whether lightning traps may be placed
     * @return List of placed obstacles
     */
    public List<Obstacle> placeObstacles(Maze maze, int trapCount, long seed, Room excludedRoom, boolean allowLightningTraps) {

        List<Obstacle> placementResult = new ArrayList<>();
        Map<Class<? extends Obstacle>, List<Point>> placedByType = new HashMap<>();

        Random randomGen = new Random(seed);

        List<Point> validFloorCells = getFloorPoints(maze);
        Collections.shuffle(validFloorCells, randomGen);

        for(Point cell : validFloorCells) {

            if (placementResult.size() >= trapCount) break;

            if (excludedRoom != null && isInsideRoom(excludedRoom, cell.x, cell.y)) continue;

            if (isNearSpawnRoom(cell, excludedRoom, maze)) continue;

            Obstacle trap = spawnRandomTrap(cell, maze, randomGen, allowLightningTraps);
            if(trap == null) continue;

            Point trapTile = new Point(cell.x, cell.y);
            Class<? extends Obstacle> type = trap.getClass();

            List<Point> placedOfSameType = getPlacedListForType(placedByType, type);

            if (!isFarEnoughFromSameType(trapTile, placedOfSameType)) continue;

            if (trap instanceof LightningTrap) {
                placementResult.add(trap);
                placedOfSameType.add(trapTile);
            } else if(isValidArea(trap, maze)) {
                placementResult.add(trap);
                placedOfSameType.add(trapTile);
            }

        }
        return placementResult;
    }


    private boolean isInsideRoom(Room r, int tx, int ty) {
        return tx >= r.x() && tx < r.x() + r.w() && ty >= r.y() && ty < r.y() + r.h();
    }

    /**
     * Collects all valid floor tiles inside the maze.
     */
    private List<Point> getFloorPoints(Maze maze) {

        List<Point> cellList = new ArrayList<>();

        for (int y = 1; y < maze.getHeight() - 1; y++) {
            for(int x = 1; x < maze.getWidth() - 1; x++) {

                if(maze.getTile(x, y) == Tiles.FLOOR) {
                    cellList.add(new Point(x, y));
                }

            }
        }
        return cellList;
    }

    /**
     * Spawns a random obstacle at a tile if placement conditions allow it.
     *
     * @return A new obstacle or null if placement is invalid
     */
    private Obstacle spawnRandomTrap(Point cell, Maze maze, Random rng, boolean allowLightningTraps) {

        float tileSize = maze.getTileSize();

        Vector2 spawnPos = new Vector2(cell.x * tileSize, cell.y * tileSize);

        int roll = rng.nextInt(3);

        if (!allowLightningTraps && roll == 0) {
            roll = rng.nextInt(2) + 1;
        }

        float baseX = cell.x * tileSize;
        float baseY = cell.y * tileSize;

        if (roll == 0) {

            int x = cell.x;
            int y = cell.y;

            if (maze.inBounds(x, y + 1) && maze.getTile(x, y) == Tiles.FLOOR && maze.getTile(x, y + 1) == Tiles.FLOOR && !maze.isRoomCell(x, y) && !maze.isRoomCell(x, y + 1) && maze.inBounds(x, y - 1) && maze.inBounds(x, y + 2) && maze.getTile(x, y - 1) == Tiles.WALL && maze.getTile(x, y + 2) == Tiles.WALL) {
                return new LightningTrap(new Vector2(baseX, baseY), false);
            }

            if(maze.inBounds(x + 1, y) && maze.getTile(x, y) == Tiles.FLOOR && maze.getTile(x + 1, y) == Tiles.FLOOR && !maze.isRoomCell(x, y) && !maze.isRoomCell(x + 1, y) && maze.inBounds(x - 1, y) && maze.inBounds(x + 2, y) && maze.getTile(x - 1, y) == Tiles.WALL && maze.getTile(x + 2, y) == Tiles.WALL) {
                return new LightningTrap(new Vector2(baseX + tileSize * 0.5f, baseY), true);
            }

            return null;
        }

        if (roll == 1)
            return new SpikeTrap(spawnPos);


        return new FloorHazard(spawnPos);
    }

    /**
     * Verifies that the obstacle occupies a fully walkable area.
     */
    private boolean isValidArea(Obstacle obstacle, Maze maze) {

        Rectangle hitBox = obstacle.getCollisionBox();
        float ts = maze.getTileSize();

        int gridX = (int)((hitBox.x + hitBox.width / 2f) / ts);
        int gridY = (int)((hitBox.y + hitBox.height / 2f) / ts);

        for(int dy = gridY - 1; dy <= gridY + 1; dy++) {
            for (int dx = gridX - 1; dx <= gridX + 1; dx++) {

                if (!maze.inBounds(dx, dy)) return false;
                if (maze.getTile(dx, dy) != Tiles.FLOOR) return false;

            }
        }
        return true;
    }


    private boolean isFarEnoughFromSameType(Point candidate, List<Point> placed) {

        for(Point p : placed) {

            int dx = p.x - candidate.x;
            int dy = p.y - candidate.y;

            if (dx * dx + dy * dy < MIN_SAME_TRAP_DISTANCE_TILES * MIN_SAME_TRAP_DISTANCE_TILES) {
                return false;
            }
        }
        return true;
    }


    private boolean isNearSpawnRoom(Point cell, Room spawnRoom, Maze maze) {

        if(spawnRoom == null) {
            Point entry = maze.getEntryCell();
            if (entry == null) return false;
            if (!maze.isHallTile(cell.x, cell.y)) return false;

            int minX = entry.x - SPAWN_ROOM_BUFFER_TILES;
            int maxX = entry.x + SPAWN_ROOM_BUFFER_TILES;
            int minY = entry.y - SPAWN_ROOM_BUFFER_TILES;
            int maxY = entry.y + SPAWN_ROOM_BUFFER_TILES;

            return cell.x >= minX && cell.x <= maxX && cell.y >= minY && cell.y <= maxY;
        }

        int minX = spawnRoom.x() - SPAWN_ROOM_BUFFER_TILES;
        int maxX = spawnRoom.x() + spawnRoom.w() + SPAWN_ROOM_BUFFER_TILES - 1;
        int minY = spawnRoom.y() - SPAWN_ROOM_BUFFER_TILES;
        int maxY = spawnRoom.y() + spawnRoom.h() + SPAWN_ROOM_BUFFER_TILES - 1;

        if (!maze.isHallTile(cell.x, cell.y)) return false;

        return cell.x >= minX && cell.x <= maxX && cell.y >= minY && cell.y <= maxY;
    }


    /**
     * Finds potential lightning trap positions without activating them.
     *
     * @param maze Maze reference
     * @param seed Random seed
     * @param excludedRoom Room where traps are not allowed
     * @return List of lightning trap candidates
     */
    public List<LightningTrap> findLightningTrapCandidates(Maze maze, long seed, Room excludedRoom) {

        List<LightningTrap> result = new ArrayList<>();
        Map<Class<? extends Obstacle>, List<Point>> placedByType = new HashMap<>();

        Random rng = new Random(seed ^ 0xBEEF);
        List<Point> floorCells = getFloorPoints(maze);
        Collections.shuffle(floorCells, rng);

        for(Point cell : floorCells) {

            if (excludedRoom != null && isInsideRoom(excludedRoom, cell.x, cell.y)) continue;

            if (isNearSpawnRoom(cell, excludedRoom, maze)) continue;

            Obstacle o = spawnRandomTrap(cell, maze, rng, true);
            if (!(o instanceof LightningTrap lt)) continue;

            Point trapTile = new Point(cell.x, cell.y);
            Class<? extends Obstacle> type = lt.getClass();

            List<Point> placedOfSameType = getPlacedListForType(placedByType, type);

            if(!isFarEnoughFromSameType(trapTile, placedOfSameType)) continue;

            result.add(lt);
            placedOfSameType.add(trapTile);
        }

        return result;
    }


    private List<Point> getPlacedListForType(Map<Class<? extends Obstacle>, List<Point>> map, Class<? extends Obstacle> type) {
        return map.computeIfAbsent(type, k -> new ArrayList<>());
    }

}
