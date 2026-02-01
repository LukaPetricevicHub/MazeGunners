package de.tum.cit.fop.maze.score;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.utils.Json;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class LeaderboardManager {
    private Map<String, ArrayList<ScoreEntry>> boards;
    private final String FILE_PATH = "scores.json";

    public LeaderboardManager() {
        this.boards = new HashMap<>();
        loadScores();
        // Reset level-1 leaderboard as requested
        if (boards.remove("level-1") != null) {
            saveScores();
        }
    }

    public void addScore(String boardKey, String name, int score) {
        ArrayList<ScoreEntry> scores = boards.computeIfAbsent(boardKey, k -> new ArrayList<>());
        scores.add(new ScoreEntry(name, score));
        Collections.sort(scores);
        if (scores.size() > 10) scores.remove(scores.size() - 1);
        saveScores();
    }

    public void addScore(String boardKey, String name, int score, int extra) {
        ArrayList<ScoreEntry> scores = boards.computeIfAbsent(boardKey, k -> new ArrayList<>());
        scores.add(new ScoreEntry(name, score, extra));
        Collections.sort(scores);
        if (scores.size() > 10) scores.remove(scores.size() - 1);
        saveScores();
    }

    public ArrayList<ScoreEntry> getScores(String boardKey) {
        return boards.getOrDefault(boardKey, new ArrayList<>());
    }

    private void saveScores() {
        Json json = new Json();
        FileHandle file = Gdx.files.local(FILE_PATH);
        file.writeString(json.toJson(boards), false);
    }

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
