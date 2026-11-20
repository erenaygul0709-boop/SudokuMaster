package com.sudokumaster;

import com.sudokumaster.service.SudokuGameController;
import com.sudokumaster.ui.cli.ConsoleUI;
import com.sudokumaster.ui.gui.SudokuFrame;

import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import java.awt.GraphicsEnvironment;

/**
 * Primary application entry point for SudokuMaster.
 * <ul>
 *   <li>By default, launches the modern desktop Java Swing GUI ({@link SudokuFrame}).</li>
 *   <li>When invoked with {@code --cli} or {@code --console} (or in a headless environment),
 *       launches the interactive Console CLI ({@link ConsoleUI}).</li>
 * </ul>
 */
public class Main {

    public static void main(String[] args) {
        boolean forceCli = false;
        if (args != null) {
            for (String arg : args) {
                if ("--cli".equalsIgnoreCase(arg) || "--console".equalsIgnoreCase(arg) || "-c".equalsIgnoreCase(arg)) {
                    forceCli = true;
                    break;
                }
            }
        }

        SudokuGameController controller = new SudokuGameController();

        if (forceCli || GraphicsEnvironment.isHeadless()) {
            ConsoleUI consoleUI = new ConsoleUI(controller);
            consoleUI.start();
        } else {
            SwingUtilities.invokeLater(() -> {
                try {
                    UIManager.setLookAndFeel(UIManager.getCrossPlatformLookAndFeelClassName());
                } catch (Exception ignored) {
                    // Fallback to default Look and Feel
                }
                SudokuFrame frame = new SudokuFrame(controller);
                frame.setVisible(true);
            });
        }
    }
}
