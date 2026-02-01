package de.tum.cit.fop.maze.util;

/**
 * Represents a single game achievement.
 *
 * An achievement has an identifier, a title, a description,
 * and a persistent unlocked state.
 */
public class Achievement {
    private final String id;
    private final String title;
    private final String description;
    private boolean unlocked;

    /**
     * Creates a new locked achievement.
     *
     * @param id Unique achievement identifier
     * @param title Display name
     * @param description Achievement description
     */
    public Achievement(String id, String title, String description) {
        this.id = id;
        this.title = title;
        this.description = description;
        this.unlocked = false;
    }

    // Getters & Setters
    public String getId() { return id; }
    public String getTitle() { return title; }
    public String getDescription() { return description; }
    public boolean isUnlocked() { return unlocked; }
    public void setUnlocked(boolean unlocked) { this.unlocked = unlocked; }
}