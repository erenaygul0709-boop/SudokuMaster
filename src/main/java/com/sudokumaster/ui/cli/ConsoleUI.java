package com.sudokumaster.ui.cli;

import com.sudokumaster.command.SudokuCommand;
import com.sudokumaster.domain.Difficulty;
import com.sudokumaster.domain.GameState;
import com.sudokumaster.domain.HintCell;
import com.sudokumaster.domain.ScoreRecord;
import com.sudokumaster.domain.SudokuBoard;
import com.sudokumaster.domain.SudokuCell;
import com.sudokumaster.exception.InvalidSudokuMoveException;
import com.sudokumaster.service.SudokuGameController;

import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Scanner;

/**
 * Interactive Console CLI interface for SudokuMaster.
 * Satisfies the mandatory console interface requirement while sharing the exact same
 * domain model, CommandHistory stack, exception handling, and persistence as the Swing GUI.
 */
public class ConsoleUI {

    private final SudokuGameController controller;
    private final InputStream in;
    private final PrintStream out;
    private long lastTickMillis;

    public ConsoleUI(SudokuGameController controller) {
        this(controller, System.in, System.out);
    }

    public ConsoleUI(SudokuGameController controller, InputStream in, PrintStream out) {
        this.controller = controller;
        this.in = in;
        this.out = out;
        this.lastTickMillis = System.currentTimeMillis();
    }

    public void start() {
        out.println("============================================================");
        out.println("          SUDOKU MASTER — Interactive Console Mode          ");
        out.println("============================================================");
        printHelp();

        try (Scanner scanner = new Scanner(in)) {
            boolean running = true;
            while (running) {
                updateElapsedTime();
                printBoardAndStatus();
                out.print("\n[SudokuMaster]> ");
                if (!scanner.hasNextLine()) {
                    break;
                }
                String line = scanner.nextLine().trim();
                if (line.isEmpty()) {
                    continue;
                }
                running = processCommand(line, scanner);
            }
        }
        out.println("Thank you for playing SudokuMaster!");
    }

    private void updateElapsedTime() {
        long now = System.currentTimeMillis();
        int deltaSec = (int) ((now - lastTickMillis) / 1000L);
        if (deltaSec > 0 && !controller.getGameState().isGameOverByMistakes()) {
            GameState state = controller.getGameState();
            state.setElapsedSeconds(state.getElapsedSeconds() + deltaSec);
        }
        lastTickMillis = now;
    }

    public boolean processCommand(String input, Scanner scanner) {
        String[] parts = input.split("\\s+");
        String cmd = parts[0].toLowerCase(Locale.ROOT);

        try {
            switch (cmd) {
                case "place", "p" -> {
                    if (parts.length < 4) {
                        out.println("[!] Usage: place <row 1-9> <col 1-9> <digit 1-9>");
                        return true;
                    }
                    int row = Integer.parseInt(parts[1]) - 1;
                    int col = Integer.parseInt(parts[2]) - 1;
                    int digit = Integer.parseInt(parts[3]);
                    controller.placeDigit(row, col, digit);
                    out.printf("[OK] Placed %d at (%d, %d).%n", digit, row + 1, col + 1);
                    checkCompletion(scanner);
                }
                case "erase", "e" -> {
                    if (parts.length < 3) {
                        out.println("[!] Usage: erase <row 1-9> <col 1-9>");
                        return true;
                    }
                    int row = Integer.parseInt(parts[1]) - 1;
                    int col = Integer.parseInt(parts[2]) - 1;
                    controller.eraseDigit(row, col);
                    out.printf("[OK] Cleared cell (%d, %d).%n", row + 1, col + 1);
                }
                case "undo", "u" -> {
                    Optional<SudokuCommand> undone = controller.undo();
                    if (undone.isPresent()) {
                        out.println("[UNDO] Reverted: " + undone.get().getDescription());
                    } else {
                        out.println("[!] Nothing to undo — command history stack is empty.");
                    }
                }
                case "hint", "h" -> {
                    int row = -1;
                    int col = -1;
                    if (parts.length >= 3) {
                        row = Integer.parseInt(parts[1]) - 1;
                        col = Integer.parseInt(parts[2]) - 1;
                    }
                    HintCell hint = controller.useHint(row, col);
                    out.printf("[HINT] Revealed %d at (%d, %d)!%n",
                            hint.getValue(), hint.getRow() + 1, hint.getCol() + 1);
                    checkCompletion(scanner);
                }
                case "new", "n" -> {
                    Difficulty diff = Difficulty.EASY;
                    if (parts.length >= 2) {
                        diff = Difficulty.valueOf(parts[1].toUpperCase(Locale.ROOT));
                    }
                    controller.startNewGame(diff, controller.getGameState().getPlayerName());
                    lastTickMillis = System.currentTimeMillis();
                    out.println("[OK] Started a new " + diff.getDisplayName() + " game.");
                }
                case "save" -> {
                    controller.saveCurrentGame();
                    out.println("[OK] Game state saved successfully to JSON.");
                }
                case "load" -> {
                    controller.loadSavedGame();
                    lastTickMillis = System.currentTimeMillis();
                    out.println("[OK] Saved game state loaded successfully.");
                }
                case "scores", "leaderboard" -> {
                    String sortMode = parts.length >= 2 ? parts[1].toLowerCase(Locale.ROOT) : "score";
                    printLeaderboard(sortMode);
                }
                case "help", "?" -> printHelp();
                case "quit", "exit", "q" -> {
                    return false;
                }
                default -> out.println("[!] Unknown command '" + cmd + "'. Type 'help' for available commands.");
            }
        } catch (NumberFormatException ex) {
            out.println("[ERROR] Coordinates and digits must be valid integers (1..9).");
        } catch (IllegalArgumentException ex) {
            out.println("[ERROR] " + ex.getMessage() + " (Valid difficulties: EASY, MEDIUM, HARD)");
        } catch (InvalidSudokuMoveException ex) {
            out.println("[INVALID MOVE] " + ex.getMessage());
            if (controller.getGameState().isGameOverByMistakes()) {
                out.println("[GAME OVER] You have reached 3/3 mistakes! Type 'new <EASY|MEDIUM|HARD>' to start a fresh game.");
            }
        } catch (IOException ex) {
            out.println("[I/O ERROR] " + ex.getMessage());
        }
        return true;
    }

