package com.sudokumaster.command;

import com.sudokumaster.exception.InvalidSudokuMoveException;

import java.util.Objects;
import java.util.Optional;
import java.util.Stack;

/**
 * Invoker class in the GoF Command Pattern.
 * Maintains a {@link Stack} of executed {@link SudokuCommand} objects to provide multi-level Undo.
 */
public class CommandHistory {

    private final Stack<SudokuCommand> historyStack;

    public CommandHistory() {
        this.historyStack = new Stack<>();
    }

    /**
     * Executes a command and pushes it onto the history stack if execution succeeds.
     *
     * @param command the command to execute
     * @throws InvalidSudokuMoveException if execution fails validation
     */
    public void executeCommand(SudokuCommand command) throws InvalidSudokuMoveException {
        Objects.requireNonNull(command, "Command cannot be null.");
        command.execute();
        historyStack.push(command);
    }

    /**
     * Pops and undoes the most recent command from the stack.
     *
     * @return the undone command, or {@link Optional#empty()} if the history stack is empty
     */
    public Optional<SudokuCommand> undoLast() {
        if (historyStack.isEmpty()) {
            return Optional.empty();
        }
        SudokuCommand lastCommand = historyStack.pop();
        lastCommand.undo();
        return Optional.of(lastCommand);
    }

    public boolean canUndo() {
        return !historyStack.isEmpty();
    }

    public int size() {
        return historyStack.size();
    }

    public void clear() {
        historyStack.clear();
    }

    /**
     * Returns a copy of the underlying Stack<SudokuCommand> for inspection or testing.
     */
    public Stack<SudokuCommand> getHistoryStack() {
        Stack<SudokuCommand> copy = new Stack<>();
        copy.addAll(historyStack);
        return copy;
    }
}
