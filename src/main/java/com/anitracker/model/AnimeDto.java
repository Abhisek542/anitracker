package com.anitracker.model;

public record AnimeDto(
        int malId,
        String title,
        String imageUrl,
        Double score,
        Integer episodes,
        String type
) {}