    private void checkCompletion(Scanner scanner) throws IOException {
        if (controller.getGameState().getBoard().isSolved()) {
            out.println("\n*** CONGRATULATIONS! You solved the Sudoku puzzle! ***");
            out.print("Enter your player name for the leaderboard: ");
            String name = scanner.hasNextLine() ? scanner.nextLine().trim() : "Player";
            if (name.isEmpty()) {
                name = "Player";
            }
            ScoreRecord record = controller.recordCompletedGame(name);
            out.println("[SAVED SCORE] " + record);
            printLeaderboard("score");
        }
    }

    public void printBoardAndStatus() {
        GameState state = controller.getGameState();
        SudokuBoard board = state.getBoard();

        out.println();
        out.printf("Difficulty: %-6s | Mistakes: %d/%d | Score: %-4d | Time: %s | Undo Stack: %d | Hints: %d%n",
                state.getDifficulty().getDisplayName(),
                state.getMistakes(),
                state.getMaxMistakes(),
                state.getScore(),
                state.getFormattedTimer(),
                controller.getCommandHistory().size(),
                state.getHintsRemaining());

        out.println("    C1 C2 C3   C4 C5 C6   C7 C8 C9");
        out.println("  +----------+----------+----------+");
        for (int r = 0; r < SudokuBoard.SIZE; r++) {
            out.printf("R%d| ", r + 1);
            for (int c = 0; c < SudokuBoard.SIZE; c++) {
                SudokuCell cell = board.getCell(r, c);
                int val = cell.getValue();
                String cellStr;
                if (val == 0) {
                    cellStr = " .";
                } else if (!cell.isEditable()) {
                    cellStr = " " + val;
                } else {
                    cellStr = "*" + val;
                }
                out.print(cellStr + " ");
                if ((c + 1) % 3 == 0) {
                    out.print("| ");
                }
            }
            out.println();
            if ((r + 1) % 3 == 0) {
                out.println("  +----------+----------+----------+");
            }
        }
        out.println("  (Legend: ' 5' = Fixed/Hint cell, '*5' = User-entered cell, ' .' = Empty)");
    }

    private void printLeaderboard(String sortMode) {
        List<ScoreRecord> records = switch (sortMode) {
            case "diff", "difficulty" -> controller.getLeaderboardSortedBy(ScoreRecord.BY_DIFFICULTY_AND_TIME);
            case "date" -> controller.getLeaderboardSortedBy(ScoreRecord.BY_DATE_DESCENDING);
            case "time" -> controller.getLeaderboardSortedBy(ScoreRecord.BY_FASTEST_TIME);
            default -> controller.getLeaderboardSortedByScore();
        };

        out.println("\n---------------- HIGH SCORES LEADERBOARD (" + sortMode.toUpperCase(Locale.ROOT) + ") ----------------");
        if (records.isEmpty()) {
            out.println("  No high scores recorded yet.");
        } else {
            for (int i = 0; i < records.size(); i++) {
                out.printf("  #%d  %s%n", i + 1, records.get(i));
            }
        }
        out.println("-------------------------------------------------------------------------");
    }

    public void printHelp() {
        out.println("Commands:");
        out.println("  place <row> <col> <digit>  (or: p r c d)   Place digit 1-9 at row (1-9), col (1-9)");
        out.println("  erase <row> <col>          (or: e r c)     Clear user digit at row (1-9), col (1-9)");
        out.println("  undo                       (or: u)         Undo the last move (Command Pattern Stack)");
        out.println("  hint [row] [col]           (or: h)         Reveal a HintCell on the board");
        out.println("  new [EASY|MEDIUM|HARD]     (or: n)         Start a new game");
        out.println("  save                                       Save current game progress to JSON");
        out.println("  load                                       Load saved game progress from JSON");
        out.println("  scores [score|diff|date]                   View sorted high scores leaderboard");
        out.println("  help                       (or: ?)         Show this command reference");
        out.println("  quit                       (or: q)         Exit SudokuMaster");
    }
}
