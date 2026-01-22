package de.tum.cit.fop.maze.score;

public class ScoreEntry implements Comparable<ScoreEntry> {
    private String playerName;
    private int score;

    // Empty constructor for JSON serialization
    public ScoreEntry() {}

    public ScoreEntry(String name, int score) {
        this.playerName = name;
        this.score = score;
    }

    public String getPlayerName() { return playerName; }
    public int getScore() { return score; }

    // Sort descending (Highest score first)
    @Override
    public int compareTo(ScoreEntry other) {
        return Integer.compare(other.score, this.score);
    }
}