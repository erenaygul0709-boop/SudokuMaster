package com.sudokumaster.domain;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.Objects;

/**
 * Immutable domain record representing a completed Sudoku game entry on the leaderboard.
 * Implements {@link Comparable} for natural ordering (highest score first, then fastest completion time)
 * and provides secondary {@link Comparator} instances for sorting by difficulty/date/time.
 */
public class ScoreRecord implements Comparable<ScoreRecord> {

    public static final DateTimeFormatter TIMESTAMP_FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    /**
     * Secondary comparator sorting records by difficulty (Hard -> Medium -> Easy), then fastest time.
     */
    public static final Comparator<ScoreRecord> BY_DIFFICULTY_AND_TIME =
            Comparator.comparing(ScoreRecord::getDifficulty, Comparator.reverseOrder())
                    .thenComparingInt(ScoreRecord::getCompletionSeconds)
                    .thenComparing(Comparator.comparingInt(ScoreRecord::getScore).reversed());

    /**
     * Secondary comparator sorting records chronologically (most recent first).
     */
    public static final Comparator<ScoreRecord> BY_DATE_DESCENDING =
            Comparator.comparing(ScoreRecord::getCompletedAt).reversed()
                    .thenComparing(Comparator.naturalOrder());

    /**
     * Secondary comparator sorting records purely by fastest completion time ascending.
     */
    public static final Comparator<ScoreRecord> BY_FASTEST_TIME =
            Comparator.comparingInt(ScoreRecord::getCompletionSeconds)
                    .thenComparing(Comparator.comparingInt(ScoreRecord::getScore).reversed());

    private final String playerName;
    private final int score;
    private final int completionSeconds;
    private final int mistakes;
    private final Difficulty difficulty;
    private final LocalDateTime completedAt;

    public ScoreRecord(String playerName, int score, int completionSeconds,
                       int mistakes, Difficulty difficulty, LocalDateTime completedAt) {
        if (playerName == null || playerName.trim().isEmpty()) {
            throw new IllegalArgumentException("Player name cannot be null or blank.");
        }
        if (score < 0) {
            throw new IllegalArgumentException("Score cannot be negative.");
        }
        if (completionSeconds < 0) {
            throw new IllegalArgumentException("Completion time cannot be negative.");
        }
        if (mistakes < 0) {
            throw new IllegalArgumentException("Mistakes cannot be negative.");
        }
        this.playerName = playerName.trim();
        this.score = score;
        this.completionSeconds = completionSeconds;
        this.mistakes = mistakes;
        this.difficulty = Objects.requireNonNull(difficulty, "Difficulty cannot be null.");
        this.completedAt = Objects.requireNonNull(completedAt, "CompletedAt timestamp cannot be null.");
    }

    public String getPlayerName() {
        return playerName;
    }

    public int getScore() {
        return score;
    }

    public int getCompletionSeconds() {
        return completionSeconds;
    }

    public int getMistakes() {
        return mistakes;
    }

    public Difficulty getDifficulty() {
        return difficulty;
    }

    public LocalDateTime getCompletedAt() {
        return completedAt;
    }

    public String getFormattedTime() {
        int mins = completionSeconds / 60;
        int secs = completionSeconds % 60;
        return String.format("%02d:%02d", mins, secs);
    }

    public String getFormattedDate() {
        return completedAt.format(TIMESTAMP_FORMATTER);
    }

    /**
     * Natural ordering: Highest score first; ties broken by fastest completion time (ascending),
     * then fewer mistakes (ascending).
     */
    @Override
    public int compareTo(ScoreRecord other) {
        int scoreCompare = Integer.compare(other.score, this.score);
        if (scoreCompare != 0) {
            return scoreCompare;
        }
        int timeCompare = Integer.compare(this.completionSeconds, other.completionSeconds);
        if (timeCompare != 0) {
            return timeCompare;
        }
        return Integer.compare(this.mistakes, other.mistakes);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ScoreRecord that)) return false;
        return score == that.score
                && completionSeconds == that.completionSeconds
                && mistakes == that.mistakes
                && Objects.equals(playerName, that.playerName)
                && difficulty == that.difficulty
                && Objects.equals(completedAt, that.completedAt);
    }

    @Override
    public int hashCode() {
        return Objects.hash(playerName, score, completionSeconds, mistakes, difficulty, completedAt);
    }

    @Override
    public String toString() {
        return String.format("%s | %s | Score: %d | Time: %s | Mistakes: %d | Date: %s",
                playerName, difficulty.getDisplayName(), score, getFormattedTime(), mistakes, getFormattedDate());
    }
}
