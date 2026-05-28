package com.anitracker.controller;

import com.anitracker.model.FavoriteDto;
import com.anitracker.service.FavoriteService;
import com.anitracker.service.FavoriteService.AddResult;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/favorites")
public class FavoriteController {

    private final FavoriteService favoriteService;

    public FavoriteController(FavoriteService favoriteService) {
        this.favoriteService = favoriteService;
    }

    /** GET /api/favorites — list all favorites, most-recently-added first. */
    @GetMapping
    public List<FavoriteDto> listFavorites() {
        return favoriteService.getFavorites();
    }

    /**
     * POST /api/favorites — add a favorite.
     * Returns 201 Created for a new entry, 200 OK if the malId was already saved (idempotent).
     */
    @PostMapping
    public ResponseEntity<FavoriteDto> addFavorite(@Valid @RequestBody FavoriteDto request) {
        AddResult result = favoriteService.addFavorite(request);
        HttpStatus status = result.created() ? HttpStatus.CREATED : HttpStatus.OK;
        return ResponseEntity.status(status).body(result.dto());
    }

    /**
     * DELETE /api/favorites/{malId} — remove a favorite.
     * Returns 204 No Content on success, 404 Not Found if malId does not exist.
     */
    @DeleteMapping("/{malId}")
    public ResponseEntity<Void> removeFavorite(@PathVariable int malId) {
        favoriteService.removeFavorite(malId);
        return ResponseEntity.noContent().build();
    }

    /**
     * GET /api/favorites/{malId} — check if an anime is favorited.
     * Always returns 200 OK; body contains {"favorited": true/false}.
     */
    @GetMapping("/{malId}")
    public Map<String, Boolean> checkFavorited(@PathVariable int malId) {
        return Map.of("favorited", favoriteService.isFavorited(malId));
    }
}
