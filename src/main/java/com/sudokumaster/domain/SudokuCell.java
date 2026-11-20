package com.sudokumaster.domain;

import com.sudokumaster.exception.InvalidSudokuMoveException;

import java.awt.Color;

/**
 * Abstract base class representing a single cell in a 9x9 Sudoku grid.
 * Demonstrates Abstraction, Encapsulation, and serves as the base for Inheritance & Polymorphism.
 */
public abstract class SudokuCell {

    private final int row;
    private final int col;
    private final int solutionValue;
    private int value;

    /**
     * Constructs a validated SudokuCell.
     *
     * @param row           row index (0..8)
     * @param col           column index (0..8)
     * @param value         initial displayed value (0 for empty, 1..9 for filled)
     * @param solutionValue true solution value for this cell (1..9)
     */
    protected SudokuCell(int row, int col, int value, int solutionValue) {
        if (row < 0 || row > 8 || col < 0 || col > 8) {
            throw new IllegalArgumentException("Cell coordinates must be between 0 and 8, got: (" + row + ", " + col + ")");
        }
        if (value < 0 || value > 9) {
            throw new IllegalArgumentException("Cell value must be between 0 and 9, got: " + value);
        }
        if (solutionValue < 1 || solutionValue > 9) {
            throw new IllegalArgumentException("Cell solution value must be between 1 and 9, got: " + solutionValue);
        }
        this.row = row;
        this.col = col;
        this.value = value;
        this.solutionValue = solutionValue;
    }

    public int getRow() {
        return row;
    }

    public int getCol() {
        return col;
    }

    public int getValue() {
        return value;
    }

    /**
     * Controlled internal mutator for subclasses that permit value updates.
     */
    protected final void setInternalValue(int newValue) {
        if (newValue < 0 || newValue > 9) {
            throw new IllegalArgumentException("Cell value must be between 0 and 9, got: " + newValue);
        }
        this.value = newValue;
    }

    public int getSolutionValue() {
        return solutionValue;
    }

    public boolean isEmpty() {
        return value == 0;
    }

    public boolean isCorrect() {
        return value != 0 && value == solutionValue;
    }

    /**
     * Polymorphic contract: updates the cell's value if permitted by the concrete cell type.
     *
     * @param newValue new digit (0 to clear, or 1..9)
     * @throws InvalidSudokuMoveException if the cell is not editable or the digit is invalid
     */
    public abstract void setValue(int newValue) throws InvalidSudokuMoveException;

    /**
     * Polymorphic contract: returns whether the user is allowed to edit this cell.
     */
    public abstract boolean isEditable();

    /**
     * Polymorphic contract: returns the foreground text color used to render this cell's digit.
     */
    public abstract Color getDisplayColor();

    /**
     * Polymorphic contract: returns a string identifier for persistence ("FIXED", "USER", "HINT").
     */
    public abstract String getCellType();

    /**
     * Creates a deep copy of this cell.
     */
    public abstract SudokuCell copy();
}
