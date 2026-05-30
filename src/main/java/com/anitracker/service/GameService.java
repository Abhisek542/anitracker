package com.anitracker.service;

import com.anitracker.model.AnimeDto;
import com.anitracker.model.GameDto;
import com.anitracker.model.GameRoundDto;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class GameService {

    private static final int ROUNDS = 10;
    private static final int CHOICES_PER_ROUND = 4;
    private static final int MIN_POOL_SIZE = CHOICES_PER_ROUND;

    private final JikanService jikanService;

    public GameService(JikanService jikanService) {
        this.jikanService = jikanService;
    }

    public GameDto generateGame() {
        List<AnimeDto> raw = jikanService.getTrendingAnime();

        // Filter nulls and deduplicate by malId
        Map<Integer, AnimeDto> seen = new LinkedHashMap<>();
        for (AnimeDto a : raw) {
            if (a.imageUrl() != null && !seen.containsKey(a.malId())) {
                seen.put(a.malId(), a);
            }
        }
        List<AnimeDto> pool = new ArrayList<>(seen.values());

        if (pool.size() < MIN_POOL_SIZE) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Insufficient anime data");
        }

        Collections.shuffle(pool);

        int roundCount = Math.min(ROUNDS, pool.size() - (CHOICES_PER_ROUND - 1));
        List<GameRoundDto> rounds = new ArrayList<>(roundCount);

        for (int i = 0; i < roundCount; i++) {
            AnimeDto answer = pool.get(i);

            List<AnimeDto> decoyPool = new ArrayList<>(pool);
            decoyPool.remove(i);
            Collections.shuffle(decoyPool);

            List<String> choices = new ArrayList<>();
            choices.add(answer.title());
            for (int d = 0; d < CHOICES_PER_ROUND - 1; d++) {
                choices.add(decoyPool.get(d).title());
            }

            Collections.shuffle(choices);
            int answerIndex = choices.indexOf(answer.title());

            rounds.add(new GameRoundDto(i + 1, answer.imageUrl(), choices, answerIndex));
        }

        return new GameDto(rounds);
    }
}
