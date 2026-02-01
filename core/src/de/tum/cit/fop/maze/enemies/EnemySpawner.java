package de.tum.cit.fop.maze.enemies;

import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.Array;
import de.tum.cit.fop.maze.ai.EntranceFinder;
import de.tum.cit.fop.maze.ai.RoomEntrance;
import de.tum.cit.fop.maze.ai.RoomPatrolBuilder;
import de.tum.cit.fop.maze.map.Maze;
import de.tum.cit.fop.maze.map.Room;
import de.tum.cit.fop.maze.map.RoomType;
import de.tum.cit.fop.maze.map.Tiles;
import de.tum.cit.fop.maze.player.Player;

import java.awt.Point;
import java.util.*;

/**
 * Spawns enemies into the maze.
 *
 * Enemies are placed either as room patrols or as random enemies
 * on safe floor tiles, avoiding spawn rooms.
 */
public class EnemySpawner {

    /** Approximate enemy width used for spawn safety checks. */
    private static final float AVG_ENEMY_W = 32f;
    /** Approximate enemy height used for spawn safety checks. */
    private static final float AVG_ENEMY_H = 32f;

    /**
     * Spawns enemies into the maze.
     *
     * Enemies are placed either as room patrols or as random enemies
     * on safe floor tiles, avoiding spawn rooms.
     */
    public List<Enemy> spawnEnemies(Maze maze, List<Room> rooms, Room excludedRoom, Player targetPlayer, int enemyCount, long seed) {
        List<Enemy> enemies = new ArrayList<>();
        Random rng = new Random(seed);

        // Filter usable rooms
        List<Room> usableRooms = rooms;
        if (rooms != null && excludedRoom != null) {
            usableRooms = new ArrayList<>();
            for (Room r : rooms) {
                if (r != excludedRoom && r.getType() != RoomType.SPAWN) {
                    usableRooms.add(r);
                }
            }
        }

        System.out.println("[EnemySpawner] rooms=" +
                (usableRooms == null ? 0 : usableRooms.size()) +
                " enemyCount=" + enemyCount);

        // 1) Patrol enemies
        if (usableRooms != null && !usableRooms.isEmpty()) {
            spawnRoomPatrolEnemies(maze, usableRooms, targetPlayer, rng, enemies);
        } else {
            System.out.println("[EnemySpawner] No rooms available -> skipping patrol enemies");
        }

        // 2) Random enemies
        spawnRandomEnemies(maze, usableRooms, excludedRoom, targetPlayer, enemyCount, rng, enemies);

        System.out.println("[EnemySpawner] Spawned total enemies=" + enemies.size());
        return enemies;
    }

    // Patrolling enemies
    /**
     * Spawns patrol enemies in rooms using entrance-based patrols.
     *
     * @param maze Maze reference
     * @param rooms Rooms to consider
     * @param targetPlayer Player target
     * @param rng Random generator
     * @param out Output list to append to
     */
    private void spawnRoomPatrolEnemies(Maze maze, List<Room> rooms, Player targetPlayer, Random rng, List<Enemy> out) {
        int roomsWithPatrol = 0;
        for (Room room : rooms) {

            if (room.getType() == RoomType.SPAWN)
                continue;

            // 1) Entrances
            List<RoomEntrance> entrances = EntranceFinder.findEntrances(maze, room);

            // 2) Patrols
            List<Array<Vector2>> patrols = new ArrayList<>();
            for (RoomEntrance e : entrances) {
                Array<Vector2> patrol =
                        RoomPatrolBuilder.buildPatrol(maze, room, e);
                if (patrol != null)
                    patrols.add(patrol);
            }

            if (patrols.isEmpty())
                continue;

            // 3) Pick one patrol
            Array<Vector2> chosenPatrol =
                    patrols.get(rng.nextInt(patrols.size()));

            if (chosenPatrol.size < 2)
                continue;

            Vector2 spawn = new Vector2(chosenPatrol.first());

            Enemy enemy = rng.nextBoolean()
                    ? new RobotGuard(spawn, 60f, chosenPatrol, targetPlayer)
                    : new FlyingDrone(spawn, 100f, chosenPatrol, targetPlayer);

            out.add(enemy);
            roomsWithPatrol++;

            System.out.println(
                    "[EnemySpawner] Patrol enemies=" +
                            roomsWithPatrol + " / rooms=" + rooms.size()
            );
        }
    }

    // Random enemies
    /**
     * Spawns random enemies across valid tiles.
     *
     * @param maze Maze reference
     * @param rooms Rooms to consider
     * @param excludedRoom Room to avoid
     * @param targetPlayer Player target
     * @param maxEnemies Maximum number of enemies
     * @param rng Random generator
     * @param out Output list to append to
     */
    private void spawnRandomEnemies(Maze maze, List<Room> rooms, Room excludedRoom, Player targetPlayer, int maxEnemies, Random rng, List<Enemy> out) {
        List<Point> candidates;

        // Prefer room interiors
        if (rooms != null && !rooms.isEmpty()) {
            candidates = collectSafeRoomInteriorTiles(maze, rooms);

            // Fallback if room detection fails
            if (candidates.isEmpty()) {
                System.out.println("[EnemySpawner] Room interiors empty -> fallback to floor cells");
                candidates = collectFloorCells(maze, excludedRoom);
            }
        } else {
            candidates = collectFloorCells(maze, excludedRoom);
        }

        Collections.shuffle(candidates, rng);

        int spawned = 0;
        for (Point cell : candidates) {
            if (spawned >= maxEnemies)
                break;

            float wx = cell.x * maze.getTileSize();
            float wy = cell.y * maze.getTileSize();

            if (!isSafeToSpawn(wx, wy, maze))
                continue;

            Enemy enemy = createRandomEnemyType(maze, rng, cell, targetPlayer);
            if (enemy == null) {
                out.add(new MutantWorker(new Vector2(wx, wy), 100f, targetPlayer));
            } else {
                out.add(enemy);
            }
            spawned++;
        }

        System.out.println("[EnemySpawner] Random enemies spawned=" + spawned + " (requested " + maxEnemies + ")");
    }

