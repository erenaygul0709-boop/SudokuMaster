package com.sudokumaster.domain;

import com.sudokumaster.exception.InvalidSudokuMoveException;

import java.awt.Color;

/**
 * Represents a cell revealed via the game's Hint feature.
 * Displays in distinct emerald green and locks in the correct solution digit.
 */
public class HintCell extends SudokuCell {

    private static final Color HINT_TEXT_COLOR = new Color(5, 150, 105); // Emerald-600

    public HintCell(int row, int col, int solutionValue) {
        super(row, col, solutionValue, solutionValue);
    }

    @Override
    public void setValue(int newValue) throws InvalidSudokuMoveException {
        throw new InvalidSudokuMoveException(
                String.format("Cell (%d, %d) is a revealed hint cell and cannot be overwritten.", getRow() + 1, getCol() + 1),
                getRow(), getCol(), newValue, false
        );
    }

    @Override
    public boolean isEditable() {
        return false;
    }

    @Override
    public Color getDisplayColor() {
        return HINT_TEXT_COLOR;
    }

    @Override
    public String getCellType() {
        return "HINT";
    }

    @Override
    public SudokuCell copy() {
        return new HintCell(getRow(), getCol(), getSolutionValue());
    }
}
