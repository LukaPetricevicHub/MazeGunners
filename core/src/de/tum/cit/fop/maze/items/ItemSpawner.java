package de.tum.cit.fop.maze.items;

import de.tum.cit.fop.maze.map.Maze;
import de.tum.cit.fop.maze.map.Tiles;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Spawns collectable items at random walkable positions in a maze.
 *
 * Items are placed only on free floor tiles and never overlap.
 * A gun is always spawned, followed by other items and coins.
 */
public class ItemSpawner {

    /** Random generator used for item placement and coin values. */
    private final Random random = new Random();

    /** Possible coin values for weighted selection. */
    private static final int[] COIN_VALUES  = { 5, 20, 50};
    /** Weights aligned with {@link #COIN_VALUES}. */
    private static final int[] COIN_WEIGHTS = { 80, 16, 4}; // sums to 100

    /**
     * Spawns items with a default number of coins.
     *
     * @param maze  the maze where items are placed
     * @param count number of non-coin items to spawn
     * @return list of spawned collectable items
     */
    public List<Collectable> spawnItems(Maze maze, int count) {
        int defaultCoinCount = Math.max(6, count * 2);
        return spawnItems(maze, count, defaultCoinCount);
    }

    /**
     * Spawns items with a custom number of coins.
     *
     * A gun is always spawned first, followed by random items
     * (heart, speed, shield) and coins.
     *
     * @param maze      the maze where items are placed
     * @param count     number of non-coin items to spawn
     * @param coinCount number of coins to spawn
     * @return list of spawned collectable items
     */
    public List<Collectable> spawnItems(Maze maze, int count, int coinCount) {
        List<Collectable> items = new ArrayList<>();

        // 1) GUARANTEED GUN
        spawnSingleItem(maze, items, 4);

        // 2) OTHER ITEMS
        for (int i = 0; i < Math.max(0, count - 1); i++) {
            int type = random.nextInt(4); // heart/speed/shield/ammo
            spawnSingleItem(maze, items, type);
        }

        // Extra ammo pickups
        int extraAmmo = Math.max(2, count);
        for (int i = 0; i < extraAmmo; i++) {
            spawnSingleItem(maze, items, 3);
        }

        // 3) COINS
        for (int i = 0; i < coinCount; i++) {
            spawnSingleItem(maze, items, 5);
        }

        return items;
    }

    /**
     * Spawns a single item of the given type on a random free tile.
     *
     * @param maze Maze to place items in
     * @param items List to append the item to
     * @param type Item type id to spawn
     */
    private void spawnSingleItem(Maze maze, List<Collectable> items, int type) {
        int x, y;
        int attempts = 300;

        do {
            x = random.nextInt(maze.getWidth());
            y = random.nextInt(maze.getHeight());
            attempts--;
        } while ((maze.isWallCell(x, y)
                || maze.getTile(x, y) == Tiles.EXIT
                || maze.getTile(x, y) == Tiles.SPAWN
                || isExitCell(maze, x, y)
                || tileUsed(items, maze, x, y)) && attempts > 0);

        if (maze.isWallCell(x, y)
                || maze.getTile(x, y) == Tiles.EXIT
                || maze.getTile(x, y) == Tiles.SPAWN
                || isExitCell(maze, x, y)
                || tileUsed(items, maze, x, y)) return;

        float ts = maze.getTileSize();
        float wx = x * ts;
        float wy = y * ts;

        switch (type) {
            case 0 -> items.add(new HeartItem(wx, wy));
            case 1 -> items.add(new SpeedItem(wx, wy));
            case 2 -> items.add(new ShieldItem(wx, wy));
            case 3 -> items.add(new AmmoItem(wx, wy));
            case 4 -> items.add(new GunCollectable(wx, wy));
            case 5 -> items.add(new Coin(wx, wy, rollCoinValue()));
        }
    }

    /**
     * Returns a coin value based on predefined weights.
     *
     * @return Selected coin value
     */
    private int rollCoinValue() {
        int total = 0;
        for (int w : COIN_WEIGHTS) total += w;

        int r = random.nextInt(total);
        int running = 0;

        for (int i = 0; i < COIN_VALUES.length; i++) {
            running += COIN_WEIGHTS[i];
            if (r < running) return COIN_VALUES[i];
        }
        return 1;
    }

    /**
     * Checks whether a tile already contains an item.
     *
     * @param items Current item list
     * @param maze Maze used for tile size
     * @param tx Tile x-coordinate
     * @param ty Tile y-coordinate
     * @return True if the tile is already occupied
     */
    private boolean tileUsed(List<Collectable> items, Maze maze, int tx, int ty) {
        float ts = maze.getTileSize();
        float wx = tx * ts;
        float wy = ty * ts;

        for (Collectable c : items) {
            if (c.getBounds().x == wx && c.getBounds().y == wy) return true;
        }
        return false;
    }

    /**
     * Determines whether a tile matches any maze exit cell.
     *
     * @param maze Maze used for exit cells
     * @param tx Tile x-coordinate
     * @param ty Tile y-coordinate
     * @return True if the tile is an exit cell
     */
    private boolean isExitCell(Maze maze, int tx, int ty) {
        for (var p : maze.getExitCells()) {
            if (p.x == tx && p.y == ty) return true;
        }
        return false;
    }
}
