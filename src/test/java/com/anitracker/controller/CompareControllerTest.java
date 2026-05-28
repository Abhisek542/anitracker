package com.anitracker.controller;

import com.anitracker.exception.AnimeNotFoundException;
import com.anitracker.model.AnimeDetailDto;
import com.anitracker.model.ComparisonDto;
import com.anitracker.service.JikanService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.when;
import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AnimeController.class)
class CompareControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private JikanService jikanService;

    private static final AnimeDetailDto FMA = new AnimeDetailDto(
            5114,
            "Fullmetal Alchemist: Brotherhood",
            "https://cdn.myanimelist.net/images/anime/1223/96541.jpg",
            9.11,
            64,
            "TV",
            "Two brothers.",
            List.of("Action", "Adventure"),
            null
    );

    private static final AnimeDetailDto BEBOP = new AnimeDetailDto(
            1,
            "Cowboy Bebop",
            "https://cdn.myanimelist.net/images/anime/4/19644.jpg",
            8.76,
            26,
            "TV",
            "Space bounty hunters.",
            List.of("Action", "Sci-Fi"),
            null
    );

    @Test
    void compare_returns200WithLeftAndRight() throws Exception {
        when(jikanService.getComparison(5114, 1)).thenReturn(new ComparisonDto(FMA, BEBOP));

        mockMvc.perform(get("/api/anime/compare").param("id1", "5114").param("id2", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.left.malId", is(5114)))
                .andExpect(jsonPath("$.left.title", is("Fullmetal Alchemist: Brotherhood")))
                .andExpect(jsonPath("$.left.score", is(9.11)))
                .andExpect(jsonPath("$.left.episodes", is(64)))
                .andExpect(jsonPath("$.left.type", is("TV")))
                .andExpect(jsonPath("$.left.genres[0]", is("Action")))
                .andExpect(jsonPath("$.right.malId", is(1)))
                .andExpect(jsonPath("$.right.title", is("Cowboy Bebop")));
    }

    @Test
    void compare_returns400WhenSameIdGiven() throws Exception {
        when(jikanService.getComparison(5114, 5114))
                .thenThrow(new ResponseStatusException(BAD_REQUEST, "id1 and id2 must be different"));

        mockMvc.perform(get("/api/anime/compare").param("id1", "5114").param("id2", "5114"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void compare_returns404WhenLeftAnimeNotFound() throws Exception {
        when(jikanService.getComparison(9999, 1)).thenThrow(new AnimeNotFoundException(9999));

        mockMvc.perform(get("/api/anime/compare").param("id1", "9999").param("id2", "1"))
                .andExpect(status().isNotFound());
    }

    @Test
    void compare_returns404WhenRightAnimeNotFound() throws Exception {
        when(jikanService.getComparison(5114, 9999)).thenThrow(new AnimeNotFoundException(9999));

        mockMvc.perform(get("/api/anime/compare").param("id1", "5114").param("id2", "9999"))
                .andExpect(status().isNotFound());
    }

    @Test
    void compare_returns400WhenId1Missing() throws Exception {
        mockMvc.perform(get("/api/anime/compare").param("id2", "1"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void compare_returns400WhenId2Missing() throws Exception {
        mockMvc.perform(get("/api/anime/compare").param("id1", "5114"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void compare_returns400WhenId1IsNotInteger() throws Exception {
        mockMvc.perform(get("/api/anime/compare").param("id1", "abc").param("id2", "1"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void compare_returnsImageUrlsInBothSlots() throws Exception {
        when(jikanService.getComparison(5114, 1)).thenReturn(new ComparisonDto(FMA, BEBOP));

        mockMvc.perform(get("/api/anime/compare").param("id1", "5114").param("id2", "1"))
                .andExpect(jsonPath("$.left.imageUrl", is("https://cdn.myanimelist.net/images/anime/1223/96541.jpg")))
                .andExpect(jsonPath("$.right.imageUrl", is("https://cdn.myanimelist.net/images/anime/4/19644.jpg")));
    }

    @Test
    void compare_handlesNullScoreAndEpisodes() throws Exception {
        AnimeDetailDto noScore = new AnimeDetailDto(42, "Unknown Anime", null, null, null, "OVA", null, List.of(), null);
        AnimeDetailDto other = new AnimeDetailDto(1, "Cowboy Bebop", null, 8.76, 26, "TV", null, List.of(), null);

        when(jikanService.getComparison(42, 1)).thenReturn(new ComparisonDto(noScore, other));

        mockMvc.perform(get("/api/anime/compare").param("id1", "42").param("id2", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.left.score").doesNotExist())
                .andExpect(jsonPath("$.left.episodes").doesNotExist());
    }
}
