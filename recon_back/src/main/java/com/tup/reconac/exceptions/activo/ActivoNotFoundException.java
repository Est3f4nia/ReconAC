package com.tup.reconac.exceptions.activo;

import com.tup.reconac.exceptions.CustomException;
import org.springframework.http.HttpStatus;

import java.net.URI;
import java.util.List;

public class ActivoNotFoundException extends CustomException {
    public ActivoNotFoundException(String message) {
        super(message, HttpStatus.NOT_FOUND, null, URI.create("/errors/activo-not-found"), null);
    }

    public ActivoNotFoundException(String message, List<String> errors) {
        super(message, HttpStatus.NOT_FOUND, errors, URI.create("/errors/activo-not-found"), null);
    }
}
