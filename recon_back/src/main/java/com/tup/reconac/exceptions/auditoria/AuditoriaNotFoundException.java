package com.tup.reconac.exceptions.auditoria;

import com.tup.reconac.exceptions.CustomException;
import org.springframework.http.HttpStatus;

import java.net.URI;
import java.util.List;

public class AuditoriaNotFoundException extends CustomException {
    public AuditoriaNotFoundException(String message) {
        super(message, HttpStatus.NOT_FOUND, null, URI.create("/errors/auditoria-not-found"), null);
    }

    public AuditoriaNotFoundException(String message, List<String> errors) {
        super(message, HttpStatus.NOT_FOUND, errors, URI.create("/errors/auditoria-not-found"), null);
    }
}

/**
 * Qué va en el último parámetro? RuntimeException instance?
 */