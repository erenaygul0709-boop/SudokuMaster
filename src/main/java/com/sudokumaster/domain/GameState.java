package com.sudokumaster.domain;

import java.util.Objects;

/**
 * Encapsulates the complete runtime state of an active Sudoku session.
 */
public class GameState {

    public static final int DEFAULT_MAX_MISTAKES = 3;
    public static final int DEFAULT_INITIAL_HINTS = 3;

    private final SudokuBoard board;
    private final Difficulty difficulty;
    private final int maxMistakes;
    private int mistakes;
    private int elapsedSeconds;
    private int score;
    private int hintsRemaining;
    private String playerName;

    public GameState(SudokuBoard board, Difficulty difficulty, String playerName) {
        this(board, difficulty, playerName, 0, DEFAULT_MAX_MISTAKES, 0, 0, DEFAULT_INITIAL_HINTS);
    }

    public GameState(SudokuBoard board, Difficulty difficulty, String playerName,
                     int mistakes, int maxMistakes, int elapsedSeconds, int score, int hintsRemaining) {
        this.board = Objects.requireNonNull(board, "Board cannot be null.");
        this.difficulty = Objects.requireNonNull(difficulty, "Difficulty cannot be null.");
        if (maxMistakes < 1) {
            throw new IllegalArgumentException("Max mistakes must be at least 1.");
        }
        if (mistakes < 0 || elapsedSeconds < 0 || score < 0 || hintsRemaining < 0) {
            throw new IllegalArgumentException("Game metrics cannot be negative.");
        }
        this.playerName = (playerName == null || playerName.trim().isEmpty()) ? "Player" : playerName.trim();
        this.mistakes = mistakes;
        this.maxMistakes = maxMistakes;
        this.elapsedSeconds = elapsedSeconds;
        this.score = score;
        this.hintsRemaining = hintsRemaining;
    }

    public SudokuBoard getBoard() {
        return board;
    }

    public Difficulty getDifficulty() {
        return difficulty;
    }

    public int getMistakes() {
        return mistakes;
    }

    public void setMistakes(int mistakes) {
        if (mistakes < 0) {
            throw new IllegalArgumentException("Mistakes cannot be negative.");
        }
        this.mistakes = mistakes;
    }

    public void incrementMistakes() {
        this.mistakes++;
    }

    public int getMaxMistakes() {
        return maxMistakes;
    }

    public boolean isGameOverByMistakes() {
        return mistakes >= maxMistakes;
    }

    public int getElapsedSeconds() {
        return elapsedSeconds;
    }

    public void setElapsedSeconds(int elapsedSeconds) {
        if (elapsedSeconds < 0) {
            throw new IllegalArgumentException("Elapsed seconds cannot be negative.");
        }
        this.elapsedSeconds = elapsedSeconds;
    }

    public void incrementElapsedSeconds() {
        this.elapsedSeconds++;
    }

    public String getFormattedTimer() {
        int mins = elapsedSeconds / 60;
        int secs = elapsedSeconds % 60;
        return String.format("%02d:%02d", mins, secs);
    }

    public int getScore() {
        return score;
    }

    public void setScore(int score) {
        this.score = Math.max(0, score);
    }

    public void addScore(int delta) {
        this.score = Math.max(0, this.score + delta);
    }

    public int getHintsRemaining() {
        return hintsRemaining;
    }

    public void setHintsRemaining(int hintsRemaining) {
        if (hintsRemaining < 0) {
            throw new IllegalArgumentException("Hints remaining cannot be negative.");
        }
        this.hintsRemaining = hintsRemaining;
    }

    public void decrementHints() {
        if (hintsRemaining > 0) {
            hintsRemaining--;
        }
    }

    public String getPlayerName() {
        return playerName;
    }

    public void setPlayerName(String playerName) {
        if (playerName != null && !playerName.trim().isEmpty()) {
            this.playerName = playerName.trim();
        }
    }
}
