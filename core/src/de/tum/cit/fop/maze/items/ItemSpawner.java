package de.tum.cit.fop.maze.items;

import de.tum.cit.fop.maze.map.Maze;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class ItemSpawner {

    private final Random random = new Random();

    public List<AbstractCollectable> spawnItems(Maze maze, int count) {
        List<AbstractCollectable> items = new ArrayList<>();
        int tileSize = (int) maze.getTileSize();

        // --- 1. SPAWN ONE GUARANTEED GUN ---
        spawnSingleItem(maze, items, 3); // Type 3 = Gun

        // --- 2. SPAWN REMAINING ITEMS RANDOMLY ---
        // We subtract 1 because we already spawned the gun
        for (int i = 0; i < count - 1; i++) {
            // Random type between 0 and 2 (Heart, Speed, Shield)
            // We exclude 3 so we don't spawn extra guns
            int type = random.nextInt(3);
            spawnSingleItem(maze, items, type);
        }

        return items;
    }

    // Helper method to find a safe spot and add the item
    private void spawnSingleItem(Maze maze, List<AbstractCollectable> items, int type) {
        int x, y;
        int maxAttempts = 20; // Prevent infinite loop if map is full

        do {
            x = random.nextInt(maze.getWidth());
            y = random.nextInt(maze.getHeight());
            maxAttempts--;
        } while (maze.isWallCell(x, y) && maxAttempts > 0);

        // If we failed to find a spot, just return (skip this item)
        if (maze.isWallCell(x, y)) return;

        float tileSize = maze.getTileSize();
        float worldX = x * tileSize;
        float worldY = y * tileSize;

        switch (type) {
            case 0 -> items.add(new HeartItem(worldX, worldY));
            case 1 -> items.add(new SpeedItem(worldX, worldY));
            case 2 -> items.add(new ShieldItem(worldX, worldY));
            case 3 -> items.add(new GunCollectable(worldX, worldY));
        }
    }
}