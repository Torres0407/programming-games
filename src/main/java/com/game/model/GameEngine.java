package com.game.model;

import java.util.*;

public class GameEngine {
    private static final Map<Integer, int[][]> LEVELS = new HashMap<>();
    private static final Map<Integer, int[]> START_POSITIONS = new HashMap<>();
    private static final Map<Integer, Integer> TARGET_STARS = new HashMap<>();

    static {
        // Hydrate baseline structures for stages 1 to 4
        LEVELS.put(1, new int[][]{{0,0,0,0,0},{0,1,1,2,0},{0,0,0,0,0}}); START_POSITIONS.put(1, new int[]{1, 1}); TARGET_STARS.put(1, 1);
        LEVELS.put(2, new int[][]{{0,0,0,0,0},{0,1,1,2,0},{0,0,1,0,0},{0,0,0,0,0}}); START_POSITIONS.put(2, new int[]{1, 1}); TARGET_STARS.put(2, 1);
        LEVELS.put(3, new int[][]{{0,0,0,0,0,0},{0,2,1,1,2,0},{0,1,0,0,1,0},{0,2,1,1,2,0},{0,0,0,0,0,0}}); START_POSITIONS.put(3, new int[]{1, 2}); TARGET_STARS.put(3, 4);
        LEVELS.put(4, new int[][]{{0,0,0,0,0,0},{0,1,1,1,2,0},{0,0,0,0,1,0},{0,2,1,1,1,0},{0,0,0,0,0,0}}); START_POSITIONS.put(4, new int[]{1, 1}); TARGET_STARS.put(4, 2);

        // Stage 6: The Recursive Staircase (Hardcoded to support staircase testing)
        LEVELS.put(6, new int[][]{{0,0,0,0,0,0,0},{0,1,1,1,1,1,0},{0,1,2,1,1,1,0},{0,1,1,2,1,1,0},{0,0,0,0,0,0,0}});
        START_POSITIONS.put(6, new int[]{1, 1});
        TARGET_STARS.put(6, 2);

        // --- DYNAMIC SCALING ALGORITHMIC PATTERNS FROM STAGES 5 TO 50 ---
        for (int i = 5; i <= 50; i++) {
            if (i == 6) continue;
            int rows = 5 + (int) Math.floor((i - 5) / 12.0); // up to 8 max
            int cols = 7 + (int) Math.floor((i - 5) / 5.0);  // up to 16 max
            
            int[][] dynamicGrid = new int[rows][cols];
            
            // Generate matrix paths
            for (int r = 1; r < rows - 1; r++) {
                for (int c = 1; c < cols - 1; c++) {
                    if ((r % 2 != 0) || (c % 2 != 0) || ((r + c + i) % 3 == 0)) {
                        dynamicGrid[r][c] = 1;
                    }
                }
            }
            
            // Distribute stars dynamically
            int assignedStars = 0;
            for (int r = 1; r < rows - 1; r++) {
                for (int c = 1; c < cols - 1; c++) {
                    if (dynamicGrid[r][c] == 1 && (r * c + i) % 5 == 0) {
                        dynamicGrid[r][c] = 2;
                        assignedStars++;
                    }
                }
            }
            
            // Fallback checkpoint safety constraint
            if (assignedStars == 0) {
                dynamicGrid[rows - 2][cols - 2] = 2;
                assignedStars = 1;
            }

            LEVELS.put(i, dynamicGrid);
            START_POSITIONS.put(i, new int[]{1, 1});
            TARGET_STARS.put(i, assignedStars);
        }
    }

    public static class StepMetadata {
        public int x; int y; String dir; boolean crashed;
        public StepMetadata(int x, int y, String dir, boolean crashed) {
            this.x = x; this.y = y; this.dir = dir; this.crashed = crashed;
        }
    }

    public static class GraphicsExecutionResponse {
        public boolean success; public String message;
        public List<StepMetadata> pathHistory; public int[][] activeGrid;
        public GraphicsExecutionResponse(boolean success, String message, List<StepMetadata> pathHistory, int[][] activeGrid) {
            this.success = success; this.message = message; this.pathHistory = pathHistory; this.activeGrid = activeGrid;
        }
    }

