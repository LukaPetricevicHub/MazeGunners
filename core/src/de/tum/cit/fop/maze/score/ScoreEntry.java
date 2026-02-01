package de.tum.cit.fop.maze.score;

public class ScoreEntry implements Comparable<ScoreEntry> {
    private String playerName;
    private int score;
    private int extra;

    // Empty constructor for JSON serialization
    public ScoreEntry() {}

    public ScoreEntry(String name, int score) {
        this.playerName = name;
        this.score = score;
        this.extra = 0;
    }

    public ScoreEntry(String name, int score, int extra) {
        this.playerName = name;
        this.score = score;
        this.extra = extra;
    }

    public String getPlayerName() { return playerName; }
    public int getScore() { return score; }
    public int getExtra() { return extra; }

    // Sort descending (Highest score first)
    @Override
    public int compareTo(ScoreEntry other) {
        return Integer.compare(other.score, this.score);
    }
}
