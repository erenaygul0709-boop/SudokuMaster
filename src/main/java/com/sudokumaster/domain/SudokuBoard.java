package com.sudokumaster.domain;

import com.sudokumaster.exception.InvalidSudokuMoveException;

/**
 * Represents the 9x9 Sudoku board composed of 81 polymorphic {@link SudokuCell} instances.
 * Demonstrates Composition (1 SudokuBoard owns 81 SudokuCells) and encapsulates Sudoku domain rules.
 */
public class SudokuBoard {

    public static final int SIZE = 9;
    public static final int SUBGRID_SIZE = 3;

    private final SudokuCell[][] grid;

    /**
     * Constructs a SudokuBoard from an initial puzzle matrix and its full solution matrix.
     *
     * @param initialClues 9x9 matrix where 1..9 are fixed clues and 0 is empty
     * @param solution     9x9 matrix with complete valid solution digits (1..9)
     */
    public SudokuBoard(int[][] initialClues, int[][] solution) {
        validateMatrix(initialClues, true);
        validateMatrix(solution, false);
        this.grid = new SudokuCell[SIZE][SIZE];
        for (int r = 0; r < SIZE; r++) {
            for (int c = 0; c < SIZE; c++) {
                int clue = initialClues[r][c];
                int sol = solution[r][c];
                if (clue != 0) {
                    this.grid[r][c] = new FixedCell(r, c, clue);
                } else {
                    this.grid[r][c] = new UserCell(r, c, 0, sol);
                }
            }
        }
    }

    /**
     * Constructs a SudokuBoard directly from a pre-populated 9x9 array of SudokuCell objects.
     */
    public SudokuBoard(SudokuCell[][] cells) {
        if (cells == null || cells.length != SIZE) {
            throw new IllegalArgumentException("Cells array must be 9x9.");
        }
        this.grid = new SudokuCell[SIZE][SIZE];
        for (int r = 0; r < SIZE; r++) {
            if (cells[r] == null || cells[r].length != SIZE) {
                throw new IllegalArgumentException("Row " + r + " must have 9 cells.");
            }
            for (int c = 0; c < SIZE; c++) {
                if (cells[r][c] == null) {
                    throw new IllegalArgumentException("Cell at (" + r + ", " + c + ") cannot be null.");
                }
                this.grid[r][c] = cells[r][c].copy();
            }
        }
    }

    private void validateMatrix(int[][] matrix, boolean allowZero) {
        if (matrix == null || matrix.length != SIZE) {
            throw new IllegalArgumentException("Matrix must be 9x9.");
        }
        int min = allowZero ? 0 : 1;
        for (int r = 0; r < SIZE; r++) {
            if (matrix[r] == null || matrix[r].length != SIZE) {
                throw new IllegalArgumentException("Row " + r + " must have length 9.");
            }
            for (int c = 0; c < SIZE; c++) {
                int v = matrix[r][c];
                if (v < min || v > 9) {
                    throw new IllegalArgumentException(
                            "Invalid value " + v + " at (" + r + ", " + c + "). Expected " + min + "..9.");
                }
            }
        }
    }

    public void validateCoordinates(int row, int col) throws InvalidSudokuMoveException {
        if (row < 0 || row >= SIZE || col < 0 || col >= SIZE) {
            throw new InvalidSudokuMoveException(
                    String.format("Coordinates (%d, %d) are outside the 1..9 board boundaries.", row + 1, col + 1),
                    row, col, 0, false
            );
        }
    }

    public SudokuCell getCell(int row, int col) {
        if (row < 0 || row >= SIZE || col < 0 || col >= SIZE) {
            throw new IndexOutOfBoundsException("Invalid cell coordinates: (" + row + ", " + col + ")");
        }
        return grid[row][col];
    }

    public void replaceCell(int row, int col, SudokuCell newCell) {
        if (row < 0 || row >= SIZE || col < 0 || col >= SIZE) {
            throw new IndexOutOfBoundsException("Invalid cell coordinates: (" + row + ", " + col + ")");
        }
        if (newCell == null || newCell.getRow() != row || newCell.getCol() != col) {
            throw new IllegalArgumentException("Replacement cell coordinates do not match target position.");
        }
        grid[row][col] = newCell;
    }

    /**
     * Checks whether placing {@code digit} at {@code (row, col)} conflicts with any existing
     * visible digit in the same row, column, or 3x3 box (excluding {@code (row, col)} itself).
     *
     * @param row   row index (0..8)
     * @param col   column index (0..8)
     * @param digit value to test (1..9)
     * @return true if there is NO conflict in the current visible board state
     */
    public boolean isPlacementValidInCurrentGrid(int row, int col, int digit) {
        if (digit < 1 || digit > 9) {
            return false;
        }
        // Check row
        for (int c = 0; c < SIZE; c++) {
            if (c != col && grid[row][c].getValue() == digit) {
                return false;
            }
        }
        // Check column
        for (int r = 0; r < SIZE; r++) {
            if (r != row && grid[r][col].getValue() == digit) {
                return false;
            }
        }
        // Check 3x3 box
        int boxStartRow = (row / SUBGRID_SIZE) * SUBGRID_SIZE;
        int boxStartCol = (col / SUBGRID_SIZE) * SUBGRID_SIZE;
        for (int r = boxStartRow; r < boxStartRow + SUBGRID_SIZE; r++) {
            for (int c = boxStartCol; c < boxStartCol + SUBGRID_SIZE; c++) {
                if ((r != row || c != col) && grid[r][c].getValue() == digit) {
                    return false;
                }
            }
        }
        return true;
    }

    /**
     * Checks whether placing {@code digit} at {@code (row, col)} matches the true puzzle solution.
     */
    public boolean matchesSolution(int row, int col, int digit) {
        return grid[row][col].getSolutionValue() == digit;
    }

    /**
     * Returns true if every cell on the board is filled with its correct solution value.
     */
    public boolean isSolved() {
        for (int r = 0; r < SIZE; r++) {
            for (int c = 0; c < SIZE; c++) {
                if (!grid[r][c].isCorrect()) {
                    return false;
                }
            }
        }
        return true;
    }

    /**
     * Counts how many occurrences of a specific digit (1..9) are currently correctly placed on the board.
     */
    public int countDigitOccurrences(int digit) {
        int count = 0;
        for (int r = 0; r < SIZE; r++) {
            for (int c = 0; c < SIZE; c++) {
                if (grid[r][c].getValue() == digit) {
                    count++;
                }
            }
        }
        return count;
    }

    /**
     * Counts remaining empty or incorrect cells.
     */
    public int countRemainingCells() {
        int remaining = 0;
        for (int r = 0; r < SIZE; r++) {
            for (int c = 0; c < SIZE; c++) {
                if (!grid[r][c].isCorrect()) {
                    remaining++;
                }
            }
        }
        return remaining;
    }
}
