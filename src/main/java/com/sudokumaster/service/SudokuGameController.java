package com.sudokumaster.service;

import com.sudokumaster.command.CommandHistory;
import com.sudokumaster.command.EraseDigitCommand;
import com.sudokumaster.command.PlaceDigitCommand;
import com.sudokumaster.command.SudokuCommand;
import com.sudokumaster.domain.Difficulty;
import com.sudokumaster.domain.GameState;
import com.sudokumaster.domain.HintCell;
import com.sudokumaster.domain.ScoreRecord;
import com.sudokumaster.domain.SudokuBoard;
import com.sudokumaster.domain.SudokuCell;
import com.sudokumaster.exception.InvalidSudokuMoveException;
import com.sudokumaster.persistence.GamePersistenceManager;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Application service / controller managing game rules, CommandHistory (Undo),
 * strike limits, scoring, hints, and persistence.
 */
public class SudokuGameController {

    private final SudokuGenerator generator;
    private final GamePersistenceManager persistenceManager;
    private final CommandHistory commandHistory;
    private final ArrayList<ScoreRecord> leaderboard;
    private GameState gameState;

    public SudokuGameController() {
        this(new SudokuGenerator(), new GamePersistenceManager());
    }

    public SudokuGameController(SudokuGenerator generator, GamePersistenceManager persistenceManager) {
        this.generator = Objects.requireNonNull(generator, "SudokuGenerator cannot be null.");
        this.persistenceManager = Objects.requireNonNull(persistenceManager, "GamePersistenceManager cannot be null.");
        this.commandHistory = new CommandHistory();
        this.leaderboard = new ArrayList<>();
        loadLeaderboardQuietly();
        startNewGame(Difficulty.EASY, "Player");
    }

    public void startNewGame(Difficulty difficulty, String playerName) {
        SudokuBoard board = generator.generateBoard(difficulty);
        this.gameState = new GameState(board, difficulty, playerName);
        this.commandHistory.clear();
    }

    public void startCustomGame(GameState customState) {
        this.gameState = Objects.requireNonNull(customState, "GameState cannot be null.");
        this.commandHistory.clear();
    }

    /**
     * Places a digit on the board using {@link PlaceDigitCommand} and records it in {@link CommandHistory}.
     *
     * @throws InvalidSudokuMoveException if strike limit exceeded, cell is fixed/hint, or placement violates rules
     */
    public void placeDigit(int row, int col, int digit) throws InvalidSudokuMoveException {
        ensureNotGameOver(row, col, digit);

        SudokuBoard board = gameState.getBoard();
        PlaceDigitCommand command = new PlaceDigitCommand(board, row, col, digit);
        try {
            commandHistory.executeCommand(command);
            if (board.getCell(row, col).isCorrect()) {
                gameState.addScore(gameState.getDifficulty().getPointsPerMove());
            }
        } catch (InvalidSudokuMoveException ex) {
            if (ex.isStrikeInduced()) {
                gameState.incrementMistakes();
                gameState.addScore(-5);
            }
            throw ex;
        }
    }

    /**
     * Erases a user digit from the board using {@link EraseDigitCommand} and records it in {@link CommandHistory}.
     */
    public void eraseDigit(int row, int col) throws InvalidSudokuMoveException {
        ensureNotGameOver(row, col, 0);

        EraseDigitCommand command = new EraseDigitCommand(gameState.getBoard(), row, col);
        commandHistory.executeCommand(command);
    }

    /**
     * Undoes the most recent command on the command history stack.
     */
    public Optional<SudokuCommand> undo() {
        return commandHistory.undoLast();
    }

