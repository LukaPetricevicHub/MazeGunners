package de.tum.cit.fop.maze.items;

import de.tum.cit.fop.maze.map.Maze;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class ItemSpawner {

    private final Random random = new Random();

    public List<Collectable> spawnItems(Maze maze, int count) {
        List<Collectable> items = new ArrayList<>();

        // 1) GUARANTEED GUN
        spawnSingleItem(maze, items, 3); // 3 = Gun

        // 2) REMAINING ITEMS (no extra guns, but include coins if you want)
        for (int i = 0; i < count - 1; i++) {
            // 0..4 : Heart, Speed, Shield, (skip gun), Coin
            int type = random.nextInt(4); // 0..3 (Heart/Speed/Shield/Coin)
            spawnSingleItem(maze, items, type == 3 ? 4 : type); // map 3 -> coin
        }

        return items;
    }

    private void spawnSingleItem(Maze maze, List<Collectable> items, int type) {
        int x, y;
        int maxAttempts = 200;

        do {
            x = random.nextInt(maze.getWidth());
            y = random.nextInt(maze.getHeight());
            maxAttempts--;
        } while ((maze.isWallCell(x, y)) && maxAttempts > 0);

        if (maze.isWallCell(x, y)) return;

        float tileSize = maze.getTileSize();
        float worldX = x * tileSize;
        float worldY = y * tileSize;

        switch (type) {
            case 0 -> items.add(new HeartItem(worldX, worldY));
            case 1 -> items.add(new SpeedItem(worldX, worldY));
            case 2 -> items.add(new ShieldItem(worldX, worldY));
            case 3 -> items.add(new GunCollectable(worldX, worldY));     // gun
            case 4 -> items.add(new Coin(worldX, worldY, 1));            // coin
        }
    }
}
