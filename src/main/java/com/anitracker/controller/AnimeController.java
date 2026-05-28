package com.anitracker.controller;

import com.anitracker.model.AnimeDto;
import com.anitracker.service.JikanService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/anime")
public class AnimeController {

    private final JikanService jikanService;

    public AnimeController(JikanService jikanService) {
        this.jikanService = jikanService;
    }

    @GetMapping("/trending")
    public List<AnimeDto> getTrending() {
        return jikanService.getTrendingAnime();
    }
}
