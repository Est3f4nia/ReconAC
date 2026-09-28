package com.tup.reconac.exceptions.escaneo;

import com.tup.reconac.exceptions.CustomException;
import org.springframework.http.HttpStatus;

import java.net.URI;
import java.util.List;

public class EscaneoNotFoundException  extends CustomException {
    public EscaneoNotFoundException (String message) {
        super(message, HttpStatus.NOT_FOUND, null, URI.create("/errors/escaneo-not-found"), null);
    }

    public EscaneoNotFoundException (String message, List<String> errors) {
        super(message, HttpStatus.NOT_FOUND, errors, URI.create("/errors/escaneo-not-found"), null);
    }
}
