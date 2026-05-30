package com.anitracker.controller;

import com.anitracker.model.GameDto;
import com.anitracker.service.GameService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/game")
public class GameController {

    private final GameService gameService;

    public GameController(GameService gameService) {
        this.gameService = gameService;
    }

    @GetMapping("/start")
    public GameDto startGame() {
        return gameService.generateGame();
    }
}
