package com.anitracker.model;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;

public record FavoriteDto(
        Long id,
        @NotNull(message = "malId is required") Integer malId,
        @NotBlank(message = "title must not be blank") String title,
        String imageUrl,
        Double score,
        LocalDateTime addedAt
) {}
