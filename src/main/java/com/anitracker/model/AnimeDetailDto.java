package com.anitracker.model;

import java.util.List;

public record AnimeDetailDto(
        int malId,
        String title,
        String imageUrl,
        Double score,
        Integer episodes,
        String type,
        String synopsis,
        List<String> genres,
        String trailerUrl
) {}
