package com.sudokumaster.service;

import com.sudokumaster.domain.Difficulty;
import com.sudokumaster.domain.SudokuBoard;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

/**
 * Generates valid 9x9 Sudoku puzzles with complete solutions for any {@link Difficulty}.
 */
public class SudokuGenerator {

    private static final int[][] BASE_SOLUTION = {
            {5, 3, 4, 6, 7, 8, 9, 1, 2},
            {6, 7, 2, 1, 9, 5, 3, 4, 8},
            {1, 9, 8, 3, 4, 2, 5, 6, 7},
            {8, 5, 9, 7, 6, 1, 4, 2, 3},
            {4, 2, 6, 8, 5, 3, 7, 9, 1},
            {7, 1, 3, 9, 2, 4, 8, 5, 6},
            {9, 6, 1, 5, 3, 7, 2, 8, 4},
            {2, 8, 7, 4, 1, 9, 6, 3, 5},
            {3, 4, 5, 2, 8, 6, 1, 7, 9}
    };

    private final Random random;

    public SudokuGenerator() {
        this(new Random());
    }

    public SudokuGenerator(Random random) {
        this.random = random;
    }

    /**
     * Generates a randomized valid {@link SudokuBoard} for the specified difficulty.
     */
    public SudokuBoard generateBoard(Difficulty difficulty) {
        int[][] solution = createRandomizedSolution();
        int[][] clues = maskClues(solution, difficulty.getInitialClues());
        return new SudokuBoard(clues, solution);
    }

    /**
     * Creates a deterministic, canonical SudokuBoard useful for repeatable unit tests.
     */
    public static SudokuBoard createDeterministicBoard() {
        int[][] solution = copyMatrix(BASE_SOLUTION);
        int[][] clues = copyMatrix(BASE_SOLUTION);
        // Clear specific cells for predictable testing:
        // (0, 2) solution is 4; (0, 3) solution is 6; (1, 1) solution is 7; (4, 4) solution is 5
        clues[0][2] = 0;
        clues[0][3] = 0;
        clues[1][1] = 0;
        clues[4][4] = 0;
        clues[8][8] = 0;
        return new SudokuBoard(clues, solution);
    }

    private int[][] createRandomizedSolution() {
        int[][] grid = copyMatrix(BASE_SOLUTION);

        // 1. Relabel digits 1..9 randomly
        List<Integer> digits = new ArrayList<>();
        for (int i = 1; i <= 9; i++) {
            digits.add(i);
        }
        Collections.shuffle(digits, random);
        for (int r = 0; r < 9; r++) {
            for (int c = 0; c < 9; c++) {
                grid[r][c] = digits.get(grid[r][c] - 1);
            }
        }

        // 2. Shuffle rows within each 3-row band
        for (int band = 0; band < 3; band++) {
            int start = band * 3;
            for (int i = 0; i < 3; i++) {
                int r1 = start + random.nextInt(3);
                int r2 = start + random.nextInt(3);
                int[] temp = grid[r1];
                grid[r1] = grid[r2];
                grid[r2] = temp;
            }
        }

        // 3. Shuffle columns within each 3-column stack
        for (int stack = 0; stack < 3; stack++) {
            int start = stack * 3;
            for (int i = 0; i < 3; i++) {
                int c1 = start + random.nextInt(3);
                int c2 = start + random.nextInt(3);
                for (int r = 0; r < 9; r++) {
                    int temp = grid[r][c1];
                    grid[r][c1] = grid[r][c2];
                    grid[r][c2] = temp;
                }
            }
        }

        return grid;
    }

    private int[][] maskClues(int[][] solution, int cluesToKeep) {
        int[][] clues = copyMatrix(solution);
        List<int[]> positions = new ArrayList<>(81);
        for (int r = 0; r < 9; r++) {
            for (int c = 0; c < 9; c++) {
                positions.add(new int[]{r, c});
            }
        }
        Collections.shuffle(positions, random);

        int cellsToRemove = Math.max(0, Math.min(81, 81 - cluesToKeep));
        for (int i = 0; i < cellsToRemove; i++) {
            int[] pos = positions.get(i);
            clues[pos[0]][pos[1]] = 0;
        }
        return clues;
    }

    private static int[][] copyMatrix(int[][] src) {
        int[][] copy = new int[src.length][src[0].length];
        for (int r = 0; r < src.length; r++) {
            System.arraycopy(src[r], 0, copy[r], 0, src[r].length);
        }
        return copy;
    }
}
