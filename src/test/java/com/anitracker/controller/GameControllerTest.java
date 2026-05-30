package com.anitracker.controller;

import com.anitracker.model.GameDto;
import com.anitracker.model.GameRoundDto;
import com.anitracker.service.GameService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(GameController.class)
class GameControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private GameService gameService;

    private static final GameDto SAMPLE_GAME = new GameDto(List.of(
            new GameRoundDto(1,
                    "https://cdn.myanimelist.net/images/anime/1223/96541.jpg",
                    List.of("Fullmetal Alchemist: Brotherhood", "Attack on Titan", "Demon Slayer", "One Piece"),
                    0),
            new GameRoundDto(2,
                    "https://cdn.myanimelist.net/images/anime/1000/110000.jpg",
                    List.of("Naruto", "Attack on Titan", "Bleach", "One Piece"),
                    1)
    ));

    // --- GET /api/game/start ---

    @Test
    void startGame_returns200WithRounds() throws Exception {
        when(gameService.generateGame()).thenReturn(SAMPLE_GAME);

        mockMvc.perform(get("/api/game/start"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.rounds").isArray())
                .andExpect(jsonPath("$.rounds.length()").value(2));
    }

    @Test
    void startGame_roundHasExpectedShape() throws Exception {
        when(gameService.generateGame()).thenReturn(SAMPLE_GAME);

        mockMvc.perform(get("/api/game/start"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.rounds[0].roundNumber").value(1))
                .andExpect(jsonPath("$.rounds[0].imageUrl").value("https://cdn.myanimelist.net/images/anime/1223/96541.jpg"))
                .andExpect(jsonPath("$.rounds[0].choices").isArray())
                .andExpect(jsonPath("$.rounds[0].choices.length()").value(4))
                .andExpect(jsonPath("$.rounds[0].answerIndex").value(0));
    }

    @Test
    void startGame_propagates502WhenJikanInsufficient() throws Exception {
        when(gameService.generateGame())
                .thenThrow(new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Insufficient anime data"));

        mockMvc.perform(get("/api/game/start"))
                .andExpect(status().isBadGateway());
    }

    @Test
    void startGame_answerIndexIsWithinChoicesBounds() throws Exception {
        when(gameService.generateGame()).thenReturn(SAMPLE_GAME);

        mockMvc.perform(get("/api/game/start"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.rounds[0].answerIndex").value(0))
                .andExpect(jsonPath("$.rounds[1].answerIndex").value(1));
    }
}
