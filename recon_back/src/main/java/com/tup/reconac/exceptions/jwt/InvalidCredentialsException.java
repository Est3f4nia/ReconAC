package com.tup.reconac.exceptions.jwt;

import com.tup.reconac.exceptions.CustomException;
import org.springframework.http.HttpStatus;

import java.net.URI;

public class InvalidCredentialsException extends CustomException {
    public InvalidCredentialsException(String message) {
        super(message, HttpStatus.UNAUTHORIZED, null, URI.create("/errors/unauthorized"), null);
    }
}
