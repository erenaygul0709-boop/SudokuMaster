package com.sudokumaster.domain;

import com.sudokumaster.exception.InvalidSudokuMoveException;

import java.awt.Color;

/**
 * Represents an editable player-controlled cell in a Sudoku board.
 */
public class UserCell extends SudokuCell {

    private static final Color USER_VALID_COLOR = new Color(37, 99, 235);   // Royal Blue-600
    private static final Color USER_CONFLICT_COLOR = new Color(220, 38, 38); // Red-600

    public UserCell(int row, int col, int initialValue, int solutionValue) {
        super(row, col, initialValue, solutionValue);
    }

    public UserCell(int row, int col, int solutionValue) {
        this(row, col, 0, solutionValue);
    }

    @Override
    public void setValue(int newValue) throws InvalidSudokuMoveException {
        if (newValue < 0 || newValue > 9) {
            throw new InvalidSudokuMoveException(
                    "Digit must be between 1 and 9 (or 0 to clear). Got: " + newValue,
                    getRow(), getCol(), newValue, false
            );
        }
        setInternalValue(newValue);
    }

    @Override
    public boolean isEditable() {
        return true;
    }

    @Override
    public Color getDisplayColor() {
        if (value == 0) {
            return USER_VALID_COLOR;
        }
        return isCorrect() ? USER_VALID_COLOR : USER_CONFLICT_COLOR;
    }

    @Override
    public String getCellType() {
        return "USER";
    }

    @Override
    public SudokuCell copy() {
        return new UserCell(getRow(), getCol(), getValue(), getSolutionValue());
    }
}
