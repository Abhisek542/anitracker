package com.anitracker.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.NOT_FOUND)
public class AnimeNotFoundException extends RuntimeException {

    public AnimeNotFoundException(int id) {
        super("Anime with id " + id + " not found");
    }
}
