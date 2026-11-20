package com.sudokumaster.exception;

/**
 * Custom domain exception thrown when a player attempts an invalid move in SudokuMaster.
 * Examples include:
 * - Overwriting an unmodifiable FixedCell or HintCell
 * - Entering a digit outside the valid 1..9 range or violating Sudoku row/column/box constraints
 * - Making a move after exceeding the maximum allowed mistake strikes (3/3)
 */
public class InvalidSudokuMoveException extends Exception {

    private final int row;
    private final int col;
    private final int attemptedValue;
    private final boolean strikeInduced;

    public InvalidSudokuMoveException(String message) {
        this(message, -1, -1, 0, false);
    }

    public InvalidSudokuMoveException(String message, int row, int col, int attemptedValue) {
        this(message, row, col, attemptedValue, false);
    }

    public InvalidSudokuMoveException(String message, int row, int col, int attemptedValue, boolean strikeInduced) {
        super(message);
        this.row = row;
        this.col = col;
        this.attemptedValue = attemptedValue;
        this.strikeInduced = strikeInduced;
    }

    public int getRow() {
        return row;
    }

    public int getCol() {
        return col;
    }

    public int getAttemptedValue() {
        return attemptedValue;
    }

    public boolean isStrikeInduced() {
        return strikeInduced;
    }
}
