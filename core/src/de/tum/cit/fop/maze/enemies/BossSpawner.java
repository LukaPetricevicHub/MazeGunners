package de.tum.cit.fop.maze.enemies;

import de.tum.cit.fop.maze.map.Maze;
import de.tum.cit.fop.maze.map.Room;
import de.tum.cit.fop.maze.map.RoomType;

public final class BossSpawner {

    private BossSpawner() {}

    public static Boss spawnBossIfFinalLevel(Maze maze, int level, BossAnimations anims) {
        boolean isFinalLevel = (Math.max(1, Math.min(5, level)) == 5);
        if (!isFinalLevel) return null;

        Room bossRoom = null;
        for (Room r : maze.getRooms()) {
            if (r.getType() == RoomType.BOSS) { bossRoom = r; break; }
        }
        if (bossRoom == null) return null;

        float tile = maze.getTileSize();

        // room center in world coords
        float cx = (bossRoom.centerX() + 0.5f) * tile;
        float cy = (bossRoom.centerY() + 0.5f) * tile;

        // Keep spawn safely inside room (padding in tiles)
        int padTiles = 3; // because boss is huge
        float minX = (bossRoom.x() + padTiles) * tile;
        float minY = (bossRoom.y() + padTiles) * tile;
        float maxX = (bossRoom.x() + bossRoom.w() - padTiles) * tile;
        float maxY = (bossRoom.y() + bossRoom.h() - padTiles) * tile;

        cx = Math.max(minX, Math.min(maxX, cx));
        cy = Math.max(minY, Math.min(maxY, cy));

        return new Boss(maze, bossRoom, anims, cx, cy);
    }
}
