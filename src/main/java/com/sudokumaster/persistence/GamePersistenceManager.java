package com.sudokumaster.persistence;

import com.sudokumaster.domain.Difficulty;
import com.sudokumaster.domain.FixedCell;
import com.sudokumaster.domain.GameState;
import com.sudokumaster.domain.HintCell;
import com.sudokumaster.domain.ScoreRecord;
import com.sudokumaster.domain.SudokuBoard;
import com.sudokumaster.domain.SudokuCell;
import com.sudokumaster.domain.UserCell;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Handles file persistence for high scores ({@link ArrayList} of {@link ScoreRecord})
 * and unfinished puzzle progress ({@link GameState}) in JSON format using try-with-resources.
 */
public class GamePersistenceManager {

    private static final Path DEFAULT_DATA_DIR = Paths.get("data");
    private static final Path DEFAULT_SCORES_FILE = DEFAULT_DATA_DIR.resolve("highscores.json");
    private static final Path DEFAULT_SAVE_FILE = DEFAULT_DATA_DIR.resolve("saved_game.json");

    private final Path scoresFilePath;
    private final Path saveFilePath;

    public GamePersistenceManager() {
        this(DEFAULT_SCORES_FILE, DEFAULT_SAVE_FILE);
    }

    public GamePersistenceManager(Path scoresFilePath, Path saveFilePath) {
        this.scoresFilePath = scoresFilePath;
        this.saveFilePath = saveFilePath;
    }

    /**
     * Saves the leaderboard list of {@link ScoreRecord} entries in JSON format using try-with-resources.
     */
    public void saveHighScores(List<ScoreRecord> scores) throws IOException {
        ensureParentDirectory(scoresFilePath);
        List<ScoreRecord> sorted = new ArrayList<>(scores);
        Collections.sort(sorted);

        try (BufferedWriter writer = Files.newBufferedWriter(scoresFilePath, StandardCharsets.UTF_8)) {
            writer.write("[\n");
            for (int i = 0; i < sorted.size(); i++) {
                ScoreRecord r = sorted.get(i);
                writer.write("  {\n");
                writer.write("    \"playerName\": \"" + escapeJson(r.getPlayerName()) + "\",\n");
                writer.write("    \"score\": " + r.getScore() + ",\n");
                writer.write("    \"completionSeconds\": " + r.getCompletionSeconds() + ",\n");
                writer.write("    \"mistakes\": " + r.getMistakes() + ",\n");
                writer.write("    \"difficulty\": \"" + r.getDifficulty().name() + "\",\n");
                writer.write("    \"completedAt\": \"" + r.getFormattedDate() + "\"\n");
                writer.write(i < sorted.size() - 1 ? "  },\n" : "  }\n");
            }
            writer.write("]\n");
        }
    }

    /**
     * Loads the leaderboard as an {@link ArrayList} of {@link ScoreRecord} from the JSON file
     * using try-with-resources.
     */
    public ArrayList<ScoreRecord> loadHighScores() throws IOException {
        ArrayList<ScoreRecord> records = new ArrayList<>();
        if (!Files.exists(scoresFilePath)) {
            return records;
        }

        StringBuilder jsonBuilder = new StringBuilder();
        try (BufferedReader reader = Files.newBufferedReader(scoresFilePath, StandardCharsets.UTF_8)) {
            String line;
            while ((line = reader.readLine()) != null) {
                jsonBuilder.append(line).append('\n');
            }
        }

        String json = jsonBuilder.toString();
        Pattern objectPattern = Pattern.compile("\\{([^}]+)\\}");
        Matcher matcher = objectPattern.matcher(json);
        while (matcher.find()) {
            String block = matcher.group(1);
            String playerName = extractString(block, "playerName", "Player");
            int score = extractInt(block, "score", 0);
            int completionSeconds = extractInt(block, "completionSeconds", 0);
            int mistakes = extractInt(block, "mistakes", 0);
            String diffStr = extractString(block, "difficulty", "EASY");
            String dateStr = extractString(block, "completedAt", LocalDateTime.now().format(ScoreRecord.TIMESTAMP_FORMATTER));

            Difficulty difficulty;
            try {
                difficulty = Difficulty.valueOf(diffStr);
            } catch (IllegalArgumentException e) {
                difficulty = Difficulty.EASY;
            }
            LocalDateTime completedAt;
            try {
                completedAt = LocalDateTime.parse(dateStr, ScoreRecord.TIMESTAMP_FORMATTER);
            } catch (Exception e) {
                completedAt = LocalDateTime.now();
            }
            records.add(new ScoreRecord(playerName, score, completionSeconds, mistakes, difficulty, completedAt));
        }

        Collections.sort(records);
        return records;
    }

