package com.tup.reconac.modules.vulnEnum.clients;

public class NvdLookupException extends RuntimeException {
    public NvdLookupException(String message) {
        super(message);
    }
    public NvdLookupException(String message, Throwable cause) {
        super(message, cause);
    }
}
