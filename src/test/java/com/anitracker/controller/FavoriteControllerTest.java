package com.anitracker.controller;

import com.anitracker.exception.FavoriteNotFoundException;
import com.anitracker.model.FavoriteDto;
import com.anitracker.service.FavoriteService;
import com.anitracker.service.FavoriteService.AddResult;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(FavoriteController.class)
class FavoriteControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private FavoriteService favoriteService;

    private static final FavoriteDto SAMPLE_DTO = new FavoriteDto(
            1L, 5114, "Fullmetal Alchemist: Brotherhood",
            "https://cdn.myanimelist.net/images/anime/1223/96541.jpg",
            9.1, LocalDateTime.of(2026, 5, 27, 14, 32, 0)
    );

    // --- GET /api/favorites ---

    @Test
    void listFavorites_returns200WithList() throws Exception {
        when(favoriteService.getFavorites()).thenReturn(List.of(SAMPLE_DTO));

        mockMvc.perform(get("/api/favorites"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].malId").value(5114))
                .andExpect(jsonPath("$[0].title").value("Fullmetal Alchemist: Brotherhood"));
    }

    @Test
    void listFavorites_returns200WithEmptyList() throws Exception {
        when(favoriteService.getFavorites()).thenReturn(List.of());

        mockMvc.perform(get("/api/favorites"))
                .andExpect(status().isOk())
                .andExpect(content().json("[]"));
    }

    // --- POST /api/favorites ---

    @Test
    void addFavorite_returns201ForNewEntry() throws Exception {
        when(favoriteService.addFavorite(any(FavoriteDto.class)))
                .thenReturn(new AddResult(SAMPLE_DTO, true));

        mockMvc.perform(post("/api/favorites")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "malId": 5114,
                                  "title": "Fullmetal Alchemist: Brotherhood",
                                  "imageUrl": "https://cdn.myanimelist.net/images/anime/1223/96541.jpg",
                                  "score": 9.1
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.malId").value(5114));
    }

    @Test
    void addFavorite_returns200ForDuplicate() throws Exception {
        when(favoriteService.addFavorite(any(FavoriteDto.class)))
                .thenReturn(new AddResult(SAMPLE_DTO, false));

        mockMvc.perform(post("/api/favorites")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "malId": 5114,
                                  "title": "Fullmetal Alchemist: Brotherhood"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1));
    }

    @Test
    void addFavorite_returns400WhenMalIdMissing() throws Exception {
        mockMvc.perform(post("/api/favorites")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "title": "Some Anime"
                                }
                                """))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(favoriteService);
    }

    @Test
    void addFavorite_returns400WhenTitleBlank() throws Exception {
        mockMvc.perform(post("/api/favorites")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "malId": 5114,
                                  "title": "   "
                                }
                                """))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(favoriteService);
    }

    // --- DELETE /api/favorites/{malId} ---

    @Test
    void removeFavorite_returns204OnSuccess() throws Exception {
        doNothing().when(favoriteService).removeFavorite(5114);

        mockMvc.perform(delete("/api/favorites/5114"))
                .andExpect(status().isNoContent());
    }

    @Test
    void removeFavorite_returns404WhenNotFound() throws Exception {
        doThrow(new FavoriteNotFoundException(9999)).when(favoriteService).removeFavorite(9999);

        mockMvc.perform(delete("/api/favorites/9999"))
                .andExpect(status().isNotFound());
    }

    // --- GET /api/favorites/{malId} ---

    @Test
    void checkFavorited_returnsTrue() throws Exception {
        when(favoriteService.isFavorited(5114)).thenReturn(true);

        mockMvc.perform(get("/api/favorites/5114"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.favorited").value(true));
    }

    @Test
    void checkFavorited_returnsFalse() throws Exception {
        when(favoriteService.isFavorited(9999)).thenReturn(false);

        mockMvc.perform(get("/api/favorites/9999"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.favorited").value(false));
    }
}
