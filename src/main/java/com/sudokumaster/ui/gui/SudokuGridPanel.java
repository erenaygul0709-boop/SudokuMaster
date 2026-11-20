package com.sudokumaster.ui.gui;

import com.sudokumaster.domain.SudokuBoard;
import com.sudokumaster.domain.SudokuCell;

import javax.swing.JPanel;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.function.BiConsumer;

/**
 * Custom-rendered 9x9 interactive Sudoku grid panel inspired by modern mobile Sudoku apps.
 * Features distinct 3x3 box borders, selected cell & peer highlighting, and polymorphic cell colors.
 */
public class SudokuGridPanel extends JPanel {

    private static final Color BG_OUTER = new Color(248, 250, 252);
    private static final Color CELL_DEFAULT_BG = Color.WHITE;
    private static final Color CELL_FIXED_BG = new Color(241, 245, 249);
    private static final Color CELL_PEER_BG = new Color(226, 232, 240);
    private static final Color CELL_SAME_DIGIT_BG = new Color(191, 219, 254);
    private static final Color CELL_SELECTED_BG = new Color(147, 197, 253);
    private static final Color GRID_THIN_LINE = new Color(203, 213, 225);
    private static final Color GRID_BOX_LINE = new Color(30, 41, 59);

    private SudokuBoard board;
    private int selectedRow = 0;
    private int selectedCol = 0;
    private BiConsumer<Integer, Integer> onCellSelected;

    public SudokuGridPanel(SudokuBoard board) {
        this.board = board;
        setBackground(BG_OUTER);
        setPreferredSize(new Dimension(500, 500));
        setFocusable(true);

        addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                requestFocusInWindow();
                handleMouseClick(e.getX(), e.getY());
            }
        });
    }

    public void setBoard(SudokuBoard board) {
        this.board = board;
        repaint();
    }

    public void setOnCellSelected(BiConsumer<Integer, Integer> onCellSelected) {
        this.onCellSelected = onCellSelected;
    }

    public int getSelectedRow() {
        return selectedRow;
    }

    public int getSelectedCol() {
        return selectedCol;
    }

    public void setSelectedCell(int row, int col) {
        this.selectedRow = Math.max(0, Math.min(8, row));
        this.selectedCol = Math.max(0, Math.min(8, col));
        if (onCellSelected != null) {
            onCellSelected.accept(this.selectedRow, this.selectedCol);
        }
        repaint();
    }

    public void moveSelection(int dRow, int dCol) {
        setSelectedCell((selectedRow + dRow + 9) % 9, (selectedCol + dCol + 9) % 9);
    }

    private void handleMouseClick(int mouseX, int mouseY) {
        int[] geom = computeGridGeometry();
        int startX = geom[0];
        int startY = geom[1];
        int cellSize = geom[2];
        int boardPixels = cellSize * 9;

        if (mouseX >= startX && mouseX < startX + boardPixels
                && mouseY >= startY && mouseY < startY + boardPixels) {
            int col = (mouseX - startX) / cellSize;
            int row = (mouseY - startY) / cellSize;
            setSelectedCell(row, col);
        }
    }

    private int[] computeGridGeometry() {
        int pad = 16;
        int availW = Math.max(180, getWidth() - pad * 2);
        int availH = Math.max(180, getHeight() - pad * 2);
        int side = Math.min(availW, availH);
        int cellSize = side / 9;
        int boardPixels = cellSize * 9;
        int startX = (getWidth() - boardPixels) / 2;
        int startY = (getHeight() - boardPixels) / 2;
        return new int[]{startX, startY, cellSize};
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        if (board == null) {
            return;
        }

        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_LCD_HRGB);

        int[] geom = computeGridGeometry();
        int startX = geom[0];
        int startY = geom[1];
        int cellSize = geom[2];
        int boardPixels = cellSize * 9;

        int selectedDigit = (selectedRow >= 0 && selectedCol >= 0)
                ? board.getCell(selectedRow, selectedCol).getValue()
                : 0;

        // 1. Paint cell backgrounds
        for (int r = 0; r < 9; r++) {
            for (int c = 0; c < 9; c++) {
                SudokuCell cell = board.getCell(r, c);
                int x = startX + c * cellSize;
                int y = startY + r * cellSize;

                Color bg = getCellBackgroundColor(r, c, cell, selectedDigit);
                g2.setColor(bg);
                g2.fillRect(x, y, cellSize, cellSize);
            }
        }

        // 2. Paint cell digits using polymorphic getDisplayColor()
        Font fixedFont = new Font("Segoe UI", Font.BOLD, Math.max(16, (int) (cellSize * 0.52)));
        Font editableFont = new Font("Segoe UI", Font.PLAIN, Math.max(16, (int) (cellSize * 0.52)));
        Font badgeFont = new Font("Segoe UI", Font.BOLD, Math.max(9, (int) (cellSize * 0.18)));

        for (int r = 0; r < 9; r++) {
            for (int c = 0; c < 9; c++) {
                SudokuCell cell = board.getCell(r, c);
                int val = cell.getValue();
                if (val != 0) {
                    int x = startX + c * cellSize;
                    int y = startY + r * cellSize;

                    g2.setFont(cell.isEditable() ? editableFont : fixedFont);
                    g2.setColor(cell.getDisplayColor()); // Runtime polymorphism!

                    String text = String.valueOf(val);
                    FontMetrics fm = g2.getFontMetrics();
                    int textX = x + (cellSize - fm.stringWidth(text)) / 2;
                    int textY = y + ((cellSize - fm.getHeight()) / 2) + fm.getAscent();
                    g2.drawString(text, textX, textY);

                    if ("HINT".equals(cell.getCellType())) {
                        g2.setFont(badgeFont);
                        g2.drawString("★", x + cellSize - 13, y + 13);
                    }
                }
            }
        }

        // 3. Paint thin cell gridlines (1px)
        g2.setColor(GRID_THIN_LINE);
        g2.setStroke(new BasicStroke(1.0f));
        for (int i = 1; i < 9; i++) {
            if (i % 3 != 0) {
                int pos = i * cellSize;
                g2.drawLine(startX + pos, startY, startX + pos, startY + boardPixels);
                g2.drawLine(startX, startY + pos, startX + boardPixels, startY + pos);
            }
        }

        // 4. Paint bold 3x3 subgrid borders and outer border (3px)
        g2.setColor(GRID_BOX_LINE);
        g2.setStroke(new BasicStroke(3.0f));
        for (int i = 0; i <= 9; i += 3) {
            int pos = i * cellSize;
            g2.drawLine(startX + pos, startY, startX + pos, startY + boardPixels);
            g2.drawLine(startX, startY + pos, startX + boardPixels, startY + pos);
        }

        g2.dispose();
    }

    private Color getCellBackgroundColor(int r, int c, SudokuCell cell, int selectedDigit) {
        if (r == selectedRow && c == selectedCol) {
            return CELL_SELECTED_BG;
        }
        if (selectedDigit != 0 && cell.getValue() == selectedDigit) {
            return CELL_SAME_DIGIT_BG;
        }
        boolean sameBox = (r / 3 == selectedRow / 3) && (c / 3 == selectedCol / 3);
        if (r == selectedRow || c == selectedCol || sameBox) {
            return CELL_PEER_BG;
        }
        return cell.isEditable() ? CELL_DEFAULT_BG : CELL_FIXED_BG;
    }
}
