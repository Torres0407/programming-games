package com.game.controller;

import com.game.model.GameEngine;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/game")
public class GameController {

    public static class MultiRegisterPayload {
        public int level;
        public List<String> mainSequence;
        public List<String> p1Sequence;
        public List<String> p2Sequence;
    }

    @PostMapping("/run-procedural")
    public GameEngine.GraphicsExecutionResponse executeProceduralStage(@RequestBody MultiRegisterPayload payload) {
        GameEngine engine = new GameEngine();
        return engine.runLevel(payload.level, payload.mainSequence, payload.p1Sequence, payload.p2Sequence);
    }
}