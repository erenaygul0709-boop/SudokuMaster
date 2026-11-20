package com.sudokumaster.domain;

import com.sudokumaster.exception.InvalidSudokuMoveException;

import java.awt.Color;

/**
 * Represents a pre-filled clue cell in the initial Sudoku puzzle.
 * Cannot be modified or erased by the player.
 */
public class FixedCell extends SudokuCell {

    private static final Color FIXED_TEXT_COLOR = new Color(30, 41, 59); // Slate-800 dark ink

    public FixedCell(int row, int col, int clueValue) {
        super(row, col, clueValue, clueValue);
        if (clueValue < 1 || clueValue > 9) {
            throw new IllegalArgumentException("FixedCell clue value must be between 1 and 9.");
        }
    }

    @Override
    public void setValue(int newValue) throws InvalidSudokuMoveException {
        throw new InvalidSudokuMoveException(
                String.format("Cell (%d, %d) is a fixed puzzle clue and cannot be modified.", getRow() + 1, getCol() + 1),
                getRow(), getCol(), newValue, false
        );
    }

    @Override
    public boolean isEditable() {
        return false;
    }

    @Override
    public Color getDisplayColor() {
        return FIXED_TEXT_COLOR;
    }

    @Override
    public String getCellType() {
        return "FIXED";
    }

    @Override
    public SudokuCell copy() {
        return new FixedCell(getRow(), getCol(), getValue());
    }
}
