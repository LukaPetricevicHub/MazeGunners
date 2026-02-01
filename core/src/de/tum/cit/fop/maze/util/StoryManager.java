package de.tum.cit.fop.maze.util;

/**
 * Provides narrative text and story item metadata for each level.
 */
public final class StoryManager {

    /**
     * Prevents instantiation of utility class.
     */
    private StoryManager() {
        
    }

    /**
     * Retrieves the intro title for a level.
     *
     * @param level level number
     * @return intro title string, or empty string if not defined
     */
    public static String getIntroTitle(int level) {
        return switch (level) {
            case 1 -> "SYSTEM BOOT";
            case 2 -> "RESTRICTED AREA";
            case 3 -> "TOXIC ZONE";
            case 4 -> "HIVE CORE";
            case 5 -> "FINAL SHOWDOWN";
            default -> "";
        };
    }

    /**
     * Retrieves the intro narrative text for a level.
     *
     * @param level level number
     * @return intro text string, or empty string if not defined
     */
    public static String getIntroText(int level) {
        return switch (level) {
            case 1 -> "My head hurts... I don't remember how I got here. This facility looks abandoned, but I hear machines running. "
                    + "I need to find the Memory Chip to recover my identity and the Key to escape this floor.";
            case 2 -> "I remember fragments now. I was a test subject. This area is under strict lockdown. "
                    + "The elevators are frozen. I must find the Security Admin Card to override the system.";
            case 3 -> "The air is thick with toxins here. The experiments... they mutated. It's unsafe to breathe for too long. "
                    + "I need to locate the Blue Antidote Vial before I move deeper.";
            case 4 -> "I am getting closer to the center. The drone activity is intense. They are protecting something. "
                    + "I need to steal the Master Processor to disable their hive mind network.";
            case 5 -> "This is it. The Central Chamber. The Source is here—a massive guardian guarding the exit. "
                    + "I cannot leave until I defeat it and retrieve the Omega Encryption Key. It's time to end this.";
            default -> "";
        };
    }

    /**
     * Retrieves the story item name required for a level.
     *
     * @param level level number
     * @return story item name, or empty string if not defined
     */
    public static String getStoryItemName(int level) {
        return switch (level) {
            case 1 -> "Memory Chip";
            case 2 -> "Security Admin Card";
            case 3 -> "Blue Antidote Vial";
            case 4 -> "Master Processor";
            case 5 -> "Omega Encryption Key";
            default -> "";
        };
    }
}
