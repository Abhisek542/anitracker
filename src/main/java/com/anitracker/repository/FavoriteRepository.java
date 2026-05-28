package com.anitracker.repository;

import com.anitracker.model.Favorite;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface FavoriteRepository extends JpaRepository<Favorite, Long> {

    Optional<Favorite> findByMalId(int malId);

    boolean existsByMalId(int malId);

    List<Favorite> findAllByOrderByAddedAtDesc();
}
