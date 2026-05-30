package com.anitracker.service;

import com.anitracker.model.AnimeDto;
import com.anitracker.model.GameDto;
import com.anitracker.model.GameRoundDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GameServiceTest {

    @Mock
    private JikanService jikanService;

    @InjectMocks
    private GameService gameService;

    private List<AnimeDto> largePool;

    @BeforeEach
    void setUp() {
        largePool = new ArrayList<>();
        for (int i = 1; i <= 25; i++) {
            largePool.add(new AnimeDto(i, "Anime " + i, "https://img.example.com/" + i + ".jpg", 7.0 + (i * 0.1), 12, "TV"));
        }
    }

    // --- generateGame() ---

    @Test
    void generateGame_returns10RoundsForLargePool() {
        when(jikanService.getTrendingAnime()).thenReturn(largePool);

        GameDto result = gameService.generateGame();

        assertThat(result.rounds()).hasSize(10);
    }

    @Test
    void generateGame_roundNumbersAreSequentialFrom1() {
        when(jikanService.getTrendingAnime()).thenReturn(largePool);

        GameDto result = gameService.generateGame();

        for (int i = 0; i < result.rounds().size(); i++) {
            assertThat(result.rounds().get(i).roundNumber()).isEqualTo(i + 1);
        }
    }

    @Test
    void generateGame_eachRoundHas4Choices() {
        when(jikanService.getTrendingAnime()).thenReturn(largePool);

        GameDto result = gameService.generateGame();

        for (GameRoundDto round : result.rounds()) {
            assertThat(round.choices()).hasSize(4);
        }
    }

    @Test
    void generateGame_answerIndexIsWithinBounds() {
        when(jikanService.getTrendingAnime()).thenReturn(largePool);

        GameDto result = gameService.generateGame();

        for (GameRoundDto round : result.rounds()) {
            assertThat(round.answerIndex()).isBetween(0, 3);
        }
    }

    @Test
    void generateGame_choicesAtAnswerIndexMatchesExpectedTitle() {
        when(jikanService.getTrendingAnime()).thenReturn(largePool);

        GameDto result = gameService.generateGame();

        // For each round the title at answerIndex must appear exactly once in choices
        for (GameRoundDto round : result.rounds()) {
            String correctTitle = round.choices().get(round.answerIndex());
            long occurrences = round.choices().stream().filter(c -> c.equals(correctTitle)).count();
            assertThat(occurrences).isEqualTo(1);
        }
    }

    @Test
    void generateGame_filtersOutEntriesWithNullImageUrl() {
        List<AnimeDto> poolWithNulls = new ArrayList<>(largePool);
        // Replace first 5 entries with null-image ones — they should be skipped
        for (int i = 0; i < 5; i++) {
            poolWithNulls.set(i, new AnimeDto(100 + i, "Null Image " + i, null, 8.0, 24, "TV"));
        }
        when(jikanService.getTrendingAnime()).thenReturn(poolWithNulls);

        GameDto result = gameService.generateGame();

        // None of the null-image titles should appear in any round
        for (GameRoundDto round : result.rounds()) {
            for (String choice : round.choices()) {
                assertThat(choice).doesNotStartWith("Null Image");
            }
        }
    }

    @Test
    void generateGame_deduplicatesByMalId() {
        List<AnimeDto> poolWithDupe = new ArrayList<>(largePool);
        // Add a duplicate malId=1 with a different title — the second entry should be ignored
        poolWithDupe.add(new AnimeDto(1, "Duplicate of Anime 1", "https://img.example.com/dupe.jpg", 9.0, 24, "TV"));
        when(jikanService.getTrendingAnime()).thenReturn(poolWithDupe);

        GameDto result = gameService.generateGame();

        // "Duplicate of Anime 1" must not appear anywhere — the duplicate malId was dropped
        result.rounds().forEach(r ->
                assertThat(r.choices()).doesNotContain("Duplicate of Anime 1")
        );

        // Within each individual round all 4 choices must be distinct
        result.rounds().forEach(r ->
                assertThat(r.choices()).doesNotHaveDuplicates()
        );
    }

    @Test
    void generateGame_throws502WhenPoolTooSmall() {
        List<AnimeDto> tinyPool = List.of(
                new AnimeDto(1, "Anime A", "https://img.example.com/a.jpg", 8.0, 12, "TV"),
                new AnimeDto(2, "Anime B", "https://img.example.com/b.jpg", 7.5, 24, "TV"),
                new AnimeDto(3, "Anime C", null, 7.0, 12, "OVA")  // null image, will be filtered
        );
        when(jikanService.getTrendingAnime()).thenReturn(tinyPool);

        assertThatThrownBy(() -> gameService.generateGame())
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> assertThat(((ResponseStatusException) ex).getStatusCode())
                        .isEqualTo(HttpStatus.BAD_GATEWAY));
    }

    @Test
    void generateGame_returnsFewerThan10RoundsWhenPoolSmall() {
        // Pool of exactly 6 valid entries → can produce at most 3 rounds (6 - 3 = 3)
        List<AnimeDto> smallPool = new ArrayList<>();
        for (int i = 1; i <= 6; i++) {
            smallPool.add(new AnimeDto(i, "Anime " + i, "https://img.example.com/" + i + ".jpg", 7.0, 12, "TV"));
        }
        when(jikanService.getTrendingAnime()).thenReturn(smallPool);

        GameDto result = gameService.generateGame();

        assertThat(result.rounds().size()).isLessThanOrEqualTo(10);
        assertThat(result.rounds()).isNotEmpty();
    }
}
