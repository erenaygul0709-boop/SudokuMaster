# SudokuMaster — User Stories & Backlog

## Target Users & Problem Statement
Logic puzzle enthusiasts and students want a clean, responsive Sudoku game that enforces standard 9x9 Sudoku rules, prevents accidental modification of initial puzzle clues, supports multi-step Undo when exploring candidate digits, tracks mistake strikes (`0/3`), and preserves both unfinished games and high-score leaderboards across sessions.

---

## User Stories

### US-1: Interactive 9x9 Sudoku Gameplay (GUI & Console CLI)
- **As a** player,
- **I want to** play Sudoku in either a modern desktop Swing GUI (with 3x3 box borders, status bar, and 1–9 keypad) or an interactive Console CLI mode,
- **So that** I can enjoy a visual mobile-inspired experience on desktop or play directly inside a terminal session.
- **Acceptance Criteria:**
  - Running `Main` without arguments opens the desktop Swing GUI (`SudokuFrame`).
  - Running `Main --cli` opens the interactive Console CLI (`ConsoleUI`).
  - Both modes display the 9x9 grid, difficulty, elapsed timer, score, and mistakes (`Mistakes: X/3`).

### US-2: Polymorphic Cell Behavior & Rule Validation
- **As a** player,
- **I want** initial puzzle clues (`FixedCell`) and revealed hints (`HintCell`) to be protected from accidental overwrites while editable cells (`UserCell`) validate row, column, and 3x3 box uniqueness,
- **So that** the integrity of the puzzle is preserved and rule violations are caught immediately.
- **Acceptance Criteria:**
  - Attempting to modify a `FixedCell` or `HintCell` throws `InvalidSudokuMoveException` with a clear message and does not increment mistake strikes.
  - Placing a conflicting digit in the same row, column, or 3x3 box throws `InvalidSudokuMoveException`, increments `Mistakes` (`1/3`, `2/3`, `3/3`), and blocks moves once `3/3` is reached.

### US-3: Multi-Level Undo via Command Pattern
- **As a** player,
- **I want to** undo my previous digit placements and erasures step-by-step using the "Undo (Geri Al)" button or `undo` console command,
- **So that** I can backtrack cleanly without restarting the entire puzzle.
- **Acceptance Criteria:**
  - Every valid `PlaceDigitCommand` and `EraseDigitCommand` is pushed onto a `Stack<SudokuCommand>` managed by `CommandHistory`.
  - Clicking "Undo (Geri Al)" or typing `undo` pops the most recent command and restores the exact previous cell value.

### US-4: Save & Load Unfinished Game Progress
- **As a** player,
- **I want to** save my current board, timer, score, difficulty, and mistake count to disk and load it later,
- **So that** I never lose progress on a puzzle when closing the application.
- **Acceptance Criteria:**
  - `GamePersistenceManager` writes and reads `data/saved_game.json` using `try-with-resources`.
  - All 81 cell types (`FIXED`, `USER`, `HINT`), values, timer, and mistakes are accurately restored.

### US-5: High Scores Leaderboard & Multi-Criteria Sorting
- **As a** competitive player,
- **I want to** view a persistent leaderboard sorted by highest score (`Comparable<ScoreRecord>`) or by difficulty/time and date (`Comparator<ScoreRecord>`),
- **So that** I can track my best performances over time.
- **Acceptance Criteria:**
  - High scores are stored in an `ArrayList<ScoreRecord>` and persisted to `data/highscores.json`.
  - Users can sort by Score (`Comparable`), Difficulty & Fastest Time (`Comparator`), or Date (`Comparator`).
