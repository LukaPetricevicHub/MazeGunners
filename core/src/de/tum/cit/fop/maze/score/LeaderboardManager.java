package de.tum.cit.fop.maze.score;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.utils.Json;
import java.util.ArrayList;
import java.util.Collections;

public class LeaderboardManager {
    private ArrayList<ScoreEntry> scores;
    private final String FILE_PATH = "scores.json";

    public LeaderboardManager() {
        this.scores = new ArrayList<>();
        loadScores();
    }

    public void addScore(String name, int score) {
        scores.add(new ScoreEntry(name, score));
        // Sort and keep only Top 10
        Collections.sort(scores);
        if (scores.size() > 10) {
            scores.remove(scores.size() - 1);
        }
        saveScores();
    }

    public ArrayList<ScoreEntry> getScores() {
        return scores;
    }

    private void saveScores() {
        Json json = new Json();
        FileHandle file = Gdx.files.local(FILE_PATH);
        file.writeString(json.toJson(scores), false);
    }

    @SuppressWarnings("unchecked")
    private void loadScores() {
        FileHandle file = Gdx.files.local(FILE_PATH);
        if (file.exists()) {
            Json json = new Json();
            try {
                scores = json.fromJson(ArrayList.class, ScoreEntry.class, file.readString());
            } catch (Exception e) {
                e.printStackTrace();
                scores = new ArrayList<>();
            }
        }
    }
}