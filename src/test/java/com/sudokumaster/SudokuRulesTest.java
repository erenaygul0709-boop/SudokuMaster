package com.sudokumaster;

import com.sudokumaster.domain.Difficulty;
import com.sudokumaster.domain.FixedCell;
import com.sudokumaster.domain.GameState;
import com.sudokumaster.domain.HintCell;
import com.sudokumaster.domain.ScoreRecord;
import com.sudokumaster.domain.SudokuBoard;
import com.sudokumaster.domain.SudokuCell;
import com.sudokumaster.domain.UserCell;
import com.sudokumaster.exception.InvalidSudokuMoveException;
import com.sudokumaster.persistence.GamePersistenceManager;
import com.sudokumaster.service.SudokuGameController;
import com.sudokumaster.service.SudokuGenerator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * JUnit 5 test suite validating OOP polymorphism, Sudoku board rules, custom domain exceptions,
 * collections sorting (Comparable & Comparator), and JSON persistence.
 */
class SudokuRulesTest {

    private SudokuBoard board;
    private SudokuGameController controller;

    @TempDir
    Path tempDir;

    @BeforeEach
    void setUp() {
        board = SudokuGenerator.createDeterministicBoard();
        GamePersistenceManager persistence = new GamePersistenceManager(
                tempDir.resolve("highscores.json"),
                tempDir.resolve("saved_game.json")
        );
        controller = new SudokuGameController(new SudokuGenerator(), persistence);
        controller.startCustomGame(new GameState(board, Difficulty.MEDIUM, "TestPlayer"));
    }

    @Test
    @DisplayName("Polymorphism: FixedCell, UserCell, and HintCell exhibit distinct runtime behaviors")
    void testCellPolymorphism() throws InvalidSudokuMoveException {
        SudokuCell fixedCell = new FixedCell(0, 0, 5);
        SudokuCell userCell = new UserCell(0, 2, 0, 4);
        SudokuCell hintCell = new HintCell(1, 1, 7);

        // Runtime polymorphism via common SudokuCell reference
        assertFalse(fixedCell.isEditable(), "FixedCell must not be editable");
        assertTrue(userCell.isEditable(), "UserCell must be editable");
        assertFalse(hintCell.isEditable(), "HintCell must not be editable");

        assertEquals("FIXED", fixedCell.getCellType());
        assertEquals("USER", userCell.getCellType());
        assertEquals("HINT", hintCell.getCellType());

        // Distinct polymorphic display colors
        assertNotEquals(fixedCell.getDisplayColor(), userCell.getDisplayColor());
        assertNotEquals(userCell.getDisplayColor(), hintCell.getDisplayColor());

        // UserCell allows valid modification
        userCell.setValue(4);
        assertEquals(4, userCell.getValue());
        assertTrue(userCell.isCorrect());

        // FixedCell and HintCell throw InvalidSudokuMoveException on setValue()
        assertThrows(InvalidSudokuMoveException.class, () -> fixedCell.setValue(9));
        assertThrows(InvalidSudokuMoveException.class, () -> hintCell.setValue(3));
    }

    @Test
    @DisplayName("Exception: Overwriting a FixedCell via controller throws InvalidSudokuMoveException")
    void testOverwriteFixedCellThrowsException() {
        // (0, 0) is FixedCell with clue 5 in the deterministic board
        InvalidSudokuMoveException ex = assertThrows(
                InvalidSudokuMoveException.class,
                () -> controller.placeDigit(0, 0, 9)
        );
        assertTrue(ex.getMessage().contains("fixed puzzle clue"));
        assertEquals(5, board.getCell(0, 0).getValue());
    }

    @Test
    @DisplayName("Exception: Duplicate digit in same row, column, or 3x3 box throws InvalidSudokuMoveException")
    void testRowColumnBoxConflictThrowsException() {
        // (0, 2) is an empty UserCell (solution = 4).
        // Digit 5 is already at (0, 0) in Row 0 and top-left 3x3 box.
        InvalidSudokuMoveException ex = assertThrows(
                InvalidSudokuMoveException.class,
                () -> controller.placeDigit(0, 2, 5)
        );
        assertTrue(ex.isStrikeInduced());
        assertEquals(1, controller.getGameState().getMistakes());
        assertEquals(0, board.getCell(0, 2).getValue());
    }

