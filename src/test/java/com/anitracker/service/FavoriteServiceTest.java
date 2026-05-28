package com.anitracker.service;

import com.anitracker.exception.FavoriteNotFoundException;
import com.anitracker.model.Favorite;
import com.anitracker.model.FavoriteDto;
import com.anitracker.repository.FavoriteRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FavoriteServiceTest {

    @Mock
    private FavoriteRepository favoriteRepository;

    @InjectMocks
    private FavoriteService favoriteService;

    private Favorite sampleFavorite;

    @BeforeEach
    void setUp() {
        sampleFavorite = new Favorite();
        sampleFavorite.setId(1L);
        sampleFavorite.setMalId(5114);
        sampleFavorite.setTitle("Fullmetal Alchemist: Brotherhood");
        sampleFavorite.setImageUrl("https://cdn.myanimelist.net/images/anime/1223/96541.jpg");
        sampleFavorite.setScore(9.1);
        sampleFavorite.setAddedAt(LocalDateTime.now());
    }

    // --- getFavorites() ---

    @Test
    void getFavorites_returnsMappedDtos() {
        when(favoriteRepository.findAllByOrderByAddedAtDesc()).thenReturn(List.of(sampleFavorite));

        List<FavoriteDto> result = favoriteService.getFavorites();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).malId()).isEqualTo(5114);
        assertThat(result.get(0).title()).isEqualTo("Fullmetal Alchemist: Brotherhood");
    }

    @Test
    void getFavorites_returnsEmptyListWhenNoFavorites() {
        when(favoriteRepository.findAllByOrderByAddedAtDesc()).thenReturn(List.of());

        List<FavoriteDto> result = favoriteService.getFavorites();

        assertThat(result).isEmpty();
    }

    // --- addFavorite() ---

    @Test
    void addFavorite_createsNewEntry_whenMalIdNotExists() {
        FavoriteDto request = new FavoriteDto(null, 5114, "FMA: Brotherhood", null, 9.1, null);

        when(favoriteRepository.findByMalId(5114)).thenReturn(Optional.empty());
        when(favoriteRepository.save(any(Favorite.class))).thenReturn(sampleFavorite);

        FavoriteService.AddResult result = favoriteService.addFavorite(request);

        assertThat(result.created()).isTrue();
        assertThat(result.dto().malId()).isEqualTo(5114);
        verify(favoriteRepository).save(any(Favorite.class));
    }

    @Test
    void addFavorite_returnsExisting_whenMalIdAlreadyExists() {
        FavoriteDto request = new FavoriteDto(null, 5114, "FMA: Brotherhood", null, 9.1, null);

        when(favoriteRepository.findByMalId(5114)).thenReturn(Optional.of(sampleFavorite));

        FavoriteService.AddResult result = favoriteService.addFavorite(request);

        assertThat(result.created()).isFalse();
        assertThat(result.dto().id()).isEqualTo(1L);
        verify(favoriteRepository, never()).save(any());
    }

    @Test
    void addFavorite_truncatesImageUrlOver1024Chars() {
        String longUrl = "https://example.com/" + "x".repeat(1020);
        FavoriteDto request = new FavoriteDto(null, 9999, "Some Anime", longUrl, null, null);

        when(favoriteRepository.findByMalId(9999)).thenReturn(Optional.empty());
        when(favoriteRepository.save(any(Favorite.class))).thenAnswer(inv -> {
            Favorite f = inv.getArgument(0);
            f.setId(2L);
            f.setAddedAt(LocalDateTime.now());
            return f;
        });

        favoriteService.addFavorite(request);

        verify(favoriteRepository).save(argThat(f -> f.getImageUrl().length() == 1024));
    }

    // --- removeFavorite() ---

    @Test
    void removeFavorite_deletesEntry_whenMalIdExists() {
        when(favoriteRepository.findByMalId(5114)).thenReturn(Optional.of(sampleFavorite));

        favoriteService.removeFavorite(5114);

        verify(favoriteRepository).deleteById(1L);
    }

    @Test
    void removeFavorite_throwsNotFoundException_whenMalIdNotFound() {
        when(favoriteRepository.findByMalId(9999)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> favoriteService.removeFavorite(9999))
                .isInstanceOf(FavoriteNotFoundException.class)
                .hasMessageContaining("9999");
    }

    // --- isFavorited() ---

    @Test
    void isFavorited_returnsTrue_whenExists() {
        when(favoriteRepository.existsByMalId(5114)).thenReturn(true);
        assertThat(favoriteService.isFavorited(5114)).isTrue();
    }

    @Test
    void isFavorited_returnsFalse_whenNotExists() {
        when(favoriteRepository.existsByMalId(9999)).thenReturn(false);
        assertThat(favoriteService.isFavorited(9999)).isFalse();
    }
}
