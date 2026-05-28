package com.anitracker.controller;

import com.anitracker.exception.AnimeNotFoundException;
import com.anitracker.model.AnimeDetailDto;
import com.anitracker.service.JikanService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AnimeController.class)
class AnimeControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private JikanService jikanService;

    private static final AnimeDetailDto SAMPLE_DETAIL = new AnimeDetailDto(
            5114,
            "Fullmetal Alchemist: Brotherhood",
            "https://cdn.myanimelist.net/images/anime/1223/96541.jpg",
            9.1,
            64,
            "TV",
            "Two brothers seek the Philosopher's Stone.",
            List.of("Action", "Adventure"),
            "https://www.youtube.com/watch?v=abc"
    );

    @Test
    void getDetail_returns200WithAllDetailFields() throws Exception {
        when(jikanService.getAnimeDetail(5114)).thenReturn(SAMPLE_DETAIL);

        mockMvc.perform(get("/api/anime/5114"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.malId", is(5114)))
                .andExpect(jsonPath("$.title", is("Fullmetal Alchemist: Brotherhood")))
                .andExpect(jsonPath("$.imageUrl", is("https://cdn.myanimelist.net/images/anime/1223/96541.jpg")))
                .andExpect(jsonPath("$.score", is(9.1)))
                .andExpect(jsonPath("$.episodes", is(64)))
                .andExpect(jsonPath("$.type", is("TV")))
                .andExpect(jsonPath("$.synopsis", is("Two brothers seek the Philosopher's Stone.")))
                .andExpect(jsonPath("$.genres[0]", is("Action")))
                .andExpect(jsonPath("$.genres[1]", is("Adventure")))
                .andExpect(jsonPath("$.trailerUrl", is("https://www.youtube.com/watch?v=abc")));
    }

    @Test
    void getDetail_returns404ForInvalidId() throws Exception {
        when(jikanService.getAnimeDetail(9999)).thenThrow(new AnimeNotFoundException(9999));

        mockMvc.perform(get("/api/anime/9999"))
                .andExpect(status().isNotFound());
    }

    @Test
    void getDetail_returns200WhenSynopsisIsNull() throws Exception {
        AnimeDetailDto dto = new AnimeDetailDto(
                5114,
                "Fullmetal Alchemist: Brotherhood",
                "https://cdn.myanimelist.net/images/anime/1223/96541.jpg",
                9.1,
                64,
                "TV",
                null,
                List.of("Action", "Adventure"),
                "https://www.youtube.com/watch?v=abc"
        );
        when(jikanService.getAnimeDetail(5114)).thenReturn(dto);

        mockMvc.perform(get("/api/anime/5114"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.synopsis").value(nullValue()));
    }
}
