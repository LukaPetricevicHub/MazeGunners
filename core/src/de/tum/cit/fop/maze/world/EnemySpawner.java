package de.tum.cit.fop.maze.world;

import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.Array;
import de.tum.cit.fop.maze.ai.EntranceFinder;
import de.tum.cit.fop.maze.ai.RoomEntrance;
import de.tum.cit.fop.maze.ai.RoomPatrolBuilder;
import de.tum.cit.fop.maze.enemies.*;
import de.tum.cit.fop.maze.map.Maze;
import de.tum.cit.fop.maze.map.Room;
import de.tum.cit.fop.maze.map.RoomType;
import de.tum.cit.fop.maze.map.Tiles;
import de.tum.cit.fop.maze.player.Player;

import java.awt.Point;
import java.util.*;

public class EnemySpawner {

    private static final float AVG_ENEMY_W = 32f;
    private static final float AVG_ENEMY_H = 32f;


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
        spawnRandomEnemies(maze, usableRooms, targetPlayer, enemyCount, rng, enemies);

        System.out.println("[EnemySpawner] Spawned total enemies=" + enemies.size());
        return enemies;
    }

    // Patrolling enemies
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
    private void spawnRandomEnemies(Maze maze, List<Room> rooms, Player targetPlayer, int maxEnemies, Random rng, List<Enemy> out) {
        List<Point> candidates;

        // Prefer room interiors
        if (rooms != null && !rooms.isEmpty()) {
            candidates = collectSafeRoomInteriorTiles(maze, rooms);

            // Fallback if room detection fails
            if (candidates.isEmpty()) {
                System.out.println("[EnemySpawner] Room interiors empty -> fallback to floor cells");
                candidates = collectFloorCells(maze);
            }
        } else {
            candidates = collectFloorCells(maze);
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

            out.add(new MutantWorker(new Vector2(wx, wy), 100f, targetPlayer));
            spawned++;
        }

        System.out.println("[EnemySpawner] Random enemies spawned=" + spawned + " (requested " + maxEnemies + ")");
    }

    // Helpers
    private List<Point> collectFloorCells(Maze maze) {
        List<Point> cells = new ArrayList<>();
        for (int y = 1; y < maze.getHeight() - 1; y++) {
            for (int x = 1; x < maze.getWidth() - 1; x++) {
                if (maze.getTile(x, y) == Tiles.FLOOR) {
                    cells.add(new Point(x, y));
                }
            }
        }
        return cells;
    }

    private boolean isSafeToSpawn(float x, float y, Maze maze) {
        float pad = 2f;

        float l = x + pad;
        float r = x + AVG_ENEMY_W - pad;
        float b = y + pad;
        float t = y + AVG_ENEMY_H - pad;

        return !maze.isWallWorld(l, b) && !maze.isWallWorld(r, b) && !maze.isWallWorld(l, t) && !maze.isWallWorld(r, t);
    }

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
