package com.sudokumaster;

import com.sudokumaster.command.CommandHistory;
import com.sudokumaster.command.EraseDigitCommand;
import com.sudokumaster.command.PlaceDigitCommand;
import com.sudokumaster.command.SudokuCommand;
import com.sudokumaster.domain.SudokuBoard;
import com.sudokumaster.exception.InvalidSudokuMoveException;
import com.sudokumaster.service.SudokuGenerator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Optional;
import java.util.Stack;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * JUnit 5 test suite verifying the GoF Command Pattern implementation:
 * {@link SudokuCommand}, {@link PlaceDigitCommand}, {@link EraseDigitCommand},
 * and multi-level Undo via {@link CommandHistory} ({@code Stack<SudokuCommand>}).
 */
class CommandHistoryTest {

    private SudokuBoard board;
    private CommandHistory commandHistory;

    @BeforeEach
    void setUp() {
        board = SudokuGenerator.createDeterministicBoard();
        commandHistory = new CommandHistory();
    }

    @Test
    @DisplayName("Command Pattern: PlaceDigitCommand executes and Undo restores empty cell")
    void testPlaceDigitAndSingleUndo() throws InvalidSudokuMoveException {
        assertEquals(0, board.getCell(0, 2).getValue());
        assertFalse(commandHistory.canUndo());

        SudokuCommand placeCmd = new PlaceDigitCommand(board, 0, 2, 4);
        commandHistory.executeCommand(placeCmd);

        assertEquals(4, board.getCell(0, 2).getValue());
        assertTrue(commandHistory.canUndo());
        assertEquals(1, commandHistory.size());

        Optional<SudokuCommand> undone = commandHistory.undoLast();
        assertTrue(undone.isPresent());
        assertEquals(0, board.getCell(0, 2).getValue());
        assertFalse(commandHistory.canUndo());
    }

    @Test
    @DisplayName("Command Pattern: EraseDigitCommand clears user digit and Undo restores it")
    void testEraseDigitAndUndo() throws InvalidSudokuMoveException {
        // First place 4 at (0, 2)
        commandHistory.executeCommand(new PlaceDigitCommand(board, 0, 2, 4));
        assertEquals(4, board.getCell(0, 2).getValue());

        // Now erase (0, 2)
        commandHistory.executeCommand(new EraseDigitCommand(board, 0, 2));
        assertEquals(0, board.getCell(0, 2).getValue());
        assertEquals(2, commandHistory.size());

        // Undo the erase -> should restore 4
        commandHistory.undoLast();
        assertEquals(4, board.getCell(0, 2).getValue());

        // Undo the initial placement -> should restore 0
        commandHistory.undoLast();
        assertEquals(0, board.getCell(0, 2).getValue());
        assertEquals(0, commandHistory.size());
    }

    @Test
    @DisplayName("Command Pattern: Multi-level Undo across multiple cells reverses in strict LIFO Stack order")
    void testMultiLevelUndoStack() throws InvalidSudokuMoveException {
        // Deterministic board editable cells: (0,2)->4, (0,3)->6, (1,1)->7, (4,4)->5
        commandHistory.executeCommand(new PlaceDigitCommand(board, 0, 2, 4));
        commandHistory.executeCommand(new PlaceDigitCommand(board, 0, 3, 6));
        commandHistory.executeCommand(new PlaceDigitCommand(board, 1, 1, 7));
        commandHistory.executeCommand(new EraseDigitCommand(board, 0, 3));
        commandHistory.executeCommand(new PlaceDigitCommand(board, 4, 4, 5));

        Stack<SudokuCommand> stackSnapshot = commandHistory.getHistoryStack();
        assertEquals(5, stackSnapshot.size());
        assertEquals(4, board.getCell(0, 2).getValue());
        assertEquals(0, board.getCell(0, 3).getValue());
        assertEquals(7, board.getCell(1, 1).getValue());
        assertEquals(5, board.getCell(4, 4).getValue());

        // Undo 1: Reverts Place 5 at (4, 4)
        commandHistory.undoLast();
        assertEquals(0, board.getCell(4, 4).getValue());

        // Undo 2: Reverts Erase at (0, 3), restoring 6
        commandHistory.undoLast();
        assertEquals(6, board.getCell(0, 3).getValue());

        // Undo 3: Reverts Place 7 at (1, 1)
        commandHistory.undoLast();
        assertEquals(0, board.getCell(1, 1).getValue());

        // Undo 4: Reverts Place 6 at (0, 3)
        commandHistory.undoLast();
        assertEquals(0, board.getCell(0, 3).getValue());

        // Undo 5: Reverts Place 4 at (0, 2)
        commandHistory.undoLast();
        assertEquals(0, board.getCell(0, 2).getValue());

        assertFalse(commandHistory.canUndo());
        assertTrue(commandHistory.undoLast().isEmpty());
    }

    @Test
    @DisplayName("Command Pattern: Failed command is not pushed onto CommandHistory stack")
    void testFailedCommandNotAddedToHistory() {
        // Attempt invalid move on FixedCell (0, 0)
        assertThrows(InvalidSudokuMoveException.class,
                () -> commandHistory.executeCommand(new PlaceDigitCommand(board, 0, 0, 9)));
        assertEquals(0, commandHistory.size());

        // Attempt erasing an already empty cell (0, 2)
        assertThrows(InvalidSudokuMoveException.class,
                () -> commandHistory.executeCommand(new EraseDigitCommand(board, 0, 2)));
        assertEquals(0, commandHistory.size());
    }
}
