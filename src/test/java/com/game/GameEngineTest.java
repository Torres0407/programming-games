package com.game;

import com.game.model.GameEngine;
import org.junit.jupiter.api.Test;
import java.util.Arrays;
import java.util.ArrayList;
import static org.junit.jupiter.api.Assertions.*;

public class GameEngineTest {

    @Test
    public void testProceduralStaircaseRecursion() {
        GameEngine engine = new GameEngine();
        
        // Stage 6 Solution: MAIN executes P1 to kickstart loop operations
        java.util.List<String> mainSeq = Arrays.asList("P1");
        
        // P1 moves across the stairs and triggers infinite recursive self-loop calls
        java.util.List<String> p1Seq = Arrays.asList("FORWARD", "RIGHT", "FORWARD", "LEFT", "P1");
        java.util.List<String> p2Seq = new ArrayList<>(); // Unused register channel slot
        
        GameEngine.GraphicsExecutionResponse response = engine.runLevel(6, mainSeq, p1Seq, p2Seq);
        
        // Assert that loop evaluation cleanly cleared targets before tripping timeout safeguards
        assertTrue(response.success, "The recursive block structure solution algorithm should clear stage objectives.");
    }
}
