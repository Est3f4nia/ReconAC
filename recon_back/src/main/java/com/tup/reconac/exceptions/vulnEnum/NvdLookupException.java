package com.tup.reconac.exceptions.vulnEnum;

public class NvdLookupException extends RuntimeException {
    public NvdLookupException(String message) {
        super(message);
    }
    public NvdLookupException(String message, Throwable cause) {
        super(message, cause);
    }
}
