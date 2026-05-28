package com.anitracker.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.NOT_FOUND)
public class FavoriteNotFoundException extends RuntimeException {

    public FavoriteNotFoundException(int malId) {
        super("Favorite with malId " + malId + " not found");
    }
}
