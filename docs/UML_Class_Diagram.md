# SudokuMaster — UML Class Diagram & Architecture

This document provides the rendered Mermaid UML Class Diagram (viewable directly in GitHub/GitLab/Markdown viewers) alongside the editable PlantUML source located at [uml_diagram.puml](uml_diagram.puml).

## Mermaid UML Class Diagram

```mermaid
classDiagram
    direction TB

    class Difficulty {
        <<enumeration>>
        EASY
        MEDIUM
        HARD
        -String displayName
        -int initialClues
        -int pointsPerMove
        +getDisplayName() String
        +getInitialClues() int
        +getPointsPerMove() int
    }

    class SudokuCell {
        <<abstract>>
        -int row
        -int col
        -int solutionValue
        -int value
        #SudokuCell(int row, int col, int value, int solutionValue)
        #setInternalValue(int newValue) void
        +getRow() int
        +getCol() int
        +getValue() int
        +getSolutionValue() int
        +isEmpty() boolean
        +isCorrect() boolean
        +setValue(int newValue)* void
        +isEditable()* boolean
        +getDisplayColor()* Color
        +getCellType()* String
        +copy()* SudokuCell
    }

    class FixedCell {
        +FixedCell(int row, int col, int clueValue)
        +setValue(int newValue) void
        +isEditable() boolean
        +getDisplayColor() Color
        +getCellType() String
        +copy() SudokuCell
    }

    class UserCell {
        +UserCell(int row, int col, int initialValue, int solutionValue)
        +setValue(int newValue) void
        +isEditable() boolean
        +getDisplayColor() Color
        +getCellType() String
        +copy() SudokuCell
    }

    class HintCell {
        +HintCell(int row, int col, int solutionValue)
        +setValue(int newValue) void
        +isEditable() boolean
        +getDisplayColor() Color
        +getCellType() String
        +copy() SudokuCell
    }

    class SudokuBoard {
        +int SIZE = 9$
        +int SUBGRID_SIZE = 3$
        -SudokuCell[][] grid
        +getCell(int row, int col) SudokuCell
        +replaceCell(int row, int col, SudokuCell newCell) void
        +isPlacementValidInCurrentGrid(int row, int col, int digit) boolean
        +isSolved() boolean
    }

    class GameState {
        -SudokuBoard board
        -Difficulty difficulty
        -int maxMistakes
        -int mistakes
        -int elapsedSeconds
        -int score
        -int hintsRemaining
        -String playerName
        +isGameOverByMistakes() boolean
        +incrementMistakes() void
        +incrementElapsedSeconds() void
    }

    class ScoreRecord {
        +Comparator~ScoreRecord~ BY_DIFFICULTY_AND_TIME$
        +Comparator~ScoreRecord~ BY_DATE_DESCENDING$
        -String playerName
        -int score
        -int completionSeconds
        -int mistakes
        -Difficulty difficulty
        -LocalDateTime completedAt
        +compareTo(ScoreRecord other) int
    }

    class SudokuCommand {
        <<interface>>
        +execute() void
        +undo() void
        +getDescription() String
    }

    class PlaceDigitCommand {
        -SudokuBoard board
        -int row
        -int col
        -int newDigit
        -int previousValue
        -boolean executed
        +execute() void
        +undo() void
        +getDescription() String
    }

    class EraseDigitCommand {
        -SudokuBoard board
        -int row
        -int col
        -int erasedValue
        -boolean executed
        +execute() void
        +undo() void
        +getDescription() String
    }

    class CommandHistory {
        -Stack~SudokuCommand~ historyStack
        +executeCommand(SudokuCommand command) void
        +undoLast() Optional~SudokuCommand~
        +canUndo() boolean
        +size() int
    }

    class InvalidSudokuMoveException {
        -int row
        -int col
        -int attemptedValue
        -boolean strikeInduced
        +isStrikeInduced() boolean
    }

    class GamePersistenceManager {
        -Path scoresFilePath
        -Path saveFilePath
        +saveHighScores(List~ScoreRecord~ scores) void
        +loadHighScores() ArrayList~ScoreRecord~
        +saveGameState(GameState state) void
        +loadGameState() GameState
    }

    class SudokuGameController {
        -SudokuGenerator generator
        -GamePersistenceManager persistenceManager
        -CommandHistory commandHistory
        -ArrayList~ScoreRecord~ leaderboard
        -GameState gameState
        +placeDigit(int row, int col, int digit) void
        +eraseDigit(int row, int col) void
        +undo() Optional~SudokuCommand~
        +useHint(int row, int col) HintCell
    }

    SudokuCell <|-- FixedCell : extends
    SudokuCell <|-- UserCell : extends
    SudokuCell <|-- HintCell : extends

    SudokuBoard "1" *-- "81" SudokuCell : composition
    GameState "1" *-- "1" SudokuBoard : composition
    GameState "1" --> "1" Difficulty : association
    ScoreRecord "*" --> "1" Difficulty : association

    SudokuCommand <|.. PlaceDigitCommand : implements
    SudokuCommand <|.. EraseDigitCommand : implements
    CommandHistory "1" o-- "*" SudokuCommand : aggregation
    PlaceDigitCommand --> SudokuBoard : receiver
    EraseDigitCommand --> SudokuBoard : receiver

    SudokuGameController "1" *-- "1" CommandHistory : composition
    SudokuGameController "1" *-- "1" GameState : composition
    SudokuGameController "1" o-- "*" ScoreRecord : aggregation
    SudokuGameController "1" --> "1" GamePersistenceManager : association
    SudokuGameController ..> InvalidSudokuMoveException : throws
```
