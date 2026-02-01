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

    /**
     * Retrieves the unique identifier of the achievement.
     *
     * @return unique achievement ID
     */
    public String getId() { return id; }

    /**
     * Retrieves the display title of the achievement.
     *
     * @return achievement title
     */
    public String getTitle() { return title; }

    /**
     * Retrieves the description text of the achievement.
     *
     * @return achievement description
     */
    public String getDescription() { return description; }

    /**
     * Indicates whether the achievement has been unlocked.
     *
     * @return true if unlocked, otherwise false
     */
    public boolean isUnlocked() { return unlocked; }

    /**
     * Sets the unlocked state of the achievement.
     *
     * @param unlocked true to mark as unlocked, false to mark as locked
     */
    public void setUnlocked(boolean unlocked) { this.unlocked = unlocked; }
}
