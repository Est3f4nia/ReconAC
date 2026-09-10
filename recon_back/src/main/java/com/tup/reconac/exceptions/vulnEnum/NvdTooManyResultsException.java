package com.tup.reconac.exceptions.vulnEnum;

import com.tup.reconac.exceptions.CustomException;
import org.springframework.http.HttpStatus;

import java.net.URI;

public class NvdTooManyResultsException extends CustomException {
    public NvdTooManyResultsException(String message) {
        super(message, HttpStatus.TOO_MANY_REQUESTS, null, URI.create("/errors/too_many_requests"), null);
    }
}
