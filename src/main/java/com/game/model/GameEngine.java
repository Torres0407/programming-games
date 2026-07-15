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

        // --- DYNAMIC SCALING ALGORITHMIC PATTERNS FROM STAGES 5 TO 50 ---
        for (int i = 5; i <= 50; i++) {
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

    public GraphicsExecutionResponse runLevel(int levelId, List<String> mainProc, List<String> p1Proc, List<String> p2Proc) {
        if (!LEVELS.containsKey(levelId)) {
            return new GraphicsExecutionResponse(false, "Invalid Stage ID.", new ArrayList<>(), new int[0][0]);
        }

        int[][] levelGrid = deepCopyMatrix(LEVELS.get(levelId));
        int[] start = START_POSITIONS.get(levelId);
        int currentX = start[0]; int currentY = start[1];
        String direction = "EAST"; int starsCollected = 0; int target = TARGET_STARS.get(levelId);

        List<StepMetadata> pathHistory = new ArrayList<>();
        pathHistory.add(new StepMetadata(currentX, currentY, direction, false));

        List<String> flatInstructions = new ArrayList<>();
        try {
            flattenInstructions(mainProc, p1Proc, p2Proc, flatInstructions, 0);
        } catch (StackOverflowError e) {
            return new GraphicsExecutionResponse(false, "Runtime Stack Panic: Max Call Recursion Depth Exceeded!", pathHistory, levelGrid);
        }

        int opsCounter = 0;
        for (String cmd : flatInstructions) {
            opsCounter++;
            if (opsCounter > 1000) { 
                return new GraphicsExecutionResponse(false, "Pipeline Timeout: Infinite Execution Loop Halted.", pathHistory, levelGrid);
            }

            switch (cmd.toUpperCase()) {
                case "FORWARD":
                    if (direction.equals("NORTH")) currentX--;
                    else if (direction.equals("SOUTH")) currentX++;
                    else if (direction.equals("EAST")) currentY++;
                    else if (direction.equals("WEST")) currentY--;
                    break;
                case "LEFT":
                    direction = turnDirection(direction, -1);
                    break;
                case "RIGHT":
                    direction = turnDirection(direction, 1);
                    break;
            }

            if (currentX < 0 || currentX >= levelGrid.length || currentY < 0 || currentY >= levelGrid[0].length || levelGrid[currentX][currentY] == 0) {
                pathHistory.add(new StepMetadata(currentX, currentY, direction, true));
                return new GraphicsExecutionResponse(false, "Result: System Crash! Collision exception at grid elements (" + currentX + ", " + currentY + ").", pathHistory, levelGrid);
            }

            if (levelGrid[currentX][currentY] == 2) {
                starsCollected++;
                levelGrid[currentX][currentY] = 1;
            }

            pathHistory.add(new StepMetadata(currentX, currentY, direction, false));
        }

        if (starsCollected >= target) {
            return new GraphicsExecutionResponse(true, "Result: Stage Integration Success! Tracked all " + starsCollected + " target loops.", pathHistory, levelGrid);
        }
        return new GraphicsExecutionResponse(false, "Result: Execution Interrupted: Compiled " + starsCollected + " out of " + target + " targets.", pathHistory, levelGrid);
    }

    private void flattenInstructions(List<String> currentTokens, List<String> p1, List<String> p2, List<String> output, int depth) {
        if (depth > 60) throw new StackOverflowError(); 
        
        for (String token : currentTokens) {
            if (token.equals("P1")) {
                flattenInstructions(p1, p1, p2, output, depth + 1);
            } else if (token.equals("P2")) {
                flattenInstructions(p2, p1, p2, output, depth + 1);
            } else {
                output.add(token);
            }
        }
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