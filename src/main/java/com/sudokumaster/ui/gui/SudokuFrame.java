package com.sudokumaster.ui.gui;

import com.sudokumaster.command.SudokuCommand;
import com.sudokumaster.domain.Difficulty;
import com.sudokumaster.domain.GameState;
import com.sudokumaster.domain.HintCell;
import com.sudokumaster.domain.ScoreRecord;
import com.sudokumaster.domain.SudokuCell;
import com.sudokumaster.exception.InvalidSudokuMoveException;
import com.sudokumaster.service.SudokuGameController;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.SwingConstants;
import javax.swing.Timer;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.io.IOException;
import java.util.List;
import java.util.Optional;

/**
 * Primary desktop Java Swing GUI window inspired by modern mobile Sudoku games.
 */
public class SudokuFrame extends JFrame {

    private static final Color HEADER_BG = new Color(15, 23, 42);
    private static final Color SURFACE_BG = new Color(248, 250, 252);
    private static final Color CARD_BG = Color.WHITE;
    private static final Color PRIMARY_BLUE = new Color(37, 99, 235);
    private static final Color DANGER_RED = new Color(220, 38, 38);
    private static final Color SUCCESS_GREEN = new Color(5, 150, 105);
    private static final Color NEUTRAL_DARK = new Color(51, 65, 85);

    private final SudokuGameController controller;
    private final SudokuGridPanel gridPanel;

    private final JLabel difficultyBadgeLabel;
    private final JLabel mistakesLabel;
    private final JLabel scoreLabel;
    private final JLabel timerLabel;
    private final JLabel statusMessageLabel;
    private final JButton undoButton;
    private final JButton eraseButton;
    private final JButton hintButton;
    private final JComboBox<Difficulty> difficultyCombo;
    private final JButton[] keypadButtons = new JButton[9];
    private final Timer swingTimer;

