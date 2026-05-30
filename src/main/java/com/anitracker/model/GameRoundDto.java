package com.anitracker.model;

import java.util.List;

public record GameRoundDto(
        int roundNumber,
        String imageUrl,
        List<String> choices,
        int answerIndex
) {}
