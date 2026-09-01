package com.tup.reconac.exceptions.usuario;

import com.tup.reconac.exceptions.CustomException;
import org.springframework.http.HttpStatus;

import java.net.URI;

public class KeyNotValidException extends CustomException {
    public KeyNotValidException(String message) {
        super(message, HttpStatus.CONFLICT, null, URI.create("/errors/conflict"), null);
    }
}
