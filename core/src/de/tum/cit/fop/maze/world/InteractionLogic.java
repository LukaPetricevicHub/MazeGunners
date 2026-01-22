package de.tum.cit.fop.maze.world;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.math.Rectangle;
import de.tum.cit.fop.maze.MazeRunnerGame;
import de.tum.cit.fop.maze.map.Maze;
import de.tum.cit.fop.maze.map.Tiles;
import de.tum.cit.fop.maze.player.Player;

import java.awt.Point;

public class InteractionLogic {

    public boolean handleKeyPickup(Player player, Maze maze) {
        if (player.hasKey()) return false;

        Point tile = getPlayerTile(player, maze);
        if (maze.getTile(tile.x, tile.y) == Tiles.KEY) {
            maze.setTile(tile.x, tile.y, Tiles.FLOOR);
            player.giveKey();

            MazeRunnerGame game = (MazeRunnerGame) Gdx.app.getApplicationListener();
            game.getAchievementManager().unlock("collector");
            return true;
        }
        return false;
    }

    public boolean isPlayerOnExit(Player player, Maze maze) {
        Rectangle feet = new Rectangle(
                player.getX() + 4f,
                player.getY(),
                24f,
                20f
        );

        for (Point e : maze.getExitCells()) {
            Rectangle r = new Rectangle(
                    e.x * maze.getTileSize(),
                    e.y * maze.getTileSize(),
                    maze.getTileSize(),
                    maze.getTileSize()
            );
            if (feet.overlaps(r)) return true;
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

    public Point getPlayerTile(Player player, Maze maze) {
        int x = (int)((player.getX() + 16f) / maze.getTileSize());
        int y = (int)((player.getY() + 20f) / maze.getTileSize());
        return new Point(x, y);
    }
}

