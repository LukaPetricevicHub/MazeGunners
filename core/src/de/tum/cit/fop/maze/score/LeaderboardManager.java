package de.tum.cit.fop.maze.score;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.utils.Json;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * Manages leaderboard data for all game modes and levels.
 *
 * Handles loading, saving, and sorting score entries and
 * persists leaderboard data using JSON serialization.
 */
public class LeaderboardManager {
    private Map<String, ArrayList<ScoreEntry>> boards;
    private final String FILE_PATH = "scores.json";

    /**
     * Creates a new leaderboard manager.
     *
     * Loads stored scores from disk and performs any required
     * initialization or cleanup of leaderboard data.
     */
    public LeaderboardManager() {
        this.boards = new HashMap<>();
        loadScores();
        if (boards.remove("level-1") != null) {
            saveScores();
        }
    }

    /**
     * Adds a score entry to the specified leaderboard.
     *
     * The leaderboard is kept sorted in descending order and
     * limited to the top 10 entries.
     *
     * @param boardKey Identifier of the leaderboard
     * @param name Player name
     * @param score Achieved score
     */
    public void addScore(String boardKey, String name, int score) {
        ArrayList<ScoreEntry> scores = boards.computeIfAbsent(boardKey, k -> new ArrayList<>());
        scores.add(new ScoreEntry(name, score));
        Collections.sort(scores);
        if (scores.size() > 10) scores.remove(scores.size() - 1);
        saveScores();
    }

    /**
     * Adds a score entry with an additional value to the specified leaderboard.
     *
     * Used for modes that require extra data such as floors reached.
     * The leaderboard is kept sorted and limited to the top 10 entries.
     *
     * @param boardKey Identifier of the leaderboard
     * @param name Player name
     * @param score Achieved score
     * @param extra Additional value (e.g. floor reached)
     */
    public void addScore(String boardKey, String name, int score, int extra) {
        ArrayList<ScoreEntry> scores = boards.computeIfAbsent(boardKey, k -> new ArrayList<>());
        scores.add(new ScoreEntry(name, score, extra));
        Collections.sort(scores);
        if (scores.size() > 10) scores.remove(scores.size() - 1);
        saveScores();
    }

    /**
     * Returns the score entries for a given leaderboard.
     *
     * @param boardKey Identifier of the leaderboard
     * @return List of score entries (may be empty)
     */
    public ArrayList<ScoreEntry> getScores(String boardKey) {
        return boards.getOrDefault(boardKey, new ArrayList<>());
    }

    /**
     * Saves all leaderboard data to disk using JSON serialization.
     */
    private void saveScores() {
        Json json = new Json();
        FileHandle file = Gdx.files.local(FILE_PATH);
        file.writeString(json.toJson(boards), false);
    }

    /**
     * Loads leaderboard data from disk.
     *
     * Supports legacy formats and initializes empty data
     * if loading fails or no file is present.
     */
    @SuppressWarnings("unchecked")
    private void loadScores() {
        FileHandle file = Gdx.files.local(FILE_PATH);
        if (file.exists()) {
            Json json = new Json();
            try {
                String raw = file.readString();
                if (raw.trim().startsWith("[")) {
                    ArrayList<ScoreEntry> legacy = json.fromJson(ArrayList.class, ScoreEntry.class, raw);
                    boards.put("level-1", legacy == null ? new ArrayList<>() : legacy);
                } else {
                    boards = json.fromJson(HashMap.class, ArrayList.class, raw);
                    if (boards == null) boards = new HashMap<>();
                }
            } catch (Exception e) {
                e.printStackTrace();
                boards = new HashMap<>();
            }
        }
    }
}