    private static class GridState {
        int currentX;
        int currentY;
        String direction;
        int starsCollected;
        int target;
        int[][] grid;
        List<StepMetadata> pathHistory;
        int opsCounter = 0;
        boolean success = false;
        String error = null;
    }

    public GraphicsExecutionResponse runLevel(int levelId, List<String> mainProc, List<String> p1Proc, List<String> p2Proc) {
        if (!LEVELS.containsKey(levelId)) {
            return new GraphicsExecutionResponse(false, "Invalid Stage ID.", new ArrayList<>(), new int[0][0]);
        }

        int[][] levelGrid = deepCopyMatrix(LEVELS.get(levelId));
        int[] start = START_POSITIONS.get(levelId);
        
        GridState state = new GridState();
        state.currentX = start[0];
        state.currentY = start[1];
        state.direction = "EAST";
        state.starsCollected = 0;
        state.target = TARGET_STARS.get(levelId);
        state.grid = levelGrid;
        state.pathHistory = new ArrayList<>();
        state.pathHistory.add(new StepMetadata(state.currentX, state.currentY, state.direction, false));

        execute(mainProc, p1Proc, p2Proc, state, 0);

        if (state.success) {
            return new GraphicsExecutionResponse(true, "Result: Stage Integration Success! Tracked all " + state.starsCollected + " target loops.", state.pathHistory, state.grid);
        }
        if (state.error != null) {
            return new GraphicsExecutionResponse(false, state.error, state.pathHistory, state.grid);
        }
        return new GraphicsExecutionResponse(false, "Result: Execution Interrupted: Compiled " + state.starsCollected + " out of " + state.target + " targets.", state.pathHistory, state.grid);
    }

    private boolean execute(List<String> tokens, List<String> p1, List<String> p2, GridState state, int depth) {
        if (depth > 60) {
            state.error = "Runtime Stack Panic: Max Call Recursion Depth Exceeded!";
            return false;
        }

        for (String token : tokens) {
            if (state.opsCounter >= 1000) {
                state.error = "Pipeline Timeout: Infinite Execution Loop Halted.";
                return false;
            }

            if (token.equals("P1")) {
                if (!execute(p1, p1, p2, state, depth + 1)) return false;
            } else if (token.equals("P2")) {
                if (!execute(p2, p1, p2, state, depth + 1)) return false;
            } else {
                state.opsCounter++;
                switch (token.toUpperCase()) {
                    case "FORWARD":
                        if (state.direction.equals("NORTH")) state.currentX--;
                        else if (state.direction.equals("SOUTH")) state.currentX++;
                        else if (state.direction.equals("EAST")) state.currentY++;
                        else if (state.direction.equals("WEST")) state.currentY--;
                        break;
                    case "LEFT":
                        state.direction = turnDirection(state.direction, -1);
                        break;
                    case "RIGHT":
                        state.direction = turnDirection(state.direction, 1);
                        break;
                }

                if (state.currentX < 0 || state.currentX >= state.grid.length || state.currentY < 0 || state.currentY >= state.grid[0].length || state.grid[state.currentX][state.currentY] == 0) {
                    state.pathHistory.add(new StepMetadata(state.currentX, state.currentY, state.direction, true));
                    state.error = "Result: System Crash! Collision exception at grid elements (" + state.currentX + ", " + state.currentY + ").";
                    return false;
                }

                if (state.grid[state.currentX][state.currentY] == 2) {
                    state.starsCollected++;
                    state.grid[state.currentX][state.currentY] = 1;
                }

                state.pathHistory.add(new StepMetadata(state.currentX, state.currentY, state.direction, false));

                if (state.starsCollected >= state.target) {
                    state.success = true;
                    return false;
                }
            }
        }
        return true;
    }

    private String turnDirection(String current, int offset) {
        String[] dirs = {"NORTH", "EAST", "SOUTH", "WEST"};
        int idx = Arrays.asList(dirs).indexOf(current);
        return dirs[(idx + offset + 4) % 4];
    }

    private int[][] deepCopyMatrix(int[][] original) {
        int[][] result = new int[original.length][];
        for (int i = 0; i < original.length; i++) result[i] = original[i].clone();
        return result;
    }
}