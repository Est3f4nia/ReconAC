package com.tup.reconac.exceptions.jwt;

import com.tup.reconac.exceptions.CustomException;
import org.springframework.http.HttpStatus;

import java.net.URI;
import java.util.List;

public class UnauthorizedException extends CustomException {
    public UnauthorizedException(String message) {
        super(message, HttpStatus.UNAUTHORIZED, null, URI.create("/errors/unauthorized"), null);
    }

    public UnauthorizedException(String message, List<String> errors) {
        super(message, HttpStatus.UNAUTHORIZED, errors, URI.create("/errors/unauthorized"), null);
    }
}
