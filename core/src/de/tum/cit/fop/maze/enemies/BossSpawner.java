package de.tum.cit.fop.maze.enemies;

// =======================================================
// BossSpawner.java
// =======================================================

import de.tum.cit.fop.maze.map.Maze;
import de.tum.cit.fop.maze.map.Room;
import de.tum.cit.fop.maze.map.RoomType;

public final class BossSpawner {

    private BossSpawner() {

    }

    public static Boss spawnBossIfFinalLevel(Maze maze, int level, BossAnimations anims) {
        boolean isFinalLevel = (Math.max(1, Math.min(5, level)) == 5);
        if (!isFinalLevel) return null;

        Room bossRoom = null;
        for (Room r : maze.getRooms()) {
            if (r.getType() == RoomType.BOSS) { bossRoom = r; break; }
        }
        if (bossRoom == null) return null;

        float cx = (bossRoom.centerX() + 0.5f) * maze.getTileSize();
        float cy = (bossRoom.centerY() + 0.5f) * maze.getTileSize();

        return new Boss(maze, bossRoom, anims, cx, cy);
    }
}
