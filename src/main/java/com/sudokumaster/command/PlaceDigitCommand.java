package com.sudokumaster.command;

import com.sudokumaster.domain.SudokuBoard;
import com.sudokumaster.domain.SudokuCell;
import com.sudokumaster.exception.InvalidSudokuMoveException;

import java.util.Objects;

/**
 * Concrete Command that places a digit (1..9) into an editable cell on the {@link SudokuBoard}.
 * Stores the cell's previous value to support multi-level undo.
 */
public class PlaceDigitCommand implements SudokuCommand {

    private final SudokuBoard board;
    private final int row;
    private final int col;
    private final int newDigit;
    private int previousValue;
    private boolean executed;

    public PlaceDigitCommand(SudokuBoard board, int row, int col, int newDigit) {
        this.board = Objects.requireNonNull(board, "SudokuBoard cannot be null.");
        this.row = row;
        this.col = col;
        this.newDigit = newDigit;
        this.previousValue = 0;
        this.executed = false;
    }

    @Override
    public void execute() throws InvalidSudokuMoveException {
        board.validateCoordinates(row, col);
        if (newDigit < 1 || newDigit > 9) {
            throw new InvalidSudokuMoveException(
                    "Invalid digit: " + newDigit + ". Please enter a number between 1 and 9.",
                    row, col, newDigit, false
            );
        }

        SudokuCell cell = board.getCell(row, col);
        if (!cell.isEditable()) {
            // Delegate to polymorphic setValue() which throws InvalidSudokuMoveException for FixedCell/HintCell
            cell.setValue(newDigit);
        }

        if (!board.isPlacementValidInCurrentGrid(row, col, newDigit)) {
            throw new InvalidSudokuMoveException(
                    String.format("Rule violation: Digit %d already exists in row %d, column %d, or its 3x3 box.",
                            newDigit, row + 1, col + 1),
                    row, col, newDigit, true
            );
        }

        this.previousValue = cell.getValue();
        cell.setValue(newDigit);
        this.executed = true;
    }

    @Override
    public void undo() {
        if (!executed) {
            return;
        }
        try {
            SudokuCell cell = board.getCell(row, col);
            cell.setValue(previousValue);
            this.executed = false;
        } catch (InvalidSudokuMoveException ignored) {
            // Cell was verified as editable during execute()
        }
    }

    @Override
    public String getDescription() {
        return String.format("Place %d at (%d, %d)", newDigit, row + 1, col + 1);
    }

    public int getRow() {
        return row;
    }

    public int getCol() {
        return col;
    }

    public int getNewDigit() {
        return newDigit;
    }

    public int getPreviousValue() {
        return previousValue;
    }
}
