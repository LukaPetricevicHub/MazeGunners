package de.tum.cit.fop.maze.items;

import de.tum.cit.fop.maze.player.Player;

/**
 * Collectable story item required to complete a level.
 */
public class StoryItem extends Collectable {
    /** Display name of the story item. */
    private final String itemName;

    /**
     * Creates a story item at the given position.
     *
     * @param x World x-coordinate
     * @param y World y-coordinate
     * @param itemName Display name for the item
     */
    public StoryItem(float x, float y, String itemName) {
        super(x, y, resolveTexture(itemName));
        this.itemName = itemName == null ? "" : itemName;
    }

    /**
     * Retrieves the display name of the story item.
     *
     * @return Item name
     */
    public String getItemName() {
        return itemName;
    }

    /**
     * Applies the story item effect to the player and marks it collected.
     *
     * @param player Player who collected the item
     */
    @Override
    public void applyEffect(Player player) {
        player.giveStoryItem(itemName);
        player.notifyHud("Acquired: " + itemName + "!");
        collect();
    }

    /**
     * Resolves the texture path for a given story item name.
     *
     * @param name Story item name
     * @return Texture file name
     */
    private static String resolveTexture(String name) {
        if (name == null) return "skull.png";
        return switch (name) {
            case "Memory Chip" -> "memory.png";
            case "Security Admin Card" -> "pass.png";
            case "Blue Antidote Vial" -> "antidote.png";
            case "Master Processor" -> "processor.png";
            default -> "skull.png";
        };
    }
}