    /**
     * Saves an unfinished {@link GameState} (board cells, elapsed time, mistakes, score, difficulty)
     * in JSON format using try-with-resources.
     */
    public void saveGameState(GameState state) throws IOException {
        ensureParentDirectory(saveFilePath);
        SudokuBoard board = state.getBoard();

        try (BufferedWriter writer = Files.newBufferedWriter(saveFilePath, StandardCharsets.UTF_8)) {
            writer.write("{\n");
            writer.write("  \"playerName\": \"" + escapeJson(state.getPlayerName()) + "\",\n");
            writer.write("  \"difficulty\": \"" + state.getDifficulty().name() + "\",\n");
            writer.write("  \"mistakes\": " + state.getMistakes() + ",\n");
            writer.write("  \"maxMistakes\": " + state.getMaxMistakes() + ",\n");
            writer.write("  \"elapsedSeconds\": " + state.getElapsedSeconds() + ",\n");
            writer.write("  \"score\": " + state.getScore() + ",\n");
            writer.write("  \"hintsRemaining\": " + state.getHintsRemaining() + ",\n");
            writer.write("  \"cells\": [\n");
            for (int r = 0; r < SudokuBoard.SIZE; r++) {
                for (int c = 0; c < SudokuBoard.SIZE; c++) {
                    SudokuCell cell = board.getCell(r, c);
                    boolean last = (r == SudokuBoard.SIZE - 1 && c == SudokuBoard.SIZE - 1);
                    writer.write(String.format(
                            "    {\"row\": %d, \"col\": %d, \"type\": \"%s\", \"value\": %d, \"solution\": %d}%s\n",
                            r, c, cell.getCellType(), cell.getValue(), cell.getSolutionValue(),
                            last ? "" : ","
                    ));
                }
            }
            writer.write("  ]\n");
            writer.write("}\n");
        }
    }

    /**
     * Loads a previously saved {@link GameState} from disk using try-with-resources.
     */
    public GameState loadGameState() throws IOException {
        if (!Files.exists(saveFilePath)) {
            throw new IOException("No saved game file found at: " + saveFilePath.toAbsolutePath());
        }

        StringBuilder jsonBuilder = new StringBuilder();
        try (BufferedReader reader = Files.newBufferedReader(saveFilePath, StandardCharsets.UTF_8)) {
            String line;
            while ((line = reader.readLine()) != null) {
                jsonBuilder.append(line).append('\n');
            }
        }

        String json = jsonBuilder.toString();
        String playerName = extractString(json, "playerName", "Player");
        String diffStr = extractString(json, "difficulty", "EASY");
        int mistakes = extractInt(json, "mistakes", 0);
        int maxMistakes = extractInt(json, "maxMistakes", GameState.DEFAULT_MAX_MISTAKES);
        int elapsedSeconds = extractInt(json, "elapsedSeconds", 0);
        int score = extractInt(json, "score", 0);
        int hintsRemaining = extractInt(json, "hintsRemaining", GameState.DEFAULT_INITIAL_HINTS);

        Difficulty difficulty;
        try {
            difficulty = Difficulty.valueOf(diffStr);
        } catch (IllegalArgumentException e) {
            difficulty = Difficulty.EASY;
        }

        SudokuCell[][] cells = new SudokuCell[SudokuBoard.SIZE][SudokuBoard.SIZE];
        Pattern cellPattern = Pattern.compile(
                "\\{\\s*\"row\"\\s*:\\s*(\\d+)\\s*,\\s*\"col\"\\s*:\\s*(\\d+)\\s*,\\s*\"type\"\\s*:\\s*\"([A-Z]+)\"\\s*,\\s*\"value\"\\s*:\\s*(\\d+)\\s*,\\s*\"solution\"\\s*:\\s*(\\d+)\\s*\\}"
        );
        Matcher matcher = cellPattern.matcher(json);
        int count = 0;
        while (matcher.find()) {
            int r = Integer.parseInt(matcher.group(1));
            int c = Integer.parseInt(matcher.group(2));
            String type = matcher.group(3);
            int val = Integer.parseInt(matcher.group(4));
            int sol = Integer.parseInt(matcher.group(5));

            SudokuCell cell = switch (type) {
                case "FIXED" -> new FixedCell(r, c, sol);
                case "HINT" -> new HintCell(r, c, sol);
                default -> new UserCell(r, c, val, sol);
            };
            cells[r][c] = cell;
            count++;
        }

        if (count != 81) {
            throw new IOException("Corrupted saved game file: expected 81 cells but found " + count);
        }

        SudokuBoard board = new SudokuBoard(cells);
        return new GameState(board, difficulty, playerName, mistakes, maxMistakes, elapsedSeconds, score, hintsRemaining);
    }

    public boolean hasSavedGame() {
        return Files.exists(saveFilePath);
    }

    public Path getScoresFilePath() {
        return scoresFilePath;
    }

    public Path getSaveFilePath() {
        return saveFilePath;
    }

    private void ensureParentDirectory(Path filePath) throws IOException {
        Path parent = filePath.getParent();
        if (parent != null && !Files.exists(parent)) {
            Files.createDirectories(parent);
        }
    }

    private static String escapeJson(String input) {
        if (input == null) {
            return "";
        }
        return input.replace("\\", "\\\\").replace("\"", "\\\"");
    }

    private static String extractString(String text, String key, String defaultVal) {
        Pattern p = Pattern.compile("\"" + Pattern.quote(key) + "\"\\s*:\\s*\"([^\"]*)\"");
        Matcher m = p.matcher(text);
        return m.find() ? m.group(1) : defaultVal;
    }

    private static int extractInt(String text, String key, int defaultVal) {
        Pattern p = Pattern.compile("\"" + Pattern.quote(key) + "\"\\s*:\\s*(-?\\d+)");
        Matcher m = p.matcher(text);
        if (m.find()) {
            try {
                return Integer.parseInt(m.group(1));
            } catch (NumberFormatException ignored) {
                return defaultVal;
            }
        }
        return defaultVal;
    }
}
