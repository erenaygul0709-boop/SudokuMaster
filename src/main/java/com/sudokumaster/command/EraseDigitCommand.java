package com.sudokumaster.command;

import com.sudokumaster.domain.SudokuBoard;
import com.sudokumaster.domain.SudokuCell;
import com.sudokumaster.exception.InvalidSudokuMoveException;

import java.util.Objects;

/**
 * Concrete Command that clears a user-entered digit from an editable cell on the {@link SudokuBoard}.
 * Stores the erased digit so that {@link #undo()} restores it.
 */
public class EraseDigitCommand implements SudokuCommand {

    private final SudokuBoard board;
    private final int row;
    private final int col;
    private int erasedValue;
    private boolean executed;

    public EraseDigitCommand(SudokuBoard board, int row, int col) {
        this.board = Objects.requireNonNull(board, "SudokuBoard cannot be null.");
        this.row = row;
        this.col = col;
        this.erasedValue = 0;
        this.executed = false;
    }

    @Override
    public void execute() throws InvalidSudokuMoveException {
        board.validateCoordinates(row, col);
        SudokuCell cell = board.getCell(row, col);

        if (!cell.isEditable()) {
            // Triggers polymorphic InvalidSudokuMoveException on FixedCell / HintCell
            cell.setValue(0);
        }

        if (cell.isEmpty()) {
            throw new InvalidSudokuMoveException(
                    String.format("Cell (%d, %d) is already empty.", row + 1, col + 1),
                    row, col, 0, false
            );
        }

        this.erasedValue = cell.getValue();
        cell.setValue(0);
        this.executed = true;
    }

    @Override
    public void undo() {
        if (!executed) {
            return;
        }
        try {
            SudokuCell cell = board.getCell(row, col);
            cell.setValue(erasedValue);
            this.executed = false;
        } catch (InvalidSudokuMoveException ignored) {
            // Cell was verified as editable during execute()
        }
    }

    @Override
    public String getDescription() {
        return String.format("Erase %d at (%d, %d)", erasedValue, row + 1, col + 1);
    }

    public int getRow() {
        return row;
    }

    public int getCol() {
        return col;
    }

    public int getErasedValue() {
        return erasedValue;
    }
}
