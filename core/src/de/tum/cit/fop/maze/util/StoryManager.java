package de.tum.cit.fop.maze.util;

public final class StoryManager {

    private StoryManager() {
        // Utility class
    }

    public static String getIntroTitle(int level) {
        return switch (level) {
            case 1 -> "WAKING PROTOCOL";
            case 2 -> "SUBLEVEL ECHOES";
            case 3 -> "CONTAINMENT BREACH";
            case 4 -> "DEEPENING SIGNAL";
            case 5 -> "OMEGA PROTOCOL";
            default -> "";
        };
    }

    public static String getIntroText(int level) {
        return switch (level) {
            case 1 -> "Cold light. Blank memory. Your name is scrubbed, "
                    + "your mission overwritten. The only command left: escape.";
            case 2 -> "The corridors remember you, even if you do not. "
                    + "Each door you open feels like a file you were never meant to read.";
            case 3 -> "Containment is down. Slime blooms in the vents and crawls into the walls. "
                    + "The alarms lie; the breach is already inside.";
            case 4 -> "A signal pulses from below, too steady to be random. "
                    + "Someone wants you to keep moving.";
            case 5 -> "Omega protocol engaged. The last sentinel is awake. "
                    + "If you want your memory back, you take it from the core.";
            default -> "";
        };
    }
}
