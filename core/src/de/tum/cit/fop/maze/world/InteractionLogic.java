package de.tum.cit.fop.maze.world;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.math.Rectangle;
import de.tum.cit.fop.maze.MazeRunnerGame;
import de.tum.cit.fop.maze.map.Maze;
import de.tum.cit.fop.maze.map.Tiles;
import de.tum.cit.fop.maze.player.Player;

import java.awt.Point;

/**
 * Handles basic interactions between the player and the maze.
 *
 * This class encapsulates simple game rules such as key pickup,
 * exit detection, and tile-based player positioning.
 */
public class InteractionLogic {

    /**
     * Checks whether the player picks up a key on the current tile.
     *
     * @param player Player instance
     * @param maze Maze reference
     * @return true if a key was picked up
     */
    public boolean handleKeyPickup(Player player, Maze maze) {
        if (player.hasKey()) return false;

        Point tile = getPlayerTile(player, maze);
        if (maze.getTile(tile.x, tile.y) == Tiles.KEY) {
            maze.setTile(tile.x, tile.y, Tiles.FLOOR);
            player.giveKey();

            MazeRunnerGame game = (MazeRunnerGame) Gdx.app.getApplicationListener();
            game.getAchievementManager().unlockAchievement("collector");
            return true;
        }
        return false;
    }

    /**
     * Determines whether the player is currently standing on an exit tile.
     *
     * @param player Player instance
     * @param maze Maze reference
     * @return true if the player overlaps an exit
     */
    public boolean isPlayerOnExit(Player player, Maze maze) {
        Rectangle feet = new Rectangle(player.getX() + 4f, player.getY(), 24f, 20f);

        for (Point e : maze.getExitCells()) {
            Rectangle r = new Rectangle(e.x * maze.getTileSize(), e.y * maze.getTileSize(), maze.getTileSize(), maze.getTileSize());
            if(feet.overlaps(r))
                return true;
        }
        return false;
    }

    public Point findNearestExit(Player player, Maze maze) {
        Point best = null;
        float bestD = Float.MAX_VALUE;

        float px = player.getX() + 16f;
        float py = player.getY() + 32f;

        for (Point e : maze.getExitCells()) {
            float ex = (e.x + 0.5f) * maze.getTileSize();
            float ey = (e.y + 0.5f) * maze.getTileSize();
            float dx = ex - px;
            float dy = ey - py;
            float d = dx * dx + dy * dy;

            if (d < bestD) {
                bestD = d;
                best = e;
            }
        }
        return best;
    }

    /**
     * Computes the maze tile currently occupied by the player.
     *
     * @param player Player instance
     * @param maze Maze reference
     * @return Tile coordinates of the player
     */
    public Point getPlayerTile(Player player, Maze maze) {
        int x = (int)((player.getX() + 16f) / maze.getTileSize());
        int y = (int)((player.getY() + 20f) / maze.getTileSize());
        return new Point(x, y);
    }
}