    public SudokuFrame(SudokuGameController controller) {
        super("SudokuMaster — Java OOP Edition");
        this.controller = controller;

        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setMinimumSize(new Dimension(560, 720));
        setSize(620, 780);
        setLocationRelativeTo(null);
        getContentPane().setBackground(SURFACE_BG);
        setLayout(new BorderLayout(0, 8));

        // 1. Top Header & Status Bar
        JPanel topContainer = new JPanel(new BorderLayout());
        topContainer.setBackground(SURFACE_BG);

        JPanel appBar = new JPanel(new BorderLayout(12, 0));
        appBar.setBackground(HEADER_BG);
        appBar.setBorder(new EmptyBorder(12, 18, 12, 18));

        JLabel titleLabel = new JLabel("SudokuMaster");
        titleLabel.setFont(new Font("Segoe UI", Font.BOLD, 22));
        titleLabel.setForeground(Color.WHITE);
        appBar.add(titleLabel, BorderLayout.WEST);

        JPanel topRightControls = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        topRightControls.setOpaque(false);

        difficultyCombo = new JComboBox<>(Difficulty.values());
        difficultyCombo.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        difficultyCombo.setSelectedItem(controller.getGameState().getDifficulty());

        JButton newGameBtn = createHeaderButton("New Game", PRIMARY_BLUE);
        newGameBtn.addActionListener(e -> promptNewGame());

        JButton saveBtn = createHeaderButton("Save", new Color(71, 85, 105));
        saveBtn.addActionListener(e -> handleSaveGame());

        JButton loadBtn = createHeaderButton("Load", new Color(71, 85, 105));
        loadBtn.addActionListener(e -> handleLoadGame());

        JButton scoresBtn = createHeaderButton("Leaderboard", new Color(217, 119, 6));
        scoresBtn.addActionListener(e -> showLeaderboardDialog());

        topRightControls.add(difficultyCombo);
        topRightControls.add(newGameBtn);
        topRightControls.add(saveBtn);
        topRightControls.add(loadBtn);
        topRightControls.add(scoresBtn);
        appBar.add(topRightControls, BorderLayout.EAST);

        // Status metrics bar (Difficulty | Mistakes: 0/3 | Score: 0 | Time: 00:00)
        JPanel metricsBar = new JPanel(new GridLayout(1, 4, 10, 0));
        metricsBar.setBackground(CARD_BG);
        metricsBar.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 1, 0, new Color(226, 232, 240)),
                new EmptyBorder(10, 18, 10, 18)
        ));

        difficultyBadgeLabel = createMetricLabel("Difficulty: Easy", NEUTRAL_DARK);
        mistakesLabel = createMetricLabel("Mistakes: 0/3", NEUTRAL_DARK);
        scoreLabel = createMetricLabel("Score: 0", PRIMARY_BLUE);
        timerLabel = createMetricLabel("Time: 00:00", NEUTRAL_DARK);

        metricsBar.add(difficultyBadgeLabel);
        metricsBar.add(mistakesLabel);
        metricsBar.add(scoreLabel);
        metricsBar.add(timerLabel);

        topContainer.add(appBar, BorderLayout.NORTH);
        topContainer.add(metricsBar, BorderLayout.SOUTH);
        add(topContainer, BorderLayout.NORTH);

        // 2. Center Interactive 9x9 Sudoku Grid
        gridPanel = new SudokuGridPanel(controller.getGameState().getBoard());
        gridPanel.setOnCellSelected((r, c) -> updateStatusForCell(r, c));
        add(gridPanel, BorderLayout.CENTER);

        // 3. Bottom Control Deck (Status banner + Action Buttons + 1..9 Numeric Keypad)
        JPanel bottomDeck = new JPanel();
        bottomDeck.setLayout(new BoxLayout(bottomDeck, BoxLayout.Y_AXIS));
        bottomDeck.setBackground(SURFACE_BG);
        bottomDeck.setBorder(new EmptyBorder(4, 18, 16, 18));

        statusMessageLabel = new JLabel("Select a cell and press a number (1-9) to play.", SwingConstants.CENTER);
        statusMessageLabel.setAlignmentX(CENTER_ALIGNMENT);
        statusMessageLabel.setFont(new Font("Segoe UI", Font.BOLD, 13));
        statusMessageLabel.setForeground(NEUTRAL_DARK);
        statusMessageLabel.setBorder(new EmptyBorder(4, 4, 8, 4));
        bottomDeck.add(statusMessageLabel);

        // Action row: Undo (Geri Al), Erase, Hint
        JPanel actionPanel = new JPanel(new GridLayout(1, 3, 12, 0));
        actionPanel.setBackground(SURFACE_BG);
        actionPanel.setMaximumSize(new Dimension(540, 44));

        undoButton = createActionButton("↶ Undo (Geri Al)", new Color(51, 65, 85));
        undoButton.addActionListener(e -> handleUndo());

        eraseButton = createActionButton("⌫ Erase", new Color(185, 28, 28));
        eraseButton.addActionListener(e -> handleErase());

        hintButton = createActionButton("💡 Hint (3)", SUCCESS_GREEN);
        hintButton.addActionListener(e -> handleHint());

        actionPanel.add(undoButton);
        actionPanel.add(eraseButton);
        actionPanel.add(hintButton);
        bottomDeck.add(actionPanel);
        bottomDeck.add(Box.createVerticalStrut(10));

        // Numeric keypad 1..9
        JPanel keypadPanel = new JPanel(new GridLayout(1, 9, 6, 0));
        keypadPanel.setBackground(SURFACE_BG);
        keypadPanel.setMaximumSize(new Dimension(560, 56));

        for (int digit = 1; digit <= 9; digit++) {
            final int d = digit;
            JButton btn = new JButton(String.valueOf(d));
            btn.setFont(new Font("Segoe UI", Font.BOLD, 20));
            btn.setBackground(CARD_BG);
            btn.setForeground(PRIMARY_BLUE);
            btn.setFocusPainted(false);
            btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            btn.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(new Color(191, 219, 254), 2, true),
                    new EmptyBorder(8, 0, 8, 0)
            ));
            btn.addActionListener(e -> handlePlaceDigit(d));
            keypadButtons[digit - 1] = btn;
            keypadPanel.add(btn);
        }
        bottomDeck.add(keypadPanel);

        add(bottomDeck, BorderLayout.SOUTH);

        // Keyboard bindings for seamless desktop play
        installKeyBindings();

        // 1-second Swing timer
        swingTimer = new Timer(1000, e -> {
            GameState state = controller.getGameState();
            if (!state.isGameOverByMistakes() && !state.getBoard().isSolved()) {
                state.incrementElapsedSeconds();
                timerLabel.setText("Time: " + state.getFormattedTimer());
            }
        });
        swingTimer.start();

        refreshUI();
    }

    private void installKeyBindings() {
        KeyAdapter keyAdapter = new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                int code = e.getKeyCode();
                if (code >= KeyEvent.VK_1 && code <= KeyEvent.VK_9) {
                    handlePlaceDigit(code - KeyEvent.VK_0);
                } else if (code >= KeyEvent.VK_NUMPAD1 && code <= KeyEvent.VK_NUMPAD9) {
                    handlePlaceDigit(code - KeyEvent.VK_NUMPAD0);
                } else if (code == KeyEvent.VK_BACK_SPACE || code == KeyEvent.VK_DELETE || code == KeyEvent.VK_0) {
                    handleErase();
                } else if (code == KeyEvent.VK_U || (code == KeyEvent.VK_Z && e.isControlDown())) {
                    handleUndo();
                } else if (code == KeyEvent.VK_H) {
                    handleHint();
                } else if (code == KeyEvent.VK_UP || code == KeyEvent.VK_W) {
                    gridPanel.moveSelection(-1, 0);
                } else if (code == KeyEvent.VK_DOWN || code == KeyEvent.VK_S) {
                    gridPanel.moveSelection(1, 0);
                } else if (code == KeyEvent.VK_LEFT || code == KeyEvent.VK_A) {
                    gridPanel.moveSelection(0, -1);
                } else if (code == KeyEvent.VK_RIGHT || code == KeyEvent.VK_D) {
                    gridPanel.moveSelection(0, 1);
                }
            }
        };
        gridPanel.addKeyListener(keyAdapter);
        addKeyListener(keyAdapter);
    }

    private void handlePlaceDigit(int digit) {
        int r = gridPanel.getSelectedRow();
        int c = gridPanel.getSelectedCol();
        try {
            controller.placeDigit(r, c, digit);
            setStatusMessage(String.format("Placed %d at Row %d, Col %d.", digit, r + 1, c + 1), SUCCESS_GREEN);
            refreshUI();
            checkGameCompletion();
        } catch (InvalidSudokuMoveException ex) {
            refreshUI();
            setStatusMessage(ex.getMessage(), DANGER_RED);
            if (controller.getGameState().isGameOverByMistakes()) {
                JOptionPane.showMessageDialog(
                        this,
                        ex.getMessage() + "\n\nMistakes reached 3/3. Click 'New Game' to try again!",
                        "Game Over — Strike Limit Reached",
                        JOptionPane.ERROR_MESSAGE
                );
            }
        }
        gridPanel.requestFocusInWindow();
    }

    private void handleErase() {
        int r = gridPanel.getSelectedRow();
        int c = gridPanel.getSelectedCol();
        try {
            controller.eraseDigit(r, c);
            setStatusMessage(String.format("Cleared cell at Row %d, Col %d.", r + 1, c + 1), NEUTRAL_DARK);
            refreshUI();
        } catch (InvalidSudokuMoveException ex) {
            setStatusMessage(ex.getMessage(), DANGER_RED);
        }
        gridPanel.requestFocusInWindow();
    }

    private void handleUndo() {
        Optional<SudokuCommand> undone = controller.undo();
        if (undone.isPresent()) {
            setStatusMessage("Undid move: " + undone.get().getDescription(), PRIMARY_BLUE);
            refreshUI();
        } else {
            setStatusMessage("Nothing to undo — command history stack is empty.", NEUTRAL_DARK);
        }
        gridPanel.requestFocusInWindow();
    }

    private void handleHint() {
        int r = gridPanel.getSelectedRow();
        int c = gridPanel.getSelectedCol();
        try {
            HintCell hint = controller.useHint(r, c);
            gridPanel.setSelectedCell(hint.getRow(), hint.getCol());
            setStatusMessage(
                    String.format("Hint revealed: %d at Row %d, Col %d!",
                            hint.getValue(), hint.getRow() + 1, hint.getCol() + 1),
                    SUCCESS_GREEN
            );
            refreshUI();
            checkGameCompletion();
        } catch (InvalidSudokuMoveException ex) {
            setStatusMessage(ex.getMessage(), DANGER_RED);
        }
        gridPanel.requestFocusInWindow();
    }

    private void promptNewGame() {
        Difficulty selectedDiff = (Difficulty) difficultyCombo.getSelectedItem();
        if (selectedDiff == null) {
            selectedDiff = Difficulty.EASY;
        }
        controller.startNewGame(selectedDiff, controller.getGameState().getPlayerName());
        gridPanel.setBoard(controller.getGameState().getBoard());
        setStatusMessage("Started a new " + selectedDiff.getDisplayName() + " Sudoku game!", PRIMARY_BLUE);
        refreshUI();
        gridPanel.requestFocusInWindow();
    }

    private void handleSaveGame() {
        try {
            controller.saveCurrentGame();
            setStatusMessage("Game progress saved to " + controller.getGameState().getDifficulty().getDisplayName() + " save file.", SUCCESS_GREEN);
            JOptionPane.showMessageDialog(this,
                    "Unfinished puzzle progress (board, timer, mistakes, score) saved successfully!",
                    "Game Saved", JOptionPane.INFORMATION_MESSAGE);
        } catch (IOException ex) {
            setStatusMessage("Failed to save game: " + ex.getMessage(), DANGER_RED);
        }
        gridPanel.requestFocusInWindow();
    }

    private void handleLoadGame() {
        try {
            controller.loadSavedGame();
            gridPanel.setBoard(controller.getGameState().getBoard());
            difficultyCombo.setSelectedItem(controller.getGameState().getDifficulty());
            setStatusMessage("Loaded saved game progress!", SUCCESS_GREEN);
            refreshUI();
        } catch (IOException ex) {
            setStatusMessage("Could not load saved game: " + ex.getMessage(), DANGER_RED);
            JOptionPane.showMessageDialog(this,
                    ex.getMessage(),
                    "Load Game", JOptionPane.WARNING_MESSAGE);
        }
        gridPanel.requestFocusInWindow();
    }

    private void checkGameCompletion() {
        GameState state = controller.getGameState();
        if (state.getBoard().isSolved()) {
            String playerName = JOptionPane.showInputDialog(
                    this,
                    String.format("Congratulations! You solved the %s puzzle in %s with %d/%d mistakes!\nEnter your name for the Leaderboard:",
                            state.getDifficulty().getDisplayName(),
                            state.getFormattedTimer(),
                            state.getMistakes(),
                            state.getMaxMistakes()),
                    state.getPlayerName()
            );
            if (playerName == null || playerName.trim().isEmpty()) {
                playerName = "Player";
            }
            try {
                ScoreRecord record = controller.recordCompletedGame(playerName);
                refreshUI();
                setStatusMessage("Puzzle Solved! Final Score: " + record.getScore(), SUCCESS_GREEN);
                showLeaderboardDialog();
            } catch (IOException ex) {
                setStatusMessage("Solved! (Could not save high score: " + ex.getMessage() + ")", DANGER_RED);
            }
        }
    }

    private void showLeaderboardDialog() {
        JDialog dialog = new JDialog(this, "SudokuMaster — High Scores Leaderboard", true);
        dialog.setSize(600, 380);
        dialog.setLocationRelativeTo(this);
        dialog.setLayout(new BorderLayout(8, 8));

        String[] columns = {"Rank", "Player", "Difficulty", "Score", "Time", "Mistakes", "Date"};
        DefaultTableModel tableModel = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        JTable table = new JTable(tableModel);
        table.setRowHeight(26);
        table.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        table.getTableHeader().setFont(new Font("Segoe UI", Font.BOLD, 13));

        Runnable populateByNaturalOrder = () -> {
            tableModel.setRowCount(0);
            List<ScoreRecord> list = controller.getLeaderboardSortedByScore();
            for (int i = 0; i < list.size(); i++) {
                ScoreRecord r = list.get(i);
                tableModel.addRow(new Object[]{
                        i + 1, r.getPlayerName(), r.getDifficulty().getDisplayName(),
                        r.getScore(), r.getFormattedTime(), r.getMistakes(), r.getFormattedDate()
                });
            }
        };

        Runnable populateByDifficultyAndTime = () -> {
            tableModel.setRowCount(0);
            List<ScoreRecord> list = controller.getLeaderboardSortedBy(ScoreRecord.BY_DIFFICULTY_AND_TIME);
            for (int i = 0; i < list.size(); i++) {
                ScoreRecord r = list.get(i);
                tableModel.addRow(new Object[]{
                        i + 1, r.getPlayerName(), r.getDifficulty().getDisplayName(),
                        r.getScore(), r.getFormattedTime(), r.getMistakes(), r.getFormattedDate()
                });
            }
        };

        Runnable populateByDate = () -> {
            tableModel.setRowCount(0);
            List<ScoreRecord> list = controller.getLeaderboardSortedBy(ScoreRecord.BY_DATE_DESCENDING);
            for (int i = 0; i < list.size(); i++) {
                ScoreRecord r = list.get(i);
                tableModel.addRow(new Object[]{
                        i + 1, r.getPlayerName(), r.getDifficulty().getDisplayName(),
                        r.getScore(), r.getFormattedTime(), r.getMistakes(), r.getFormattedDate()
                });
            }
        };

        populateByNaturalOrder.run();

        JPanel sortPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 8));
        JButton sortScoreBtn = new JButton("Sort by Score (Comparable)");
        sortScoreBtn.addActionListener(e -> populateByNaturalOrder.run());

        JButton sortDiffBtn = new JButton("Sort by Difficulty & Time (Comparator)");
        sortDiffBtn.addActionListener(e -> populateByDifficultyAndTime.run());

        JButton sortDateBtn = new JButton("Sort by Date (Comparator)");
        sortDateBtn.addActionListener(e -> populateByDate.run());

        sortPanel.add(sortScoreBtn);
        sortPanel.add(sortDiffBtn);
        sortPanel.add(sortDateBtn);

        dialog.add(sortPanel, BorderLayout.NORTH);
        dialog.add(new JScrollPane(table), BorderLayout.CENTER);
        dialog.setVisible(true);
    }

    private void updateStatusForCell(int r, int c) {
        SudokuCell cell = controller.getGameState().getBoard().getCell(r, c);
        String typeDesc = switch (cell.getCellType()) {
            case "FIXED" -> "Fixed Clue (Unmodifiable)";
            case "HINT" -> "Revealed Hint Cell";
            default -> "Editable Player Cell";
        };
        setStatusMessage(String.format("Selected (%d, %d) — %s", r + 1, c + 1, typeDesc), NEUTRAL_DARK);
    }

    private void setStatusMessage(String msg, Color color) {
        statusMessageLabel.setText(msg);
        statusMessageLabel.setForeground(color);
    }

    public void refreshUI() {
        GameState state = controller.getGameState();
        difficultyBadgeLabel.setText("Difficulty: " + state.getDifficulty().getDisplayName());
        mistakesLabel.setText(String.format("Mistakes: %d/%d", state.getMistakes(), state.getMaxMistakes()));
        mistakesLabel.setForeground(state.getMistakes() > 0 ? DANGER_RED : NEUTRAL_DARK);
        scoreLabel.setText("Score: " + state.getScore());
        timerLabel.setText("Time: " + state.getFormattedTimer());

        undoButton.setText(String.format("↶ Undo / Geri Al (%d)", controller.getCommandHistory().size()));
        undoButton.setEnabled(controller.getCommandHistory().canUndo());
        hintButton.setText(String.format("💡 Hint (%d)", state.getHintsRemaining()));
        hintButton.setEnabled(state.getHintsRemaining() > 0 && !state.isGameOverByMistakes());

        for (int digit = 1; digit <= 9; digit++) {
            int count = state.getBoard().countDigitOccurrences(digit);
            JButton btn = keypadButtons[digit - 1];
            btn.setEnabled(!state.isGameOverByMistakes());
            btn.setForeground(count >= 9 ? new Color(148, 163, 184) : PRIMARY_BLUE);
        }

        gridPanel.repaint();
    }

    private JButton createHeaderButton(String text, Color bg) {
        JButton btn = new JButton(text);
        btn.setFont(new Font("Segoe UI", Font.BOLD, 12));
        btn.setBackground(bg);
        btn.setForeground(Color.WHITE);
        btn.setFocusPainted(false);
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btn.setBorder(new EmptyBorder(6, 12, 6, 12));
        return btn;
    }

    private JButton createActionButton(String text, Color fgColor) {
        JButton btn = new JButton(text);
        btn.setFont(new Font("Segoe UI", Font.BOLD, 13));
        btn.setBackground(CARD_BG);
        btn.setForeground(fgColor);
        btn.setFocusPainted(false);
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btn.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(203, 213, 225), 1, true),
                new EmptyBorder(8, 10, 8, 10)
        ));
        return btn;
    }

    private JLabel createMetricLabel(String text, Color fg) {
        JLabel lbl = new JLabel(text, SwingConstants.CENTER);
        lbl.setFont(new Font("Segoe UI", Font.BOLD, 14));
        lbl.setForeground(fg);
        return lbl;
    }
}
