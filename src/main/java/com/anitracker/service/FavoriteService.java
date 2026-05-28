package com.anitracker.service;

import com.anitracker.exception.FavoriteNotFoundException;
import com.anitracker.model.Favorite;
import com.anitracker.model.FavoriteDto;
import com.anitracker.repository.FavoriteRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class FavoriteService {

    private static final int MAX_IMAGE_URL_LENGTH = 1024;

    private final FavoriteRepository favoriteRepository;

    public FavoriteService(FavoriteRepository favoriteRepository) {
        this.favoriteRepository = favoriteRepository;
    }

    /** Returns all favorites ordered most-recently-added first. */
    public List<FavoriteDto> getFavorites() {
        return favoriteRepository.findAllByOrderByAddedAtDesc()
                .stream()
                .map(this::toDto)
                .toList();
    }

    /**
     * Adds a new favorite, or returns the existing one if {@code malId} is already saved.
     *
     * @return an {@link AddResult} containing the DTO and a flag indicating whether a new row was created
     */
    public AddResult addFavorite(FavoriteDto request) {
        Optional<Favorite> existing = favoriteRepository.findByMalId(request.malId());
        if (existing.isPresent()) {
            return new AddResult(toDto(existing.get()), false);
        }

        Favorite entity = toEntity(request);
        Favorite saved = favoriteRepository.save(entity);
        return new AddResult(toDto(saved), true);
    }

    /**
     * Removes the favorite with the given {@code malId}.
     *
     * @throws FavoriteNotFoundException if no favorite with that {@code malId} exists
     */
    public void removeFavorite(int malId) {
        Favorite favorite = favoriteRepository.findByMalId(malId)
                .orElseThrow(() -> new FavoriteNotFoundException(malId));
        favoriteRepository.deleteById(favorite.getId());
    }

    /** Returns {@code true} if an anime with the given {@code malId} is in the favorites list. */
    public boolean isFavorited(int malId) {
        return favoriteRepository.existsByMalId(malId);
    }

    // --- Mapping helpers ---

    private FavoriteDto toDto(Favorite entity) {
        return new FavoriteDto(
                entity.getId(),
                entity.getMalId(),
                entity.getTitle(),
                entity.getImageUrl(),
                entity.getScore(),
                entity.getAddedAt()
        );
    }

    private Favorite toEntity(FavoriteDto dto) {
        Favorite entity = new Favorite();
        entity.setMalId(dto.malId());
        entity.setTitle(dto.title());
        entity.setImageUrl(truncate(dto.imageUrl(), MAX_IMAGE_URL_LENGTH));
        entity.setScore(dto.score());
        // addedAt is set by @PrePersist — do not set here
        return entity;
    }

    private String truncate(String value, int maxLength) {
        if (value == null) return null;
        return value.length() <= maxLength ? value : value.substring(0, maxLength);
    }

    // --- Result carrier ---

    /** Carries the saved/existing DTO alongside a flag indicating if a new row was created. */
    public record AddResult(FavoriteDto dto, boolean created) {}
}