    /**
     * Reveals a hint at the specified cell (or the first available editable cell if row/col < 0)
     * by converting the cell to a polymorphic {@link HintCell}.
     */
    public HintCell useHint(int row, int col) throws InvalidSudokuMoveException {
        ensureNotGameOver(row, col, 0);
        if (gameState.getHintsRemaining() <= 0) {
            throw new InvalidSudokuMoveException("No hints remaining for this game!");
        }

        SudokuBoard board = gameState.getBoard();
        int targetRow = row;
        int targetCol = col;

        if (targetRow < 0 || targetCol < 0 || !board.getCell(targetRow, targetCol).isEditable()
                || board.getCell(targetRow, targetCol).isCorrect()) {
            int[] found = findFirstUnresolvedEditableCell(board);
            if (found == null) {
                throw new InvalidSudokuMoveException("All editable cells are already correctly filled!");
            }
            targetRow = found[0];
            targetCol = found[1];
        }

        SudokuCell currentCell = board.getCell(targetRow, targetCol);
        if (!currentCell.isEditable()) {
            throw new InvalidSudokuMoveException(
                    String.format("Cell (%d, %d) cannot receive a hint because it is already fixed.", targetRow + 1, targetCol + 1),
                    targetRow, targetCol, 0, false
            );
        }

        HintCell hintCell = new HintCell(targetRow, targetCol, currentCell.getSolutionValue());
        board.replaceCell(targetRow, targetCol, hintCell);
        gameState.decrementHints();
        return hintCell;
    }

    private int[] findFirstUnresolvedEditableCell(SudokuBoard board) {
        for (int r = 0; r < SudokuBoard.SIZE; r++) {
            for (int c = 0; c < SudokuBoard.SIZE; c++) {
                SudokuCell cell = board.getCell(r, c);
                if (cell.isEditable() && !cell.isCorrect()) {
                    return new int[]{r, c};
                }
            }
        }
        return null;
    }

    private void ensureNotGameOver(int row, int col, int attemptedDigit) throws InvalidSudokuMoveException {
        if (gameState.isGameOverByMistakes()) {
            throw new InvalidSudokuMoveException(
                    String.format("Maximum mistake limit reached (%d/%d)! Start a New Game to continue.",
                            gameState.getMistakes(), gameState.getMaxMistakes()),
                    row, col, attemptedDigit, true
            );
        }
    }

    /**
     * Records a completed game into the leaderboard, sorts via {@link Comparable}, and persists to disk.
     */
    public ScoreRecord recordCompletedGame(String playerName) throws IOException {
        if (playerName != null && !playerName.trim().isEmpty()) {
            gameState.setPlayerName(playerName.trim());
        }
        int timeBonus = Math.max(0, 500 - gameState.getElapsedSeconds());
        int finalScore = gameState.getScore() + timeBonus;
        gameState.setScore(finalScore);

        ScoreRecord record = new ScoreRecord(
                gameState.getPlayerName(),
                finalScore,
                gameState.getElapsedSeconds(),
                gameState.getMistakes(),
                gameState.getDifficulty(),
                LocalDateTime.now()
        );
        leaderboard.add(record);
        Collections.sort(leaderboard);
        persistenceManager.saveHighScores(leaderboard);
        return record;
    }

    public void addScoreRecord(ScoreRecord record) {
        leaderboard.add(Objects.requireNonNull(record));
        Collections.sort(leaderboard);
    }

    /**
     * Returns leaderboard sorted by natural order ({@link ScoreRecord#compareTo(ScoreRecord)}).
     */
    public ArrayList<ScoreRecord> getLeaderboardSortedByScore() {
        ArrayList<ScoreRecord> copy = new ArrayList<>(leaderboard);
        Collections.sort(copy);
        return copy;
    }

    /**
     * Returns leaderboard sorted by a custom {@link Comparator} (e.g., {@link ScoreRecord#BY_DIFFICULTY_AND_TIME}
     * or {@link ScoreRecord#BY_DATE_DESCENDING}).
     */
    public List<ScoreRecord> getLeaderboardSortedBy(Comparator<ScoreRecord> comparator) {
        ArrayList<ScoreRecord> copy = new ArrayList<>(leaderboard);
        copy.sort(comparator);
        return copy;
    }

    public void saveCurrentGame() throws IOException {
        persistenceManager.saveGameState(gameState);
    }

    public void loadSavedGame() throws IOException {
        this.gameState = persistenceManager.loadGameState();
        this.commandHistory.clear();
    }

    public boolean hasSavedGame() {
        return persistenceManager.hasSavedGame();
    }

    public GameState getGameState() {
        return gameState;
    }

    public CommandHistory getCommandHistory() {
        return commandHistory;
    }

    private void loadLeaderboardQuietly() {
        try {
            leaderboard.clear();
            leaderboard.addAll(persistenceManager.loadHighScores());
        } catch (IOException ignored) {
            // Start with empty leaderboard if file does not exist yet
        }
    }
}
