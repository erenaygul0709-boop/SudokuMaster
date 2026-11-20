package com.sudokumaster.command;

import com.sudokumaster.exception.InvalidSudokuMoveException;

/**
 * GoF Command Pattern interface representing a reversible user action on the Sudoku board.
 */
public interface SudokuCommand {

    /**
     * Executes the command on the target SudokuBoard.
     *
     * @throws InvalidSudokuMoveException if the move violates Sudoku rules or cell editability
     */
    void execute() throws InvalidSudokuMoveException;

    /**
     * Reverts the effects of a previously executed command, restoring prior board state.
     */
    void undo();

    /**
     * Returns a human-readable description of this command for logs and UI status.
     */
    String getDescription();
}
