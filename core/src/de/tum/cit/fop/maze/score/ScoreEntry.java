package de.tum.cit.fop.maze.score;

/**
 * Represents a single leaderboard entry.
 *
 * Stores player name, score, and an optional extra value
 * (e.g. floor reached in endless mode).
 */
public class ScoreEntry implements Comparable<ScoreEntry> {
    private String playerName;
    private int score;
    private int extra;

    /**
     * No-argument constructor required for JSON deserialization.
     */
    public ScoreEntry() {}

    /**
     * Creates a score entry without an extra value.
     *
     * @param name Player name
     * @param score Achieved score
     */
    public ScoreEntry(String name, int score) {
        this.playerName = name;
        this.score = score;
        this.extra = 0;
    }

    /**
     * Creates a score entry with an additional value.
     *
     * @param name Player name
     * @param score Achieved score
     * @param extra Additional value (e.g. floor reached)
     */
    public ScoreEntry(String name, int score, int extra) {
        this.playerName = name;
        this.score = score;
        this.extra = extra;
    }

    /**
     * Returns the stored values of this score entry.
     *
     * @return Player name, score, or extra value depending on the getter
     */
    public String getPlayerName() { return playerName; }
    public int getScore() { return score; }
    public int getExtra() { return extra; }

    /**
     * Compares two score entries in descending order.
     *
     * @param other Score entry to compare against
     * @return Comparison result for sorting
     */
    @Override
    public int compareTo(ScoreEntry other) {
        return Integer.compare(other.score, this.score);
    }
}