    /**
     * Creates a random enemy type for a given tile.
     *
     * @param maze Maze reference
     * @param rng Random generator
     * @param cell Tile location
     * @param targetPlayer Player target
     * @return Created enemy or null if not possible
     */
    private Enemy createRandomEnemyType(Maze maze, Random rng, Point cell, Player targetPlayer) {
        float roll = rng.nextFloat();

        // 50% MutantWorker, 25% RobotGuard, 25% FlyingDrone
        if (roll < 0.50f) {
            return new MutantWorker(new Vector2(cell.x * maze.getTileSize(), cell.y * maze.getTileSize()), 100f, targetPlayer);
        }

        Array<Vector2> patrol = buildSimplePatrol(maze, rng, cell);
        if (patrol.size < 2) return null;

        Vector2 spawn = new Vector2(cell.x * maze.getTileSize(), cell.y * maze.getTileSize());
        if (roll < 0.75f) {
            return new RobotGuard(spawn, 60f, patrol, targetPlayer);
        }
        return new FlyingDrone(spawn, 100f, patrol, targetPlayer);
    }

    /**
     * Builds a simple two-point patrol path.
     *
     * @param maze Maze reference
     * @param rng Random generator
     * @param cell Origin tile
     * @return Patrol path
     */
    private Array<Vector2> buildSimplePatrol(Maze maze, Random rng, Point cell) {
        Array<Vector2> patrol = new Array<>(2);

        int[][] dirs = new int[][] { {1,0}, {-1,0}, {0,1}, {0,-1} };
        List<Point> candidates = new ArrayList<>();
        for (int[] d : dirs) {
            int nx = cell.x + d[0];
            int ny = cell.y + d[1];
            if (!maze.inBounds(nx, ny)) continue;
            if (maze.getTile(nx, ny) != Tiles.FLOOR) continue;
            candidates.add(new Point(nx, ny));
        }

        int tile = maze.getTileSize();
        Vector2 a = new Vector2(cell.x * tile, cell.y * tile);
        patrol.add(a);

        if (candidates.isEmpty()) {
            patrol.add(new Vector2(a));
            return patrol;
        }

        Point b = candidates.get(rng.nextInt(candidates.size()));
        patrol.add(new Vector2(b.x * tile, b.y * tile));
        return patrol;
    }

    // Helpers
    /**
     * Collects floor cells, excluding the given room if specified.
     *
     * @param maze Maze reference
     * @param excludedRoom Room to exclude
     * @return List of floor tiles
     */
    private List<Point> collectFloorCells(Maze maze, Room excludedRoom) {
        List<Point> cells = new ArrayList<>();
        for (int y = 1; y < maze.getHeight() - 1; y++) {
            for (int x = 1; x < maze.getWidth() - 1; x++) {
                if (maze.getTile(x, y) == Tiles.FLOOR) {
                    if (excludedRoom != null) {
                        if (x >= excludedRoom.x() && x < excludedRoom.x() + excludedRoom.w()
                                && y >= excludedRoom.y() && y < excludedRoom.y() + excludedRoom.h()) {
                            continue;
                        }
                    }
                    cells.add(new Point(x, y));
                }
            }
        }
        return cells;
    }

    /**
     * Checks whether an enemy can safely spawn at a location.
     *
     * @param x World x-coordinate
     * @param y World y-coordinate
     * @param maze Maze reference
     * @return True if spawn is safe
     */
    private boolean isSafeToSpawn(float x, float y, Maze maze) {
        float pad = 2f;

        float l = x + pad;
        float r = x + AVG_ENEMY_W - pad;
        float b = y + pad;
        float t = y + AVG_ENEMY_H - pad;

        return !maze.isWallWorld(l, b) && !maze.isWallWorld(r, b) && !maze.isWallWorld(l, t) && !maze.isWallWorld(r, t);
    }

    /**
     * Collects interior floor tiles from rooms.
     *
     * @param maze Maze reference
     * @param rooms Rooms to scan
     * @return List of interior floor tiles
     */
    private List<Point> collectSafeRoomInteriorTiles(Maze maze, List<Room> rooms) {
        List<Point> result = new ArrayList<>();

        for(Room room : rooms) {
            if (room.getType() == RoomType.SPAWN)
                continue;

            for (int x = room.x() + 1; x < room.x() + room.w() - 1; x++) {
                for (int y = room.y() + 1; y < room.y() + room.h() - 1; y++) {

                    if (!maze.inBounds(x, y)) continue;
                    if (maze.getTile(x, y) != Tiles.FLOOR) continue;

                    result.add(new Point(x, y));
                }
            }
        }

        return result;
    }
}