    @Test
    @DisplayName("Exception: Exceeding maximum mistake strikes (3/3) blocks further moves")
    void testExceedingStrikeLimitThrowsException() {
        // Trigger 3 strike-inducing rule violations on empty cell (0, 2)
        assertThrows(InvalidSudokuMoveException.class, () -> controller.placeDigit(0, 2, 5)); // Mistake 1/3
        assertThrows(InvalidSudokuMoveException.class, () -> controller.placeDigit(0, 2, 3)); // Mistake 2/3
        assertThrows(InvalidSudokuMoveException.class, () -> controller.placeDigit(0, 2, 8)); // Mistake 3/3

        assertTrue(controller.getGameState().isGameOverByMistakes());
        assertEquals(3, controller.getGameState().getMistakes());

        // Even a valid digit (4) must now be rejected because mistakes reached 3/3
        InvalidSudokuMoveException gameOverEx = assertThrows(
                InvalidSudokuMoveException.class,
                () -> controller.placeDigit(0, 2, 4)
        );
        assertTrue(gameOverEx.getMessage().contains("Maximum mistake limit reached"));
    }

    @Test
    @DisplayName("Collections & Sorting: Comparable and Comparator order ScoreRecords accurately")
    void testScoreRecordSorting() {
        LocalDateTime now = LocalDateTime.of(2026, 11, 18, 12, 0, 0);
        ScoreRecord r1 = new ScoreRecord("Alice", 450, 180, 1, Difficulty.EASY, now.minusDays(2));
        ScoreRecord r2 = new ScoreRecord("Bob", 620, 210, 0, Difficulty.HARD, now.minusDays(1));
        ScoreRecord r3 = new ScoreRecord("Charlie", 620, 150, 0, Difficulty.MEDIUM, now);

        ArrayList<ScoreRecord> list = new ArrayList<>(List.of(r1, r2, r3));

        // 1. Natural ordering via Comparable (highest score first, then fastest time)
        Collections.sort(list);
        assertEquals("Charlie", list.get(0).getPlayerName()); // 620 pts, 150s
        assertEquals("Bob", list.get(1).getPlayerName());     // 620 pts, 210s
        assertEquals("Alice", list.get(2).getPlayerName());   // 450 pts, 180s

        // 2. Secondary ordering via Comparator (Difficulty HARD -> MEDIUM -> EASY)
        list.sort(ScoreRecord.BY_DIFFICULTY_AND_TIME);
        assertEquals("Bob", list.get(0).getPlayerName());     // HARD
        assertEquals("Charlie", list.get(1).getPlayerName()); // MEDIUM
        assertEquals("Alice", list.get(2).getPlayerName());   // EASY

        // 3. Secondary ordering via Comparator (Most recent date first)
        list.sort(ScoreRecord.BY_DATE_DESCENDING);
        assertEquals("Charlie", list.get(0).getPlayerName());
        assertEquals("Bob", list.get(1).getPlayerName());
        assertEquals("Alice", list.get(2).getPlayerName());
    }

    @Test
    @DisplayName("Persistence: Saves and loads unfinished GameState and HighScores via JSON")
    void testPersistenceSaveAndLoad() throws Exception {
        // Make a valid placement and use a hint
        controller.placeDigit(0, 2, 4);
        controller.useHint(1, 1);
        controller.getGameState().setElapsedSeconds(95);
        controller.saveCurrentGame();

        // Reset controller to a blank game, then load saved game
        controller.startNewGame(Difficulty.EASY, "Other");
        controller.loadSavedGame();

        GameState loaded = controller.getGameState();
        assertEquals("TestPlayer", loaded.getPlayerName());
        assertEquals(Difficulty.MEDIUM, loaded.getDifficulty());
        assertEquals(95, loaded.getElapsedSeconds());
        assertEquals(4, loaded.getBoard().getCell(0, 2).getValue());
        assertEquals("HINT", loaded.getBoard().getCell(1, 1).getCellType());
        assertEquals(7, loaded.getBoard().getCell(1, 1).getValue());

        // Test high score persistence
        ScoreRecord savedScore = controller.recordCompletedGame("Champion");
        assertEquals("Champion", savedScore.getPlayerName());

        GamePersistenceManager pm = new GamePersistenceManager(
                tempDir.resolve("highscores.json"),
                tempDir.resolve("saved_game.json")
        );
        ArrayList<ScoreRecord> loadedScores = pm.loadHighScores();
        assertEquals(1, loadedScores.size());
        assertEquals("Champion", loadedScores.get(0).getPlayerName());
    }
}
