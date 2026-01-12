package de.tum.cit.fop.maze.world;

import com.badlogic.gdx.math.Rectangle;
import de.tum.cit.fop.maze.level.Maze;
import de.tum.cit.fop.maze.player.Player;

import java.awt.Point;

public class InteractionLogic {

    // What tile are we on?
    public Point getPlayerTile(Player player, Maze maze) {
        int cx = (int) ((player.getX() + 16f) / maze.getTileSize());
        int cy = (int) ((player.getY() + 20f) / maze.getTileSize());
        return new Point(cx, cy);
    }


    // Check if Player stands on exit
    public boolean isPlayerOnExit(Player player, Maze maze) {
        Rectangle feet = getPlayerFeetRect(player);

        for (Point exit : maze.getExitCells()) {
            Rectangle exitRect = getExitRect(exit, maze);
            if (feet.overlaps(exitRect)) {
                return true;
            }
        }
        return false;
    }


    // Player StandingSpace
    private Rectangle getPlayerFeetRect(Player player) {
        float feetHeight = 20f;
        return new Rectangle(
                player.getX() + 4f,
                player.getY(),
                32f - 8f,
                feetHeight
        );
    }


    // Exit rectangle
    private Rectangle getExitRect(Point exit, Maze maze) {
        float ts = maze.getTileSize();
        return new Rectangle(
                exit.x * ts,
                exit.y * ts,
                ts,
                ts
        );
    }


    // Find the nearest exit
    public Point findNearestExit(Player player, Maze maze) {
        Point nearest = null;
        float bestDistSq = Float.MAX_VALUE;

        float px = player.getX() + 16f;
        float py = player.getY() + 32f;

        for (Point exit : maze.getExitCells()) {
            float ex = exit.x * maze.getTileSize() + maze.getTileSize() / 2f;
            float ey = exit.y * maze.getTileSize() + maze.getTileSize() / 2f;

            float dx = ex - px;
            float dy = ey - py;
            float distSq = dx * dx + dy * dy;

            if (distSq < bestDistSq) {
                bestDistSq = distSq;
                nearest = exit;
            }
        }

        return nearest;
    }


    // Key Pickup logic
    public void handleKeyPickup(Player player, Maze maze) {
        if (player.hasKey()) return;

        Point tile = getPlayerTile(player, maze);
        if (maze.hasKeyAt(tile.x, tile.y)) {
            maze.removeKeyAt(tile.x, tile.y);
            player.giveKey();
        }
    }
}
