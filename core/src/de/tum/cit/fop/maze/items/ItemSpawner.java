package de.tum.cit.fop.maze.items;

import de.tum.cit.fop.maze.map.Maze;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class ItemSpawner {
    public List<Collectable> spawnItems(Maze maze, int count) {
        List<Collectable> items = new ArrayList<>();
        Random random = new Random();
        int tileSize = (int) maze.getTileSize();

        for (int i = 0; i < count; i++) {
            int x, y;
            // Keep picking random spots until we find a floor tile
            do {
                x = random.nextInt(maze.getWidth());
                y = random.nextInt(maze.getHeight());
            } while (!maze.getTile(x, y).isWalkable());

            float worldX = x * tileSize;
            float worldY = y * tileSize;

            int type = random.nextInt(3);

            if (type == 0) items.add(new HeartItem(worldX, worldY));
            else if (type == 1) items.add(new SpeedItem(worldX, worldY));
            else items.add(new ShieldItem(worldX, worldY));
        }
        return items;
    }
}