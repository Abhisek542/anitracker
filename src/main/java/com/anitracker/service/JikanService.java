package com.anitracker.service;

import com.anitracker.exception.AnimeNotFoundException;
import com.anitracker.model.AnimeDetailDto;
import com.anitracker.model.AnimeDto;
import com.anitracker.model.ComparisonDto;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.server.ResponseStatusException;

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

        return toAnimeDtoList(response);
    }

    public List<AnimeDto> searchAnime(String query) {
        JikanTopAnimeResponse response = restClient.get()
                .uri("/anime?q={q}", query)
                .retrieve()
                .body(JikanTopAnimeResponse.class);

        return toAnimeDtoList(response);
    }

    public AnimeDetailDto getAnimeDetail(int id) {
        JikanSingleAnimeResponse response = restClient.get()
                .uri("/anime/{id}", id)
                .retrieve()
                .body(JikanSingleAnimeResponse.class);

        if (response == null || response.data() == null) {
            throw new AnimeNotFoundException(id);
        }

        JikanAnimeDetailEntry entry = response.data();
        String imageUrl = entry.images() != null && entry.images().jpg() != null
                ? entry.images().jpg().imageUrl()
                : null;
        List<String> genres = entry.genres() != null
                ? entry.genres().stream().map(JikanGenre::name).toList()
                : List.of();
        String trailerUrl = entry.trailer() != null ? entry.trailer().url() : null;

        return new AnimeDetailDto(
                entry.malId(),
                entry.title(),
                imageUrl,
                entry.score(),
                entry.episodes(),
                entry.type(),
                entry.synopsis(),
                genres,
                trailerUrl
        );
    }

    public ComparisonDto getComparison(int id1, int id2) {
        if (id1 == id2) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "id1 and id2 must be different");
        }
        AnimeDetailDto left = getAnimeDetail(id1);
        AnimeDetailDto right = getAnimeDetail(id2);
        return new ComparisonDto(left, right);
    }

    private List<AnimeDto> toAnimeDtoList(JikanTopAnimeResponse response) {
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

    record JikanSingleAnimeResponse(JikanAnimeDetailEntry data) {}

    record JikanAnimeDetailEntry(
            @JsonProperty("mal_id") int malId,
            String title,
            JikanImages images,
            Double score,
            Integer episodes,
            String type,
            String synopsis,
            List<JikanGenre> genres,
            JikanTrailer trailer
    ) {}

    record JikanGenre(String name) {}

    record JikanTrailer(String url) {}
}
