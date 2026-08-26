package com.tup.reconac.exceptions.usuario;

import com.tup.reconac.exceptions.CustomException;
import org.springframework.http.HttpStatus;

import java.net.URI;

public class UserNotFoundException extends CustomException {
    public UserNotFoundException(String message) {
        super(message, HttpStatus.NOT_FOUND, null, URI.create("/errors/not-found"), null);
    }
}
