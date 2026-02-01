package de.tum.cit.fop.maze.enemies;

import de.tum.cit.fop.maze.map.Maze;
import de.tum.cit.fop.maze.map.Room;
import de.tum.cit.fop.maze.map.RoomType;

import java.awt.Point;
import java.util.List;
import java.util.Random;

/**
 * Utility class responsible for spawning the boss.
 *
 * The boss is only spawned on the final level and is placed
 * inside a suitable room of the maze.
 */
public class BossSpawner {

    /**
     * Spawns the boss if the given level is the final level.
     *
     * The boss is placed in a boss room if available, otherwise
     * in a large fallback room.
     *
     * @param maze the maze in which the boss should be spawned
     * @param level the current level number
     * @param anims the animations used for the boss
     * @return the spawned boss, or {@code null} if no boss is spawned
     */
    public static Boss spawnBossIfFinalLevel(Maze maze, int level, BossAnimations anims) {
        if (maze == null || anims == null) return null;
        if (level != 5) return null;

        List<Room> rooms = maze.getRooms();
        if (rooms == null || rooms.isEmpty()) {
            System.out.println("[BOSS] maze.getRooms() is null/empty -> cannot spawn boss.");
            return null;
        }

        Room bossRoom = findBossRoomOrFallbackBiggest(rooms);
        if (bossRoom == null) {
            System.out.println("[BOSS] No valid boss room found.");
            return null;
        }

        // Pick a random interior tile with padding for boss size (10x10 tiles drawn)
        Random rng = new Random();
        Point tile = randomInteriorWithPadding(bossRoom, rng, 5);

        float spawnX = tile.x * maze.getTileSize() + maze.getTileSize() / 2f;
        float spawnY = tile.y * maze.getTileSize() + maze.getTileSize() / 2f;

        System.out.println("[BOSS] Spawning in room type=" + bossRoom.getType()
                + " tile=(" + tile.x + "," + tile.y + ") world=(" + spawnX + "," + spawnY + ")");

        return new Boss(maze, bossRoom, anims, spawnX, spawnY);
    }

    private static Room findBossRoomOrFallbackBiggest(List<Room> rooms) {
        // 1) Prefer explicit boss room
        for (Room r : rooms) {
            if (r != null && r.getType() == RoomType.BOSS) return r;
        }

        // 2) Fallback: biggest non-spawn/exit/key
        Room best = null;
        int bestArea = -1;

        for (Room r : rooms) {
            if (r == null) continue;
            RoomType t = r.getType();
            if (t == RoomType.SPAWN || t == RoomType.EXIT || t == RoomType.KEY) continue;

            int area = r.w() * r.h();
            if (area > bestArea) {
                bestArea = area;
                best = r;
            }
        }

        // 3) Absolute fallback: biggest room of anything
        if (best == null) {
            for (Room r : rooms) {
                if (r == null) continue;
                int area = r.w() * r.h();
                if (area > bestArea) {
                    bestArea = area;
                    best = r;
                }
            }
        }

        return best;
    }

    private static Point randomInteriorWithPadding(Room room, Random rng, int padTiles) {
        int ix0 = room.x() + padTiles;
        int iy0 = room.y() + padTiles;
        int ix1 = room.x() + room.w() - 1 - padTiles;
        int iy1 = room.y() + room.h() - 1 - padTiles;

        if (ix1 < ix0 || iy1 < iy0) {
            return room.center();
        }

        int rx = ix0 + rng.nextInt(ix1 - ix0 + 1);
        int ry = iy0 + rng.nextInt(iy1 - iy0 + 1);
        return new Point(rx, ry);
    }
}
