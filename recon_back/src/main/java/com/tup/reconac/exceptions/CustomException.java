package com.tup.reconac.exceptions;

import lombok.Getter;
import org.springframework.http.HttpStatus;

import java.net.URI;
import java.util.List;

@Getter
public abstract class CustomException extends RuntimeException {

    private final HttpStatus status;
    private final List<String> errors;
    private final URI type;
    private final String instance;

    public CustomException(String message, HttpStatus status, List<String> errors, URI type, String instance) {
        super(message);
        this.status = status;
        this.errors = (errors == null || errors.isEmpty()) ? List.of(message) : List.copyOf(errors);
        this.type = (type != null) ? type : URI.create("about:blank");
        this.instance = instance;
    }
}

