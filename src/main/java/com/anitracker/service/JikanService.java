package com.anitracker.service;

import com.anitracker.model.AnimeDto;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.List;

@Service
public class JikanService {

    private static final String JIKAN_BASE_URL = "https://api.jikan.moe/v4";

    private final RestClient restClient;

    public JikanService(RestClient.Builder builder) {
        this.restClient = builder
                .baseUrl(JIKAN_BASE_URL)
                .build();
    }

    public List<AnimeDto> getTrendingAnime() {
        JikanTopAnimeResponse response = restClient.get()
                .uri("/top/anime")
                .retrieve()
                .body(JikanTopAnimeResponse.class);

        if (response == null || response.data() == null) {
            return List.of();
        }

        return response.data().stream()
                .map(entry -> new AnimeDto(
                        entry.malId(),
                        entry.title(),
                        entry.images() != null && entry.images().jpg() != null
                                ? entry.images().jpg().imageUrl()
                                : null,
                        entry.score(),
                        entry.episodes(),
                        entry.type()
                ))
                .toList();
    }

    // --- Jikan API response shape (used only for deserialization) ---

    record JikanTopAnimeResponse(List<JikanAnimeEntry> data) {}

    record JikanAnimeEntry(
            @JsonProperty("mal_id") int malId,
            String title,
            JikanImages images,
            Double score,
            Integer episodes,
            String type
    ) {}

    record JikanImages(JikanJpgImages jpg) {}

    record JikanJpgImages(
            @JsonProperty("image_url") String imageUrl
    ) {}
}
