package com.sudokumaster.domain;

/**
 * Represents the difficulty tiers in SudokuMaster.
 */
public enum Difficulty {
    EASY("Easy", 45, 10),
    MEDIUM("Medium", 36, 20),
    HARD("Hard", 28, 35);

    private final String displayName;
    private final int initialClues;
    private final int pointsPerMove;

    Difficulty(String displayName, int initialClues, int pointsPerMove) {
        this.displayName = displayName;
        this.initialClues = initialClues;
        this.pointsPerMove = pointsPerMove;
    }

    public String getDisplayName() {
        return displayName;
    }

    public int getInitialClues() {
        return initialClues;
    }

    public int getPointsPerMove() {
        return pointsPerMove;
    }
}
